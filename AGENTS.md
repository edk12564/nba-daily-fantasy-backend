# Backend Agent Guide

Read the workspace-level `../AGENTS.md` first. This file applies specifically to the `nba-daily-fantasy-backend/` Git repository.

## Purpose and architecture

This is the Java 21/Spring Boot API used by the Discord Activity:

- `src/main/java/com/picknroll/demo/controllers/ActivitiesController.java`: Activity REST endpoints and Discord OAuth exchange.
- `controllers/GachaController.java` + `services/GachaServices.java` / `GachaPoolService.java`: Rolls wallet, inventory, rates, salary-weighted pulls, pity, grants.
- `gacha/GachaEconomy.java`: pull cost, rates, dust-on-dupe, pity, podium amounts, ★ salary bands.
- `services/`: roster, player, guild/channel, lock, and JWT business logic.
- `repositories/`: Spring Data JDBC queries against PostgreSQL (including `gacha_*` and roster `equipped*` joins).
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
- `GET /schedule` (date → game IDs; server fetches cdn.nba.com with NBA browser headers)
- `GET /my-roster/{guildId}/{channelId}/{discordPlayerId}`
- `POST /my-roster`
- `DELETE /my-roster`
- `GET /rosters/global`
- `GET /rosters/weekly/{guildId}`
- `GET /rosters/{guildId}`
- `GET /lock-time`
- `POST /token`
- `GET /gacha/wallet` — rolls, dust, pity, streak (lazy-claims pending guild podium grants)
- `GET /gacha/inventory` — owned skins; `displaySkin` marks max rarity per player; includes `playerName`
- `GET /gacha/rates` — published rates / pity / dust / pull cost
- `POST /gacha/pull` — body `{ "count": 1 | 10 }`; results include `playerName`, rarity, art, frame, outcome, dustGranted

All routes except `/token` require a backend bearer JWT. `/token` exchanges a Discord authorization code, fetches `/users/@me`, and returns the backend JWT plus Discord access token.

Gacha pull / wallet / inventory resolve the actor from JWT claim `id` (same idea as roster writes). There is no `/gacha/equip`. Roster and leaderboard `DailyRosterPlayer` payloads may include `equippedSkinRarity`, `equippedArtUrl`, `frameKey`, and `playerStar` (owner’s max owned rarity for that `nba_player_id`). Starter Rolls (`GachaEconomy.STARTER_ROLLS` = 1000 = 10 pulls) are credited once via `gacha_token_grants` reason `STARTER` / period `once` on `ensureWallet` (covers wallets created earlier at 0 rolls). Completing a 5/5 roster grants Rolls once per Pacific day; streak and guild podium grants are idempotent via `gacha_token_grants`.

## Product and date rules

- Default product date is `America/Los_Angeles` via `Utils.getCaliforniaDate()`.
- Users select one `PG`, `SG`, `SF`, `PF`, and `C`.
- The server-side roster cap is `ActivitiesController.MAX_DOLLARS = 100`. A total of 100 is allowed; 101 is rejected.
- Lock time is the first scheduled game tipoff for that product day.
- A missing `is_locked` row, or a row whose `lock_time` is null, is a no-games day. `GET /lock-time` returns `404` with `{"error":"No games scheduled"}`. `POST /my-roster` and `DELETE /my-roster` return that same response and also return `400` once `lock_time` is before now.
- Salary validation and the roster upsert run in one transaction. `pg_advisory_xact_lock` serializes writes for one Discord player and product day so concurrent requests cannot both pass the cap.
- Daily leaderboards are guild and global. The guild weekly leaderboard is Monday through the requested Pacific date, inclusive: `previousOrSame(MONDAY)` and `date >= start AND date <= end`. The Python bot's completed-week query is a separate inclusive range and was left unchanged.
- Prior-season rows are historical data. Never truncate them for the 2026-27 rollover.
- Test dates such as `2025-12-25` are fixtures, not production season settings.
- Gacha: Rare/Epic/Legendary skins discount roster salary by 1/2/3 (`GachaEconomy.salaryDiscount`); cap SQL and roster enrichment use billed cost. ★ is a salary display badge, not drop weight. Current bands (tunable for FanDuel slate ~11–34): ≤14→1★, ≤19→2, ≤24→3, ≤28→4, ≥29→5. Keep `GachaEconomy.playerStar` and the three `CASE` expressions in `DailyRosterRepository` synchronized; keep discount CASE in `getTodaysRosterPriceWithPlayer` aligned with `salaryDiscount`.
- Pull player selection is salary-weighted with floor weight 8; rarity is independent of ★.

