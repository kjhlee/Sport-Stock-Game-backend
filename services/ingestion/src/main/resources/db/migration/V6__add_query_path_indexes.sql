SET search_path TO ingestion;

CREATE INDEX idx_team_roster_entries_team_id_season_year
    ON team_roster_entries (team_id, season_year);

CREATE INDEX idx_team_roster_entries_athlete_id_season_year
    ON team_roster_entries (athlete_id, season_year);

CREATE INDEX idx_events_season_year_status_completed
    ON events (season_year, status_completed);

CREATE INDEX idx_events_season_year_date
    ON events (season_year, date);

CREATE INDEX idx_athletes_position_abbreviation_full_name
    ON athletes (position_abbreviation, full_name);

CREATE INDEX idx_teams_display_name
    ON teams (display_name);
