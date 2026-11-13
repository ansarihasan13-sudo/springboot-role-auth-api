CREATE TABLE account_email_registry (
    email VARCHAR(150) PRIMARY KEY,
    account_type VARCHAR(20) NOT NULL,
    account_id UUID NOT NULL,

    CONSTRAINT chk_account_email_registry_type
    CHECK (account_type IN ('ADMIN', 'EMPLOYEE', 'USER')),
    CONSTRAINT uq_account_email_registry_account
    UNIQUE (account_type, account_id)
);

-- Existing Admin accounts
INSERT INTO account_email_registry (email, account_type, account_id)
SELECT LOWER(BTRIM(email)), 'ADMIN', id
FROM admins;

-- Existing Employee accounts
INSERT INTO account_email_registry (email, account_type, account_id)
SELECT LOWER(BTRIM(email)), 'EMPLOYEE', id
FROM employees;

-- Existing User accounts
INSERT INTO account_email_registry (email, account_type, account_id)
SELECT LOWER(BTRIM(email)), 'USER', id
FROM users;