## Authentication and authorization requirements

- Treat JWT claims as the authenticated identity.
- `POST /my-roster` and `DELETE /my-roster` compare the body Discord player id with the JWT `id` claim and return `403` on a mismatch.
- Roster reads, leaderboard guild ids, and the guild/channel writes performed by `GET /my-roster` still accept ids supplied only by the path. Do not treat those as authorized for the JWT user.
- Keep CORS origins and Discord OAuth redirect configurable and consistent with the production Discord application.
- Never log Discord access tokens, backend JWTs, client secrets, public-key signatures, or database credentials.

## Database expectations

Primary tables are `teams`, `nba_players`, `daily_roster`, `is_locked`, `discord_player_guilds`, and `discord_channels`, plus gacha: `gacha_skins`, `gacha_user_wallet`, `gacha_user_skins`, `gacha_token_grants`.

- `daily_roster` key: `(discord_player_id, date, position)`.
- The player loader requires a unique key on `(nba_player_id, date)`.
- The score loader updates `fantasy_score`, `pts`, `reb`, `ast`, `blk`, `tov`, and `stl`.
- Gacha catalog seed: `scripts/src/seed_gacha_skins.py` (also invoked from `load-todays-players.py`); see `DATABASE.md`.
- Production already supports the loader contract, and the checked-in test schema mirrors its player constraint and score/stat columns.
- Spring uses `ddl-auto=validate`; there is no checked-in production schema or Flyway/Liquibase migration history.

Use `DATABASE.md` to verify the production contract. Use additive, versioned migrations and back up production before future schema changes.

## Required 2026-27 work

Done in this repository: no-games lock responses, transactional cap enforcement, inclusive Monday-through-requested-day weekly bounds, and JWT checks on roster create and delete.

Still required before launch:

1. Verify the corrected datasource URL and UTC database session timezone in the deployment environment.
2. Load and validate 2026-27 lock rows, daily players, live NBA box scores, persisted scoring, and guild/global leaderboard isolation. Opening night, October 20, 2026, stays closed until the loaders have written that date's `is_locked` and `nba_players` rows.
3. Leave slash commands retired unless the complete signed interaction flow is restored. `InteractionsController.java` is commented out and signature verification is a stub.
4. Keep `DATABASE.md` synchronized with production and the loader SQL.

## Deployment

Production is modeled as:

- AWS EC2 and Route 53 for `picknrolls.click`.
- Spring `prod` profile on port 8443.
- TLS certificate/key synced from Let's Encrypt into `/opt/nba-daily-fantasy-backend` (`fullchain.pem` mode 644, `privkey.pem` mode 600).
- Secrets fetched from AWS SSM Parameter Store into `/etc/sysconfig/myapp.env`.
- `nba.service` runs the packaged JAR.

Environment names include `DATABASE_USER`, `DATABASE_URL`, `DB_PASSWORD`, `VITE_DISCORD_CLIENT_ID`, `DISCORD_CLIENT_SECRET`, `DISCORD_API_PUBLIC_KEY`, and `JWT_SECRET`. Never commit their values. `setup.sh` also fetches `BOT_ACCESS_KEY`; confirm whether it is still required.

For the Supabase **shared pooler**, `DATABASE_USER` must be `postgres.<project-ref>` (not plain `postgres`). Datasource username/password are set via `spring.datasource.username` / `spring.datasource.password` against `aws-0-us-east-1.pooler.supabase.com:6543` (transaction pooler). Do not put credentials in the JDBC URL query string. Validate SSM and `/etc/sysconfig/myapp.env` with psql on port 6543 before blaming Route53 or TLS. The JDBC URL and Hikari props must set `prepareThreshold=0` so the driver does not use server-side prepared statements (transaction mode drops them between checkouts).

`terraform/setup.sh` is idempotent: it installs Java 21 only if missing, replaces the JAR without wiping `/opt`, rewrites the SSM env file as root, issues a Certbot standalone cert for `picknrolls.click` only when the live cert is missing, syncs PEMs into the app dir, installs a single renew job at `/etc/cron.d/nba-certbot`, and restarts `nba.service`. Port 80 must stay open for HTTP-01; the app listens on 8443. AL2023 keeps system `python3` at 3.9; when Certbot is installed via pip, the script uses a **Python 3.11** venv under `/opt/certbot` (Certbot 5+ drops 3.9).

