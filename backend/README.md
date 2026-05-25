# Movie Tracker Backend

Spring Boot backend для курсового проекта: каталог фильмов/сериалов, пользователи, личный список, оценки и рекомендации.

## MySQL

Создай базу и пользователя:

```sql
CREATE DATABASE IF NOT EXISTS movie_tracker
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'movie_user'@'localhost' IDENTIFIED BY 'movie_pass';
GRANT ALL PRIVILEGES ON movie_tracker.* TO 'movie_user'@'localhost';
FLUSH PRIVILEGES;
```

Этот же скрипт лежит в `sql/create-database.sql`.

## Запуск

Если Java не прописана в PATH, можно временно использовать JBR от IntelliJ:

```powershell
cd D:\курсач\backend
$env:JAVA_HOME="C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\jbr"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
$env:DB_USERNAME="movie_user"
$env:DB_PASSWORD="movie_pass"
.\mvnw.cmd spring-boot:run
```

Backend запустится на:

```text
http://localhost:8080
```

По умолчанию приложение также умеет подключаться как `root` без пароля, если переменные `DB_USERNAME` и `DB_PASSWORD` не заданы.

## Демо-аккаунты

При первом запуске в MySQL автоматически создаются:

```text
student / 123456
admin / admin123
```

## Основные API

```text
POST   /api/auth/register
POST   /api/auth/login
GET    /api/catalog
GET    /api/catalog/{id}
POST   /api/catalog
PUT    /api/catalog/{id}
DELETE /api/catalog/{id}
GET    /api/users/{userId}/watchlist
POST   /api/users/{userId}/watchlist
PATCH  /api/users/{userId}/watchlist/{recordId}
DELETE /api/users/{userId}/watchlist/{recordId}
GET    /api/users/{userId}/recommendations
```

Примеры запросов лежат в `http/requests.http`.
