# Kafka vs Debezium - Architectural Guide
> **Understanding the Relationship: The Highway vs The Delivery Truck**

This document explains the fundamental differences between **Apache Kafka** and **Debezium**, how they work together, and how they power the event-driven Change Data Capture (CDC) pipeline in your Core Banking microservices.

---

## 1. Executive Summary

| Feature | Apache Kafka | Debezium |
| :--- | :--- | :--- |
| **Category** | **Event Streaming Platform** / Distributed Message Broker | **Change Data Capture (CDC)** Engine / Connector Plugin |
| **Primary Role** | Stores, routes, and delivers messages reliably at massive scale | Listens to database transaction logs (`INSERT`, `UPDATE`, `DELETE`) and generates events |
| **Database Awareness** | **None.** Kafka has no native understanding of Oracle, PostgreSQL, or MySQL | **Deep.** Connects directly to internal database engines (Oracle LogMiner, PostgreSQL WAL) |
| **Can it run alone?** | **Yes.** Kafka runs independently to connect microservices | **No.** Debezium is built as a plugin that runs **inside Kafka Connect** |
| **Real-World Analogy** | **The Railway Tracks & Cargo Trains** 🚂 | **The Cargo Loader** 👷‍♂️ loading boxes from the warehouse onto the train |

---

## 2. How They Work Together in Your Banking Architecture

```
  ┌─────────────────────────────────────────────────────────────┐
  │ 1. Oracle 19c Database (ORCLPDB1)                           │
  │    INSERT INTO CORE_BANKING.RECORD_XML (RECID, XMLDATA)...  │
  └──────────────────────────────┬──────────────────────────────┘
                                 │
                 Writes mutation │ to Redo Logs
                                 ▼
  ┌─────────────────────────────────────────────────────────────┐
  │ 2. Debezium CDC Connector (The Watcher / Worker)            │
  │    • Connected as c##debezium                               │
  │    • Mines Oracle Redo Logs via DBMS_LOGMNR                 │
  │    • Captures before/after values + XML payload             │
  │    • Encodes payload using Confluent Avro                   │
  └──────────────────────────────┬──────────────────────────────┘
                                 │
                 Publishes event │ onto topic
                                 ▼
  ┌─────────────────────────────────────────────────────────────┐
  │ 3. Apache Kafka Cluster (The Highway / Broker :9090)        │
  │    • Topic: `ITP.CORE_BANKING.RECORD_XML`                   │
  │    • Stores event persistently across partitions            │
  │    • Manages consumer group offsets                         │
  └──────────────────────────────┬──────────────────────────────┘
                                 │
                 Streams event   │ to subscribed consumer
                                 ▼
  ┌─────────────────────────────────────────────────────────────┐
  │ 4. Spring Boot Microservice (pipeline-service)              │
  │    • DbzStreamConfig.java: captureXmlData() / consumeDbzEvent│
  │    • Deserializes Avro & extracts `<name>John Doe</name>`   │
  └─────────────────────────────────────────────────────────────┘
```

---

## 3. Deep-Dive: What is Apache Kafka?

### What is it?
**Apache Kafka** is a distributed, fault-tolerant publish-subscribe messaging system designed to handle trillions of events a day.

### What Kafka Does:
1. **Pub/Sub Messaging:** Producers push messages into **Topics**; consumers pull messages from topics independently.
2. **Persistence & Replayability:** Unlike traditional queues (like RabbitMQ) that delete messages once consumed, Kafka keeps messages on disk for days or weeks. If a microservice crashes, it can replay old events from scratch.
3. **Partitioning & Scale:** Splits topics across multiple brokers so thousands of requests run concurrently without bottlenecking.

### What Kafka DOES NOT Do:
* Kafka **cannot** monitor a database on its own.
* Kafka **cannot** read Oracle Redo Logs or detect when a bank account balance changes.

---

## 4. Deep-Dive: What is Debezium?

### What is it?
**Debezium** is an open-source distributed **Change Data Capture (CDC)** platform built on top of **Kafka Connect**.

