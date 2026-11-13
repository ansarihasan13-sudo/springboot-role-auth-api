-- Convert users.id from BIGINT to UUID
ALTER TABLE users
    ALTER COLUMN id DROP IDENTITY IF EXISTS;

ALTER TABLE users
ALTER COLUMN id TYPE UUID
    USING gen_random_uuid();

-- Convert employees.id from BIGINT to UUID
ALTER TABLE employees
    ALTER COLUMN id DROP IDENTITY IF EXISTS;

ALTER TABLE employees
ALTER COLUMN id TYPE UUID
    USING gen_random_uuid();

-- Add password_hash to employees
ALTER TABLE employees
    ADD COLUMN password_hash VARCHAR(255) NOT NULL;

-- Create admins table
CREATE TABLE admins (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        name VARCHAR(100) NOT NULL,
                        email VARCHAR(150) NOT NULL UNIQUE,
                        password_hash VARCHAR(255) NOT NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);