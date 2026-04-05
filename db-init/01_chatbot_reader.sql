-- Create a read-only role for the chatbot Python service.
-- This SQL is run by postgres entrypoint on first initialisation.
-- The CHATBOT_DB_PASS variable is substituted via docker-entrypoint-initdb.d.

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'chatbot_reader') THEN
        CREATE ROLE chatbot_reader WITH LOGIN PASSWORD 'chatbot_reader_pass';
    END IF;
END
$$;

GRANT CONNECT ON DATABASE ecommerce TO chatbot_reader;
GRANT USAGE ON SCHEMA public TO chatbot_reader;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO chatbot_reader;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO chatbot_reader;
