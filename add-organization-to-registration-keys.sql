-- Connect to aegis_security database
\c aegis_security

-- Add organization column to registration_keys table
ALTER TABLE registration_keys 
ADD COLUMN IF NOT EXISTS organization VARCHAR(255);

-- Update existing records to have a default organization (you may want to set these properly)
UPDATE registration_keys 
SET organization = 'Default Organization' 
WHERE organization IS NULL;

-- Make the column NOT NULL after setting values
ALTER TABLE registration_keys 
ALTER COLUMN organization SET NOT NULL;

-- Create an index for faster queries by organization
CREATE INDEX IF NOT EXISTS idx_registration_keys_organization ON registration_keys(organization);

-- Verify the changes
\d registration_keys