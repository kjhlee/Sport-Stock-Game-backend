SET search_path = scheduler;

CREATE TABLE event_state (
     event_espn_id VARCHAR(50) PRIMARY KEY,
     status VARCHAR(30) NOT NULL,
     locked_at TIMESTAMPTZ,
     finalized_at TIMESTAMPTZ,
     week_number INT NOT NULL,
     season_year INT NOT NULL,
     season_type INT NOT NULL,
     created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
     updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);