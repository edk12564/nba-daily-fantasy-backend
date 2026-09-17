# Backend Agent Guide

Read the workspace-level `../AGENTS.md` first. This file applies specifically to the `nba-daily-fantasy-backend/` Git repository.

## Purpose and architecture

This is the Java 21/Spring Boot API used by the Discord Activity:

- `src/main/java/com/picknroll/demo/controllers/ActivitiesController.java`: Activity REST endpoints and Discord OAuth exchange.
- `services/`: roster, player, guild/channel, lock, and JWT business logic.
- `repositories/`: Spring Data JDBC queries against PostgreSQL.
- `httpclient/NbaAPIClient.java`: NBA live box-score proxy.
- `interceptors/JwtInterceptor.java`: protects Activity routes.
- `terraform/`: AWS EC2, Route 53, SSM access, TLS, and service provisioning.

The active user flow is the Activity REST API. `InteractionsController.java` is fully commented out and `SignatureVerificationInterceptor.java` is an empty stub, so the legacy slash-command path is not functional.

## Commands

Run from this repository:

- `./mvnw test`
- `./mvnw package`

The project uses Java 21 and Testcontainers/PostgreSQL for integration tests. Do not install or upgrade tools/dependencies without explicit user approval.

## API contract

Active routes are under `/api/activity`:

- `GET /todays-players`
- `GET /livedata/{gameId}`
- `GET /my-roster/{guildId}/{channelId}/{discordPlayerId}`
- `POST /my-roster`
- `DELETE /my-roster`
- `GET /rosters/global`
- `GET /rosters/weekly/{guildId}`
- `GET /rosters/{guildId}`
- `GET /lock-time`
- `POST /token`

All routes except `/token` require a backend bearer JWT. `/token` exchanges a Discord authorization code, fetches `/users/@me`, and returns the backend JWT plus Discord access token.

## Product and date rules

- Default product date is `America/Los_Angeles` via `Utils.getCaliforniaDate()`.
- Users select one `PG`, `SG`, `SF`, `PF`, and `C`.
- The server-side roster cap is `ActivitiesController.MAX_DOLLARS = 100`.
- Lock time is the first scheduled game tipoff for that product day.
- Daily leaderboards are guild and global; weekly leaderboards are guild-scoped.
- Prior-season rows are historical data. Never truncate them for the 2026-27 rollover.
- Test dates such as `2025-12-25` are fixtures, not production season settings.

## Authentication and authorization requirements

- Treat JWT claims as the authenticated identity.
- Do not trust `discordPlayerId`, `discord_player_id`, guild IDs, or nicknames supplied only by path/body parameters.
- Validate that roster reads/writes and guild/channel registration are authorized for the JWT user and current Discord context.
- Fail closed when the lock row is absent or malformed.
- Keep CORS origins and Discord OAuth redirect configurable and consistent with the production Discord application.
- Never log Discord access tokens, backend JWTs, client secrets, public-key signatures, or database credentials.

## Database expectations

Primary tables are `teams`, `nba_players`, `daily_roster`, `is_locked`, `discord_player_guilds`, and `discord_channels`.

- `daily_roster` key: `(discord_player_id, date, position)`.
- The player loader requires a unique key on `(nba_player_id, date)`.
- The score loader updates `fantasy_score`, `pts`, `reb`, `ast`, `blk`, `tov`, and `stl`.
- Production already supports the loader contract, and the checked-in test schema mirrors its player constraint and score/stat columns.
- Spring uses `ddl-auto=validate`; there is no checked-in production schema or Flyway/Liquibase migration history.

Use `DATABASE.md` to verify the production contract. Use additive, versioned migrations and back up production before future schema changes.

## Required 2026-27 work

1. Verify the corrected datasource URL and UTC database session timezone in the deployment environment.
2. Define an explicit response for no-games days and missing lock rows. Avoid returning `null`; roster writes must remain closed.
3. Make lock checking, salary validation, and roster upsert one transaction to prevent concurrent requests from exceeding the cap.
4. Validate 2026-27 lock rows, daily players, live NBA box scores, persisted scoring, and guild/global leaderboard isolation.
5. Fix and test weekly boundaries. The repository SQL uses exclusive dates while the Python bot uses inclusive dates.
6. Decide whether slash commands are retired. If so, delete stale registrations/docs; if restored, implement Discord signature verification and the complete secured interaction flow.
7. Keep `DATABASE.md` synchronized with production and the loader SQL.

## Deployment

Production is modeled as:

- AWS EC2 and Route 53 for `picknrolls.click`.
- Spring `prod` profile on port 8443.
- TLS certificate/key copied under `/opt/nba-daily-fantasy-backend`.
- Secrets fetched from AWS SSM Parameter Store into `/etc/sysconfig/myapp.env`.
- `nba.service` runs the packaged JAR.

Environment names include `DATABASE_USER`, `DATABASE_URL`, `DB_PASSWORD`, `VITE_DISCORD_CLIENT_ID`, `DISCORD_CLIENT_SECRET`, `DISCORD_API_PUBLIC_KEY`, and `JWT_SECRET`. Never commit their values.

## Known risks

- Base and production logging default to INFO; development restores DEBUG overrides.
- CORS origins, OAuth redirect, database host, domain, ports, AMI, and paths are hard-coded.
- Terraform allows SSH from `0.0.0.0/0`.
- The EC2 role can read SSM parameters from `Resource = "*"`.
- TLS private-key copies are set to mode `644`.
- The provisioning script installs packages imperatively and uses Terraform provisioners for deployment.
- The checked-in `terraform/nba-ubuntu.service` runs the `dev` profile and does not match the service generated by `setup.sh`.
- `BOT_ACCESS_KEY` is fetched by backend provisioning although the active backend does not run the Python bot; verify whether it is obsolete.
- `isTodayLocked()` is named opposite to its implementation: it returns true while lock time is still in the future.
- Some roster GET operations also write guild/channel mappings.
- Exception handling around OAuth uses `@SneakyThrows`; return controlled API errors for network/JSON failures.
- Backend README package paths and slash-command claims are stale.

## Verification

- Run `./mvnw test` and `./mvnw package`.
- Add tests for absent lock rows, no-game days, DST/date boundaries, JWT identity mismatches, malformed Discord responses, cap races, complete/incomplete rosters, inclusive weekly bounds, and production-schema constraints.
- Smoke-test OAuth and all Activity endpoints through Discord's proxy in a test guild.
- Confirm production starts with the `prod` profile and that secrets/logs are not exposed.
