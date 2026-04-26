SET search_path TO ingestion;

CREATE TABLE boxscore_team_stats (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    home_away VARCHAR(5) NOT NULL,
    stat_name VARCHAR(50) NOT NULL,
    stat_value VARCHAR(20),
    display_value VARCHAR(20),
    label VARCHAR(50),
    CONSTRAINT fk_boxscore_team_stats_event FOREIGN KEY (event_id) REFERENCES events (id),
    CONSTRAINT fk_boxscore_team_stats_team FOREIGN KEY (team_id) REFERENCES teams (id),
    CONSTRAINT uk_boxscore_team_stats_event_team_stat UNIQUE (event_id, team_id, stat_name)
);

CREATE TABLE player_game_stats (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    athlete_id BIGINT NOT NULL,
    athlete_espn_id VARCHAR(15) NOT NULL,
    team_id BIGINT NOT NULL,
    stat_category VARCHAR(30) NOT NULL,
    stats JSONB NOT NULL,
    ingested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_player_game_stats_event FOREIGN KEY (event_id) REFERENCES events (id),
    CONSTRAINT fk_player_game_stats_athlete FOREIGN KEY (athlete_id) REFERENCES athletes (id),
    CONSTRAINT fk_player_game_stats_team FOREIGN KEY (team_id) REFERENCES teams (id),
    CONSTRAINT uk_player_game_stats_event_athlete_category UNIQUE (event_id, athlete_espn_id, stat_category)
);

CREATE TABLE fantasy_snapshot (
      id                          BIGSERIAL PRIMARY KEY,
      event_id                    BIGINT NOT NULL,
      subject_type                VARCHAR(20) NOT NULL,
      espn_id                     VARCHAR(15) NOT NULL,
      full_name                   VARCHAR(255) NOT NULL,
      projected_stats             JSONB,
      projected_fantasy_points    DECIMAL(10,2),
      actual_fantasy_points       DECIMAL(10,2),
      completed                   BOOLEAN NOT NULL DEFAULT false,
      ingested_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      CONSTRAINT fk_fantasy_snapshot_event FOREIGN KEY (event_id) REFERENCES events(id),
      CONSTRAINT ck_fantasy_snapshot_type CHECK (subject_type IN ('PLAYER', 'TEAM_DEFENSE')),
      CONSTRAINT uk_fantasy_snapshot_event_subject UNIQUE (event_id, subject_type, espn_id)
);
