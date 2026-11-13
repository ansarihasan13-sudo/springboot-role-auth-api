CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,

    token_hash VARCHAR(64) NOT NULL UNIQUE,

   token_family_id UUID NOT NULL,

   expires_at TIMESTAMPTZ NOT NULL,

   revoked_at TIMESTAMPTZ,

   replaced_by UUID,

   created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

   CONSTRAINT fk_refresh_token_user
   FOREIGN KEY (user_id)
   REFERENCES users(id)
   ON DELETE CASCADE,

  CONSTRAINT fk_refresh_token_replacement
  FOREIGN KEY (replaced_by)
  REFERENCES refresh_tokens(id)
);

CREATE INDEX idx_refresh_tokens_user_id
    ON refresh_tokens(user_id);

CREATE INDEX idx_refresh_tokens_family
    ON refresh_tokens(token_family_id);

CREATE INDEX idx_refresh_tokens_expiry
    ON refresh_tokens(expires_at);