### What Debezium Does:
1. **Zero Application Code Changes:** You do not need to add SQL triggers, audit tables, or dual-write code inside your Spring Boot services.
2. **Transaction Log Tailing:** Debezium acts as an internal database follower:
   - For **Oracle:** Reads transaction logs using **LogMiner**.
   - For **PostgreSQL:** Reads changes via **Logical Replication / WAL**.
   - For **MySQL:** Reads the **binlog**.
3. **Full Before & After State:** Captures the full row snapshot before the update and after the update.
4. **Guaranteed Commit Order:** Emits events in the exact sequence transactions were committed in the database.

---

## 5. Debezium vs Traditional Database Polling

Why not just write a Spring Boot `@Scheduled` job to query the database every 5 seconds?

| Problem | Traditional Polling (`SELECT WHERE updated_at > ...`) | Debezium CDC |
| :--- | :--- | :--- |
| **Database Load** | Heavy `SELECT` queries slow down database CPU/indexes | Reads Redo Logs asynchronously; **zero impact** on query performance |
| **Detecting DELETES** | Impossible unless you use soft-deletes (`is_deleted=true`) | Captures hard `DELETE` statements immediately |
| **Intermediate Changes** | If a row changes 3 times in 2 seconds, polling misses the first 2 | Captures **every single intermediate transaction** |
| **Latency** | Polling delay (e.g., 5 to 60 seconds) | Near real-time (**milliseconds**) |

---

## 6. Deep-Dive: What is Confluent Schema Registry?

### A. What is it?
**Confluent Schema Registry** is a standalone web service running on port `8081` that serves as the **central repository and notary public for message schemas** in an Apache Kafka ecosystem.

It provides a **RESTful interface** for storing, versioning, and retrieving schemas (primarily **Apache Avro**, **JSON Schema**, and **Protobuf**).

---

### B. The 3 Core Problems Schema Registry Solves

#### 1. Contract Enforcement & Schema Evolution (Preventing Disasters)
Without Schema Registry, Kafka is a "dumb pipe": any producer can send bad data, and consumers will crash at runtime.

Schema Registry enforces **Compatibility Modes** so schemas can evolve safely:
- **`BACKWARD` (Default):** Consumers using the new schema can read data written by producers with the old schema (e.g. adding an optional field with a default value).
- **`FORWARD`:** Consumers using the old schema can read data written by producers with the new schema (e.g. deleting an optional field).
- **`FULL`:** Both backward and forward compatible.
- **`NONE`:** Disables checks (not recommended).

> **Example:** If someone tries to change `ACCOUNT_NO` from a `String` to an `Integer`, Schema Registry **blocks the producer immediately** with an `IncompatibleSchemaException` before the bad message ever touches Kafka!

#### 2. Extreme Bandwidth & Storage Optimization (The 5-Byte Wire Format)
In standard JSON, every single message repeats all keys, data types, and brackets:
```json
{"RECID":"RECID-01","XMLDATA":"<data>...</data>","TIMESTAMP":1730000000}
```
If you stream 10 million transactions a day, you are paying for gigabytes of repetitive `"RECID"` and `"XMLDATA"` strings.

With Schema Registry and Avro:
- Field names are stored **once** in the registry.
- Kafka messages contain **only 5 bytes of header** followed by raw packed binary values:
  ```
  [0x00]                      <-- 1 Magic Byte (identifies Confluent format)
  [0x00, 0x00, 0x00, 0x2A]    <-- 4 Bytes (Schema ID: 42 in hex)
  [0x08, 0x52, 0x45...]       <-- Pure binary payload values
  ```
  Message size drops from **~1,200 bytes to ~70 bytes** (**>90% reduction**)!

#### 3. Single Source of Truth for Polyglot Microservices
Whether your consumer is written in **Java (Spring Boot)**, **Python**, **Go**, or **Node.js**, every service fetches the exact same Avro schema from `http://schema-registry-itp:8081`. Teams don't need to manually email DTO classes to each other.

