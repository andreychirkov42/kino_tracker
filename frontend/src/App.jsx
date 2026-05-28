import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  BadgeCheck,
  Bookmark,
  CalendarDays,
  Check,
  Clapperboard,
  Clock3,
  Download,
  ExternalLink,
  Film,
  Heart,
  LayoutGrid,
  ListFilter,
  LogOut,
  PenLine,
  Play,
  Plus,
  Search,
  Shield,
  SlidersHorizontal,
  Sparkles,
  Star,
  Trash2,
  Tv,
  User,
  X,
} from 'lucide-react';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
const STORAGE_SESSION = 'movie-tracker-api-session-v1';

const emptyAdminForm = {
  title: '',
  original: '',
  type: 'Фильм',
  year: new Date().getFullYear(),
  duration: '1 ч 30 мин',
  genres: 'Драма',
  mood: 'новое',
  director: '',
  posterUrl: '',
  sourceUrl: 'https://kinohub.org/',
  description: '',
};

const statusLabels = {
  all: 'Все',
  new: 'Каталог',
  planned: 'В планах',
  watched: 'Просмотрено',
  favorite: 'Избранное',
};

const viewLabels = {
  catalog: 'Каталог',
  list: 'Личный список',
  recommendations: 'Рекомендации',
  admin: 'Админка',
};

async function apiRequest(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
    ...options,
  });

  if (response.status === 204) {
    return null;
  }

  const text = await response.text();
  const data = text ? JSON.parse(text) : null;

  if (!response.ok) {
    throw new Error(data?.message || `Ошибка запроса: ${response.status}`);
  }

  return data;
}

function readSession() {
  try {
    const value = localStorage.getItem(STORAGE_SESSION);
    return value ? JSON.parse(value) : null;
  } catch {
    return null;
  }
}

function toUiType(type) {
  return type === 'SERIES' ? 'Сериал' : 'Фильм';
}

function toApiType(type) {
  return type === 'Сериал' ? 'SERIES' : 'MOVIE';
}

function toUiStatus(status) {
  if (status === 'WATCHED') return 'watched';
  if (status === 'FAVORITE') return 'favorite';
  if (status === 'PLANNED') return 'planned';
  return 'new';
}

function toApiStatus(status) {
  if (status === 'watched') return 'WATCHED';
  if (status === 'favorite') return 'FAVORITE';
  return 'PLANNED';
}

function mapContent(content, record) {
  return {
    id: content.id,
    watchRecordId: record?.id ?? null,
    title: content.title,
    original: content.originalTitle || content.title,
    type: toUiType(content.contentType),
    contentType: content.contentType,
    year: content.releaseYear,
    duration: content.duration || 'не указано',
    genres: content.genres || [],
    mood: content.mood || 'обычно',
    director: content.director || 'Не указан',
    userRating: record?.rating ?? null,
    rating: record?.rating ?? Math.round(content.averageRating || 0),
    status: record ? toUiStatus(record.status) : 'new',
    posterUrl: content.posterUrl,
    sourceUrl: content.sourceUrl || 'https://kinohub.org/',
    description: content.description,
    averageRating: content.averageRating || 0,
  };
}

function mapRecommendation(recommendation, recordMap) {
  const item = mapContent(recommendation.mediaContent, recordMap.get(recommendation.mediaContent.id));
  return {
    ...item,
    recommendationId: recommendation.id,
    score: recommendation.score,
    reason: recommendation.reason,
  };
}

