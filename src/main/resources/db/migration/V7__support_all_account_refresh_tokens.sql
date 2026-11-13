
-- 1. Remove old foreign key to users table
ALTER TABLE refresh_tokens
DROP CONSTRAINT fk_refresh_token_user;

-- 2. Rename user_id to account_id
ALTER TABLE refresh_tokens
    RENAME COLUMN user_id TO account_id;

-- 3. Add account_type.
-- Existing records automatically get USER.
ALTER TABLE refresh_tokens
    ADD COLUMN account_type VARCHAR(20) NOT NULL DEFAULT 'USER';

-- 4. Remove default because new records
-- will explicitly provide their account type.
ALTER TABLE refresh_tokens
    ALTER COLUMN account_type DROP DEFAULT;

-- 5. Rename existing index
ALTER INDEX idx_refresh_tokens_user_id
RENAME TO idx_refresh_tokens_account_id;

-- 6. Add composite index for account lookup
CREATE INDEX idx_refresh_tokens_account_type_account_id
    ON refresh_tokens(account_type, account_id);
