CREATE SCHEMA IF NOT EXISTS market;

SET search_path TO market;

CREATE TABLE stock (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   espn_id VARCHAR(15) NOT NULL,
   full_name VARCHAR(255) NOT NULL,
   position VARCHAR(10) NOT NULL,
   type VARCHAR(20) NOT NULL DEFAULT 'PLAYER',
   team_espn_id VARCHAR(15),
   current_price DECIMAL(10, 2) NOT NULL DEFAULT 0,
   status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
   game_locked BOOLEAN NOT NULL DEFAULT false,
   injury_locked BOOLEAN NOT NULL DEFAULT false,
   price_updated_at TIMESTAMPTZ,
   created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
   updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
   CONSTRAINT ck_stock_type CHECK (type IN ('PLAYER', 'TEAM_DEFENSE')),
   CONSTRAINT ck_stock_status CHECK (status IN ('ACTIVE', 'DELISTED')),
   CONSTRAINT uk_stock_espn_id_type UNIQUE (espn_id, type)
);