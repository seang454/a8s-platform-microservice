-- ====================================================================
-- A8S Cloud Platform: PostgreSQL Microservices Database Initialization
-- ====================================================================
-- NOTE:
-- The following services use MongoDB (Document Store) instead of PostgreSQL:
--   1. a8s-security-scanner       -> mongodb://.../a8s_security_db
--   2. a8s-audit-service          -> mongodb://.../a8s_audit_db
--   3. a8s-ai-copilot-service     -> mongodb://.../a8s_ai_db
--   4. a8s-terminal-console       -> mongodb://.../a8s_terminal_db
-- ====================================================================

-- --------------------------------------------------------------------
-- 1. Core Services Databases (Relational & Transactional)
-- --------------------------------------------------------------------
-- Cluster & Node Pool Management
CREATE DATABASE db_a8s_cluster WITH OWNER = itpusr ENCODING = 'UTF8';

-- Workspaces & Environments (CQRS: Command & Query Separation)
CREATE DATABASE db_a8s_workspace_command WITH OWNER = itpusr ENCODING = 'UTF8';
CREATE DATABASE db_a8s_workspace_query WITH OWNER = itpusr ENCODING = 'UTF8';

-- Database-as-a-Service (CQRS: Command & Query Separation)
CREATE DATABASE db_a8s_dbaas_command WITH OWNER = itpusr ENCODING = 'UTF8';
CREATE DATABASE db_a8s_dbaas_query WITH OWNER = itpusr ENCODING = 'UTF8';

-- Git Integrations & Webhooks
CREATE DATABASE db_a8s_git WITH OWNER = itpusr ENCODING = 'UTF8';

-- Saga Workflow Orchestrator
CREATE DATABASE db_a8s_orchestrator WITH OWNER = itpusr ENCODING = 'UTF8';

-- --------------------------------------------------------------------
-- 2. Developer Tools Services Databases
-- --------------------------------------------------------------------
-- Secret Manager & Vault Integration
CREATE DATABASE db_a8s_secret WITH OWNER = itpusr ENCODING = 'UTF8';

-- DNS & Ingress Routing
CREATE DATABASE db_a8s_dns WITH OWNER = itpusr ENCODING = 'UTF8';

-- --------------------------------------------------------------------
-- 3. Operational Services Databases
-- --------------------------------------------------------------------
-- Usage Metering & Billing (Strict Financial ACID)
CREATE DATABASE db_a8s_billing WITH OWNER = itpusr ENCODING = 'UTF8';

-- Snapshot & Volume Backup
CREATE DATABASE db_a8s_backup WITH OWNER = itpusr ENCODING = 'UTF8';

-- Notification & Alerts (Email, Slack, Telegram)
CREATE DATABASE db_a8s_notification WITH OWNER = itpusr ENCODING = 'UTF8';

-- Metrics & Prometheus Integration
CREATE DATABASE db_a8s_monitoring WITH OWNER = itpusr ENCODING = 'UTF8';

-- --------------------------------------------------------------------
-- Grant Global Privileges to itpusr
-- --------------------------------------------------------------------
GRANT ALL PRIVILEGES ON DATABASE db_a8s_cluster TO itpusr;
GRANT ALL PRIVILEGES ON DATABASE db_a8s_workspace_command TO itpusr;
GRANT ALL PRIVILEGES ON DATABASE db_a8s_workspace_query TO itpusr;
GRANT ALL PRIVILEGES ON DATABASE db_a8s_dbaas_command TO itpusr;
GRANT ALL PRIVILEGES ON DATABASE db_a8s_dbaas_query TO itpusr;
GRANT ALL PRIVILEGES ON DATABASE db_a8s_git TO itpusr;
GRANT ALL PRIVILEGES ON DATABASE db_a8s_orchestrator TO itpusr;
GRANT ALL PRIVILEGES ON DATABASE db_a8s_secret TO itpusr;
GRANT ALL PRIVILEGES ON DATABASE db_a8s_dns TO itpusr;
GRANT ALL PRIVILEGES ON DATABASE db_a8s_billing TO itpusr;
GRANT ALL PRIVILEGES ON DATABASE db_a8s_backup TO itpusr;
GRANT ALL PRIVILEGES ON DATABASE db_a8s_notification TO itpusr;
GRANT ALL PRIVILEGES ON DATABASE db_a8s_monitoring TO itpusr;
