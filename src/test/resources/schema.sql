-- PostgreSQL schema for tests

DROP TABLE IF EXISTS daily_roster CASCADE;
DROP TABLE IF EXISTS nba_players CASCADE;
DROP TABLE IF EXISTS teams CASCADE;
DROP TABLE IF EXISTS is_locked CASCADE;
DROP TABLE IF EXISTS discord_player_guilds CASCADE;
DROP TABLE IF EXISTS discord_channels CASCADE;
DROP TYPE IF EXISTS daily_roster_position CASCADE;

-- Create enum type for roster positions
CREATE TYPE daily_roster_position AS ENUM ('PG', 'SG', 'SF', 'PF', 'C');

CREATE TABLE teams (
    team_id INT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    abbr VARCHAR(16)
);

CREATE TABLE nba_players (
    nba_player_uid UUID PRIMARY KEY,
    nba_player_id INT,
    name VARCHAR(255) NOT NULL,
    date VARCHAR(32),
    position VARCHAR(8),
    against_team INT,
    dollar_value INT,
    fantasy_score DOUBLE PRECISION,
    team_id INT,
    status VARCHAR(32),
    jersey_num VARCHAR(8),
    CONSTRAINT fk_team FOREIGN KEY (team_id) REFERENCES teams(team_id)
);

CREATE TABLE daily_roster (
    discord_player_id VARCHAR(64) NOT NULL,
    nba_player_uid UUID NOT NULL,
    date DATE NOT NULL,
    nickname VARCHAR(64),
    position daily_roster_position NOT NULL,
    PRIMARY KEY (discord_player_id, date, position)
);

CREATE TABLE is_locked (
    date DATE PRIMARY KEY,
    lock_time TIMESTAMP WITH TIME ZONE
);

CREATE TABLE discord_player_guilds (
    discord_player_id VARCHAR(64) NOT NULL,
    guild_id VARCHAR(64) NOT NULL,
    PRIMARY KEY (discord_player_id, guild_id)
);

CREATE TABLE discord_channels (
    channel_id VARCHAR(64) NOT NULL,
    date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    guild_id VARCHAR(64) NOT NULL,
    PRIMARY KEY (channel_id, date)
);

-- Insert test data

-- Teams
INSERT INTO teams (team_id, name, abbr) VALUES
(1, 'Los Angeles Lakers', 'LAL'),
(2, 'Boston Celtics', 'BOS'),
(3, 'Golden State Warriors', 'GSW'),
(4, 'Denver Nuggets', 'DEN'),
(5, 'Miami Heat', 'MIA');

-- NBA Players
INSERT INTO nba_players (nba_player_uid, nba_player_id, name, date, position, against_team, dollar_value, fantasy_score, team_id, status, jersey_num) VALUES
('11111111-1111-1111-1111-111111111111', 1, 'LeBron James', '2025-12-25', 'SF', 2, 12000, 55.5, 1, 'ACTIVE', '23'),
('22222222-2222-2222-2222-222222222222', 2, 'Stephen Curry', '2025-12-25', 'PG', 4, 11500, 48.2, 3, 'ACTIVE', '30'),
('33333333-3333-3333-3333-333333333333', 3, 'Nikola Jokic', '2025-12-25', 'C', 3, 13000, 62.1, 4, 'ACTIVE', '15'),
('44444444-4444-4444-4444-444444444444', 4, 'Jimmy Butler', '2025-12-25', 'SF', 1, 10500, 42.8, 5, 'ACTIVE', '22'),
('55555555-5555-5555-5555-555555555555', 5, 'Jayson Tatum', '2025-12-25', 'SF', 1, 11000, 51.3, 2, 'ACTIVE', '0');

-- Is Locked
INSERT INTO is_locked (date, lock_time) VALUES
('2025-12-25', '2025-12-25 19:00:00-05:00');

-- Discord Player Guilds
INSERT INTO discord_player_guilds (discord_player_id, guild_id) VALUES
('player-1', 'guild-1'),
('player-2', 'guild-1');

-- Discord Channels
INSERT INTO discord_channels (channel_id, date, guild_id) VALUES
('chan-1', '2025-12-25', 'guild-1');

-- Daily Roster
INSERT INTO daily_roster (discord_player_id, nba_player_uid, date, nickname, position) VALUES
('player-1', '11111111-1111-1111-1111-111111111111', '2025-12-25', 'King', 'SF'),
('player-1', '22222222-2222-2222-2222-222222222222', '2025-12-25', 'Chef', 'PG');
