CREATE SCHEMA IF NOT EXISTS market;

SET search_path TO market;

CREATE TABLE player_stock (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    athlete_espn_id VARCHAR(15) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    position VARCHAR(10) NOT NULL,
    team_espn_id VARCHAR(15),
    current_price DECIMAL(10, 2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    price_updated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);