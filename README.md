# MIFIDEV-otp — сервис защиты операций одноразовыми кодами (OTP)

Backend-сервис, который защищает пользовательские операции с помощью временных
одноразовых кодов (OTP). Пользователь инициирует операцию, сервис генерирует
уникальный код с ограниченным временем жизни, рассылает его по выбранному каналу
(SMS, Email, Telegram или файл) и затем проверяет введённый код.

Проект выполнен по ТЗ заказчика **Promo IT** в рамках дисциплины Java.

---

## Содержание

- [Возможности](#возможности)
- [Технологический стек](#технологический-стек)
- [Архитектура](#архитектура)
- [Структура проекта](#структура-проекта)
- [Соответствие критериям приёмки](#соответствие-критериям-приёмки)
- [Быстрый старт](#быстрый-старт)
- [Конфигурация](#конфигурация)
- [Роли и авторизация](#роли-и-авторизация)
- [Справочник API](#справочник-api)
- [Каналы рассылки кодов](#каналы-рассылки-кодов)
- [Планировщик просроченных кодов](#планировщик-просроченных-кодов)
- [Логирование](#логирование)
- [Схема базы данных](#схема-базы-данных)
- [Тестирование](#тестирование)
- [Полный сценарий использования (curl)](#полный-сценарий-использования-curl)

---

## Возможности

- Регистрация и аутентификация пользователей с ролями **ADMIN** и **USER**.
- Гарантия существования **не более одного администратора**.
- **Токенная авторизация** на JWT с ограниченным сроком действия.
- Разграничение доступа: обычные пользователи не имеют доступа к API администратора.
- Генерация OTP-кода, привязанного к операции, и рассылка по **4 каналам**:
  **SMS (SMPP), Email (SMTP), Telegram (Bot API), сохранение в файл**.
- Валидация OTP-кода со статусами **ACTIVE / EXPIRED / USED**.
- Настраиваемые администратором **время жизни** и **длина** кода.
- Фоновый **планировщик**, помечающий просроченные коды статусом EXPIRED.
- Полное **логирование** каждого HTTP-запроса и подробное логирование бизнес-операций.
- Чистая трёхслойная архитектура (**API → Service → DAO**), взаимодействие с БД через **JDBC**.

---

## Технологический стек

| Назначение            | Технология                                              |
|-----------------------|---------------------------------------------------------|
| Язык / сборка         | Java 21, Maven                                          |
| HTTP-сервер           | `com.sun.net.httpserver` (JDK, без фреймворков)         |
| База данных           | PostgreSQL 17, чистый JDBC                              |
| Пул соединений        | HikariCP (выдаёт обычные JDBC-соединения)              |
| Токены                | JWT (JJWT, HMAC-SHA256)                                 |
| Хеширование паролей   | PBKDF2-HMAC-SHA256 с солью (JDK)                        |
| JSON                  | Jackson                                                 |
| Email                 | Jakarta Mail (Angus Mail)                               |
| SMS                   | jSMPP (протокол SMPP)                                    |
| Telegram              | `java.net.http.HttpClient` (JDK)                        |
| Логирование           | SLF4J + Logback                                         |
| Тесты                 | JUnit 5                                                 |

---

## Архитектура

Приложение разделено на три обязательных слоя:

```
┌──────────────────────────────────────────────────────────────┐
│  API-слой  (com.promoit.otp.api)                             │
│  HttpServerBootstrap, AuthHandler, OtpHandler, AdminHandler, │
│  LoggingFilter, AuthSupport                                  │
│  — обработка HTTP-запросов, маршрутизация, авторизация       │
└───────────────────────────────┬──────────────────────────────┘
                                 │
┌───────────────────────────────▼──────────────────────────────┐
│  Service-слой  (com.promoit.otp.service)                     │
│  AuthService, UserService, OtpService, OtpConfigService,     │
│  NotificationDispatcher + каналы рассылки                    │
│  — бизнес-логика                                            │
└───────────────────────────────┬──────────────────────────────┘
                                 │
┌───────────────────────────────▼──────────────────────────────┐
│  DAO-слой  (com.promoit.otp.dao)                             │
│  UserDao, OtpCodeDao, OtpConfigDao  — JDBC-запросы к БД      │
└──────────────────────────────────────────────────────────────┘
```

---

## Структура проекта

```
MIFIDEV-otp/
├── pom.xml                         # Maven-сборка, зависимости, fat-jar (shade)
├── docker-compose.yml              # PostgreSQL 17 + MailHog (SMTP-эмулятор)
├── README.md
└── src/
    ├── main/
    │   ├── java/com/promoit/otp/
    │   │   ├── Application.java                 # точка входа, сборка зависимостей (DI вручную)
    │   │   ├── ApiException.java                # ошибка с HTTP-статусом
    │   │   ├── config/
    │   │   │   ├── AppConfig.java               # загрузка настроек + override через env
    │   │   │   └── DatabaseManager.java         # HikariCP DataSource, инициализация схемы
    │   │   ├── model/                           # доменные модели и enum
    │   │   │   ├── User, OtpCode, OtpConfig
    │   │   │   ├── Role (ADMIN, USER)
    │   │   │   ├── OtpStatus (ACTIVE, EXPIRED, USED)
    │   │   │   └── DeliveryChannel (EMAIL, SMS, TELEGRAM, FILE)
    │   │   ├── dao/                             # DAO-слой (чистый JDBC)
    │   │   │   ├── UserDao, OtpCodeDao, OtpConfigDao
    │   │   │   └── DataAccessException, DuplicateKeyException
    │   │   ├── security/
    │   │   │   ├── PasswordEncoder.java         # PBKDF2 c солью
    │   │   │   ├── TokenService.java            # выпуск/проверка JWT
    │   │   │   └── AuthPrincipal.java           # личность из токена
    │   │   ├── service/                         # Service-слой
    │   │   │   ├── AuthService, UserService
    │   │   │   ├── OtpService, OtpConfigService
    │   │   │   ├── *Result / UserView           # DTO ответов
    │   │   │   └── notification/                # каналы рассылки
    │   │   │       ├── NotificationChannel (интерфейс)
    │   │   │       ├── EmailNotificationService
    │   │   │       ├── SmsNotificationService
    │   │   │       ├── TelegramNotificationService
    │   │   │       ├── FileNotificationService
    │   │   │       └── NotificationDispatcher
    │   │   ├── api/                             # API-слой
    │   │   │   ├── HttpServerBootstrap.java
    │   │   │   ├── handler/  (Base/Auth/Otp/Admin Handler)
    │   │   │   ├── middleware/ (LoggingFilter, AuthSupport)
    │   │   │   └── util/ (JsonUtil, HttpUtil)
    │   │   └── scheduler/
    │   │       └── OtpExpirationScheduler.java  # пометка EXPIRED по таймеру
    │   └── resources/
    │       ├── application.properties           # конфигурация по умолчанию
    │       ├── schema.sql                       # DDL (PostgreSQL 17)
    │       └── logback.xml                       # настройка логов
    └── test/java/com/promoit/otp/
        ├── PasswordEncoderTest.java
        └── TokenServiceTest.java
```

---

## Соответствие критериям приёмки

| # | Критерий (баллы) | Где реализовано |
|---|------------------|-----------------|
| 1 | Структура приложения — 3 слоя (5) | пакеты `api` / `service` / `dao` |
| 2 | Система сборки Maven/Gradle (5) | `pom.xml` (Maven) |
| 3 | Минимальный функционал основных операций (9) | `AuthService`, `OtpService`, `OtpConfigService`, `UserService` |
| 4 | Разграничение по ролям admin/user (5) | `AuthSupport.requireAdmin/requireAuth`, `AdminHandler` |
| 5 | API на `com.sun.net.httpserver` (5) | `HttpServerBootstrap`, `*Handler` |
| 6 | Минимальное логирование каждого запроса (3) | `LoggingFilter` |
| 7 | Рассылка по Email (3) | `EmailNotificationService` (Jakarta Mail) |
| 8 | Рассылка через эмулятор SMPP (3) | `SmsNotificationService` (jSMPP) |
| 9 | Рассылка через Telegram (3) | `TelegramNotificationService` (Bot API) |
| 10 | Сохранение кода в файл (3) | `FileNotificationService` → `otp-codes.txt` в корне проекта |
| 11 | Токенная аутентификация и авторизация (3) | `TokenService` (JWT), `AuthSupport` |
| 12 | Подробное логирование всех запросов (3) | `LoggingFilter` + логи в сервисах/обработчиках |

Дополнительные требования ТЗ:

- **3 таблицы**: `users`, `otp_config`, `otp_codes` — `schema.sql`.
- **otp_config ≤ 1 строки** — `PRIMARY KEY (id) DEFAULT 1 CHECK (id = 1)`.
- **Только один администратор** — частичный уникальный индекс
  `uq_users_single_admin ON users((role)) WHERE role='ADMIN'` + проверка в `AuthService`.
- **Статусы OTP**: ACTIVE / EXPIRED / USED — enum `OtpStatus` + `CHECK` в БД.
- **Удаление пользователя и его кодов** — `ON DELETE CASCADE` + `UserService.deleteUser`.

---

## Быстрый старт

### Требования

- **JDK 21+** (проект собран и протестирован на JDK 25)
- **Maven 3.9+**
- **Docker** (для PostgreSQL 17 и SMTP-эмулятора MailHog)

Все Java-зависимости (PostgreSQL JDBC, HikariCP, Jackson, JJWT, Angus Mail, jSMPP,
Logback) подтягиваются Maven автоматически — ручная установка не требуется.

### 1. Поднять инфраструктуру

```bash
docker compose up -d
```

Поднимутся два контейнера:

- **PostgreSQL 17** — `localhost:5432`, БД `otp`, пользователь `otp`, пароль `otp`;
- **MailHog** — SMTP-эмулятор на `localhost:1025`, веб-интерфейс на <http://localhost:8025>.

### 2. Собрать проект

```bash
mvn clean package
```

В результате в `target/mifidev-otp.jar` появится исполняемый fat-jar со всеми
зависимостями. Заодно прогоняются модульные тесты.

### 3. Запустить сервис

```bash
java -jar target/mifidev-otp.jar
```

Сервис стартует на <http://localhost:8080>. При старте автоматически создаётся
схема БД и строка конфигурации OTP по умолчанию (длина 6, время жизни 300 с).

Проверка живости:

```bash
curl http://localhost:8080/health      # {"status":"UP"}
```

---

## Конфигурация

Все настройки — в `src/main/resources/application.properties`. Любой параметр
переопределяется переменной окружения: ключ переводится в верхний регистр, точки
заменяются на подчёркивания (`db.url` → `DB_URL`). Переменные окружения имеют
приоритет — это удобно для Docker/CI.

| Ключ | Значение по умолчанию | Описание |
|------|-----------------------|----------|
| `server.port` | `8080` | порт HTTP-сервера |
| `db.url` | `jdbc:postgresql://localhost:5432/otp` | JDBC-URL |
| `db.user` / `db.password` | `otp` / `otp` | учётные данные БД |
| `jwt.secret` | пусто (эфемерный) | ключ подписи JWT; если пусто — генерируется случайный ключ на время запуска; задайте `JWT_SECRET` (≥ 32 байт) для стабильного ключа |
| `jwt.ttl.seconds` | `3600` | срок жизни токена |
| `otp.default.length` | `6` | длина кода при первом запуске |
| `otp.default.ttl.seconds` | `300` | время жизни кода при первом запуске |
| `otp.scheduler.interval.seconds` | `30` | период пометки EXPIRED |
| `otp.default.channel` | `FILE` | канал по умолчанию, если не указан в запросе |
| `otp.file.path` | `otp-codes.txt` | файл для канала FILE (в корне проекта) |
| `email.smtp.host` / `email.smtp.port` | `localhost` / `1025` | SMTP (MailHog) |
| `smpp.host` / `smpp.port` | `localhost` / `2775` | SMPP-эмулятор |
| `telegram.enabled` | `false` | включение Telegram-канала |
| `telegram.bot.token` / `telegram.chat.id` | — | параметры Telegram-бота |

---

## Роли и авторизация

- **USER** — обычный пользователь: генерация и валидация OTP.
- **ADMIN** — администратор: управление конфигурацией OTP и пользователями.

Авторизация — по JWT. После логина клиент получает токен и передаёт его в заголовке:

```
Authorization: Bearer <token>
```

Правила доступа:

| Эндпоинт | Доступ |
|----------|--------|
| `/api/auth/**` | публичный |
| `/api/otp/**` | любой аутентифицированный пользователь |
| `/api/admin/**` | только ADMIN |

Нарушения: нет/битый токен → **401**, недостаточно прав → **403**.

---

## Справочник API

Базовый URL: `http://localhost:8080`. Тело запросов и ответов — JSON.
Формат ошибки: `{"error": "...", "status": <код>}`.

### Аутентификация

#### `POST /api/auth/register` — регистрация

```json
{ "login": "alice", "password": "alice123", "role": "USER" }
```

- `role` необязателен (по умолчанию `USER`). Допустимо `ADMIN`, но если
  администратор уже существует — **409 Conflict**.
- Дублирующийся `login` — **409**.

Ответ `201`:

```json
{ "id": 2, "login": "alice", "role": "USER" }
```

#### `POST /api/auth/login` — вход

```json
{ "login": "alice", "password": "alice123" }
```

Ответ `200`:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "role": "USER",
  "expiresInSeconds": 3600,
  "expiresAt": "2026-06-30T18:00:00Z"
}
```

### API пользователя (роль USER или ADMIN)

#### `POST /api/otp/generate` — сгенерировать и разослать код

```json
{ "operationId": "payment-42", "channel": "EMAIL", "recipient": "alice@example.com" }
```

- `operationId` — необязательный идентификатор защищаемой операции. Если он
  указан при генерации, то при валидации необходимо передать тот же
  `operationId` (строгая привязка кода к операции). Код, сгенерированный без
  `operationId`, валидируется без него.
- `channel` — `EMAIL | SMS | TELEGRAM | FILE` (по умолчанию `otp.default.channel`).
- `recipient` — адрес/телефон/chatId; обязателен для `EMAIL` и `SMS`.

Ответ `201` (значение кода в ответе не возвращается — он приходит только по каналу):

```json
{
  "otpId": 7,
  "operationId": "payment-42",
  "status": "ACTIVE",
  "channel": "EMAIL",
  "expiresAt": "2026-06-30T17:34:08",
  "delivered": true,
  "deliveryError": null
}
```

Если канал недоступен (например, не запущен SMPP-эмулятор), код всё равно
сохраняется (`status=ACTIVE`), а в ответе будет `delivered=false` и причина в
`deliveryError`.

#### `POST /api/otp/validate` — проверить код

```json
{ "code": "123456", "operationId": "payment-42" }
```

Ответ `200` при успехе (код переходит в статус USED):

```json
{ "valid": true, "status": "USED", "message": "Code accepted" }
```

Неверный / использованный / просроченный код — **400** с описанием.

### API администратора (только роль ADMIN)

#### `GET /api/admin/config` — текущая конфигурация OTP

```json
{ "id": 1, "codeLength": 6, "ttlSeconds": 300, "updatedAt": "2026-06-30T17:00:00" }
```

#### `PUT /api/admin/config` — изменить длину кода и время жизни

```json
{ "codeLength": 4, "ttlSeconds": 120 }
```

`codeLength` — 4..10, `ttlSeconds` > 0. Ответ — обновлённая конфигурация.

#### `GET /api/admin/users` — список всех пользователей, кроме администраторов

```json
[ { "id": 2, "login": "alice", "role": "USER", "createdAt": "2026-06-30T17:29:07" } ]
```

> Пароли (их хеши) никогда не возвращаются.

#### `DELETE /api/admin/users/{id}` — удалить пользователя и его OTP-коды

```json
{ "userId": 2, "login": "alice", "deletedOtpCodes": 3 }
```

Несуществующий пользователь — **404**, попытка удалить администратора — **400**.

---

## Каналы рассылки кодов

Канал выбирается полем `channel` в запросе генерации. Реализованы все четыре.

### Email (SMTP) — `EMAIL`

Использует Jakarta Mail. По умолчанию настроен на **MailHog** (`localhost:1025`,
без аутентификации). Полученные письма видны в веб-интерфейсе
<http://localhost:8025>. Для реальной почты задайте `email.smtp.host`,
`email.smtp.port`, `email.smtp.auth=true`, `email.username`, `email.password`,
`email.smtp.starttls=true`.

### SMS (SMPP) — `SMS`

Использует jSMPP и отправляет `submit_sm` в SMPP-эмулятор на `localhost:2775`.
Подойдёт **SMPPsim** (Selenium Software): скачайте, запустите `startsmppsim.sh`
(или `.bat`), значения `smpp.system_id` / `smpp.password` возьмите из
`config/smppsim.props`. Параметры эмулятора настраиваются ключами `smpp.*`.

### Telegram (Bot API) — `TELEGRAM`

Использует `java.net.http.HttpClient`. Чтобы включить:

1. Создайте бота через [@BotFather](https://t.me/BotFather), получите токен.
2. Напишите боту любое сообщение, затем откройте
   `https://api.telegram.org/bot<token>/getUpdates` и возьмите `chat.id`.
3. Задайте `telegram.enabled=true`, `telegram.bot.token=...`,
   `telegram.chat.id=...` (можно переопределить chatId полем `recipient`).

### Файл — `FILE`

Дописывает код в файл `otp-codes.txt` в корне проекта. Это канал по умолчанию и
всегда доступная резервная опция (удобно для тестирования). Пример строки:

```
2026-06-30 17:29:08 | operation=payment-42 | code=8361
```

---

## Планировщик просроченных кодов

`OtpExpirationScheduler` на базе `ScheduledExecutorService` раз в
`otp.scheduler.interval.seconds` (по умолчанию 30 с) выполняет:

```sql
UPDATE otp_codes SET status = 'EXPIRED' WHERE status = 'ACTIVE' AND expires_at < now();
```

Количество помеченных кодов пишется в лог. Всё время вычисляется и сравнивается
на стороне БД (`now()` и `created_at`/`expires_at`, вычисляемые при вставке), что
исключает рассинхрон часов JVM и PostgreSQL. Просроченность также проверяется
непосредственно при валидации, поэтому код невозможно использовать после
истечения срока даже между запусками планировщика. Валидация атомарна (один
`UPDATE ... RETURNING`), поэтому код является строго одноразовым — его нельзя
использовать дважды даже при одновременных запросах.

---

## Логирование

SLF4J + Logback, вывод в консоль и в файл `logs/otp-service.log` (ротация по дням).

- **Каждый HTTP-запрос** логируется фильтром `LoggingFilter` на входе и выходе:
  метод, путь, IP, HTTP-статус, пользователь, время обработки.
- **Подробно** логируются бизнес-события: регистрация, вход, генерация и
  валидация кода, изменение конфигурации, удаление пользователя, результат
  доставки по каждому каналу, работа планировщика.

Пример:

```
--> POST /api/otp/generate from 127.0.0.1
INFO  c.p.otp.service.OtpService - Generated OTP id=7 for user=2 operation=payment-42 channel=EMAIL delivered=true
<-- POST /api/otp/generate status=201 user=alice/USER 34ms
```

---

## Схема базы данных

Создаётся автоматически при старте из `schema.sql` (идемпотентно).

- **users** — `id`, `login` (уникальный), `password_hash`, `role` (ADMIN/USER), `created_at`.
  Частичный уникальный индекс гарантирует не более одного администратора.
- **otp_config** — единственная строка (`id = 1`, `CHECK (id = 1)`): `code_length`, `ttl_seconds`, `updated_at`.
- **otp_codes** — `id`, `user_id` (FK, `ON DELETE CASCADE`), `operation_id`, `code`,
  `status` (ACTIVE/EXPIRED/USED), `channel`, `created_at`, `expires_at`, `used_at`.

---

## Тестирование

### Модульные тесты

```bash
mvn test
```

Покрывают хеширование паролей (`PasswordEncoderTest`) и выпуск/проверку JWT
(`TokenServiceTest`). Внешняя инфраструктура для них не нужна.

### Ручное / интеграционное тестирование

1. Поднимите инфраструктуру и запустите сервис (см. [Быстрый старт](#быстрый-старт)).
2. Прогоните сценарий из раздела ниже.
3. Письма проверяйте в MailHog (<http://localhost:8025>), коды канала FILE — в
   `otp-codes.txt`, SMS — в логе SMPP-эмулятора.

---

## Полный сценарий использования (curl)

```bash
BASE=http://localhost:8080

# 1. Регистрируем администратора (второго зарегистрировать уже не получится)
curl -s -X POST $BASE/api/auth/register \
  -d '{"login":"admin","password":"admin123","role":"ADMIN"}'

# 2. Регистрируем обычного пользователя
curl -s -X POST $BASE/api/auth/register \
  -d '{"login":"alice","password":"alice123"}'

# 3. Логинимся, сохраняем токены
ADMIN_TOKEN=$(curl -s -X POST $BASE/api/auth/login \
  -d '{"login":"admin","password":"admin123"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")
USER_TOKEN=$(curl -s -X POST $BASE/api/auth/login \
  -d '{"login":"alice","password":"alice123"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")

# 4. Админ меняет конфигурацию OTP (длина 4, время жизни 120 с)
curl -s -X PUT -H "Authorization: Bearer $ADMIN_TOKEN" \
  $BASE/api/admin/config -d '{"codeLength":4,"ttlSeconds":120}'

# 5. Админ смотрит список пользователей (без администраторов)
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" $BASE/api/admin/users

# 6. Пользователь генерирует код в файл и читает его из otp-codes.txt
curl -s -X POST -H "Authorization: Bearer $USER_TOKEN" \
  $BASE/api/otp/generate -d '{"operationId":"op-1","channel":"FILE"}'
CODE=$(tail -n1 otp-codes.txt | sed -E 's/.*code=([0-9]+).*/\1/')

# 7. Пользователь валидирует код (станет USED)
curl -s -X POST -H "Authorization: Bearer $USER_TOKEN" \
  $BASE/api/otp/validate -d "{\"code\":\"$CODE\",\"operationId\":\"op-1\"}"

# 8. Письмо через Email (видно в MailHog http://localhost:8025)
curl -s -X POST -H "Authorization: Bearer $USER_TOKEN" \
  $BASE/api/otp/generate -d '{"operationId":"op-2","channel":"EMAIL","recipient":"alice@example.com"}'

# 9. Обычный пользователь НЕ имеет доступа к админ-API (ожидаем 403)
curl -s -o /dev/null -w "%{http_code}\n" \
  -H "Authorization: Bearer $USER_TOKEN" $BASE/api/admin/users

# 10. Админ удаляет пользователя вместе с его OTP-кодами
ALICE_ID=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" $BASE/api/admin/users \
  | python3 -c "import sys,json;print(json.load(sys.stdin)[0]['id'])")
curl -s -X DELETE -H "Authorization: Bearer $ADMIN_TOKEN" $BASE/api/admin/users/$ALICE_ID
```

---

## Остановка и очистка

```bash
docker compose down       # остановить контейнеры
docker compose down -v    # остановить и удалить данные БД
```

---

**Автор:** студент дисциплины Java • **Заказчик:** Promo IT