---

### C. Step-by-Step: How a Message is Produced and Consumed

```
           ┌──────────────────────────────────────────────┐
           │ Confluent Schema Registry (Port :8081)       │
           │                                              │
           │  Subjects:                                   │
           │  • ITP.CORE_BANKING.RECORD_XML-value (ID 42) │
           └──────────────┬───────────────────────────────┘
                          │
       1. Registers &     │      3. Downloads Schema ID 42
          gets ID: 42     │         (Cached in RAM)
                          │
      ┌───────────────────┴───────────────────┐
      ▼                                       ▼
┌──────────────┐                         ┌──────────────┐
│  Debezium    │                         │ Spring Boot  │
│  (Producer)  │                         │ (Consumer)   │
└──────┬───────┘                         └──────▲───────┘
       │                                        │
       │ 2. Publishes:                          │ 4. Deserializes into
       │    [Magic Byte + ID 42 + Binary Data]  │    Envelope.java
       ▼                                        │
┌───────────────────────────────────────────────┴───────┐
│ Apache Kafka Topic: `ITP.CORE_BANKING.RECORD_XML`     │
└───────────────────────────────────────────────────────┘
```

1. **Schema Registration:** When Debezium starts up, it extracts the Oracle table structure, builds an Avro schema, and calls `POST http://schema-registry-itp:8081/subjects/ITP.CORE_BANKING.RECORD_XML-value/versions`.
2. **Schema ID Generation:** The Registry stores the schema and assigns it a unique number (e.g. `ID: 42`).
3. **Serialization:** Debezium encodes the Oracle row into binary bytes, prepends `[0x00] [ID 42]`, and pushes it to Kafka.
4. **Consumption:** When `pipeline-service` receives the message, its `io.confluent.kafka.serializers.KafkaAvroDeserializer` reads the first 5 bytes, extracts `ID: 42`, and checks its local memory cache.
5. **Local Cache Hit:** If it already has Schema 42 cached, it deserializes the binary payload instantly with zero network delay. If not cached, it queries Schema Registry once and caches it.

---

### D. Schema Registry REST API Cheat Sheet

You can inspect your schemas anytime in your browser or Postman at `http://localhost:8081`:

| Operation | HTTP Request | What it returns |
| :--- | :--- | :--- |
| **List all registered subjects** | `GET http://localhost:8081/subjects` | `["ITP.CORE_BANKING.RECORD_XML-value", ...]` |
| **Get latest schema for a topic** | `GET http://localhost:8081/subjects/ITP.CORE_BANKING.RECORD_XML-value/versions/latest` | Full JSON Avro schema definition + Schema ID |
| **Get schema by ID** | `GET http://localhost:8081/schemas/ids/42` | The exact Avro schema tied to ID 42 |
| **Check global compatibility level**| `GET http://localhost:8081/config` | `{"compatibilityLevel": "BACKWARD"}` |

---

## 7. Complete Comparison: Kafka vs Debezium vs Schema Registry

| Feature | Apache Kafka | Debezium | Confluent Schema Registry |
| :--- | :--- | :--- | :--- |
| **Analogy** | The **Highway & Trucks** 🚚 | The **Cargo Loader** 👷‍♂️ | The **Cargo Inspector & Passport Office** 🛂 |
| **Port in Docker** | `:9090` / `:29092` | `:8083` (Kafka Connect) | `:8081` |
| **Primary Job** | Store and stream messages | Capture database mutations | Enforce schemas and manage serialization IDs |
| **Storage** | Commit log partitions on disk | Offsets in Kafka topics | Schemas in a dedicated Kafka topic (`_schemas`) |
| **Data Format** | Raw bytes (dumb pipe) | Avro / JSON / Protobuf | JSON schema documents |

---

## 8. Summary in One Sentence

> **Debezium captures the database changes, Schema Registry ensures those changes conform to a strict contract and compresses them, and Kafka delivers them reliably to your microservices.**
