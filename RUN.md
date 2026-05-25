# Быстрый запуск

## Запустить весь проект

Дважды нажать:

```text
start-all.bat
```

Скрипт запустит MySQL, backend и frontend в отдельных окнах.

После запуска открыть:

```text
http://localhost:5173
```

## Остановить проект

Дважды нажать:

```text
stop-all.bat
```

Если Windows спросит права администратора для MySQL, разрешить.

## Отдельный запуск

Backend:

```text
start-backend.bat
```

Frontend:

```text
start-frontend.bat
```

## Аккаунты

```text
student / 123456
admin / admin123
```

## Пароль MySQL

По умолчанию для XAMPP используется:

```text
root без пароля
```

Если используешь обычный MySQL Installer и пароль другой, изменить `DefaultDbPassword` в `scripts/common.ps1`.
