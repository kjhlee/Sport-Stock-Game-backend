CREATE SCHEMA IF NOT EXISTS portfolio;

CREATE TABLE portfolio.portfolio (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    league_id BIGINT NOT NULL
);

CREATE TABLE portfolio.holdings (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL REFERENCES portfolio.portfolio(id),
    stock_id UUID NOT NULL,
    quantity NUMERIC(19, 4) NOT NULL,
    avg_cost_basis NUMERIC(19, 4) NOT NULL
);