### Code-only release (JAR redeploy)

Use this path when infrastructure is already up and you only need to ship a new backend JAR. Terraform provisioners upload `../target/demo-0.0.1-SNAPSHOT.jar` and run `terraform/setup.sh`, which refreshes the env file from SSM, syncs TLS PEMs, and restarts `nba.service`.

1. **Schema first (only if needed).** Spring starts with `ddl-auto=validate`. For additive SQL, back up PostgreSQL, apply the migration, and verify with `DATABASE.md` before restarting the app. Skip this step for pure code changes.

2. **SSH private key for Terraform provisioners.** The AWS `.pem` is a PEM-encoded private key; Terraform expects it at `~/.ssh/id_rsa` (same key material, OpenSSH default name):

   ```bash
   mkdir -p ~/.ssh
   cp /path/to/your-key.pem ~/.ssh/id_rsa
   chmod 600 ~/.ssh/id_rsa
   ssh-keygen -y -f ~/.ssh/id_rsa > ~/.ssh/id_rsa.pub
   chmod 644 ~/.ssh/id_rsa.pub
   ```

   The key must match the public key already authorized on the EC2 instance (`aws_key_pair.deployer_key` / `nba-app-key`). Confirm with:

   ```bash
   ssh -i ~/.ssh/id_rsa ec2-user@<EC2_PUBLIC_IP>
   ```

   If SSH fails with `Permission denied (publickey)` even though your private key matches the current `nba-app-key` in AWS, the running instance still has the key from **launch time**. Updating `aws_key_pair` or `~/.ssh/id_rsa` later does not rewrite `/home/ec2-user/.ssh/authorized_keys` on an existing VM. Use **Recover SSH access** below.

### Recover SSH access (key mismatch)

When Terraform file provisioners fail with `SSH authentication failed` and local `ssh ec2-user@<EC2_PUBLIC_IP>` rejects your key, recover through the AWS Console (no local private key required):

1. In **EC2 → Instances**, select the backend instance and choose **Connect**.
2. Open **EC2 Instance Connect** (browser session) as user **`ec2-user`**.
3. On the instance, append your **current** deploy public key (multiple lines in `authorized_keys` are fine):

   ```bash
   mkdir -p ~/.ssh
   chmod 700 ~/.ssh
   # Paste one line from your laptop: output of `cat ~/.ssh/id_rsa.pub`
   echo 'ssh-rsa AAAA... your-comment' >> ~/.ssh/authorized_keys
   chmod 600 ~/.ssh/authorized_keys
   ```

4. From your laptop, confirm SSH and then rerun Terraform:

   ```bash
   ssh -i ~/.ssh/id_rsa ec2-user@<EC2_PUBLIC_IP>
   ```

Keep the old `authorized_keys` entries unless you are sure they are obsolete; appending avoids locking out whoever still has the original key.

3. **AWS credentials for Terraform.** `aws login` credentials work for the AWS CLI but are not picked up by the Terraform AWS provider. In the same shell before any `terraform` command:

   ```bash
   aws login   # if the session expired
   eval "$(aws configure export-credentials --format env)"
   aws sts get-caller-identity
   ```

   Re-run the `eval` after credentials expire (`AWS_CREDENTIAL_EXPIRATION`). Do not put access keys in `main.tf`.

4. **Build the JAR** from this repository root:

   ```bash
   ./mvnw test
   ./mvnw package
   ```

   Confirm `target/demo-0.0.1-SNAPSHOT.jar` exists. That path is hard-coded in `terraform/main.tf`.

5. **Deploy** from `terraform/`:

   ```bash
   terraform apply -replace=null_resource.setup_instance
   ```

   A plain `terraform apply` without `-replace` will not redeploy the app unless the provisioner chain otherwise changes. Prefer `-replace=null_resource.setup_instance` for JAR-only rollouts so unrelated infra is not touched.

6. **Verify on the instance** (SSH in, then inspect systemd / journal / TLS):

   ```bash
   ssh -i ~/.ssh/id_rsa ec2-user@<EC2_PUBLIC_IP>
   sudo systemctl status nba.service
   sudo journalctl -u nba.service -n 100 --no-pager
   sudo certbot certificates
   openssl x509 -in /opt/nba-daily-fantasy-backend/fullchain.pem -noout -dates
   ls -l /opt/nba-daily-fantasy-backend/*.pem   # privkey.pem should be 600
   cat /etc/cron.d/nba-certbot                 # exactly one renew entry
   ```

   Confirm the process is running with `--spring.profiles.active=prod` and that the journal has no SSL keystore errors. Then smoke-test through Discord / `https://picknrolls.click`: OAuth (`/api/activity/token`), one roster create/replace/delete, lock-time (including no-games `404`), cap enforcement, and guild/global leaderboards.

