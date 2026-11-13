-- ====================================================================
-- A8S Cloud Platform: Microservices Database Initialization
-- ====================================================================

-- --------------------------------------------------------------------
-- 1. Core Services Databases
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

-- Terminal & Pod Console Sessions
CREATE DATABASE db_a8s_terminal WITH OWNER = itpusr ENCODING = 'UTF8';

-- --------------------------------------------------------------------
-- 3. Operational Services Databases
-- --------------------------------------------------------------------
-- Usage Metering & Billing
CREATE DATABASE db_a8s_billing WITH OWNER = itpusr ENCODING = 'UTF8';

-- Security Audit Logs
CREATE DATABASE db_a8s_audit WITH OWNER = itpusr ENCODING = 'UTF8';

-- Notification & Alerts (Email, Slack, Telegram)
CREATE DATABASE db_a8s_notification WITH OWNER = itpusr ENCODING = 'UTF8';

-- Snapshot & Volume Backup
CREATE DATABASE db_a8s_backup WITH OWNER = itpusr ENCODING = 'UTF8';

-- Container Vulnerability Scanner
CREATE DATABASE db_a8s_security WITH OWNER = itpusr ENCODING = 'UTF8';

-- Metrics & Prometheus Integration
CREATE DATABASE db_a8s_monitoring WITH OWNER = itpusr ENCODING = 'UTF8';

-- AI DevOps Copilot Service
CREATE DATABASE db_a8s_ai WITH OWNER = itpusr ENCODING = 'UTF8';

-- --------------------------------------------------------------------
-- Grant Global Permissions to itpusr
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
GRANT ALL PRIVILEGES ON DATABASE db_a8s_terminal TO itpusr;
GRANT ALL PRIVILEGES ON DATABASE db_a8s_billing TO itpusr;
GRANT ALL PRIVILEGES ON DATABASE db_a8s_audit TO itpusr;
GRANT ALL PRIVILEGES ON DATABASE db_a8s_notification TO itpusr;
GRANT ALL PRIVILEGES ON DATABASE db_a8s_backup TO itpusr;
GRANT ALL PRIVILEGES ON DATABASE db_a8s_security TO itpusr;
GRANT ALL PRIVILEGES ON DATABASE db_a8s_monitoring TO itpusr;
GRANT ALL PRIVILEGES ON DATABASE db_a8s_ai TO itpusr;
