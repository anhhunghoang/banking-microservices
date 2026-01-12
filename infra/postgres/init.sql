-- Create user and databases
-- Note: 'postgres' user is default superuser. We create a specific user for the app.
-- 'banking' database might not exist, creating it.

DO
$do$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE  rolname = 'user') THEN

      CREATE ROLE "user" LOGIN PASSWORD 'password';
   END IF;
END
$do$;

SELECT 'CREATE DATABASE banking'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'banking')\gexec

GRANT ALL PRIVILEGES ON DATABASE banking TO "user";

-- Connect to banking db to create schemas
\c banking

CREATE SCHEMA IF NOT EXISTS customer;
CREATE SCHEMA IF NOT EXISTS account;
CREATE SCHEMA IF NOT EXISTS "transaction";

GRANT ALL ON SCHEMA customer TO "user";
GRANT ALL ON SCHEMA account TO "user";
GRANT ALL ON SCHEMA "transaction" TO "user";