function App() {
  const [session, setSession] = useState(readSession);
  const [authMode, setAuthMode] = useState('login');
  const [authForm, setAuthForm] = useState({ username: '', email: '', password: '' });
  const [authError, setAuthError] = useState('');
  const [activeView, setActiveView] = useState('catalog');
  const [titles, setTitles] = useState([]);
  const [recommendations, setRecommendations] = useState([]);
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState('all');
  const [type, setType] = useState('Все');
  const [genre, setGenre] = useState('Все');
  const [year, setYear] = useState('Все');
  const [minRating, setMinRating] = useState('0');
  const [page, setPage] = useState(1);
  const [selected, setSelected] = useState(null);
  const [notice, setNotice] = useState('');
  const [apiError, setApiError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [adminForm, setAdminForm] = useState(emptyAdminForm);
  const [editingId, setEditingId] = useState(null);
  const [adminError, setAdminError] = useState('');

  useEffect(() => {
    if (session) {
      localStorage.setItem(STORAGE_SESSION, JSON.stringify(session));
    } else {
      localStorage.removeItem(STORAGE_SESSION);
    }
  }, [session]);

  const loadData = useCallback(async () => {
    if (!session?.id) return;

    setIsLoading(true);
    setApiError('');
    try {
      const [catalog, watchlist, recommendationList] = await Promise.all([
        apiRequest('/catalog'),
        apiRequest(`/users/${session.id}/watchlist`),
        apiRequest(`/users/${session.id}/recommendations`),
      ]);

      const recordMap = new Map(watchlist.map((record) => [record.mediaContent.id, record]));
      const nextTitles = catalog.map((content) => mapContent(content, recordMap.get(content.id)));
      const nextRecommendations = recommendationList.map((item) => mapRecommendation(item, recordMap));

      setTitles(nextTitles);
      setRecommendations(nextRecommendations);
      setSelected((current) => {
        if (!current) return nextTitles[0] ?? null;
        return nextTitles.find((item) => item.id === current.id) ?? nextTitles[0] ?? null;
      });
    } catch (error) {
      setApiError(`Backend недоступен или вернул ошибку: ${error.message}`);
    } finally {
      setIsLoading(false);
    }
  }, [session?.id]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const genres = useMemo(
    () => ['Все', ...Array.from(new Set(titles.flatMap((item) => item.genres))).sort()],
    [titles],
  );

  const years = useMemo(
    () => ['Все', ...Array.from(new Set(titles.map((item) => item.year))).sort((a, b) => b - a)],
    [titles],
  );

  const stats = useMemo(() => {
    const list = titles.filter((item) => item.status !== 'new');
    const watched = titles.filter((item) => ['watched', 'favorite'].includes(item.status));
    const rated = watched.filter((item) => item.userRating > 0);
    const average =
      rated.length === 0
        ? 0
        : Math.round((rated.reduce((sum, item) => sum + item.userRating, 0) / rated.length) * 10) / 10;

    return {
      inList: list.length,
      watched: watched.length,
      planned: titles.filter((item) => item.status === 'planned').length,
      average,
    };
  }, [titles]);

  const filteredTitles = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();
    const min = Number(minRating);

    return titles.filter((item) => {
      const sourceMatches =
        activeView === 'list'
          ? item.status !== 'new'
          : activeView === 'recommendations'
            ? recommendations.some((recommended) => recommended.id === item.id)
            : true;
      const queryMatches =
        !normalizedQuery ||
        [item.title, item.original, item.director, item.description].some((value) =>
          value?.toLowerCase().includes(normalizedQuery),
        );
      const statusMatches = status === 'all' || item.status === status;
      const typeMatches = type === 'Все' || item.type === type;
      const genreMatches = genre === 'Все' || item.genres.includes(genre);
      const yearMatches = year === 'Все' || item.year === Number(year);
      const ratingMatches = min === 0 || item.rating >= min;

      return (
        sourceMatches &&
        queryMatches &&
        statusMatches &&
        typeMatches &&
        genreMatches &&
        yearMatches &&
        ratingMatches
      );
    });
  }, [activeView, genre, minRating, query, recommendations, status, titles, type, year]);

  useEffect(() => {
    setPage(1);
  }, [activeView, genre, minRating, query, status, type, year]);

  const pageSize = 24;
  const totalPages = Math.max(1, Math.ceil(filteredTitles.length / pageSize));
  const paginatedTitles = useMemo(() => {
    const start = (page - 1) * pageSize;
    return filteredTitles.slice(start, start + pageSize);
  }, [filteredTitles, page]);

  async function handleAuth(event) {
    event.preventDefault();
    setAuthError('');

    const username = authForm.username.trim();
    const email = authForm.email.trim();
    const password = authForm.password;

    if (username.length < 3 || password.length < 6) {
      setAuthError('Логин должен быть от 3 символов, пароль - от 6 символов.');
      return;
    }

    if (authMode === 'register' && !/^\S+@\S+\.\S+$/.test(email)) {
      setAuthError('Введите корректную электронную почту.');
      return;
    }

    try {
      const user = await apiRequest(authMode === 'login' ? '/auth/login' : '/auth/register', {
        method: 'POST',
        body: JSON.stringify(
          authMode === 'login' ? { username, password } : { username, email, password },
        ),
      });
      setSession(user);
      setAuthForm({ username: '', email: '', password: '' });
    } catch (error) {
      setAuthError(error.message);
    }
  }

  async function loginAs(username, password) {
    setAuthError('');
    try {
      const user = await apiRequest('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username, password }),
      });
      setSession(user);
    } catch (error) {
      setAuthError(error.message);
    }
  }

  async function addToList(item, nextStatus = 'planned') {
    if (item.status !== 'new') {
      setNotice('Эта запись уже есть в личном списке.');
      return;
    }

    try {
      await apiRequest(`/users/${session.id}/watchlist`, {
        method: 'POST',
        body: JSON.stringify({
          mediaContentId: item.id,
          status: toApiStatus(nextStatus),
          rating: null,
        }),
      });
      setNotice('Контент добавлен в личный список.');
      await loadData();
    } catch (error) {
      setNotice(error.message);
    }
  }

  async function updateTitle(id, patch) {
    const item = titles.find((title) => title.id === id);
    if (!item) return;

    try {
      if (patch.status === 'new' && item.watchRecordId) {
        await apiRequest(`/users/${session.id}/watchlist/${item.watchRecordId}`, { method: 'DELETE' });
        setNotice('Запись удалена из личного списка.');
        await loadData();
        return;
      }

      const nextStatus = patch.status ?? item.status;
      const nextRating =
        patch.rating !== undefined
          ? patch.rating
          : item.userRating ?? null;

      if (item.watchRecordId) {
        await apiRequest(`/users/${session.id}/watchlist/${item.watchRecordId}`, {
          method: 'PATCH',
          body: JSON.stringify({
            status: toApiStatus(nextStatus),
            rating: nextRating,
          }),
        });
      } else {
        await apiRequest(`/users/${session.id}/watchlist`, {
          method: 'POST',
          body: JSON.stringify({
            mediaContentId: item.id,
            status: toApiStatus(nextStatus === 'new' ? 'planned' : nextStatus),
            rating: nextRating,
          }),
        });
      }

      setNotice('Изменения сохранены.');
      await loadData();
    } catch (error) {
      setNotice(error.message);
    }
  }

  async function handleAdminSubmit(event) {
    event.preventDefault();
    setAdminError('');

    const payload = {
      title: adminForm.title.trim(),
      originalTitle: adminForm.original.trim() || adminForm.title.trim(),
      contentType: toApiType(adminForm.type),
      releaseYear: Number(adminForm.year),
      duration: adminForm.duration.trim(),
      description: adminForm.description.trim(),
      director: adminForm.director.trim() || 'Не указан',
      mood: adminForm.mood.trim() || 'новое',
      posterUrl: adminForm.posterUrl.trim(),
      sourceUrl: adminForm.sourceUrl.trim() || 'https://kinohub.org/',
      genres: adminForm.genres
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean),
    };

    if (!payload.title || !payload.description) {
      setAdminError('Заполните название и описание.');
      return;
    }
    if (!payload.posterUrl.startsWith('https://')) {
      setAdminError('Ссылка на постер должна начинаться с https://');
      return;
    }
    if (!Number.isInteger(payload.releaseYear) || payload.releaseYear < 1900) {
      setAdminError('Год выпуска указан неверно.');
      return;
    }
    if (payload.genres.length === 0) {
      setAdminError('Укажите хотя бы один жанр.');
      return;
    }

    try {
      await apiRequest(editingId ? `/catalog/${editingId}` : '/catalog', {
        method: editingId ? 'PUT' : 'POST',
        body: JSON.stringify(payload),
      });
      setNotice(editingId ? 'Запись каталога обновлена.' : 'Новая запись добавлена в каталог.');
      setEditingId(null);
      setAdminForm(emptyAdminForm);
      await loadData();
    } catch (error) {
      setAdminError(error.message);
    }
  }

  function startEditing(item) {
    setEditingId(item.id);
    setAdminForm({
      title: item.title,
      original: item.original,
      type: item.type,
      year: item.year,
      duration: item.duration,
      genres: item.genres.join(', '),
      mood: item.mood,
      director: item.director,
      posterUrl: item.posterUrl,
      sourceUrl: item.sourceUrl,
      description: item.description,
    });
    setActiveView('admin');
  }

  async function deleteTitle(id) {
    try {
      await apiRequest(`/catalog/${id}`, { method: 'DELETE' });
      setNotice('Запись удалена из каталога.');
      await loadData();
    } catch (error) {
      setNotice(error.message);
    }
  }

  async function syncCatalogFromTmdb() {
    setNotice('Синхронизация с TMDB... это может занять до минуты.');
    setApiError('');
    try {
      const result = await apiRequest('/catalog/sync', { method: 'POST' });
      setNotice(
        `TMDB: добавлено ${result.created}, пропущено ${result.skipped}, ошибок ${result.failed}.`,
      );
      await loadData();
    } catch (error) {
      setApiError(`Не удалось синхронизировать с TMDB: ${error.message}`);
    }
  }

  if (!session) {
    return (
      <main className="auth-page">
        <section className="auth-panel">
          <div className="brand large">
            <div className="brand-mark">
              <Clapperboard size={28} />
            </div>
            <div>
              <strong>КиноТрекер</strong>
              <span>учет просмотров и рекомендации</span>
            </div>
          </div>

          <div className="auth-tabs">
            <button className={authMode === 'login' ? 'active' : ''} onClick={() => setAuthMode('login')}>
              Вход
            </button>
            <button
              className={authMode === 'register' ? 'active' : ''}
              onClick={() => setAuthMode('register')}
            >
              Регистрация
            </button>
          </div>

          <form className="auth-form" onSubmit={handleAuth}>
            <label>
              Логин
              <input
                value={authForm.username}
                onChange={(event) => setAuthForm((form) => ({ ...form, username: event.target.value }))}
                placeholder="student"
              />
            </label>
            {authMode === 'register' && (
              <label>
                Email
                <input
                  value={authForm.email}
                  onChange={(event) => setAuthForm((form) => ({ ...form, email: event.target.value }))}
                  placeholder="student@mail.ru"
                />
              </label>
            )}
            <label>
              Пароль
              <input
                type="password"
                value={authForm.password}
                onChange={(event) => setAuthForm((form) => ({ ...form, password: event.target.value }))}
                placeholder="123456"
              />
            </label>
            {authError && <p className="form-error">{authError}</p>}
            <button className="primary-action full" type="submit">
              {authMode === 'login' ? 'Войти' : 'Создать аккаунт'}
            </button>
          </form>

          <div className="demo-login">
            <button type="button" onClick={() => loginAs('student', '123456')}>
              <User size={16} />
              student
            </button>
            <button type="button" onClick={() => loginAs('admin', 'admin123')}>
              <Shield size={16} />
              admin
            </button>
          </div>
        </section>
      </main>
    );
  }

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">
            <Clapperboard size={24} />
          </div>
          <div>
            <strong>КиноТрекер</strong>
            <span>{session.role === 'ADMIN' ? 'администратор' : 'пользователь'}</span>
          </div>
        </div>

        <nav className="nav-list" aria-label="Основные разделы">
          <button className={`nav-item ${activeView === 'catalog' ? 'active' : ''}`} onClick={() => setActiveView('catalog')}>
            <LayoutGrid size={18} />
            Каталог
          </button>
          <button className={`nav-item ${activeView === 'list' ? 'active' : ''}`} onClick={() => setActiveView('list')}>
            <Bookmark size={18} />
            Личный список
          </button>
          <button
            className={`nav-item ${activeView === 'recommendations' ? 'active' : ''}`}
            onClick={() => setActiveView('recommendations')}
          >
            <Sparkles size={18} />
            Рекомендации
          </button>
          {session.role === 'ADMIN' && (
            <button className={`nav-item ${activeView === 'admin' ? 'active' : ''}`} onClick={() => setActiveView('admin')}>
              <Shield size={18} />
              Админка
            </button>
          )}
        </nav>

        <section className="mini-panel" id="stats">
          <span className="panel-kicker">Сводка</span>
          <div className="metric-row">
            <Film size={18} />
            <span>В списке</span>
            <strong>{stats.inList}</strong>
          </div>
          <div className="metric-row">
            <BadgeCheck size={18} />
            <span>Просмотрено</span>
            <strong>{stats.watched}</strong>
          </div>
          <div className="metric-row">
            <Star size={18} />
            <span>Средняя оценка</span>
            <strong>{stats.average || '—'}</strong>
          </div>
        </section>

        <button
          className="logout-button"
          type="button"
          onClick={() => {
            setSession(null);
            setSelected(null);
            setTitles([]);
            setRecommendations([]);
          }}
        >
          <LogOut size={18} />
          Выйти
        </button>
      </aside>

      <section className="workspace">
        <header className="topbar">
          <div>
            <span className="eyebrow">{viewLabels[activeView]}</span>
            <h1>Учет фильмов и сериалов</h1>
          </div>
          <button
            className="primary-action"
            type="button"
            onClick={() => setActiveView(session.role === 'ADMIN' ? 'admin' : 'list')}
          >
            <Plus size={18} />
            {session.role === 'ADMIN' ? 'Добавить' : 'Мой список'}
          </button>
        </header>

        {(notice || apiError || isLoading) && (
          <div className="notice">
            <Check size={18} />
            <span>{isLoading ? 'Загрузка данных из MySQL через Spring API...' : apiError || notice}</span>
            <button
              type="button"
              onClick={() => {
                setNotice('');
                setApiError('');
              }}
            >
              <X size={16} />
            </button>
          </div>
        )}

        {activeView !== 'admin' && (
          <>
            <Hero recommendations={recommendations} />
            <section className="content-grid">
              <section className="library-panel" id="library">
                <Filters
                  genre={genre}
                  genres={genres}
                  minRating={minRating}
                  query={query}
                  setGenre={setGenre}
                  setMinRating={setMinRating}
                  setQuery={setQuery}
                  setStatus={setStatus}
                  setType={setType}
                  setYear={setYear}
                  status={status}
                  type={type}
                  year={year}
                  years={years}
                  showStatus={activeView !== 'catalog'}
                />

                {activeView === 'recommendations' && recommendations.length === 0 ? (
                  <EmptyState />
                ) : (
                  <div className="cards-grid">
                    {paginatedTitles.map((item) => (
                      <TitleCard
                        item={item}
                        key={item.id}
                        onAdd={addToList}
                        onEdit={startEditing}
                        onSelect={setSelected}
                        onUpdate={updateTitle}
                        selected={selected?.id === item.id}
                        userRole={session.role}
                      />
                    ))}
                  </div>
                )}

                {filteredTitles.length > pageSize && (
                  <Pagination
                    onPageChange={setPage}
                    page={page}
                    totalItems={filteredTitles.length}
                    totalPages={totalPages}
                  />
                )}
              </section>

              <Details
                item={selected}
                onAdd={addToList}
                onClose={() => setSelected(null)}
                onEdit={startEditing}
                onUpdate={updateTitle}
                userRole={session.role}
              />
            </section>
          </>
        )}

        {activeView === 'admin' && session.role === 'ADMIN' && (
          <AdminPanel
            adminError={adminError}
            editingId={editingId}
            form={adminForm}
            onCancel={() => {
              setEditingId(null);
              setAdminForm(emptyAdminForm);
              setAdminError('');
            }}
            onDelete={deleteTitle}
            onEdit={startEditing}
            onSubmit={handleAdminSubmit}
            onSync={syncCatalogFromTmdb}
            setForm={setAdminForm}
            titles={titles}
          />
        )}

        <RecommendationPanel recommendations={recommendations} onSelect={setSelected} />
      </section>
    </main>
  );
}

