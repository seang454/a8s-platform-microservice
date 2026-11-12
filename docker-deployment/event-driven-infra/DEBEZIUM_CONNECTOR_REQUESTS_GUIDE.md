# Debezium Oracle CDC Connector - Complete REST API Guide
> **In-Depth Guide: Why Every Single Postman Request Exists, What It Does, and When to Use It**

Kafka Connect runs as a headless distributed service inside your Docker container (`debezium-kafka-connect-itp:8083`). It has no UI by default; instead, it exposes a **REST API** on port `8083`.

Below is an exhaustive breakdown of **every request** seen in your instructor's Postman collection, explaining **why** it exists, **what problem it solves**, and **what happens under the hood**.

---

## 1. Core Concepts: What is Apache Avro & Schema Registry?

Before looking at the requests, we must understand **WHY** your project uses **Avro** instead of plain JSON.

### A. The Problem with Plain JSON in Microservices
When sending change events using plain JSON:
1. **Huge Payload Size:** Every message repeats the field names over and over:
   ```json
   {"RECID": "RECID-01", "XMLDATA": "...", "OP": "c", "TIMESTAMP": 1729000000}
   ```
   In a banking system processing millions of transactions, sending redundant field names wastes massive network bandwidth and disk space.
2. **No Data Contracts (Breaking Changes):** If someone adds or renames a database column in Oracle, downstream microservices (`pipeline-service`) crash at runtime because there is no schema validation.

---

