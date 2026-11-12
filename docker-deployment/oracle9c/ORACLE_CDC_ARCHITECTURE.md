# Oracle CDC & Log Architecture Guide
> **In-Depth Guide: LogMiner vs Redo Logs vs Archive Logs vs Supplemental Logging**

This document explains how Oracle Database logs work, how transactions flow under the hood, and how Change Data Capture (CDC) engines like **Debezium** extract real-time event streams into **Apache Kafka**.

---

## 1. Architecture Flowchart

```
1. SQL Query Runs
   UPDATE ACCOUNT SET BALANCE = 1500 WHERE ID = 101;
                       │
                       ▼
2. Supplemental Logging (The Rule)
   "Hey Oracle, make sure you write BOTH the old balance,
    the new balance, AND the ID=101 into the log!"
                       │
                       ▼
3. Redo Log (Current Active Tape)
   Stores real-time binary transaction entries in memory/disk.
   (Circular buffer: Log 1 ➔ Log 2 ➔ Log 3 ➔ overwrites Log 1)
                       │
                       │ When Redo Log gets full, Oracle copies it to:
                       ▼
4. Archive Log (Permanent Storage Tape)
   Saved copies of old Redo Logs so history is never lost.
                       │
                       ▼
5. LogMiner (The Decoder / Reader)
   Reads the binary Redo & Archive logs, decodes them into
   human-readable SQL/JSON, and hands them to Debezium/Kafka.
                       │
                       ▼
6. Kafka Topic (Event Streaming)
   { "before": {"BALANCE": 1000}, "after": {"BALANCE": 1500} }
```

---

## 2. Side-by-Side Comparison

| Component | What is it? | Is it File, Tool, or Setting? | What is its job? |
| :--- | :--- | :--- | :--- |
| **Redo Logs** | The **live diary** of recent changes | **File** (Binary, Circular) | Keeps an immediate record of every change for crash recovery. It is small and constantly overwritten in a circular loop (e.g., Log 1 &rarr; Log 2 &rarr; Log 3 &rarr; Log 1). |
| **Archive Logs** | The **history shelf** of past Redo Logs | **File** (Permanent storage) | When a Redo Log gets full, Oracle saves a permanent backup copy (Archive Log) before overwriting it. Keeps historical changes safe indefinitely. |
| **Supplemental Logging** | The **detail setting** | **Configuration / Setting** | Instructs Oracle: *"Don't just log the changed column; also include the Primary Key, old values, and other columns so CDC tools know what happened."* |
| **LogMiner** | The **translator / decoder** | **Tool / Package** (`DBMS_LOGMNR`) | An Oracle utility that reads the proprietary binary format of Redo and Archive logs and translates them into readable SQL statements (`INSERT`, `UPDATE`, `DELETE`). |

---

## 3. Real-World Analogy: A Banking Office

* **Redo Log:** The cashier's **daily scratchpad**. Every deposit or withdrawal is quickly scribbled down. Space is limited, so once the page is full, it flips to the next page.
* **Archive Log:** The **permanent filing cabinet**. At the end of the hour, the filled scratchpad is photocopied and stored in the archive cabinet so past records are never lost.
* **Supplemental Logging:** The **instruction manual rule** for the cashier: *"When writing an update, don't just write '$1,500'. Also write the customer's Account Number and previous balance!"*
* **LogMiner:** The **auditor**. The auditor goes to the scratchpad (Redo Log) or filing cabinet (Archive Log), reads the shorthand notes, and writes a clean audit report for external systems (like Kafka/Debezium).

---

## 4. Deep-Dive: Each Component with Practical Examples

---

### A. Redo Logs (Online Redo Logs) & The Circular Buffer

#### What is a Circular Buffer?
A **Circular Buffer** (or ring buffer) is a fixed-size data structure that connects end-to-end like a circle. When it reaches the end, it wraps around and writes back at the beginning, **overwriting the oldest data**.

Think of a circular carousel or a clock:
```
           ┌─────────── Group 1 ───────────┐
           │        (e.g., 100 MB)         │
           │                               │
      Overwrites                      Fills up & switches
       when full                       (Log Switch)
           │                               │
           ▼                               ▼
      Group 3 ◀──────── Log Switch ───── Group 2
   (e.g., 100 MB)                      (e.g., 100 MB)
```

