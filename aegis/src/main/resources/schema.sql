-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    active BOOLEAN DEFAULT true,
    address VARCHAR(255),
    contact_person VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    email VARCHAR(255) NOT NULL UNIQUE,
    last_login TIMESTAMP NULL,
    name VARCHAR(255) NOT NULL,
    organization VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(255),
    role VARCHAR(50) NOT NULL CHECK (role IN ('ADMIN','USER')),
    approval_status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (approval_status IN ('PENDING','APPROVED','REJECTED')),
    approved_at TIMESTAMP NULL,
    approved_by VARCHAR(255),
    updated_at TIMESTAMP NULL
);

-- Create index on email (if not exists)
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Create registration_keys table
CREATE TABLE IF NOT EXISTS registration_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id VARCHAR(100) NOT NULL UNIQUE,
    registration_key VARCHAR(512) NOT NULL UNIQUE,
    description VARCHAR(255),
    organization VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    expires_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create indexes for registration_keys
CREATE INDEX IF NOT EXISTS idx_registration_keys_client_id ON registration_keys(client_id);
CREATE INDEX IF NOT EXISTS idx_registration_keys_registration_key ON registration_keys(registration_key);

-- Drop the old table if it exists to recreate with new structure
DROP TABLE IF EXISTS devices;

-- Create devices table with composite primary key
CREATE TABLE devices (
    device_id VARCHAR(255) NOT NULL,
    client_id VARCHAR(100) NOT NULL,
    secret_key VARCHAR(512) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'TEMPORARILY_BLOCKED', 'PERMANENTLY_BLOCKED')),
    last_seen TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (device_id, client_id)
);

-- Create indexes for devices (no unique constraint on device_id alone)
CREATE INDEX idx_devices_device_id ON devices(device_id);
CREATE INDEX idx_devices_client_id ON devices(client_id);
CREATE INDEX idx_devices_status ON devices(status);


-- Device Fingerprints table for fraud detection
CREATE TABLE IF NOT EXISTS device_fingerprints (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(255) NOT NULL,
    hardware_hash VARCHAR(512) NOT NULL,
    composite_hash VARCHAR(512) NOT NULL,
    is_fraudulent BOOLEAN DEFAULT false,
    fraud_reason VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create indexes for device_fingerprints
CREATE INDEX IF NOT EXISTS idx_fingerprint_device_id ON device_fingerprints(device_id);
CREATE INDEX IF NOT EXISTS idx_fingerprint_hardware_hash ON device_fingerprints(hardware_hash);
CREATE INDEX IF NOT EXISTS idx_fingerprint_composite_hash ON device_fingerprints(composite_hash);
CREATE INDEX IF NOT EXISTS idx_fingerprint_fraudulent ON device_fingerprints(is_fraudulent);