function Hero({ recommendations }) {
  const top = recommendations[0];

  return (
    <section className="hero-strip">
      <div className="hero-copy">
        <span>Алгоритм подбора</span>
        <h2>{top?.title ?? 'Оцените несколько тайтлов'}</h2>
        <p>{top?.reason ?? 'Рекомендации появятся после добавления фильмов или сериалов в личный список.'}</p>
      </div>
      <div className="hero-poster-stack" aria-hidden="true">
        {recommendations.slice(0, 3).map((item) => (
          <div className="poster-tile" key={item.id}>
            <img src={item.posterUrl} alt="" />
            <span>{item.title}</span>
          </div>
        ))}
      </div>
    </section>
  );
}

function Filters(props) {
  const {
    genre,
    genres,
    minRating,
    query,
    setGenre,
    setMinRating,
    setQuery,
    setStatus,
    setType,
    setYear,
    showStatus,
    status,
    type,
    year,
    years,
  } = props;

  return (
    <>
      <div className="toolbar">
        <label className="search-box">
          <Search size={18} />
          <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Название, режиссер" />
        </label>
        <label className="filter-inline">
          <ListFilter size={18} />
          <select value={genre} onChange={(event) => setGenre(event.target.value)}>
            {genres.map((item) => (
              <option key={item}>{item}</option>
            ))}
          </select>
        </label>
        <label className="filter-inline">
          <CalendarDays size={18} />
          <select value={year} onChange={(event) => setYear(event.target.value)}>
            {years.map((item) => (
              <option key={item}>{item}</option>
            ))}
          </select>
        </label>
        <label className="filter-inline">
          <Star size={18} />
          <select value={minRating} onChange={(event) => setMinRating(event.target.value)}>
            <option value="0">Любая</option>
            <option value="6">6+</option>
            <option value="8">8+</option>
            <option value="9">9+</option>
          </select>
        </label>
      </div>

      {showStatus && (
        <div className="segmented" aria-label="Статус">
          {Object.entries(statusLabels).map(([value, label]) => (
            <button className={status === value ? 'active' : ''} key={value} onClick={() => setStatus(value)} type="button">
              {label}
            </button>
          ))}
        </div>
      )}

      <div className="type-switch" aria-label="Тип">
        {['Все', 'Фильм', 'Сериал'].map((item) => (
          <button className={type === item ? 'active' : ''} key={item} onClick={() => setType(item)} type="button">
            {item === 'Фильм' && <Film size={16} />}
            {item === 'Сериал' && <Tv size={16} />}
            {item}
          </button>
        ))}
      </div>
    </>
  );
}

