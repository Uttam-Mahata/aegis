-- Connect to aegis_security database
\c aegis_security

-- Create registration_keys table
CREATE TABLE IF NOT EXISTS registration_keys (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(100) UNIQUE NOT NULL,
    registration_key VARCHAR(512) UNIQUE NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT true,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create devices table
CREATE TABLE IF NOT EXISTS devices (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(255) UNIQUE NOT NULL,
    client_id VARCHAR(100) NOT NULL,
    secret_key VARCHAR(512) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    last_seen TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_client_id ON devices(client_id);

-- Grant permissions to aegis user
GRANT ALL PRIVILEGES ON TABLE registration_keys TO aegis;
GRANT ALL PRIVILEGES ON TABLE devices TO aegis;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO aegis;

-- Insert registration key
INSERT INTO registration_keys (client_id, registration_key, description, is_active)
VALUES (
    'UCOBANK_PROD_ANDROID',
    'ucobank_registration_key_2025',
    'UCO Bank Android App Production Key - Hackathon Demo',
    true
) ON CONFLICT (client_id) DO NOTHING;

-- Verify
SELECT * FROM registration_keys;
SELECT * FROM devices;