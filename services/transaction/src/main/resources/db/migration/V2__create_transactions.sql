SET search_path TO transaction;

CREATE TABLE transactions (
    id                  BIGSERIAL PRIMARY KEY,
    wallet_id           BIGINT         NOT NULL REFERENCES wallets(id),
    type                VARCHAR(32)    NOT NULL,
    amount              NUMERIC(19, 4) NOT NULL CHECK (amount > 0),
    balance_before      NUMERIC(19, 4) NOT NULL,
    balance_after       NUMERIC(19, 4) NOT NULL,
    league_id           BIGINT         NOT NULL,
    user_id             BIGINT         NOT NULL,
    reference_id        VARCHAR(255),
    description         VARCHAR(512),
    idempotency_key     VARCHAR(255),
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_transactions_type CHECK (type IN (
                                                  'INITIAL_STIPEND', 'WEEKLY_STIPEND', 'STOCK_BUY', 'STOCK_SELL', 'ADJUSTMENT'
      ))
);

CREATE INDEX idx_transactions_wallet_id ON transactions (wallet_id);
CREATE INDEX idx_transactions_league_id ON transactions (league_id);
CREATE INDEX idx_transactions_user_id ON transactions (user_id);
CREATE INDEX idx_transactions_type ON transactions (type);
CREATE INDEX idx_transactions_created_at ON transactions (created_at);
CREATE INDEX idx_transactions_user_league ON transactions (user_id, league_id, created_at DESC);
CREATE UNIQUE INDEX idx_transactions_idempotency_key ON transactions (idempotency_key)
    WHERE idempotency_key IS NOT NULL;