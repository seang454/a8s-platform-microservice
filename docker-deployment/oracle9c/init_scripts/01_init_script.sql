-- 01-setup-cdc-19c.sql
-- CDC setup for Oracle 19c with PDB architecture

-- Connect to CDB$ROOT
ALTER SESSION SET CONTAINER = CDB$ROOT;

-- Enable supplemental logging at CDB level
ALTER DATABASE ADD SUPPLEMENTAL LOG DATA;
ALTER DATABASE ADD SUPPLEMENTAL LOG DATA (ALL) COLUMNS;

-- Check if common user exists, create if not
DECLARE
  user_count NUMBER;
BEGIN
  SELECT COUNT(*) INTO user_count FROM dba_users WHERE username = 'C##DEBEZIUM';
  
  IF user_count = 0 THEN
    EXECUTE IMMEDIATE 'CREATE USER c##debezium IDENTIFIED BY dbz_password CONTAINER = ALL';
    DBMS_OUTPUT.PUT_LINE('Created common user c##debezium');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Common user c##debezium already exists');
  END IF;
END;
/

-- Grant system privileges at CDB level
GRANT CREATE SESSION TO c##debezium CONTAINER = ALL;
GRANT SET CONTAINER TO c##debezium CONTAINER = ALL;
GRANT SELECT ON V_$DATABASE TO c##debezium CONTAINER = ALL;
GRANT FLASHBACK ANY TABLE TO c##debezium CONTAINER = ALL;
GRANT SELECT ANY TABLE TO c##debezium CONTAINER = ALL;
GRANT SELECT_CATALOG_ROLE TO c##debezium CONTAINER = ALL;
GRANT EXECUTE_CATALOG_ROLE TO c##debezium CONTAINER = ALL;
GRANT SELECT ANY TRANSACTION TO c##debezium CONTAINER = ALL;
GRANT LOGMINING TO c##debezium CONTAINER = ALL;
GRANT CREATE TABLE TO c##debezium CONTAINER = ALL;
GRANT LOCK ANY TABLE TO c##debezium CONTAINER = ALL;
GRANT CREATE SEQUENCE TO c##debezium CONTAINER = ALL;
GRANT EXECUTE ON DBMS_LOGMNR TO c##debezium CONTAINER = ALL;
GRANT EXECUTE ON DBMS_LOGMNR_D TO c##debezium CONTAINER = ALL;
GRANT SELECT ON V_$LOG TO c##debezium CONTAINER = ALL;
GRANT SELECT ON V_$LOG_HISTORY TO c##debezium CONTAINER = ALL;
GRANT SELECT ON V_$LOGMNR_LOGS TO c##debezium CONTAINER = ALL;
GRANT SELECT ON V_$LOGMNR_CONTENTS TO c##debezium CONTAINER = ALL;
GRANT SELECT ON V_$LOGMNR_PARAMETERS TO c##debezium CONTAINER = ALL;
GRANT SELECT ON V_$LOGFILE TO c##debezium CONTAINER = ALL;
GRANT SELECT ON V_$ARCHIVED_LOG TO c##debezium CONTAINER = ALL;
GRANT SELECT ON V_$ARCHIVE_DEST_STATUS TO c##debezium CONTAINER = ALL;
GRANT SELECT ON V_$TRANSACTION TO c##debezium CONTAINER = ALL;

-- Set tablespace quota
ALTER USER c##debezium QUOTA UNLIMITED ON USERS CONTAINER = ALL;

-- Switch to PDB (adjust PDB name if different)
ALTER SESSION SET CONTAINER = ORCLPDB1;

-- Create test user if not exists
DECLARE
  user_count NUMBER;
BEGIN
  SELECT COUNT(*) INTO user_count FROM dba_users WHERE username = 'CORE_BANKING';
  
  IF user_count = 0 THEN
    EXECUTE IMMEDIATE 'CREATE USER CORE_BANKING IDENTIFIED BY qwer';
    EXECUTE IMMEDIATE 'GRANT CONNECT, RESOURCE TO CORE_BANKING';
    EXECUTE IMMEDIATE 'ALTER USER CORE_BANKING QUOTA UNLIMITED ON USERS';
    DBMS_OUTPUT.PUT_LINE('Created CORE_BANKING');
  ELSE
    DBMS_OUTPUT.PUT_LINE('CORE_BANKING already exists');
  END IF;
END;
/

-- Create products table if not exists
DECLARE
  table_count NUMBER;
BEGIN
  SELECT COUNT(*) INTO table_count FROM dba_tables 
  WHERE owner = 'CORE_BANKING' AND table_name = 'PRODUCTS';
  
  IF table_count = 0 THEN
    EXECUTE IMMEDIATE 'CREATE TABLE CORE_BANKING.products (
        id NUMBER PRIMARY KEY,
        name VARCHAR2(100),
        description VARCHAR2(500),
        metadata XMLTYPE,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )';
    
    EXECUTE IMMEDIATE 'ALTER TABLE CORE_BANKING.products ADD SUPPLEMENTAL LOG DATA (ALL) COLUMNS';
    
    EXECUTE IMMEDIATE 'INSERT INTO CORE_BANKING.products (id, name, description, metadata) VALUES (
        1,
        ''Laptop'',
        ''High-performance laptop'',
        XMLTYPE(''<product><brand>Dell</brand><model>XPS 15</model></product>'')
    )';
    
    EXECUTE IMMEDIATE 'INSERT INTO CORE_BANKING.products (id, name, description, metadata) VALUES (
        2,
        ''Mouse'',
        ''Wireless mouse'',
        XMLTYPE(''<product><brand>Logitech</brand><model>MX Master 3</model></product>'')
    )';
    
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Created products table with sample data');
  ELSE
    DBMS_OUTPUT.PUT_LINE('products table already exists');
  END IF;
END;
/

-- Grant table access to debezium user
GRANT SELECT ON CORE_BANKING.products TO c##debezium;

-- Verification
SELECT '=== Supplemental Logging Status ===' as info FROM dual;
ALTER SESSION SET CONTAINER = CDB$ROOT;
SELECT SUPPLEMENTAL_LOG_DATA_MIN, SUPPLEMENTAL_LOG_DATA_PK, SUPPLEMENTAL_LOG_DATA_UI 
FROM V$DATABASE;

ALTER SESSION SET CONTAINER = ORCLPDB1;
SELECT '=== Users ===' as info FROM dual;
SELECT username, account_status, common, created 
FROM dba_users 
WHERE username IN ('C##DEBEZIUM', 'CORE_BANKING')
ORDER BY username;

SELECT '=== Products Table ===' as info FROM dual;
SELECT * FROM CORE_BANKING.products;

EXIT;