1. **Step 1:** Oracle writes transactions into **Group 1**.
2. **Step 2:** When Group 1 fills up to its limit (e.g., 100 MB), a **Log Switch** occurs. Oracle moves to **Group 2**.
3. **Step 3:** When Group 2 fills up, Oracle moves to **Group 3**.
4. **Step 4 (The Circle Closes):** When Group 3 fills up, Oracle wraps back around to **Group 1** and begins **overwriting** it.

> [!WARNING]
> **The Danger of Overwriting in a Circular Buffer:**
> If Oracle overwrites Group 1 before those old transactions are copied out, **that change history is permanently lost!** This is why **Archive Logs** exist: before Group 1 is overwritten, Oracle copies it to a permanent file.

#### What are Online Redo Logs?
Online Redo Logs are physical disk files where Oracle logs **every single database mutation** before writing it to datafiles. This is called **Write-Ahead Logging (WAL)**. Oracle uses the circular buffer pattern across 3 or more redo log groups:
```
Group 1 (Active) ──▶ Group 2 (Current) ──▶ Group 3 (Unused) ──▶ (cycles back to Group 1)
```

#### Code Example / Query:
```sql
-- View your current Redo Log groups and their status:
SELECT GROUP#, THREAD#, SEQUENCE#, BYTES/1024/1024 AS SIZE_MB, STATUS, ARCHIVED 
FROM V$LOG;

-- View the actual physical files on disk:
SELECT GROUP#, MEMBER FROM V$LOGFILE;
```

#### What happens during a transaction:
1. You run: `INSERT INTO CORE_BANKING.ACCOUNT (ID, BALANCE) VALUES (101, 500);`
2. Oracle writes the change to the in-memory **Redo Log Buffer**.
3. Upon `COMMIT;`, Oracle's Log Writer process (`LGWR`) flushes this buffer to the active **Online Redo Log file** on disk.

---

### B. Archive Logs (Archived Redo Logs)

#### What is it?
Because Online Redo Logs are small and circular, once Group 3 fills up, Oracle needs to overwrite Group 1.
- In **NOARCHIVELOG** mode: Oracle simply overwrites Group 1. Old transaction history is erased forever!
- In **ARCHIVELOG** mode: Before Group 1 is overwritten, the Oracle Archiver background process (`ARCn`) makes an exact copy and saves it to a persistent folder (Archive Log).

#### Why CDC needs it:
If your Kafka consumer or Debezium connector goes down for 2 hours, Oracle's active Redo Logs will have recycled several times. Thanks to **Archive Logs**, Debezium can read the missed logs and catch up without losing a single event!

#### Code Example / Query:
```sql
-- Check if Archivelog is enabled:
SELECT LOG_MODE FROM V$DATABASE;
-- Result: 'ARCHIVELOG' (Enabled) or 'NOARCHIVELOG' (Disabled)

-- View generated archive log files:
SELECT NAME, SEQUENCE#, FIRST_CHANGE#, NEXT_CHANGE#, COMPLETION_TIME 
FROM V$ARCHIVED_LOG 
ORDER BY COMPLETION_TIME DESC;

-- Enable Archivelog mode (requires DB mount state):
ALTER SYSTEM SET DB_RECOVERY_FILE_DEST_SIZE = 10G;
ALTER SYSTEM SET DB_RECOVERY_FILE_DEST = '/opt/oracle/oradata/recovery_area' SCOPE=SPFILE;
SHUTDOWN IMMEDIATE;
STARTUP MOUNT;
ALTER DATABASE ARCHIVELOG;
ALTER DATABASE OPEN;
```

---

### C. Supplemental Logging

#### What is it?
By default, Oracle minimizes log size by recording only the changed column and row pointer. For database recovery, that is enough. But for **Change Data Capture (CDC)**, it creates a massive blind spot.

#### Concrete Example:

Suppose you have this row in `CORE_BANKING.CUSTOMER`:
| ID | NAME | EMAIL | STATUS |
| :--- | :--- | :--- | :--- |
| `101` | `Dara Sok` | `dara@old.com` | `ACTIVE` |

Now you run this SQL:
```sql
UPDATE CORE_BANKING.CUSTOMER 
SET EMAIL = 'dara@new.com' 
WHERE ID = 101;
```

