# Functional Specification

## 1. System Overview
The **Financial Message Processing Application** is an enterprise-grade solution built using Spring Boot 3 and Java 21 designed to ingest, parse, validate, and respond to standardized ISO 20022 and generic XML financial messages. The system incorporates configurable authentication modes (Database or LDAP) and uses Oracle as the persistent system-of-record storage.

---

## 2. System Flow Diagram
```
                     +---------------------------------------+
                     |         API Client / Swagger          |
                     +---------------------------------------+
                                         |
                       Registers User / Authenticates
                                         v
                     +---------------------------------------+
                     |             Spring Security           |
                     |  - Basic Authentication               |
                     |  - Configurable: DB or LDAP Provider  |
                     +---------------------------------------+
                                         |
                         Valid Authorized XML Payload
                                         v
                     +---------------------------------------+
                     |       FinancialMessageController      |
                     +---------------------------------------+
                                         |
                               Parse & Detect Type
                                         v
                     +---------------------------------------+
                     |        FinancialMessageService        |
                     |  - pain.001 -> pain.002 (Reply)       |
                     |  - pacs.008 -> pacs.002 (Reply)       |
                     |  - camt.053 -> camt.054 (Reply)       |
                     |  - generic XML -> response wrapper    |
                     +---------------------------------------+
                                         |
                        Persist Request & Response Logs
                                         v
                     +---------------------------------------+
                     |           Oracle / H2 DB              |
                     +---------------------------------------+
```

---

## 3. Core Features & Capabilities

### 3.1 Custom & Standard Message Ingestion
- **ISO 20022 Standards Supported**:
  - `pain.001` (Customer Credit Transfer Initiation) -> Autoreply with `pain.002` (Customer Payment Status Report)
  - `pacs.008` (FIToFI Customer Credit Transfer) -> Autoreply with `pacs.002` (FIToFI Payment Status Report)
  - `camt.053` (Bank To Customer Statement) -> Autoreply with `camt.054` (Bank To Customer Debit Credit Notification)
- **Generic XML Messages**:
  - Automatically extracts custom root tag and generates a corresponding `<RootTag>Response` wrapper outputting processing state, timestamp, and message ID reference.

### 3.2 Dual Authentication Model
The security configuration relies on Spring Security and allows seamless toggling using configuration keys (`app.security.ldap-enabled`):
1. **LDAP Authentication Mode**: Directs authentication checks towards active LDAP Directory Servers.
2. **Local Database Mode**: Fallback to encrypted credentials stored on the local database (Postgres/Oracle/H2).

### 3.3 Strict Account Validation Rules
Account password creations are validated against configuration-driven regular expressions:
- Default Validation requirements:
  - Minimum of 6 characters in length.
  - At least 1 alphabetic character.
  - At least 1 digit character.

### 3.4 Interactive Documentation & Swagger Integration
- Built-in UI (`/swagger-ui.html`) to visualize and invoke all endpoints interactively.

---

## 4. Database Schema Specification
The persistence layer operates on two core entities:

### 4.1 `users` Table
Stores registered local accounts used when LDAP authentication is turned off.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | Primary Key, Auto-Increment | Unique identifier. |
| `username` | `VARCHAR(255)` | Unique, Not Null | Unique username. |
| `password` | `VARCHAR(255)` | Not Null | Bcrypt hash password. |
| `role` | `VARCHAR(255)` | Not Null | e.g. `ROLE_USER`. |

### 4.2 `financial_messages` Table
Audits and logs all processed XML and ISO 20022 operations.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | Primary Key, Auto-Increment | Message record key. |
| `message_type` | `VARCHAR(255)` | Not Null | Code identifier (e.g. `pain.001`). |
| `payload_request` | `CLOB` | Not Null | Input request XML document. |
| `payload_response` | `CLOB` | Not Null | Auto-generated standard reply XML document. |
| `created_at` | `TIMESTAMP` | Not Null | Processing timestamp. |
| `processed_by_user`| `VARCHAR(255)` | Nullable | Username of the agent. |
