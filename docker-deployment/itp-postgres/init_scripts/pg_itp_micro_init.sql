CREATE DATABASE db_customer WITH OWNER = itpusr ENCODING = 'UTF8' LC_COLLATE = 'en_US.utf8' LC_CTYPE = 'en_US.utf8' TABLESPACE = pg_default CONNECTION LIMIT = -1;
CREATE DATABASE db_account WITH OWNER = itpusr ENCODING = 'UTF8' LC_COLLATE = 'en_US.utf8' LC_CTYPE = 'en_US.utf8' TABLESPACE = pg_default CONNECTION LIMIT = -1;
CREATE DATABASE db_account_query WITH OWNER = itpusr ENCODING = 'UTF8' LC_COLLATE = 'en_US.utf8' LC_CTYPE = 'en_US.utf8' TABLESPACE = pg_default CONNECTION LIMIT = -1;
CREATE DATABASE db_transaction WITH OWNER = itpusr ENCODING = 'UTF8' LC_COLLATE = 'en_US.utf8' LC_CTYPE = 'en_US.utf8' TABLESPACE = pg_default CONNECTION LIMIT = -1;
CREATE DATABASE db_transfer WITH OWNER = itpusr ENCODING = 'UTF8' LC_COLLATE = 'en_US.utf8' LC_CTYPE = 'en_US.utf8' TABLESPACE = pg_default CONNECTION LIMIT = -1;
CREATE DATABASE db_payment WITH OWNER = itpusr ENCODING = 'UTF8' LC_COLLATE = 'en_US.utf8' LC_CTYPE = 'en_US.utf8' TABLESPACE = pg_default CONNECTION LIMIT = -1;
CREATE DATABASE db_card WITH OWNER = itpusr ENCODING = 'UTF8' LC_COLLATE = 'en_US.utf8' LC_CTYPE = 'en_US.utf8' TABLESPACE = pg_default CONNECTION LIMIT = -1;
-- CREATE DATABASE db_product WITH OWNER = itpusr ENCODING = 'UTF8' LC_COLLATE = 'en_US.utf8' LC_CTYPE = 'en_US.utf8' TABLESPACE = pg_default CONNECTION LIMIT = -1;
--
--        -- Connect to db_product and create tables
-- \c db_product
--
-- -- Create products table
-- CREATE TABLE IF NOT EXISTS products (
--                                         id SERIAL PRIMARY KEY,
--                                         code VARCHAR(255) NOT NULL,
--     qty INTEGER,
--     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
--     );
--
-- -- Set replica identity for CDC (CRITICAL for Debezium)
-- ALTER TABLE products REPLICA IDENTITY FULL;
--
-- -- Insert sample data
-- INSERT INTO products (code, qty) VALUES
--                                      ('D1', 100),
--                                      ('D2', 200),
--                                      ('D3', 300),
--                                      ('D4', 400),
--                                      ('D5', 500),
--                                      ('D6', 600),
--                                      ('D7', 700),
--                                      ('D8', 800),
--                                      ('D9', 900),
--                                      ('D10', 1000);
--
-- -- Grant privileges
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO itpusr;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO itpusr;