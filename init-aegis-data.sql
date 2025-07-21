-- Connect to aegis_security database
\c aegis_security

-- Insert registration key for UCO Bank Android App
INSERT INTO registration_keys (client_id, registration_key, description, is_active, created_at, updated_at)
VALUES (
    'UCOBANK_PROD_ANDROID',
    'ucobank_registration_key_2025',
    'UCO Bank Android App Production Key - Hackathon Demo',
    true,
    NOW(),
    NOW()
) ON CONFLICT (client_id) DO UPDATE
SET registration_key = 'ucobank_registration_key_2025',
    is_active = true,
    updated_at = NOW();

-- Verify the insertion
SELECT client_id, registration_key, is_active FROM registration_keys WHERE client_id = 'UCOBANK_PROD_ANDROID';