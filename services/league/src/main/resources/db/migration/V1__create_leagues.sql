CREATE SCHEMA IF NOT EXISTS league;
SET search_path TO league;

CREATE TABLE leagues
(
    id                     BIGSERIAL PRIMARY KEY,
    owner_user_id          BIGINT         NOT NULL,
    name                   VARCHAR(255)   NOT NULL,
    status                 VARCHAR(16)    NOT NULL DEFAULT 'INACTIVE',
    max_members            INT            NOT NULL CHECK (max_members > 1),
    season_start_at        TIMESTAMPTZ    NOT NULL,
    season_end_at          TIMESTAMPTZ    NOT NULL,
    initial_stipend_amount NUMERIC(19, 4) NOT NULL CHECK (initial_stipend_amount > 0),
    weekly_stipend_amount  NUMERIC(19, 4) NOT NULL CHECK (weekly_stipend_amount > 0),
    started_at             TIMESTAMPTZ,
    initial_stipend_issued_at TIMESTAMPTZ,
    initial_stipend_status VARCHAR(20) NOT NULL DEFAULT 'NOT_APPLICABLE',
    created_at             TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_leagues_status CHECK (status IN ('INACTIVE', 'ACTIVE', 'ARCHIVED')),
    CONSTRAINT ck_leagues_season_range CHECK (season_end_at > season_start_at)
);

CREATE INDEX idx_leagues_owner_user_id ON leagues (owner_user_id);
CREATE INDEX idx_leagues_status ON leagues (status);
CREATE INDEX idx_leagues_season_start_at ON leagues (season_start_at);