### B. What is Apache Avro?
**Apache Avro** is a high-performance **binary serialization format**:
- **Compact & Fast:** Field names are **stripped out**! Only pure, compact binary bytes are stored on the Kafka wire.
- **Strictly Typed:** Data types (int, string, enum, xml) are strictly enforced by an Avro Schema file (such as your project's [`XmlRecord.avsc`](file:///d:/CSTADPreUniversityTraining/ITP/researchingitp/itp-micro-learning/microservices/pipeline-service/src/main/avro/XmlRecord.avsc)).
- **Code Generation:** The Gradle plugin compiles `.avsc` files directly into strong Java classes (e.g. `ITP.CORE_BANKING.RECORD_XML.Envelope`).

---

### C. What is the Confluent Schema Registry?
Because Avro messages contain only binary bytes (without field names), **how does the consumer know how to read the message?**

That is the job of the **Schema Registry** (running on port `8081` in your Docker setup):

```
       [ Oracle 19c ]
             │
             ▼
  ┌───────────────────────┐
  │   Debezium Connector  │
  └──────────┬────────────┘
             │ 1. Registers Schema (1st time only)
             ▼
  ┌─────────────────────────────────────────┐
  │ Confluent Schema Registry (:8081)       │ ◀── Stores Schema & returns Schema ID: 42
  └─────────────────────────────────────────┘
             │
             │ 2. Prepends 5 magic bytes [0, 0, 0, 0, 42] + Binary Payload
             ▼
  ┌─────────────────────────────────────────┐
  │ Apache Kafka Topic (:9090)              │
  └────────────────────┬────────────────────┘
                       │ 3. Fetches Schema ID: 42 to deserialize
                       ▼
  ┌─────────────────────────────────────────┐
  │ pipeline-service (Spring Cloud Stream)  │
  │ Deserializes cleanly into Java Objects! │
  └─────────────────────────────────────────┘
```

1. **Producer (Debezium):** Registers the schema with the Registry at `http://schema-registry-itp:8081`. The Registry assigns it an **ID** (e.g., ID `42`).
2. **On the Wire:** Debezium writes only **5 bytes** (Magic byte + Schema ID `42`) followed by raw binary data to Kafka.
3. **Consumer (`pipeline-service`):** Reads the 5 bytes, asks the Schema Registry for Schema `42`, caches it, and deserializes the binary bytes directly into your Java `Envelope` POJO!

---

### D. Byte-Level Wire Format: Plain JSON vs Apache Avro

Suppose you run this SQL in Oracle:
```sql
INSERT INTO CORE_BANKING.RECORD_XML (RECID, XMLDATA) 
VALUES ('RECID-01', XMLType('<data><name>John</name></data>'));
```

#### 1. Wire Format in Plain JSON (`JsonConverter`):
The message stored on Kafka is a **giant plain text UTF-8 string** (~1,200 bytes) containing all repeated field names and nested schema definitions:

```json
{
  "schema": {
    "type": "struct",
    "fields": [
      {
        "type": "struct",
        "fields": [
          { "type": "string", "optional": false, "field": "RECID" },
          { "type": "string", "optional": true, "field": "XMLDATA" }
        ],
        "optional": true,
        "name": "ITP.CORE_BANKING.RECORD_XML.Value",
        "field": "after"
      },
      { "type": "string", "optional": true, "field": "op" },
      { "type": "int64", "optional": true, "field": "ts_ms" }
    ],
    "name": "ITP.CORE_BANKING.RECORD_XML.Envelope"
  },
  "payload": {
    "before": null,
    "after": {
      "RECID": "RECID-01",
      "XMLDATA": "<?xml version=\"1.0\"?><data><name>John</name></data>"
    },
    "op": "c",
    "ts_ms": 1730160000100
  }
}
```

#### 2. Wire Format in Apache Avro + Schema Registry (`AvroConverter`):
Kafka stores **zero field names**. It stores only a **5-byte header** followed by raw binary data (~70 bytes):

```
┌───────────────┬─────────────────────────────┬──────────────────────────────────────────────────┐
│ Magic Byte    │ Schema ID (4 bytes)         │ Binary Data Payload (Raw values only)            │
│ [0x00]        │ [0x00, 0x00, 0x00, 0x2A]    │ [0x08, 0x52, 0x45, 0x43, 0x49, 0x44...]          │
└───────────────┴─────────────────────────────┴──────────────────────────────────────────────────┘
  1 Byte          4 Bytes (= ID 42)             ~65 Bytes
```

* **Size Comparison:** ~70 bytes (Avro) vs ~1,200 bytes (JSON) &mdash; **Avro is ~94% smaller!**
* **Speed:** Avro binary copies directly into memory without expensive text tokenization.

| Attribute | Plain JSON | Apache Avro + Schema Registry |
| :--- | :--- | :--- |
| **Data Format** | Human-readable text (`String`) | Compact Binary (`byte[]`) |
| **Message Size** | ~1,200 bytes | ~70 bytes (**~15x smaller**) |
| **Field Names on Wire** | Repeated with every message | **Zero** (stored once in Schema Registry) |
| **Schema Validation** | None (can break silently) | **Strict** (Registry rejects breaking changes) |
| **Deserialization Speed** | Slower (parsing text quotes) | **Ultra-fast** (direct memory mapping) |

---

## 2. High-Level Architecture: How These Requests Control Debezium

```
 Postman / Developer
         │
         │  1. HTTP Request (POST / PUT / GET / DELETE)
         ▼
 ┌───────────────────────────────────────────────────────────────┐
 │ Kafka Connect REST API (http://localhost:8083)               │
 │                                                               │
 │   • Creates worker tasks                                      │
 │   • Connects to Oracle as c##debezium                         │
 │   • Initializes LogMiner dictionary                           │
 └───────────────────────────────┬───────────────────────────────┘
                                 │
                 2. Reads Redo / Archive Logs
                                 ▼
 ┌───────────────────────────────────────────────────────────────┐
 │ Oracle 19c (ORCLPDB1)                                         │
 │ CORE_BANKING.RECORD_XML / FUNDS_TRANSFER                      │
 └───────────────────────────────┬───────────────────────────────┘
                                 │
                 3. Produces Change Events
                                 ▼
 ┌───────────────────────────────────────────────────────────────┐
 │ Apache Kafka Brokers (:9090)                                  │
 │ Topic: ITP.CORE_BANKING.RECORD_XML                            │
 └───────────────────────────────────────────────────────────────┘
```

---

## 3. Master Comparison: What Makes Each Request Different?

| Request Name | HTTP Method & Endpoint | Target Table(s) | Serialization / Converter | LOB / XML Setting | Why does this specific request exist? |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`PUT Debezium OraConnector XML v19 POC`** *(Recommended)* | `PUT /connectors/ora-connector-xml-poc/config` | `CORE_BANKING.RECORD_XML` or `FUNDS_TRANSFER` | **Avro** (Schema Registry `:8081`) | `"lob.enabled": "true"` | **Upsert Mode:** Creates or updates in-place. Specifically tuned for Oracle 19c Binary XML with Avro. |
| **`POST Debezium OraConnector XML Banking`** | `POST /connectors` | **All 3 Tables:** `RECORD_XML`, `ACCOUNT_PART`, `FUNDS_TRANSFER` | **Avro** (Schema Registry `:8081`) | `"lob.enabled": "true"` | **Multi-Table Production:** Uses standard `POST` to register all banking tables at once. Fails if name exists. |
| **`POST ORA_CONNECTOR (Normal Columns)`** | `POST /connectors` | `CORE_BANKING.PRODUCTS` | **Plain JSON** (No Schema Registry) | `"lob.enabled": "false"` | **Non-XML / Performance:** For standard tables without XML. Turning LOB off saves massive DB memory/CPU. |
| **`POST ORA_CONNECTOR (XMLColumns)`** | `POST /connectors` | `CORE_BANKING.RECORD_XML` | **Plain JSON** (with embedded XML string) | `"lob.enabled": "true"` | **JSON Testing:** Tests capturing XML columns into Kafka as plain text JSON instead of Avro binary. |
| **`POST ORA_CONNECTOR (test XML) Distribute`** | `POST /connectors` | `CORE_BANKING.RECORD_XML` | **Avro** or **JSON** | `"lob.enabled": "true"` | **Cluster Load Testing:** Sets `"tasks.max": "3"` to test distributing work across multiple Kafka Connect containers. |
| **`PUT ORA_A2_SOURCE_CONNECTOR`** | `PUT /connectors/ora-a2-source-connector/config` | `CORE_BANKING.RECORD_XML` | **Avro** | `"lob.enabled": "true"` | **Isolation Testing:** Uses a separate topic prefix (`"topic.prefix": "A2_ITP"`) so two connectors run side-by-side without topic collision. |
| **`GET /connectors/{name}/status`** | `GET /connectors/.../status` | N/A | Returns JSON status | N/A | **Health Inspection:** Checks if the background task is `RUNNING` or `FAILED` (shows stack trace on crash). |
| **`DEL /connectors/{name}`** | `DELETE /connectors/...` | N/A | N/A | N/A | **Teardown / Cleanup:** Permanently stops and removes the connector when re-architecting. |

### The 4 Main Differences Explained:

#### 1. Difference in HTTP Method: `PUT` vs `POST`
* **`POST /connectors`:** Standard Kafka Connect syntax. Expects `{ "name": "...", "config": { ... } }`. If you send it twice, it throws `409 Conflict: Connector already exists`.
* **`PUT /connectors/{name}/config`:** **Idempotent Upsert.** If it doesn't exist, it creates it. If it already exists, it updates the config on the fly. Takes a flat JSON config. Much better for iterative testing!

#### 2. Difference in Data Format: `Avro` vs `Plain JSON`
* **Avro Requests (`PUT ... XML v19 POC` & `POST ... XML Banking`):** Uses `AvroConverter`. Strips field names, sends tiny ~70-byte binary payloads to Kafka, and registers schema in Schema Registry (`:8081`). Matches your Spring Boot [`XmlRecord.avsc`](file:///d:/CSTADPreUniversityTraining/ITP/researchingitp/itp-micro-learning/microservices/pipeline-service/src/main/avro/XmlRecord.avsc).
* **JSON Requests (`POST ... Normal Columns`):** Uses `JsonConverter`. Sends full, readable JSON (~1,200 bytes) with all field names repeated.

#### 3. Difference in LOB Handling: `"lob.enabled": true` vs `false`
* **`true` (`XMLColumns` / `XML v19 POC`):** Oracle stores `XMLTYPE` as Large Objects (LOB). Debezium is instructed: *"Spend the extra CPU cycles to fetch the full XML document from the database."*
* **`false` (`Normal Columns`):** Used for standard tables (`PRODUCTS`, `CUSTOMER`). Debezium skips LOB reading, making capture **much faster**.

#### 4. Difference in Scope: Single-Table POC vs Multi-Table Banking
* **POC Requests:** Target only **1 table** (`"table.include.list": "CORE_BANKING.RECORD_XML"`) to isolate bugs and test one feature at a time.
* **Banking Requests:** Target **all 3 tables** (`"table.include.list": "CORE_BANKING.RECORD_XML,CORE_BANKING.ACCOUNT_PART,CORE_BANKING.FUNDS_TRANSFER"`) so the entire bank flows into Kafka simultaneously.

---

## 4. In-Depth Breakdown of Every Request

---

### Request 1: `PUT Debezium OraConnector XML v19 POC` (The Main Workhorse)

#### 🎯 What is it used for?
This is the **most important request in the entire collection**. It configures and deploys an active Debezium connector specifically tuned to capture **Oracle 19c XML columns** (`XMLDATA`) using **Apache Avro and Schema Registry**.

#### 💡 Why use `PUT .../config` instead of `POST ...`?
- **Idempotent Upsert:** If the connector does **not** exist yet, `PUT` creates it. If it **already exists**, `PUT` automatically updates the configuration and reloads the connector without having to delete it first.
- Notice the JSON payload is **flat** (no outer `"name"` or `"config"` wrapper) because the connector name is already specified in the URL path (`/connectors/ora-connector-xml-poc/config`).

#### 🛠️ Request Details:
* **Method:** `PUT`
* **URL:** `http://localhost:8083/connectors/ora-connector-xml-poc/config`
* **Headers:** `Content-Type: application/json`
* **Body:**
```json
{
  "connector.class": "io.debezium.connector.oracle.OracleConnector",
  "tasks.max": "1",
  "database.hostname": "oracle-itp",
  "database.port": "1521",
  "database.user": "c##debezium",
  "database.password": "dbz_password",
  "database.dbname": "ORCLCDB",
  "database.pdb.name": "ORCLPDB1",
  "topic.prefix": "ITP",
  "schema.include.list": "CORE_BANKING",
  "table.include.list": "CORE_BANKING.FUNDS_TRANSFER,CORE_BANKING.RECORD_XML",
  "schema.history.internal.kafka.topic": "schema-changes.oracle",
  "schema.history.internal.kafka.bootstrap.servers": "kafka-1-itp:9090,kafka-2-itp:9090,kafka-3-itp:9090",
  "lob.enabled": "true",
  "binary.handling.mode": "base64",
  "log.mining.strategy": "online_catalog",
  "snapshot.mode": "initial",
  "decimal.handling.mode": "string",
  "time.precision.mode": "adaptive",
  "tombstones.on.delete": "true",
  "key.converter": "io.confluent.connect.avro.AvroConverter",
  "value.converter": "io.confluent.connect.avro.AvroConverter",
  "key.converter.schema.registry.url": "http://schema-registry-itp:8081",
  "value.converter.schema.registry.url": "http://schema-registry-itp:8081"
}
```

#### 🔑 Critical Properties Explained:
1. **`lob.enabled: true`**: In Oracle, `XMLTYPE` is stored internally as a Large Object (LOB) or Binary XML. By default, Debezium ignores LOBs to save memory. Setting this to `true` forces Debezium to read and stream the full XML payload.
2. **`binary.handling.mode: base64`**: Ensures binary XML tokens don't cause encoding crashes when serialized into Kafka messages.
3. **`snapshot.mode: initial`**: On the very first run, Debezium executes a `SELECT *` on the table so existing records are streamed to Kafka before it starts tailing live redo logs.
4. **`AvroConverter` & `schema-registry-itp:8081`**: Automatically generates and registers the Avro schema (`XmlRecord.avsc`) into Schema Registry so downstream services like `pipeline-service` can deserialize strong types.

---

### Request 2: `POST Debezium OraConnector XML Banking`

#### 🎯 What is it used for?
Used for the **Initial Creation** of a multi-table production connector that monitors the entire Core Banking schema in one go (`RECORD_XML`, `ACCOUNT_PART`, `FUNDS_TRANSFER`).

#### 💡 Why does this exist alongside Request 1?
- Kafka Connect standard specifications define `POST /connectors` as the official creation endpoint.
- Unlike `PUT`, `POST` expects a root object with `"name"` and `"config"` fields.
- If you call `POST` a second time with the same name, it throws an error: `409 Conflict (Connector already exists)`. That is why developers prefer `PUT` for everyday iterative testing!

#### 🛠️ Request Details:
* **Method:** `POST`
* **URL:** `http://localhost:8083/connectors`
* **Body:**
```json
{
  "name": "debezium-oraconnector-banking",
  "config": {
    "connector.class": "io.debezium.connector.oracle.OracleConnector",
    "tasks.max": "1",
    "database.hostname": "oracle-itp",
    "database.port": "1521",
    "database.user": "c##debezium",
    "database.password": "dbz_password",
    "database.dbname": "ORCLCDB",
    "database.pdb.name": "ORCLPDB1",
    "topic.prefix": "ITP",
    "schema.include.list": "CORE_BANKING",
    "table.include.list": "CORE_BANKING.RECORD_XML,CORE_BANKING.ACCOUNT_PART,CORE_BANKING.FUNDS_TRANSFER",
    "schema.history.internal.kafka.topic": "schema-changes.oracle.banking",
    "schema.history.internal.kafka.bootstrap.servers": "kafka-1-itp:9090,kafka-2-itp:9090,kafka-3-itp:9090",
    "lob.enabled": "true",
    "binary.handling.mode": "base64",
    "log.mining.strategy": "online_catalog",
    "key.converter": "io.confluent.connect.avro.AvroConverter",
    "value.converter": "io.confluent.connect.avro.AvroConverter",
    "key.converter.schema.registry.url": "http://schema-registry-itp:8081",
    "value.converter.schema.registry.url": "http://schema-registry-itp:8081"
  }
}
```

---

### Request 3: `POST ORA_CONNECTOR (Normal Columns)`

#### 🎯 What is it used for?
Used when capturing **standard relational tables** that contain **NO XML / NO LOB data** (e.g. `PRODUCTS`, `CUSTOMER`, `BRANCH`).

#### 💡 Why do we need a separate configuration for normal columns?
- Reading LOB/XML columns via LogMiner requires extra CPU, memory, and database roundtrips.
- If a table only contains standard columns (`VARCHAR2`, `NUMBER`, `TIMESTAMP`), you can turn off LOB reading (`"lob.enabled": "false"`) to make CDC capture significantly faster and use much less database memory!

#### 🛠️ Key Differences:
```json
"table.include.list": "CORE_BANKING.PRODUCTS",
"lob.enabled": "false"
```

---

### Request 4: `POST ORA_CONNECTOR (test XML) Distribute`

#### 🎯 What is it used for?
Used to test **high-throughput distributed streaming** across multiple Kafka Connect worker nodes.

#### 💡 Why does this exist?
- Under standard Oracle LogMiner, `tasks.max` is set to `1` because transaction logs must be processed sequentially.
- However, when testing Kafka Connect in a multi-container cluster mode, developers use this request to verify how the cluster distributes connector workers and tasks across different Docker containers.

---

### Request 5: `POST ORA_CONNECTOR (test XML) Copy` (Copies 1, 2, 3)

#### 🎯 What are these used for?
These are **experimental testing variations** created during development to test different serialization formats:

1. **Variation A (String + JSON):** Tests how the message looks if the key is a plain String and the body is JSON (without Schema Registry).
   ```json
   "key.converter": "org.apache.kafka.connect.storage.StringConverter",
   "value.converter": "org.apache.kafka.connect.json.JsonConverter",
   "value.converter.schemas.enable": "false"
   ```
2. **Variation B (Raw Byte Mode):** Tests passing raw XML as bytes rather than Base64 strings.
3. **Variation C (Single Table POC):** Isolating only `RECORD_XML` without capturing other tables.

---

### Request 6: `PUT ORA_A2_SOURCE_CONNECTOR (test2 XML &...)`

#### 🎯 What is it used for?
Used to run **two separate Debezium connectors simultaneously** without naming conflicts.

#### 💡 Why do we need this?
In Kafka, if two connectors use the same `topic.prefix` or the same `schema.history.internal.kafka.topic`, they will corrupt each other's schema tracking!
This request gives the second connector:
- A unique connector name: `ora-a2-source-connector`
- A unique topic prefix: `"topic.prefix": "A2_ITP"`
- A unique schema history topic: `"schema.history.internal.kafka.topic": "schema-changes.oracle.a2"`

---

### Request 7: `GET ORA_CONNECTOR Copy` (List All Active Connectors)

#### 🎯 What is it used for?
Queries the Kafka Connect cluster to see **which connectors are currently installed and running**.

* **Method:** `GET`
* **URL:** `http://localhost:8083/connectors`
* **Response Example:**
  ```json
  [
    "ora-connector-xml-poc",
    "debezium-oraconnector-banking"
  ]
  ```
* **When to use it:** When troubleshooting to confirm whether Kafka Connect actually registered your connector or if it was lost after a container restart.

---

### Request 8: `GET ORA_DEBEZIUM_CONNECTOR Copy` (Health & Task Status)

#### 🎯 What is it used for?
**The most critical debugging tool.** It checks the real-time health of the connector and its background tasks.

* **Method:** `GET`
* **URL:** `http://localhost:8083/connectors/ora-connector-xml-poc/status`

#### 🟢 Healthy State:
```json
{
  "name": "ora-connector-xml-poc",
  "connector": {
    "state": "RUNNING",
    "worker_id": "debezium-kafka-connect-itp:8083"
  },
  "tasks": [
    {
      "id": 0,
      "state": "RUNNING",
      "worker_id": "debezium-kafka-connect-itp:8083"
    }
  ]
}
```

#### 🔴 Failed State (Example: Wrong password or Oracle down):
```json
{
  "name": "ora-connector-xml-poc",
  "connector": { "state": "RUNNING" },
  "tasks": [
    {
      "id": 0,
      "state": "FAILED",
      "trace": "java.sql.SQLException: ORA-01017: invalid username/password..."
    }
  ]
}
```
* **When to use it:** Whenever events are not showing up in Kafka! The `trace` field tells you the exact Oracle error code.

---

### Request 9: `GET ORA_CONNECTOR (test XML) Copy` (Read Configuration)

#### 🎯 What is it used for?
Retrieves the exact active configuration dictionary running on the worker.

* **Method:** `GET`
* **URL:** `http://localhost:8083/connectors/ora-connector-xml-poc/config`
* **When to use it:** To double-check which tables are currently included in `table.include.list` or which user is being used.

---

### Request 10: `DEL ORA_DEBEZIUM_CONNECTOR Copy` (Delete Connector)

#### 🎯 What is it used for?
Completely halts and **unregisters** the connector from the Kafka Connect cluster.

* **Method:** `DELETE`
* **URL:** `http://localhost:8083/connectors/ora-connector-xml-poc`
* **Response:** `204 No Content`
* **When to use it:** 
  1. When you want to change fundamental settings (like switching from JSON to Avro) that cannot be edited live.
  2. When you want to wipe clean and start capturing from scratch.

---

### Request 11: `POST Restart Connector / Task`

#### 🎯 What is it used for?
Revives a connector whose task died without having to recreate the whole config.

* **Restart Connector:** `POST http://localhost:8083/connectors/ora-connector-xml-poc/restart`
* **Restart Task 0:** `POST http://localhost:8083/connectors/ora-connector-xml-poc/tasks/0/restart`
* **When to use it:** If the Oracle Docker container restarted or went to sleep, the task will crash with a connection reset error. Calling this endpoint wakes it up immediately.

---

## 5. Quick Summary: Which Request Should YOU Use?

| What is your goal? | Which request to use? |
| :--- | :--- |
| **Stream `RECORD_XML` with Avro to `pipeline-service`** | **Request 1:** `PUT .../ora-connector-xml-poc/config` |
| **Check if Debezium is running or crashed** | **Request 8:** `GET .../ora-connector-xml-poc/status` |
| **Stream plain JSON without Schema Registry** | **Request 3:** `POST ... (Normal Columns / Plain JSON)` |
| **Stop and remove the connector completely** | **Request 10:** `DEL .../ora-connector-xml-poc` |
| **Restart after Oracle container rebooted** | **Request 11:** `POST .../restart` |
