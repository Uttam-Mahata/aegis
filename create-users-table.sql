-- Connect to aegis_security database
\c aegis_security

-- Create users table for JWT authentication
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    active BOOLEAN NOT NULL DEFAULT true,
    address VARCHAR(255),
    contact_person VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    email VARCHAR(255) NOT NULL,
    last_login TIMESTAMP,
    name VARCHAR(255) NOT NULL,
    organization VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(255),
    role VARCHAR(50) NOT NULL CHECK (role IN ('ADMIN','USER')),
    updated_at TIMESTAMP,
    CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email)
);

-- Create index on email for faster lookups
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Grant all permissions to aegis user
GRANT ALL PRIVILEGES ON TABLE users TO aegis;
GRANT USAGE, SELECT ON SEQUENCE users_id_seq TO aegis;

-- Verify table creation
\dt users

-- Display table structure
\d users