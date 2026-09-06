# Telegram Bot Starosta backend

## Local configuration

Create the ignored local environment file once:

```shell
cp test.env.example test.env
```

The default `local` Spring profile imports `test.env`. Do not commit this file.

## Production configuration

Run with `prod` (or `production`) and provide these environment variables:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `TELEGRAM_BOT_TOKEN`
- `TELEGRAM_AUTH_TOKEN_SECRET` (at least 32 bytes)
- `MINI_APP_ALLOWED_ORIGINS`
- `SEMESTER_START` and `SEMESTER_END` in ISO `yyyy-MM-dd` format

The production profile does not import `test.env`. Invalid or missing required
values stop application startup.

## Database migrations

Flyway owns the schema and Hibernate runs with `ddl-auto=validate`. A non-empty
legacy schema without Flyway history is baselined at version `0`, then receives
`V1` and the repeatable index migration. A clean database receives the same
managed schema from `V1`.
