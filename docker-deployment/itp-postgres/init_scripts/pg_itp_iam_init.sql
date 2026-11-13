-- ====================================================================
-- A8S Cloud Platform: IAM Database Initialization
-- ====================================================================

CREATE DATABASE db_a8s_iam 
    WITH OWNER = itpusr 
    ENCODING = 'UTF8' 
    TABLESPACE = pg_default 
    CONNECTION LIMIT = -1;

GRANT ALL PRIVILEGES ON DATABASE db_a8s_iam TO itpusr;
