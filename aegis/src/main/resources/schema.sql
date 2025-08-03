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


-- Drop the old tables if they exist to recreate with new structure (in correct order for foreign keys)
DROP TABLE IF EXISTS device_fingerprint_sensors;
DROP TABLE IF EXISTS device_app_info;
DROP TABLE IF EXISTS device_app_fingerprints;
DROP TABLE IF EXISTS device_fingerprints;

-- Device Fingerprints table for fraud detection
CREATE TABLE device_fingerprints (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(255) NOT NULL,
    version VARCHAR(50) NOT NULL,
    composite_hash VARCHAR(64) NOT NULL,
    
    -- Hardware characteristics
    manufacturer VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    device_name VARCHAR(100) NOT NULL,
    board VARCHAR(100) NOT NULL,
    brand VARCHAR(100) NOT NULL,
    cpu_architecture VARCHAR(200) NOT NULL,
    api_level INT NOT NULL,
    hardware_hash VARCHAR(64) NOT NULL,
    
    -- Display characteristics
    width_pixels INT NOT NULL,
    height_pixels INT NOT NULL,
    density_dpi INT NOT NULL,
    display_hash VARCHAR(64) NOT NULL,
    
    -- Sensor information
    sensor_count INT NOT NULL,
    sensor_hash VARCHAR(64) NOT NULL,
    
    -- Network characteristics
    network_country_iso VARCHAR(100),
    sim_country_iso VARCHAR(100),
    phone_type INT,
    network_hash VARCHAR(64) NOT NULL,
    
    -- Fraud detection
    is_fraudulent BOOLEAN DEFAULT false,
    fraud_reported_at TIMESTAMP NULL,
    fraud_reason VARCHAR(500),
    fingerprint_timestamp TIMESTAMP NOT NULL,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create sensor types table for the @ElementCollection
CREATE TABLE device_fingerprint_sensors (
    fingerprint_id BIGINT NOT NULL,
    sensor_type INT NOT NULL,
    FOREIGN KEY (fingerprint_id) REFERENCES device_fingerprints(id) ON DELETE CASCADE
);

-- Create indexes for device_fingerprints
CREATE INDEX IF NOT EXISTS idx_fingerprint_device_id ON device_fingerprints(device_id);
CREATE INDEX IF NOT EXISTS idx_fingerprint_hardware_hash ON device_fingerprints(hardware_hash);
CREATE INDEX IF NOT EXISTS idx_fingerprint_composite_hash ON device_fingerprints(composite_hash);
CREATE INDEX IF NOT EXISTS idx_fingerprint_display_hash ON device_fingerprints(display_hash);
CREATE INDEX IF NOT EXISTS idx_fingerprint_fraudulent ON device_fingerprints(is_fraudulent);

-- Create indexes for sensor types table
CREATE INDEX IF NOT EXISTS idx_sensor_fingerprint_id ON device_fingerprint_sensors(fingerprint_id);

-- App fingerprints table for enhanced device reinstall detection
CREATE TABLE IF NOT EXISTS device_app_fingerprints (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fingerprint_id BIGINT NOT NULL,
    total_app_count INT NOT NULL,
    user_app_count INT NOT NULL,
    system_app_count INT NOT NULL,
    app_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (fingerprint_id) REFERENCES device_fingerprints(id) ON DELETE CASCADE
);

-- Individual app info table for detailed app tracking
CREATE TABLE IF NOT EXISTS device_app_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    app_fingerprint_id BIGINT NOT NULL,
    package_name VARCHAR(255) NOT NULL,
    first_install_time BIGINT NOT NULL,
    last_update_time BIGINT NOT NULL,
    is_system_app BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (app_fingerprint_id) REFERENCES device_app_fingerprints(id) ON DELETE CASCADE
);

-- Create indexes for app fingerprints
CREATE INDEX IF NOT EXISTS idx_app_fingerprint_id ON device_app_fingerprints(fingerprint_id);
CREATE INDEX IF NOT EXISTS idx_app_fingerprint_hash ON device_app_fingerprints(app_hash);
CREATE INDEX IF NOT EXISTS idx_app_info_fingerprint_id ON device_app_info(app_fingerprint_id);
CREATE INDEX IF NOT EXISTS idx_app_info_package_name ON device_app_info(package_name);