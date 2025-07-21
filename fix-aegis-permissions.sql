-- Connect to aegis_security database
\c aegis_security

-- Grant all permissions to aegis user on public schema
GRANT CREATE ON SCHEMA public TO aegis;
GRANT ALL ON SCHEMA public TO aegis;

-- Also grant permissions for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO aegis;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO aegis;