SET search_path TO transaction;

CREATE TABLE wallets (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT         NOT NULL,
    league_id       BIGINT         NOT NULL,
    balance         NUMERIC(19, 4) NOT NULL DEFAULT 0.0000 CHECK (balance >= 0),
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_wallets_user_league UNIQUE (user_id, league_id)
);

CREATE INDEX idx_wallets_user_id ON wallets (user_id);
CREATE INDEX idx_wallets_league_id ON wallets (league_id);

-- auto-update updated_at trigger (same pattern as league schema)
CREATE OR REPLACE FUNCTION transaction.update_updated_at()
    RETURNS TRIGGER AS $$ BEGIN NEW.updated_at = NOW(); RETURN NEW; END; $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_wallets_updated_at BEFORE UPDATE ON wallets
    FOR EACH ROW EXECUTE FUNCTION transaction.update_updated_at();