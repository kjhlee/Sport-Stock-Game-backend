SET search_path TO ingestion;

CREATE TABLE raw_api_responses (
    id BIGSERIAL PRIMARY KEY,
    endpoint VARCHAR(100) NOT NULL,
    url VARCHAR(500) NOT NULL,
    response_body JSONB NOT NULL,
    http_status INTEGER,
    fetched_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_teams_abbreviation ON teams (abbreviation);

CREATE INDEX idx_athletes_position_abbreviation ON athletes (position_abbreviation);
CREATE INDEX idx_athletes_full_name ON athletes (full_name);

CREATE INDEX idx_team_roster_entries_season_year ON team_roster_entries (season_year);

CREATE INDEX idx_events_season_year_week_number ON events (season_year, week_number);
CREATE INDEX idx_events_date ON events (date);

CREATE INDEX idx_event_competitors_team_id ON event_competitors (team_id);

CREATE INDEX idx_boxscore_team_stats_event_id ON boxscore_team_stats (event_id);

CREATE INDEX idx_player_game_stats_athlete_id ON player_game_stats (athlete_id);
CREATE INDEX idx_player_game_stats_event_id_team_id ON player_game_stats (event_id, team_id);

CREATE INDEX idx_raw_api_responses_endpoint ON raw_api_responses (endpoint);
CREATE INDEX idx_raw_api_responses_fetched_at ON raw_api_responses (fetched_at);
