# A8S Platform: Master Service Ports & Database Architecture Guide
> **Comprehensive Reference: Microservice Ports (20260+), Database Mapping, and Infrastructure**

---

## 1. Master Service Ports & Network Mapping (All 23 Microservices)

### A. Spring Cloud & Edge Backbone
| Service Module | Application Name | Server Port | Purpose / Role |
| :--- | :--- | :---: | :--- |
| `spring-cloud/eurekaserver` | `eurekaserver` | **`8761`** | Service Registry & Discovery |
| `spring-cloud/configserver` | `configserver` | **`8888`** | Centralized Configuration Server |
| `spring-cloud/gateway-server` | `gateway-server` | **`9999`** | API Gateway (Rate Limiting & Routing) |
| `spring-cloud/iam-service` | `iam-service` | **`9090`** | OAuth2 / OIDC Identity Provider |
| `spring-cloud/front-bff` | `front-bff` | **`9990`** | Web Client UI Backend-For-Frontend |
| `spring-cloud/admin-bff` | `admin-bff` | **`9991`** | Admin Console Backend-For-Frontend |

---

### B. Core Business Microservices (`20260` ➔ `20266`)
| Service Module | Application Name | Server Port | Database Assigned |
| :--- | :--- | :---: | :--- |
| `core-services/a8s-cluster/a8s-cluster-main` | `a8s-cluster-main` | **`20260`** | `db_a8s_cluster` (PostgreSQL) |
| `core-services/a8s-workspace/.../workspace-command-main` | `workspace-command-main` | **`20261`** | `db_a8s_workspace_command` (PostgreSQL) |
| `core-services/a8s-workspace/.../workspace-query-main` | `workspace-query-main` | **`20262`** | `db_a8s_workspace_query` (PostgreSQL) |
| `core-services/a8s-dbaas/.../dbaas-command-main` | `dbaas-command-main` | **`20263`** | `db_a8s_dbaas_command` (PostgreSQL) |
| `core-services/a8s-dbaas/.../dbaas-query-main` | `dbaas-query-main` | **`20264`** | `db_a8s_dbaas_query` (PostgreSQL) |
| `core-services/a8s-git-integration-service/git-main` | `git-main` | **`20265`** | `db_a8s_git` (PostgreSQL) |
| `core-services/a8s-orchestrator-service/orchestrator-main` | `orchestrator-main` | **`20266`** | `db_a8s_orchestrator` (PostgreSQL) |

---

### C. Developer Tools Microservices (`20267` ➔ `20269`)
| Service Module | Application Name | Server Port | Database Assigned |
| :--- | :--- | :---: | :--- |
| `dev-tools/a8s-secret-manager-service/secret-main` | `secret-main` | **`20267`** | `db_a8s_secret` (PostgreSQL + Vault) |
| `dev-tools/a8s-dns-ingress-service/dns-main` | `dns-main` | **`20268`** | `db_a8s_dns` (PostgreSQL) |
| `dev-tools/a8s-terminal-console-service/terminal-main` | `terminal-main` | **`20269`** | `a8s_terminal_db` (MongoDB) |

---

### D. Operational Microservices (`20270` ➔ `20276`)
| Service Module | Application Name | Server Port | Database Assigned |
| :--- | :--- | :---: | :--- |
| `operational-services/a8s-billing-service/billing-main` | `billing-main` | **`20270`** | `db_a8s_billing` (PostgreSQL) |
| `operational-services/a8s-audit-service/audit-main` | `audit-main` | **`20271`** | `a8s_audit_db` (MongoDB) |
| `operational-services/a8s-notification-service/notification-main`| `notification-main` | **`20272`** | `db_a8s_notification` (PostgreSQL) |
| `operational-services/a8s-backup-service/backup-main` | `backup-main` | **`20273`** | `db_a8s_backup` (PostgreSQL) |
| `operational-services/a8s-security-scanner/security-main` | `security-main` | **`20274`** | `a8s_security_db` (MongoDB) |
| `operational-services/a8s-monitoring-metrics.../monitoring-main`| `monitoring-main` | **`20275`** | `db_a8s_monitoring` (PostgreSQL) |
| `operational-services/a8s-ai-copilot-service/ai-main` | `ai-main` | **`20276`** | `a8s_ai_db` (MongoDB) |

---

## 2. Infrastructure Backing Services Reference (Kali Linux: `100.105.135.56`)

| Engine / Service | Port(s) | Credentials |
| :--- | :---: | :--- |
| **PostgreSQL (Microservices)** | `16851` | `itpusr` / `itp@168` |
| **PostgreSQL (IAM Auth)** | `16852` | `itpusr` / `itp@168` |
| **MongoDB (Document Store)** | `27017` | `itpusr` / `itp@168` |
| **Mongo Express Web UI** | `8085` | `admin` / `qwer` |
| **Redis (Cache & Rate Limiter)** | `6379` | `qwer` |
| **Oracle 19c Enterprise** | `1521` | `sys` / `Oracle@168` (PDB: `ORCLPDB1`) |
| **Kafka Brokers** | `29092, 29094, 29096` | Plaintext |
| **Kafka UI Dashboard** | `18000` | `admin` / `qwer` |
| **Schema Registry** | `8081` | REST API |
| **Debezium Connect** | `8083` | REST API |
| **HashiCorp Vault** | `8200` | Token: `myroot` |
