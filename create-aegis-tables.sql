-- Connect to aegis_security database
\c aegis_security

-- Create registration_keys table
CREATE TABLE IF NOT EXISTS registration_keys (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(100) UNIQUE NOT NULL,
    registration_key VARCHAR(512) UNIQUE NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Create devices table
CREATE TABLE IF NOT EXISTS devices (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(100) UNIQUE NOT NULL,
    client_id VARCHAR(100) NOT NULL,
    secret_key VARCHAR(512) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    last_seen TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_device_client_id ON devices(client_id);

-- Insert registration key for UCO Bank
INSERT INTO registration_keys (client_id, registration_key, description, is_active)
VALUES (
    'UCOBANK_PROD_ANDROID',
    'ucobank_registration_key_2025',
    'UCO Bank Android App Production Key - Hackathon Demo',
    true
) ON CONFLICT (client_id) DO NOTHING;

-- Verify
SELECT 'Tables created successfully' as status;
SELECT * FROM registration_keys;