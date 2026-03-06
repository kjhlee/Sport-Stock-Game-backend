SET search_path TO league;

CREATE TABLE league_members
(
    id                         BIGSERIAL PRIMARY KEY,
    league_id                  BIGINT      NOT NULL,
    user_id                    BIGINT      NOT NULL,
    role                       VARCHAR(16) NOT NULL,
    joined_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_league_members_league FOREIGN KEY (league_id) REFERENCES leagues (id) ON DELETE CASCADE,
    CONSTRAINT uk_league_members_league_user UNIQUE (league_id, user_id),
    CONSTRAINT ck_league_members_role CHECK (role IN ('OWNER', 'MEMBER'))
);

CREATE INDEX idx_league_members_user_id ON league_members (user_id);
CREATE INDEX idx_league_members_league_id ON league_members (league_id);
