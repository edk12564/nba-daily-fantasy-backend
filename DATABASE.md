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
