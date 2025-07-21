-- Connect to aegis_security database
\c aegis_security

-- Wait a moment for tables to be created, then insert registration key
INSERT INTO registration_keys (client_id, registration_key, description, is_active, created_at, updated_at)
VALUES (
    'UCOBANK_PROD_ANDROID',
    'ucobank_registration_key_2025',
    'UCO Bank Android App Production Key - Hackathon Demo',
    true,
    NOW(),
    NOW()
);

-- Verify
SELECT * FROM registration_keys;