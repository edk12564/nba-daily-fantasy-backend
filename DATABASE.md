# Database Contract and Rollover Runbook

Production PostgreSQL already contains the constraints and stat columns required by the Python loaders. The checked-in test schema is maintained as the executable documentation of that production contract.

The 2026-27 rollover does not require deleting, truncating, or recreating any table. Existing date-keyed rows remain prior-season history.

## Expected `nba_players` contract

- `nba_player_uid` is the row primary key.
- `(nba_player_id, date)` is unique so the daily player loader can use `ON CONFLICT`.
- Daily scoring updates use `fantasy_score`, `pts`, `reb`, `ast`, `blk`, `tov`, and `stl`.
- Projection loading uses `avg_pts`, `avg_reb`, `avg_ast`, `avg_blk`, `avg_tov`, `avg_stl`, `status`, `position`, `against_team`, `team_id`, `jersey_num`, and `dollar_value`.

## Pre-deployment verification

Back up the database, then verify the live contract without changing it:

```sql
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'nba_players'
ORDER BY ordinal_position;

SELECT indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND tablename = 'nba_players';

SELECT nba_player_id, date, COUNT(*)
FROM nba_players
GROUP BY nba_player_id, date
HAVING COUNT(*) > 1;
```

The duplicate query must return no rows. Compare the column and index output with `src/test/resources/schema.sql` and the SQL used by the Python loaders.

## Date and timezone contract

- Roster and player `date` values represent the Pacific product day.
- NBA `gameDateEst` is an Eastern schedule label and must not be treated as a timestamp.
- Store lock timestamps as `TIMESTAMP WITH TIME ZONE`.
- Use UTC as the database session timezone and convert only at display or product-day boundaries.
- The frontend and backend derive the product day with `America/Los_Angeles`.
- Bot Central-time display must use `America/Chicago`, not a fixed offset.

## Future schema changes

Use additive, reviewed SQL only:

- Add missing nullable columns or constraints after validating existing rows.
- Never drop/truncate historical tables for a season rollover.
- Test migrations against a production-shaped non-production database.
- Record the SQL, prerequisites, verification query, and rollback procedure in version control before applying it.
- Apply production changes separately after explicit approval and a verified backup.

## Gacha MVP tables (additive)

Cosmetics-only gacha. Fantasy scoring, salary cap, and lock rules are unchanged. See `docs/GACHA_MVP.md`.

### Prerequisites

- Verified backup
- Postgres with `gen_random_uuid()` available

### Apply SQL

```sql
CREATE TABLE IF NOT EXISTS gacha_skins (
  skin_id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  nba_player_id     BIGINT NOT NULL,
  rarity            TEXT NOT NULL CHECK (rarity IN ('COMMON','RARE','EPIC','LEGENDARY')),
  art_url           TEXT NOT NULL,
  frame_key         TEXT NOT NULL,
  season_label      TEXT,
  UNIQUE (nba_player_id, rarity)
);

CREATE TABLE IF NOT EXISTS gacha_user_wallet (
  discord_player_id TEXT PRIMARY KEY,
  rolls             INT NOT NULL DEFAULT 1000 CHECK (rolls >= 0),
  dust              INT NOT NULL DEFAULT 0 CHECK (dust >= 0),
  pity_counter      INT NOT NULL DEFAULT 0 CHECK (pity_counter >= 0),
  streak_days       INT NOT NULL DEFAULT 0,
  streak_last_date  DATE,
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS gacha_user_skins (
  discord_player_id TEXT NOT NULL,
  skin_id           UUID NOT NULL REFERENCES gacha_skins(skin_id),
  acquired_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (discord_player_id, skin_id)
);

CREATE TABLE IF NOT EXISTS gacha_token_grants (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  discord_player_id TEXT NOT NULL,
  reason            TEXT NOT NULL,
  period_key        TEXT NOT NULL,
  amount            INT NOT NULL,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (discord_player_id, reason, period_key)
);
```

### Incremental catalog seed

`nba_players` is a per-day slate. After each daily player load, upsert skins for newly seen ids (safe to re-run):

```sql
INSERT INTO gacha_skins (nba_player_id, rarity, art_url, frame_key, season_label)
SELECT
  p.nba_player_id,
  r.rarity,
  'headshots/nba/latest/260x190/' || p.nba_player_id || '.png',
  r.frame_key,
  '2026-27'
FROM (SELECT DISTINCT nba_player_id FROM nba_players) p
CROSS JOIN (VALUES
  ('COMMON', 'frame_common'),
  ('RARE', 'frame_rare'),
  ('EPIC', 'frame_epic'),
  ('LEGENDARY', 'frame_legendary')
) AS r(rarity, frame_key)
ON CONFLICT (nba_player_id, rarity) DO NOTHING;
```

Script: `scripts/src/seed_gacha_skins.py` (invoked from `load-todays-players.py`).

### Verify

```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public' AND table_name LIKE 'gacha_%'
ORDER BY 1;

SELECT rarity, COUNT(*) FROM gacha_skins GROUP BY rarity ORDER BY 1;
```

### Rollback

Only if empty / you accept dropping ownership:

```sql
DROP TABLE IF EXISTS gacha_token_grants;
DROP TABLE IF EXISTS gacha_user_skins;
DROP TABLE IF EXISTS gacha_user_wallet;
DROP TABLE IF EXISTS gacha_skins;
```
