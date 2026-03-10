SET search_path TO league;

CREATE TABLE league_invites
(
    id         BIGSERIAL PRIMARY KEY,
    league_id  BIGINT      NOT NULL,
    code       VARCHAR(64) NOT NULL,
    created_by BIGINT      NOT NULL,
    expires_at TIMESTAMPTZ,
    max_uses   INT CHECK (max_uses IS NULL OR max_uses > 0),
    uses_count INT         NOT NULL DEFAULT 0 CHECK (uses_count >= 0),
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_league_invites_league FOREIGN KEY (league_id) REFERENCES leagues (id) ON DELETE CASCADE,
    CONSTRAINT uk_league_invites_code UNIQUE (code),
    CONSTRAINT ck_league_invites_usage_limit CHECK (max_uses IS NULL OR uses_count <= max_uses)
);

CREATE INDEX idx_league_invites_league_id ON league_invites (league_id);
CREATE INDEX idx_league_invites_expires_at ON league_invites (expires_at);
