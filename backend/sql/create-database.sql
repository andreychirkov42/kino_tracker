CREATE DATABASE IF NOT EXISTS movie_tracker
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'movie_user'@'localhost' IDENTIFIED BY 'movie_pass';
GRANT ALL PRIVILEGES ON movie_tracker.* TO 'movie_user'@'localhost';
FLUSH PRIVILEGES;