function TitleCard({ item, onAdd, onEdit, onSelect, onUpdate, selected, userRole }) {
  function toggleStatus(event, status) {
    event.stopPropagation();
    if (item.status === status) {
      onUpdate(item.id, { status: 'new' });
    } else if (item.status === 'new') {
      onAdd(item, status);
    } else {
      onUpdate(item.id, { status });
    }
  }

  return (
    <article className={`title-card ${selected ? 'selected' : ''}`} onClick={() => onSelect(item)}>
      <div className="poster">
        <img src={item.posterUrl} alt="" loading="lazy" />
        <span className="poster-year">{item.year}</span>
        <strong>{item.title}</strong>
      </div>
      <div className="card-body">
        <div className="card-heading">
          <div>
            <h3>{item.title}</h3>
            <span>{item.original}</span>
          </div>
          <button
            className={`icon-button ${item.status === 'favorite' ? 'hot' : ''}`}
            type="button"
            aria-label="Избранное"
            onClick={(event) => {
              event.stopPropagation();
              onUpdate(item.id, {
                status: item.status === 'favorite' ? 'watched' : 'favorite',
                rating: item.userRating || 8,
              });
            }}
          >
            <Heart size={18} />
          </button>
        </div>

        <div className="chip-row">
          <span>{item.type}</span>
          <span>{item.genres[0]}</span>
        </div>

        <div className="card-footer">
          <Rating value={item.rating} />
          <div className="status-pills">
            <button
              className={`status-pill planned ${item.status === 'planned' ? 'active' : ''}`}
              onClick={(event) => toggleStatus(event, 'planned')}
              type="button"
              aria-pressed={item.status === 'planned'}
            >
              <Bookmark size={14} />
              В планах
            </button>
            <button
              className={`status-pill watched ${item.status === 'watched' ? 'active' : ''}`}
              onClick={(event) => toggleStatus(event, 'watched')}
              type="button"
              aria-pressed={item.status === 'watched'}
            >
              <Check size={14} />
              Просмотрено
            </button>
          </div>
          {userRole === 'ADMIN' && (
            <button
              className="ghost-action"
              type="button"
              onClick={(event) => {
                event.stopPropagation();
                onEdit(item);
              }}
              aria-label="Редактировать"
            >
              <PenLine size={16} />
            </button>
          )}
        </div>
      </div>
    </article>
  );
}