### Production connectivity (symptoms)

- **`picknrolls.click` times out in a browser with no Spring logs:** DNS can be fine (`dig +short picknrolls.click` → EC2 public IP) while **443** has no listener. The app and security group use **8443**; test with `curl -sk https://picknrolls.click:8443/actuator/health`. Discord OAuth redirect is `https://picknrolls.click` (443); production Discord `/api` mappings still need either a 443 reverse proxy or an explicit `:8443` target. `randomicons.me:8443` was a separate **dev** backend for the dev Discord app (not production) and may be down independently—see `activity/AGENTS.md` (**Local Discord testing**).
- **Postgres `password authentication failed for user "postgres"`:** the JVM is using plain `postgres`, not the pooler user, or `myapp.env` / SSM is stale. On the instance: `sudo grep '^DATABASE_USER=' /etc/sysconfig/myapp.env` must show `postgres.<project-ref>`.
- **`prepared statement "S_1" does not exist` / `JDBC commit failed`:** the app is on the transaction pooler (`:6543`) without `prepareThreshold=0`. Confirm the running JAR’s `spring.datasource.url` includes that param (or Hikari `data-source-properties.prepareThreshold=0`), then restart. Alternatives: session-mode pooler or a direct DB host, which keep prepared statements.
- **TLS errors while TCP to 8443 works:** check expiry: `openssl x509 -in /opt/nba-daily-fantasy-backend/fullchain.pem -noout -dates`; renew with Certbot and re-sync PEMs (see verify steps above). Old duplicate renew lines may still live in `/etc/cron.d/nba-certbot` only after the new setup; remove stray `certbot` lines from `/etc/crontab` manually if present from earlier deploys.
- **`/global-protect/prelogin.esp` and similar 404s in the journal:** VPN/scanner probes; ignore unless you are hardening logging.

## Known risks

- Base and production logging default to INFO; development restores DEBUG overrides.
- CORS origins, OAuth redirect, database host, domain, ports, AMI, and paths are hard-coded.
- Terraform allows SSH from `0.0.0.0/0`.
- The EC2 role can read SSM parameters from `Resource = "*"`.
- The provisioning script installs packages imperatively and uses Terraform provisioners for deployment.
- Certbot standalone renew needs port 80 free; the app binds 8443, but anything else on 80 will break renew.
- Terraform opens 80, 22, and 8443 only—not 443—while Discord and browsers default to HTTPS on 443.
- The checked-in `terraform/nba-ubuntu.service` runs the `dev` profile and does not match the service generated by `setup.sh`.
- `BOT_ACCESS_KEY` is fetched by backend provisioning although the active backend does not run the Python bot; verify whether it is obsolete.
- `isTodayLocked()` is named opposite to its implementation: it returns true while lock time is still in the future.
- Some roster GET operations also write guild/channel mappings.
- Exception handling around OAuth uses `@SneakyThrows`; return controlled API errors for network/JSON failures.
- Backend README package paths and slash-command claims are stale.

## Verification

- Run `./mvnw test` and `./mvnw package`. Surefire's default includes run `*Test.java` only.
- `DailyRosterRepositoryIT` and `IsLockedRepositoryIT` hold the database checks for the cap race, inclusive weekly scores, and a missing lock row. Run them explicitly; `./mvnw test` does not pick up `*IT.java`.
- This workspace's current JDK is 25, while the project targets 21. JaCoCo 0.8.11 and the bundled Byte Buddy need `-Djacoco.skip=true` and `-Dnet.bytebuddy.experimental=true` on that JDK. Colima needs `DOCKER_HOST` pointed at its socket and `TESTCONTAINERS_RYUK_DISABLED=true`.
- Unit coverage now includes absent and malformed lock rows, JWT player-id mismatches on writes, the 100/101 cap boundary, and Monday-only plus Monday-through-Sunday weekly windows.
- Still add tests for DST boundaries, malformed Discord token responses, and complete/incomplete rosters.
- Smoke-test OAuth and all Activity endpoints through Discord's proxy in a test guild.
- Confirm production starts with the `prod` profile and that secrets/logs are not exposed.
