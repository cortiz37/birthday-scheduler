# Birthday Scheduler

A Spring Boot application that sends daily birthday greetings to friends celebrating their birthday and birthday reminders to all other friends via Email and/or Twilio SMS. It runs an initial check on startup and continues on a configurable cron schedule.

## Assumptions

- **Friends data source (`app.friends.source`)**: Defaults to `sqlite` (reading from the `friends` table initialized by `schema.sql` and `data.sql`, with automatic `phone_number` column migration for legacy schemas). Setting `app.friends.source=file` switches friend loading to `app.friends.file-location` (defaults to `classpath:friends.txt`).
- **CSV format**: When `app.friends.source=file`, the input file is assumed to be UTF-8 CSV with a header row. Required columns are `first_name`, `last_name`, `date_of_birth` (ISO-8601 `YYYY-MM-DD`), and `email`; `phone_number` is optional (rows may omit it or leave it blank).
- **Job execution history**: Daily run tracking (`birthday_job_runs` table) is always persisted in SQLite (`spring.datasource.url`), even when `app.friends.source=file`, so restarts or repeated scheduled triggers on the same day do not resend notifications.
- **Leap-year birthdays**: Friends born on February 29 are celebrated on February 28 in non-leap years.
- **Notification channels**: Email is enabled by default (`app.notifications.email.enabled=true`) and Twilio SMS is disabled by default (`app.notifications.sms.enabled=false`). Enabling SMS requires valid Twilio credentials; friends missing an email address or phone number are skipped on that channel.
- **CLI & scheduling behavior**: Starting without `--list` processes today's birthdays once (if not already recorded in SQLite) and keeps the scheduler alive (`0 0 8 * * *` by default). Passing `--list` prints all loaded friends and exits.

## Requirements

- **Java 21** (for local builds and running the JAR; the included `./gradlew` wrapper provides Gradle 9.7.1)
- **Docker** (and **Docker Compose**, optional, for containerized builds and runs)
- **SMTP server** (defaults to `localhost:1025`, e.g., Mailpit or MailHog) when email notifications are enabled
- **Twilio credentials** (`TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER`) if SMS notifications are enabled

## Build

Run tests and build the executable Spring Boot JAR (`build/libs/birthday-scheduler-1.0.0.jar`):

```bash
./gradlew test bootJar
```

Build the Docker image locally using Jib (produces `birthday-scheduler:1.0.0` by default):

```bash
./gradlew jibDockerBuild
```

Or build the Docker image using the `Dockerfile`:

```bash
docker build -t birthday-scheduler:1.0.0 .
```

## Run

### CLI Parameters

- `--list`: Prints all friends loaded from the configured source (`sqlite` or `file`) with their ID, name, date of birth, email, and phone number, then exits immediately without sending notifications or starting the scheduler. When running from Java, pass it as `--list`.

### 1. Manually executing the JAR

Default SQLite friends source:

```bash
java -jar build/libs/birthday-scheduler-1.0.0.jar
```

Using a CSV file as the friends source (job tracking still uses SQLite):

```bash
APP_FRIENDS_SOURCE=file \
APP_FRIENDS_FILE=file:/path/to/friends.csv \
java -jar build/libs/birthday-scheduler-1.0.0.jar
```

List loaded friends and exit:

```bash
java -jar build/libs/birthday-scheduler-1.0.0.jar --list
```

*(You can also run directly via Gradle without building the JAR separately: `./gradlew bootRun` or `./gradlew bootRun --args='--list'`.)*

### 2. Using Jib

Build the image into your local Docker daemon (or push to a container registry with `./gradlew jib -PjibImage=<registry>/<repo>:<tag>`):

```bash
./gradlew jibDockerBuild
docker run --rm -v birthday-data:/app/data birthday-scheduler:1.0.0
```

Jib configures `/app/data` as the working directory and sets `SPRING_DATASOURCE_URL=jdbc:sqlite:/app/data/birthday-scheduler.db`. Optional Jib properties: `-PjibImage=<name:tag>` and `-PjibArch=amd64|arm64`.

### 3. Using Docker

```bash
docker build -t birthday-scheduler:1.0.0 .
docker run --rm -v birthday-data:/app/data birthday-scheduler:1.0.0
```

Run with a mounted CSV file and custom SMTP host:

```bash
docker run --rm \
  -v birthday-data:/app/data \
  -v /path/to/friends.csv:/app/friends.csv:ro \
  -e APP_FRIENDS_SOURCE=file \
  -e APP_FRIENDS_FILE=file:/app/friends.csv \
  -e SMTP_HOST=host.docker.internal \
  -e SMTP_PORT=1025 \
  birthday-scheduler:1.0.0
```

List friends inside the container:

```bash
docker run --rm -v birthday-data:/app/data birthday-scheduler:1.0.0 --list
```

### 4. Using Docker Compose

Start the service in the background (builds the image from `Dockerfile` and persists SQLite data in the `birthday-data` volume):

```bash
docker compose up -d --build
```

Follow logs, list friends, or stop the service:

```bash
docker compose logs -f
docker compose run --rm birthday-scheduler --list
docker compose down
```

## Key Environment Variables

| Variable | Default | Description |
| --- | --- | --- |
| `APP_FRIENDS_SOURCE` | `sqlite` | Friends datasource: `sqlite` or `file` |
| `APP_FRIENDS_FILE` | `classpath:friends.txt` | Resource or filesystem path to CSV when `APP_FRIENDS_SOURCE=file` |
| `SPRING_DATASOURCE_URL` | `jdbc:sqlite:dev.db` | SQLite JDBC URL (set to `jdbc:sqlite:/app/data/birthday-scheduler.db` in containers) |
| `APP_BIRTHDAY_SCHEDULER_CRON` | `0 0 8 * * *` | Spring cron expression for daily birthday checks |
| `APP_NOTIFICATIONS_EMAIL_ENABLED` | `true` | Enable/disable email channel |
| `SMTP_HOST` / `SMTP_PORT` | `localhost` / `1025` | SMTP server host and port |
| `SMTP_USERNAME` / `SMTP_PASSWORD` | _(empty)_ | SMTP credentials |
| `SMTP_AUTH` / `SMTP_STARTTLS` | `false` / `false` | SMTP auth and STARTTLS flags |
| `APP_MAIL_FROM` | `myaccount@server.com` | Sender email address |
| `APP_NOTIFICATIONS_SMS_ENABLED` | `false` | Enable/disable Twilio SMS channel |
| `TWILIO_ACCOUNT_SID` / `TWILIO_AUTH_TOKEN` / `TWILIO_FROM_NUMBER` | _(empty)_ | Required when SMS notifications are enabled |
