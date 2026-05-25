import { mkdir, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';

const TARGET_COUNT = Number(process.argv[2] ?? 1000);
const OUTPUT = resolve('src/main/resources/seed/kinohub-catalog.json');
const BASE_URL = 'https://kinohub.org';

const sections = [
  { slug: 'drama', genre: 'Драма' },
  { slug: 'komediya', genre: 'Комедия' },
  { slug: 'triller', genre: 'Триллер' },
  { slug: 'boevik', genre: 'Боевик' },
  { slug: 'uzhasy', genre: 'Ужасы' },
  { slug: 'kriminal', genre: 'Криминал' },
  { slug: 'melodrama', genre: 'Мелодрама' },
  { slug: 'detektiv', genre: 'Детектив' },
  { slug: 'fantastika', genre: 'Фантастика' },
  { slug: 'fehntezi', genre: 'Фэнтези' },
  { slug: 'priklyucheniya', genre: 'Приключения' },
  { slug: 'semejnye', genre: 'Семейный' },
  { slug: 'multfilm', genre: 'Анимация' },
  { slug: 'dokumentalnyi', genre: 'Документальный' },
  { slug: 'biografiya', genre: 'Биография' },
  { slug: 'istoricheskii', genre: 'История' },
  { slug: 'voennyy', genre: 'Военный' },
  { slug: 'film', genre: 'Фильм' },
  { slug: 'serialy', genre: 'Сериал' },
];

const genrePools = [
  ['Драма', 'Триллер', 'Криминал'],
  ['Комедия', 'Мелодрама', 'Семейный'],
  ['Боевик', 'Приключения', 'Триллер'],
  ['Фантастика', 'Детектив', 'Драма'],
  ['Ужасы', 'Триллер', 'Детектив'],
  ['Документальный', 'История', 'Познавательный'],
  ['Анимация', 'Приключения', 'Семейный'],
  ['Фэнтези', 'Приключения', 'Драма'],
];

const moods = ['напряженно', 'легко', 'динамично', 'мрачно', 'загадочно', 'созерцательно', 'романтично'];

function stripTags(value) {
  return value
    .replace(/<[^>]*>/g, '')
    .replace(/&quot;/g, '"')
    .replace(/&amp;/g, '&')
    .replace(/&#039;/g, "'")
    .trim();
}

function normalizeImage(src) {
  return src.startsWith('http') ? src : `${BASE_URL}${src}`;
}

function hash(value) {
  let result = 0;
  for (const char of value) {
    result = (result * 31 + char.charCodeAt(0)) >>> 0;
  }
  return result;
}

function parseTitle(rawTitle) {
  const clean = stripTags(rawTitle);
  const yearMatch = clean.match(/\((\d{4})\)$/);
  const seasonMatch = clean.match(/\((\d+)\s+сезон\)$/i);
  const year = yearMatch ? Number(yearMatch[1]) : 2022;
  const title = clean
    .replace(/\s*\(\d{4}\)$/, '')
    .replace(/\s*\(\d+\s+сезон\)$/i, '')
    .trim();

  return {
    title,
    year,
    isSeries: Boolean(seasonMatch),
  };
}

function parseCards(html, sectionGenre) {
  const pattern =
    /<a class="movie-item__link" href="(?<href>[^"]+)">\s*<div class="movie-item__img[^>]*>\s*<img data-src="(?<img>[^"]+)" alt="(?<alt>[^"]+)">[\s\S]*?<div class="movie-item__title ws-nowrap">(?<title>[^<]+)<\/div>[\s\S]*?<div class="movie-item__meta ws-nowrap">\s*<span>(?<type>[^<]+)<\/span>/g;
  return Array.from(html.matchAll(pattern)).map((match) => {
    const parsed = parseTitle(match.groups.title || match.groups.alt);
    const typeText = stripTags(match.groups.type);
    const contentType = parsed.isSeries || typeText.toLowerCase().includes('сериал') ? 'SERIES' : 'MOVIE';
    const itemHash = hash(`${parsed.title}-${parsed.year}`);
    const genres = Array.from(new Set([sectionGenre, ...genrePools[itemHash % genrePools.length]]))
      .filter((genre) => !['Фильм', 'Сериал'].includes(genre))
      .slice(0, 4);
    const mood = moods[itemHash % moods.length];

    return {
      title: parsed.title,
      originalTitle: parsed.title,
      contentType,
      releaseYear: parsed.year,
      duration: contentType === 'SERIES' ? '1 сезон' : '1 ч 40 мин',
      description: `${parsed.title} — запись каталога KinoHub для демонстрации учета просмотров, оценок и рекомендаций.`,
      director: 'KinoHub',
      mood,
      posterUrl: normalizeImage(match.groups.img),
      sourceUrl: match.groups.href,
      genres,
    };
  });
}

async function fetchPage(section, page) {
  const url = page === 1 ? `${BASE_URL}/${section.slug}/` : `${BASE_URL}/${section.slug}/page/${page}/`;
  const response = await fetch(url, {
    headers: {
      'User-Agent': 'Mozilla/5.0 movie-tracker-seed',
    },
  });

  if (!response.ok) {
    throw new Error(`${url}: ${response.status}`);
  }

  return response.text();
}

const byKey = new Map();

for (let page = 1; byKey.size < TARGET_COUNT && page <= 12; page += 1) {
  for (const section of sections) {
    if (byKey.size >= TARGET_COUNT) {
      break;
    }

    let html;
    try {
      html = await fetchPage(section, page);
    } catch (error) {
      console.warn(`skip ${section.slug}/page/${page}: ${error.message}`);
      continue;
    }

    const cards = parseCards(html, section.genre);

    for (const card of cards) {
      const key = `${card.title}|${card.releaseYear}`;
      const existing = byKey.get(key);
      if (existing) {
        existing.genres = Array.from(new Set([...existing.genres, ...card.genres])).slice(0, 5);
      } else {
        byKey.set(key, card);
      }
      if (byKey.size >= TARGET_COUNT) {
        break;
      }
    }

    console.log(`${section.slug}/page/${page}: ${byKey.size}/${TARGET_COUNT}`);
  }
}

const items = Array.from(byKey.values()).slice(0, TARGET_COUNT);
await mkdir(dirname(OUTPUT), { recursive: true });
await writeFile(OUTPUT, `${JSON.stringify(items, null, 2)}\n`, 'utf8');

console.log(`saved ${items.length} items to ${OUTPUT}`);
