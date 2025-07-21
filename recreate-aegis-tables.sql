-- Connect to aegis_security database
\c aegis_security

-- Drop existing tables
DROP TABLE IF EXISTS devices CASCADE;
DROP TABLE IF EXISTS registration_keys CASCADE;

-- Let Hibernate create the tables on restart
-- But we'll insert the registration key