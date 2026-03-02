SET search_path TO market;

CREATE TABLE price_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    player_stock_id UUID NOT NULL REFERENCES player_stock (id),
    season_year INT NOT NULL,
    season_type INT NOT NULL,
    week INT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_price_history_unique ON price_history (
    player_stock_id,
    season_year,
    season_type,
    week
);