SET search_path TO ingestion;

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