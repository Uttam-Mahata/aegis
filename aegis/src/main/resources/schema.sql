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

-- Policy Engine Tables
CREATE TABLE IF NOT EXISTS policies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id VARCHAR(100) NOT NULL,
    policy_name VARCHAR(255) NOT NULL,
    policy_type VARCHAR(50) NOT NULL,
    enforcement_level VARCHAR(50) NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS policy_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    policy_id BIGINT NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    condition_field VARCHAR(100) NOT NULL,
    operator VARCHAR(50) NOT NULL,
    condition_value VARCHAR(2048) NOT NULL,
    error_message VARCHAR(512),
    priority INTEGER DEFAULT 100,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (policy_id) REFERENCES policies(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS policy_violations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(255) NOT NULL,
    policy_id BIGINT NOT NULL,
    rule_id BIGINT,
    action_taken VARCHAR(50) NOT NULL,
    request_details TEXT,
    violation_details TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (policy_id) REFERENCES policies(id),
    FOREIGN KEY (rule_id) REFERENCES policy_rules(id)
);

-- Policy indexes
CREATE INDEX IF NOT EXISTS idx_policy_client_id ON policies(client_id);
CREATE INDEX IF NOT EXISTS idx_policy_type ON policies(policy_type);
CREATE INDEX IF NOT EXISTS idx_policy_active ON policies(is_active);

CREATE INDEX IF NOT EXISTS idx_rule_policy_id ON policy_rules(policy_id);
CREATE INDEX IF NOT EXISTS idx_rule_active ON policy_rules(is_active);

CREATE INDEX IF NOT EXISTS idx_violation_device_id ON policy_violations(device_id);
CREATE INDEX IF NOT EXISTS idx_violation_policy_id ON policy_violations(policy_id);
CREATE INDEX IF NOT EXISTS idx_violation_created_at ON policy_violations(created_at);

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