CREATE SCHEMA IF NOT EXISTS portfolio;

CREATE TABLE portfolio.portfolio (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    league_id BIGINT NOT NULL,
    CONSTRAINT uq_portfolio_user_league UNIQUE (user_id, league_id)
);

CREATE TABLE portfolio.holdings (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL REFERENCES portfolio.portfolio(id),
    stock_id UUID NOT NULL,
    quantity NUMERIC(19, 4) NOT NULL,
    CONSTRAINT uq_holdings_portfolio_stock UNIQUE (portfolio_id, stock_id)
);

CREATE TABLE portfolio.portfolio_history (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL REFERENCES portfolio.portfolio(id),
    user_id BIGINT NOT NULL,
    league_id BIGINT NOT NULL,
    week_number INTEGER NOT NULL,
    season_type VARCHAR(32) NOT NULL,
    start_value NUMERIC(19, 4) NOT NULL,
    end_value NUMERIC(19, 4) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_portfolio_history_user_league_week_season
        UNIQUE (user_id, league_id, week_number, season_type)
);
