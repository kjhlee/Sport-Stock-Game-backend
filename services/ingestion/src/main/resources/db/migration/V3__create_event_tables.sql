SET search_path TO ingestion;

CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    espn_id VARCHAR(15) NOT NULL UNIQUE,
    espn_uid VARCHAR(60),
    name VARCHAR(200),
    short_name VARCHAR(50),
    date TIMESTAMPTZ NOT NULL,
    season_year INTEGER NOT NULL,
    season_type INTEGER,
    season_slug VARCHAR(30),
    week_number INTEGER,
    attendance INTEGER,
    neutral_site BOOLEAN,
    conference_competition BOOLEAN,
    play_by_play_available BOOLEAN,
    status_state VARCHAR(20),
    status_completed BOOLEAN,
    status_description VARCHAR(50),
    status_period INTEGER,
    status_clock DECIMAL(10,1),
    broadcast VARCHAR(100),
    note_headline VARCHAR(200),
    ingested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE event_competitors (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    home_away VARCHAR(5) NOT NULL,
    "order" INTEGER,
    winner BOOLEAN,
    score VARCHAR(10),
    record_summary VARCHAR(20),
    home_record VARCHAR(20),
    road_record VARCHAR(20),
    ingested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_event_competitors_event FOREIGN KEY (event_id) REFERENCES events (id),
    CONSTRAINT fk_event_competitors_team FOREIGN KEY (team_id) REFERENCES teams (id),
    CONSTRAINT uk_event_competitors_event_team UNIQUE (event_id, team_id)
);

CREATE TABLE event_competitor_linescores (
    id BIGSERIAL PRIMARY KEY,
    event_competitor_id BIGINT NOT NULL,
    period INTEGER NOT NULL,
    value DECIMAL(5,1),
    display_value VARCHAR(10),
    CONSTRAINT fk_event_competitor_linescores_competitor FOREIGN KEY (event_competitor_id) REFERENCES event_competitors (id),
    CONSTRAINT uk_event_competitor_linescores_competitor_period UNIQUE (event_competitor_id, period)
);
