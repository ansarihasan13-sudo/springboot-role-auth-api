-- 1. Create roles table
CREATE TABLE roles (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       role_name VARCHAR(50) NOT NULL UNIQUE
);


INSERT INTO roles (role_name)
VALUES
    ('ADMIN'),
    ('EMPLOYEE'),
    ('USER');



ALTER TABLE admins
    ADD COLUMN role_id UUID;

UPDATE admins
SET role_id = (
    SELECT id
    FROM roles
    WHERE role_name = 'ADMIN'
);

ALTER TABLE admins
    ALTER COLUMN role_id SET NOT NULL;

ALTER TABLE admins
    ADD CONSTRAINT fk_admins_role
        FOREIGN KEY (role_id)
            REFERENCES roles(id);



ALTER TABLE employees
    ADD COLUMN role_id UUID;

UPDATE employees
SET role_id = (
    SELECT id
    FROM roles
    WHERE role_name = 'EMPLOYEE'
);

ALTER TABLE employees
    ALTER COLUMN role_id SET NOT NULL;

ALTER TABLE employees
    ADD CONSTRAINT fk_employees_role
        FOREIGN KEY (role_id)
            REFERENCES roles(id);



ALTER TABLE employees
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;



ALTER TABLE users
    ADD COLUMN role_id UUID;

UPDATE users
SET role_id = (
    SELECT id
    FROM roles
    WHERE role_name = 'USER'
);

ALTER TABLE users
    ALTER COLUMN role_id SET NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT fk_users_role
        FOREIGN KEY (role_id)
            REFERENCES roles(id);
ALTER TABLE users
    DROP COLUMN role;