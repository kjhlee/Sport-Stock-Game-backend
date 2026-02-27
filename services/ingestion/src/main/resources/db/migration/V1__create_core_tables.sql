CREATE SCHEMA IF NOT EXISTS ingestion;
SET search_path TO ingestion;

CREATE TABLE teams
(
    id                 BIGSERIAL PRIMARY KEY,
    espn_id            VARCHAR(10)  NOT NULL UNIQUE,
    espn_uid           VARCHAR(50),
    slug               VARCHAR(100),
    abbreviation       VARCHAR(10)  NOT NULL,
    display_name       VARCHAR(100) NOT NULL,
    short_display_name VARCHAR(50),
    name               VARCHAR(50),
    nickname           VARCHAR(50),
    location           VARCHAR(50),
    color              VARCHAR(10),
    alternate_color    VARCHAR(10),
    is_active          BOOLEAN,
    is_all_star        BOOLEAN,
    logo_url           VARCHAR(500),
    franchise_id       VARCHAR(50),
    division_id        VARCHAR(50),
    conference_id      VARCHAR(50),
    standing_summary   VARCHAR(50),
    raw_json           JSONB,
    ingested_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE seasons
(
    id                       BIGSERIAL PRIMARY KEY,
    year                     INTEGER     NOT NULL,
    start_date               TIMESTAMPTZ,
    end_date                 TIMESTAMPTZ,
    display_name             VARCHAR(20),
    season_type_id           VARCHAR(5),
    season_type_name         VARCHAR(30),
    season_type_abbreviation VARCHAR(10),
    raw_json                 JSONB,
    ingested_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_seasons_year_type UNIQUE (year, season_type_id)
);

CREATE TABLE season_weeks
(
    id                BIGSERIAL PRIMARY KEY,
    season_id         BIGINT      NOT NULL,
    season_type_value VARCHAR(5)  NOT NULL,
    week_value        VARCHAR(5)  NOT NULL,
    label             VARCHAR(50),
    alternate_label   VARCHAR(50),
    detail            VARCHAR(50),
    start_date        TIMESTAMPTZ,
    end_date          TIMESTAMPTZ,
    ingested_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_season_weeks_season FOREIGN KEY (season_id) REFERENCES seasons (id),
    CONSTRAINT uk_season_weeks_season_type_week UNIQUE (season_id, season_type_value, week_value)
);

CREATE TABLE athletes
(
    id                           BIGSERIAL PRIMARY KEY,
    espn_id                      VARCHAR(15)  NOT NULL UNIQUE,
    espn_uid                     VARCHAR(50),
    espn_guid                    VARCHAR(60),
    first_name                   VARCHAR(100),
    last_name                    VARCHAR(100),
    full_name                    VARCHAR(200) NOT NULL,
    display_name                 VARCHAR(200),
    short_name                   VARCHAR(100),
    slug                         VARCHAR(200),
    weight                       DECIMAL(6, 1),
    height                       DECIMAL(5, 1),
    age                          INTEGER,
    date_of_birth                TIMESTAMPTZ,
    debut_year                   INTEGER,
    jersey                       VARCHAR(5),
    position_id                  VARCHAR(10),
    position_name                VARCHAR(50),
    position_abbreviation        VARCHAR(10),
    position_parent_name         VARCHAR(50),
    position_parent_abbreviation VARCHAR(10),
    birth_city                   VARCHAR(100),
    birth_state                  VARCHAR(10),
    birth_country                VARCHAR(50),
    college_espn_id              VARCHAR(20),
    college_name                 VARCHAR(100),
    college_abbreviation         VARCHAR(10),
    headshot_url                 VARCHAR(500),
    experience_years             INTEGER,
    status_id                    VARCHAR(5),
    status_name                  VARCHAR(30),
    status_type                  VARCHAR(30),
    hand_type                    VARCHAR(10),
    alternate_ids_sdr            VARCHAR(20),
    raw_json                     JSONB,
    ingested_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at                   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE coaches
(
    id          BIGSERIAL PRIMARY KEY,
    espn_id     VARCHAR(15) NOT NULL,
    first_name  VARCHAR(100),
    last_name   VARCHAR(100),
    team_id     BIGINT,
    season_year INTEGER,
    raw_json    JSONB,
    ingested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_coaches_team FOREIGN KEY (team_id) REFERENCES teams (id),
    CONSTRAINT uk_coaches_espn_team_season UNIQUE (espn_id, team_id, season_year)
);