function Details({ item, onAdd, onClose, onEdit, onUpdate, userRole }) {
  const [pendingRating, setPendingRating] = useState(item?.userRating ?? 0);

  useEffect(() => {
    setPendingRating(item?.userRating ?? 0);
  }, [item?.id, item?.userRating]);

  if (!item) {
    return (
      <aside className="details-panel empty-details">
        <Clapperboard size={32} />
        <span>Выберите запись</span>
      </aside>
    );
  }

  const commitRating = () => {
    if (pendingRating === (item.userRating ?? 0)) return;
    onUpdate(item.id, {
      rating: pendingRating,
      status: pendingRating > 0 && item.status === 'new' ? 'watched' : item.status,
    });
  };

  return (
    <aside className="details-panel">
      <div className="detail-cover">
        <img src={item.posterUrl} alt="" />
        <span>{item.type}</span>
        <strong>{item.title}</strong>
      </div>
      <div className="detail-header">
        <div>
          <h2>{item.title}</h2>
          <p>{item.original}</p>
        </div>
        <button className="icon-button" type="button" aria-label="Закрыть детали" onClick={onClose}>
          <X size={18} />
        </button>
      </div>

      <p className="description">{item.description}</p>

      <div className="facts">
        <span>
          <CalendarDays size={16} />
          {item.year}
        </span>
        <span>
          <Clock3 size={16} />
          {item.duration}
        </span>
        <span>
          <Play size={16} />
          {item.director}
        </span>
      </div>

      <div className="genre-list">
        {item.genres.map((itemGenre) => (
          <span key={itemGenre}>{itemGenre}</span>
        ))}
      </div>

      <label className="status-select">
        Статус просмотра
        <select value={item.status} onChange={(event) => onUpdate(item.id, { status: event.target.value })}>
          <option value="new">Только в каталоге</option>
          <option value="planned">В планах</option>
          <option value="watched">Просмотрено</option>
          <option value="favorite">Избранное</option>
        </select>
      </label>

      <div className="rating-editor">
        <div>
          <span className="panel-kicker">Оценка</span>
          <strong>{pendingRating || 'нет'}</strong>
        </div>
        <input
          max="10"
          min="0"
          type="range"
          value={pendingRating}
          onChange={(event) => setPendingRating(Number(event.target.value))}
          onMouseUp={commitRating}
          onTouchEnd={commitRating}
          onKeyUp={commitRating}
        />
      </div>

      <div className="action-row">
        <button className="primary-action full" type="button" onClick={() => onAdd(item, 'planned')}>
          <Bookmark size={18} />
          В список
        </button>
        <a className="ghost-action" href={item.sourceUrl} rel="noreferrer" target="_blank" aria-label="Открыть на KinoHub">
          <ExternalLink size={18} />
        </a>
        {userRole === 'ADMIN' && (
          <button className="ghost-action" type="button" onClick={() => onEdit(item)} aria-label="Редактировать">
            <PenLine size={18} />
          </button>
        )}
      </div>
    </aside>
  );
}