##### ❌ Case 1: WITHOUT Supplemental Logging
The Redo Log record only contains:
- `ROWID: AAAWwOAAFAAAAcvAAA`
- `EMAIL: dara@new.com`

> **The Problem:** Debezium doesn't know *which* customer this belongs to because `ID: 101` is **not in the log**! It cannot produce a valid Kafka message with a partition key.

##### ✅ Case 2: WITH Supplemental Logging Enabled
```sql
-- Enable database-level supplemental logging:
ALTER DATABASE ADD SUPPLEMENTAL LOG DATA;
ALTER DATABASE ADD SUPPLEMENTAL LOG DATA (ALL) COLUMNS;

-- Or enable on a specific table:
ALTER TABLE CORE_BANKING.CUSTOMER ADD SUPPLEMENTAL LOG DATA (ALL) COLUMNS;
```

The Redo Log now captures:
- Primary Key: `ID = 101`
- Old Values (BEFORE): `{ "NAME": "Dara Sok", "EMAIL": "dara@old.com", "STATUS": "ACTIVE" }`
- New Values (AFTER): `{ "NAME": "Dara Sok", "EMAIL": "dara@new.com", "STATUS": "ACTIVE" }`

Now Debezium can send a complete JSON payload to Kafka:
```json
{
  "before": {
    "ID": 101,
    "NAME": "Dara Sok",
    "EMAIL": "dara@old.com"
  },
  "after": {
    "ID": 101,
    "NAME": "Dara Sok",
    "EMAIL": "dara@new.com"
  },
  "op": "u"
}
```

---

### D. LogMiner (`DBMS_LOGMNR`)

#### What is it?
LogMiner is Oracle's built-in engine that reads the raw binary redo files and translates them into reconstructed SQL.

#### Example: How LogMiner Decodes Binary into SQL

When Debezium runs under user `c##dbzuser`, it executes:
```sql
-- 1. Tell LogMiner which log file to read:
EXECUTE DBMS_LOGMNR.ADD_LOGFILE( -
   LOGFILENAME => '/opt/oracle/oradata/ORCLCDB/redo01.log', -
   OPTIONS => DBMS_LOGMNR.ADDFILE);

-- 2. Start LogMiner using the online dictionary:
EXECUTE DBMS_LOGMNR.START_LOGMNR( -
   OPTIONS => DBMS_LOGMNR.DICT_FROM_ONLINE_CATALOG + DBMS_LOGMNR.CONTINUOUS_MINE);

-- 3. Query the decoded SQL changes:
SELECT OPERATION, SQL_REDO, SQL_UNDO, TIMESTAMP 
FROM V$LOGMNR_CONTENTS 
WHERE SEG_OWNER = 'CORE_BANKING';
```

##### What `V$LOGMNR_CONTENTS` outputs:

| OPERATION | SQL_REDO (What happened) | SQL_UNDO (How to rollback) |
| :--- | :--- | :--- |
| `UPDATE` | `update "CORE_BANKING"."CUSTOMER" set "EMAIL" = 'dara@new.com' where "ID" = 101 and "EMAIL" = 'dara@old.com';` | `update "CORE_BANKING"."CUSTOMER" set "EMAIL" = 'dara@old.com' where "ID" = 101 and "EMAIL" = 'dara@new.com';` |
| `INSERT` | `insert into "CORE_BANKING"."ACCOUNT"("ID","BALANCE") values (101, 1500);` | `delete from "CORE_BANKING"."ACCOUNT" where "ID" = 101;` |

Debezium continuously monitors `V$LOGMNR_CONTENTS`, transforms these decoded `SQL_REDO` entries into structured JSON events, and publishes them immediately to Kafka topics!

---

## 5. Summary Cheat Sheet

| Question | Answer |
| :--- | :--- |
| **Where are changes written first?** | **Redo Logs** (immediate, circular) |
| **Where do changes go so they are never lost?** | **Archive Logs** (permanent backup of Redo Logs) |
| **Why does Debezium need Supplemental Logging?** | Without it, the logs only store changed fields, omitting primary keys and old values. |
| **Who decodes the binary logs for Debezium?** | **LogMiner** (`DBMS_LOGMNR` / `V$LOGMNR_CONTENTS`) |
