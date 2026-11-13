# A8S Platform: Master Database & Infrastructure Architecture Guide
> **Comprehensive Reference: Microservice Database Mapping, Polyglot Persistence, and CDC Event Streaming**

---

## 1. Executive Architecture Overview

The **A8S Cloud Platform** utilizes the **Polyglot Persistence Pattern** (matching each microservice's data requirements to the optimal database engine):

* **🐘 PostgreSQL 17:** Primary relational database for transactions requiring strict ACID safety, foreign keys, and CQRS operations.
* **🍃 MongoDB 7.0:** Document database for high-volume append logs, deeply nested scan reports, and unstructured AI/session data.
* **⚡ Redis 8:** In-memory transient store for API Gateway rate-limiting, distributed token revocation, and user session caching.
* **🏛️ Oracle 19c:** External Legacy Core Banking system acting as the event source for Change Data Capture (CDC) streaming into Kafka.
* **🔐 HashiCorp Vault:** Enterprise cryptographic secrets manager for tokens, certificates, and database credentials.

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                   A8S CLOUD PLATFORM                                   │
├────────────────────┬────────────────────┬────────────────────┬─────────────────────────┤
│    🐘 PostgreSQL    │     🍃 MongoDB      │      ⚡ Redis       │      🏛️ Oracle 19c       │
├────────────────────┼────────────────────┼────────────────────┼─────────────────────────┤
│ Relational & ACID  │ Document & Logs    │ In-Memory Cache    │ Legacy Core System      │
│ 13 Databases       │ 4 Databases        │ In-Memory Sessions │ 1 Pluggable DB (PDB)    │
│ Port: 16851/16852  │ Port: 27017        │ Port: 6379         │ Port: 1521              │
└────────────────────┴────────────────────┴────────────────────┴─────────────────────────┘
```

---

## 2. Master Microservice Database Mapping

### A. Spring Cloud & Edge Services

| Microservice | Database Engine | Database Name | Port / Host | What It Stores |
| :--- | :---: | :--- | :--- | :--- |
| **`iam-service`** | 🐘 **PostgreSQL** | `db_a8s_iam` | `100.105.135.56:16852` | Users, roles, permissions, tenant IDs, refresh tokens. |
| **`gateway-server`** | ⚡ **Redis** | In-Memory DB | `100.105.135.56:6379` | Rate-limiting counters, blacklisted revoked JWT tokens. |
| **`front-bff`** | ⚡ **Redis** | In-Memory Cache | `100.105.135.56:6379` | Temporary web client session data & cached UI responses. |
| **`admin-bff`** | ⚡ **Redis** | In-Memory Cache | `100.105.135.56:6379` | Admin session data & cached platform analytics. |
| **`eurekaserver`** | ❌ None | *(In-Memory)* | `:8761` | Live microservice discovery and heartbeat registry. |
| **`configserver`** | ❌ None | *(Git / File)* | `:8888` | Centralized `application.yml` configuration repository. |

---

### B. Core Business Services

| Microservice | Database Engine | Database Name | Port / Host | What It Stores |
| :--- | :---: | :--- | :--- | :--- |
| **`a8s-cluster`** | 🐘 **PostgreSQL** | `db_a8s_cluster` | `100.105.135.56:16851` | Kubernetes clusters, node pools, IP subnets, node health. |
| **`a8s-workspace-command`** | 🐘 **PostgreSQL** | `db_a8s_workspace_command` | `100.105.135.56:16851` | Workspace & project creation events (Axon Event Store). |
| **`a8s-workspace-query`** | 🐘 **PostgreSQL** | `db_a8s_workspace_query` | `100.105.135.56:16851` | Flattened dashboard read views (Instant UI `SELECT *`). |
| **`a8s-dbaas-command`** | 🐘 **PostgreSQL** | `db_a8s_dbaas_command` | `100.105.135.56:16851` | Database provisioning orders, sizing parameters. |
| **`a8s-dbaas-query`** | 🐘 **PostgreSQL** | `db_a8s_dbaas_query` | `100.105.135.56:16851` | Catalog of running databases, credentials, connection URLs. |
| **`a8s-git-integration`** | 🐘 **PostgreSQL** | `db_a8s_git` | `100.105.135.56:16851` | Connected GitHub/GitLab repositories, webhooks, commit hashes. |
| **`a8s-orchestrator-service`** | 🐘 **PostgreSQL** | `db_a8s_orchestrator` | `100.105.135.56:16851` | Distributed Saga transaction state machines & step rollback logs. |

---

### C. Developer Tools Services

| Microservice | Database Engine | Database Name | Port / Host | What It Stores |
| :--- | :---: | :--- | :--- | :--- |
| **`a8s-secret-manager`** | 🐘 **PostgreSQL** + 🔐 **Vault** | `db_a8s_secret` *(Postgres)*<br/>`secret/` *(Vault)* | `100.105.135.56:16851`<br/>`100.105.135.56:8200` | Postgres stores secret metadata paths; Vault stores the actual encrypted keys/passwords. |
| **`a8s-dns-ingress-service`** | 🐘 **PostgreSQL** | `db_a8s_dns` | `100.105.135.56:16851` | Domain names, ingress routing records, SSL/TLS certificates. |
| **`a8s-terminal-console`** | 🍃 **MongoDB** | `a8s_terminal_db` | `100.105.135.56:27017` | Shell command history, keystroke logs, raw session recordings. |

---

### D. Operational Services

| Microservice | Database Engine | Database Name | Port / Host | What It Stores |
| :--- | :---: | :--- | :--- | :--- |
| **`a8s-billing-service`** | 🐘 **PostgreSQL** | `db_a8s_billing` | `100.105.135.56:16851` | **Financial data:** Subscriptions, customer balances, invoices, usage billing meters. *(Also consumes synced products from Oracle CDC)* |
| **`a8s-backup-service`** | 🐘 **PostgreSQL** | `db_a8s_backup` | `100.105.135.56:16851` | Snapshot schedules, backup status, S3 storage archive URLs. |
| **`a8s-notification-service`** | 🐘 **PostgreSQL** | `db_a8s_notification` | `100.105.135.56:16851` | Notification templates, alert logs (Email, Slack, Telegram). |
| **`a8s-monitoring-metrics`** | 🐘 **PostgreSQL** | `db_a8s_monitoring` | `100.105.135.56:16851` | Prometheus alert rules, metric thresholds, server monitoring configs. |
| **`a8s-security-scanner`** | 🍃 **MongoDB** | `a8s_security_db` | `100.105.135.56:27017` | Deeply nested container vulnerability scan reports (Trivy CVEs). |
| **`a8s-audit-service`** | 🍃 **MongoDB** | `a8s_audit_db` | `100.105.135.56:27017` | Immutable audit trail documents with 90-day auto-expiry (TTL index). |
| **`a8s-ai-copilot-service`** | 🍃 **MongoDB** | `a8s_ai_db` | `100.105.135.56:27017` | Chat conversation messages, LLM prompts, token usages. |

---

### E. Legacy Core System (CDC Source)

| Database Engine | Database / PDB | Schema & Table | Role / Description |
| :---: | :--- | :--- | :--- |
| 🏛️ **Oracle 19c** | `ORCLPDB1` | `CORE_BANKING.PRODUCTS`<br/>`CORE_BANKING.ACCOUNT_PART` | **Source of Truth for Core Banking:** DBAs write here. Debezium streams all changes via Kafka into **`a8s-billing-service`** (PostgreSQL) and **`a8s-audit-service`** (MongoDB). |

---

## 3. The Oracle ➡️ Debezium ➡️ Kafka ➡️ PostgreSQL Pipeline

```
[ Oracle 19c (ORCLPDB1) ]
           │
           │ (1. Supplemental Logging writes Old & New values to Redo Log)
           ▼
 [ LogMiner (DBMS_LOGMNR) ]
           │
           │ (2. Decodes binary log into SQL changes)
           ▼
[ Debezium Connector (:8083) ]
           │
           │ (3. Serializes change event with Avro via Schema Registry :8081)
           ▼
 [ Apache Kafka (:29092) ]
           │
           │ (4. Broadcasts to topic: ITP.CORE_BANKING.PRODUCTS)
           ▼
[ a8s-billing-service ]
           │
           │ (5. @KafkaListener saves updated copy into PostgreSQL)
           ▼
[ PostgreSQL (db_a8s_billing) ] ──▶ Frontend User Reads (Fast 1ms SELECT)
```

### Core Architecture Rules:
1. **Spring Boot NEVER connects to Oracle:** Spring Boot has zero Oracle drivers. Debezium handles all communication with Oracle.
2. **Always Read from PostgreSQL:** Frontend and microservice APIs read exclusively from PostgreSQL or MongoDB. They never touch Oracle directly.
3. **Old & New Data Handled Automatically:**
   * **Old Data (Snapshot):** Debezium copies all existing records on first startup (`op: "r"`).
   * **New Data (Streaming):** Debezium streams live `INSERT`, `UPDATE`, `DELETE` transactions in ~30ms (`op: "c"`, `op: "u"`, `op: "d"`).

---

## 4. Spring Boot Configuration Cheat Sheet (`application.yml`)

### A. PostgreSQL Services:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://100.105.135.56:16851/<database_name>
    username: itpusr
    password: itp@168
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```
*(Note: For `iam-service`, use port `16852`)*

### B. MongoDB Services:
```yaml
spring:
  data:
    mongodb:
      uri: mongodb://itpusr:itp@168@100.105.135.56:27017/<database_name>?authSource=admin
```

### C. Redis Services:
```yaml
spring:
  data:
    redis:
      host: 100.105.135.56
      port: 6379
      password: qwer
```

### D. Kafka Event Streaming:
```yaml
spring:
  kafka:
    bootstrap-servers: 100.105.135.56:29092,100.105.135.56:29094,100.105.135.56:29096
    properties:
      schema.registry.url: http://100.105.135.56:8081
```

---

## 5. Web Dashboards & Management UIs

| Tool | URL | Credentials |
| :--- | :--- | :--- |
| **Kafka UI** | [http://100.105.135.56:18000](http://100.105.135.56:18000) | `admin` / `qwer` |
| **Mongo Express** | [http://100.105.135.56:8085](http://100.105.135.56:8085) | `admin` / `qwer` |
| **HashiCorp Vault UI** | [http://100.105.135.56:8200](http://100.105.135.56:8200) | Token: `myroot` |
| **Confluent Schema Registry** | [http://100.105.135.56:8081](http://100.105.135.56:8081) | *(REST API)* |
| **Debezium Connect REST API**| [http://100.105.135.56:8083](http://100.105.135.56:8083) | *(REST API)* |