function AdminPanel(props) {
  const { adminError, editingId, form, onCancel, onDelete, onEdit, onSubmit, onSync, setForm, titles } = props;

  return (
    <section className="admin-grid">
      <form className="admin-form" onSubmit={onSubmit}>
        <div className="section-title">
          <div>
            <span className="eyebrow">CatalogService</span>
            <h2>{editingId ? 'Редактирование записи' : 'Новая запись каталога'}</h2>
          </div>
          {editingId && (
            <button className="ghost-action" type="button" onClick={onCancel} aria-label="Отменить">
              <X size={18} />
            </button>
          )}
        </div>

        <div className="form-grid">
          <label>
            Название
            <input value={form.title} onChange={(event) => setForm((value) => ({ ...value, title: event.target.value }))} />
          </label>
          <label>
            Оригинальное название
            <input
              value={form.original}
              onChange={(event) => setForm((value) => ({ ...value, original: event.target.value }))}
            />
          </label>
          <label>
            Тип
            <select value={form.type} onChange={(event) => setForm((value) => ({ ...value, type: event.target.value }))}>
              <option>Фильм</option>
              <option>Сериал</option>
            </select>
          </label>
          <label>
            Год
            <input
              type="number"
              value={form.year}
              onChange={(event) => setForm((value) => ({ ...value, year: event.target.value }))}
            />
          </label>
          <label>
            Длительность
            <input
              value={form.duration}
              onChange={(event) => setForm((value) => ({ ...value, duration: event.target.value }))}
            />
          </label>
          <label>
            Режиссер
            <input
              value={form.director}
              onChange={(event) => setForm((value) => ({ ...value, director: event.target.value }))}
            />
          </label>
          <label className="wide">
            Жанры через запятую
            <input value={form.genres} onChange={(event) => setForm((value) => ({ ...value, genres: event.target.value }))} />
          </label>
          <label className="wide">
            Постер
            <input
              value={form.posterUrl}
              onChange={(event) => setForm((value) => ({ ...value, posterUrl: event.target.value }))}
              placeholder="https://kinohub.org/uploads/posts/..."
            />
          </label>
          <label className="wide">
            Описание
            <textarea
              value={form.description}
              onChange={(event) => setForm((value) => ({ ...value, description: event.target.value }))}
            />
          </label>
        </div>

        {adminError && <p className="form-error">{adminError}</p>}
        <button className="primary-action full" type="submit">
          {editingId ? 'Сохранить изменения' : 'Добавить в каталог'}
        </button>
      </form>

      <section className="admin-list">
        <div className="section-title">
          <div>
            <span className="eyebrow">MediaContent</span>
            <h2>Управление каталогом</h2>
          </div>
          <button className="primary-action" type="button" onClick={onSync}>
            <Download size={18} />
            Синхронизировать с TMDB
          </button>
        </div>
        {titles.map((item) => (
          <article className="admin-row" key={item.id}>
            <img src={item.posterUrl} alt="" />
            <div>
              <h3>{item.title}</h3>
              <span>
                {item.type} • {item.year} • {item.genres.join(', ')}
              </span>
            </div>
            <button className="ghost-action" type="button" onClick={() => onEdit(item)} aria-label="Редактировать">
              <PenLine size={17} />
            </button>
            <button className="ghost-action danger" type="button" onClick={() => onDelete(item.id)} aria-label="Удалить">
              <Trash2 size={17} />
            </button>
          </article>
        ))}
      </section>
    </section>
  );
}

function RecommendationPanel({ recommendations, onSelect }) {
  return (
    <section className="recommendation-panel" id="recommendations">
      <div className="section-title">
        <div>
          <span className="eyebrow">RecommendationService</span>
          <h2>Персональная выдача</h2>
        </div>
        <SlidersHorizontal size={22} />
      </div>

      <div className="recommendation-list">
        {recommendations.map((item, index) => (
          <article className="recommendation-card" key={item.id}>
            <span className="rank">#{index + 1}</span>
            <div>
              <h3>{item.title}</h3>
              <p>{item.reason}</p>
            </div>
            <button className="ghost-action" type="button" onClick={() => onSelect(item)}>
              <Sparkles size={18} />
            </button>
          </article>
        ))}
      </div>
    </section>
  );
}

function Pagination({ onPageChange, page, totalItems, totalPages }) {
  const pages = Array.from({ length: totalPages }, (_, index) => index + 1)
    .filter((item) => item === 1 || item === totalPages || Math.abs(item - page) <= 2);

  return (
    <div className="pagination">
      <span>
        Страница {page} из {totalPages} • {totalItems} записей
      </span>
      <div>
        <button disabled={page === 1} type="button" onClick={() => onPageChange(page - 1)}>
          Назад
        </button>
        {pages.map((item, index) => (
          <React.Fragment key={item}>
            {index > 0 && item - pages[index - 1] > 1 && <span className="page-gap">...</span>}
            <button
              className={item === page ? 'active' : ''}
              type="button"
              onClick={() => onPageChange(item)}
            >
              {item}
            </button>
          </React.Fragment>
        ))}
        <button disabled={page === totalPages} type="button" onClick={() => onPageChange(page + 1)}>
          Вперед
        </button>
      </div>
    </div>
  );
}

function EmptyState() {
  return (
    <div className="empty-state">
      <Sparkles size={28} />
      <h3>Рекомендации пока не найдены</h3>
      <p>Добавьте в личный список несколько записей и поставьте оценки.</p>
    </div>
  );
}

function Rating({ value }) {
  return (
    <div className="rating" title={`Оценка ${value || 'нет'}`}>
      <Star size={15} fill={value ? 'currentColor' : 'none'} />
      <span>{value || '—'}</span>
    </div>
  );
}

export default App;
