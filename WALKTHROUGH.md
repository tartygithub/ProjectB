# End-to-End Walkthrough

This walkthrough guides you through the process of registering a user, logging in, and sending financial ISO 20022 and XML messages.

## Step 1: Accessing the Application

Start the application using Maven, Docker, or Kubernetes.
- Swagger UI will be accessible at: `http://localhost:8080/swagger-ui.html`
- OpenAPI Docs can be viewed at: `http://localhost:8080/api-docs`

---

## Step 2: User Registration (Account Creation)

Create a user account. Our application validates that the password contains at least 6 characters, one letter, and one number.

**Endpoint:** `POST /api/auth/register`
**Content-Type:** `application/json`

### Request Payload:
```json
{
  "username": "financial_agent",
  "password": "SecurePassword1"
}
```

### Expected Response:
```text
User registered successfully: financial_agent
```

---

## Step 3: User Authentication (Login)

Log in to authenticate your user credentials. Authentication can be dynamically configured to run against the application database or an LDAP directory server.

**Endpoint:** `POST /api/auth/login`
**Content-Type:** `application/json`

### Request Payload:
```json
{
  "username": "financial_agent",
  "password": "SecurePassword1"
}
```

### Expected Response:
```text
Login successful for user: financial_agent
```

---

## Step 4: Submitting a `pain.001` Financial Message (ISO 20022)

Once logged in, send an ISO 20022 `pain.001` message (Customer Credit Transfer Initiation). The application will process, persist, and automatically return a matching ISO 20022 `pain.002` response (Customer Payment Status Report).

**Endpoint:** `POST /api/messages`
**Content-Type:** `application/xml`
**Headers:** Include Basic Authentication (`financial_agent` / `SecurePassword1`)

### Request XML Payload:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Document xmlns="urn:iso:std:iso:20022:tech:xsd:pain.001.001.08">
  <CstmrCdtTrfInitn>
    <GrpHdr>
      <MsgId>TXN-PAIN-99001</MsgId>
      <CreDtTm>2026-07-18T12:00:00</CreDtTm>
    </GrpHdr>
  </CstmrCdtTrfInitn>
</Document>
```

### Expected Response Payload (`pain.002`):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Document xmlns="urn:iso:std:iso:20022:tech:xsd:pain.002.001.10">
  <CstmrPmtStsRpt>
    <GrpHdr>
      <MsgId>RPT-1779900001</MsgId>
      <CreDtTm>2026-07-18T12:00:05.123</CreDtTm>
    </GrpHdr>
    <OrgnlGrpInfAndSts>
      <OrgnlMsgId>TXN-PAIN-99001</OrgnlMsgId>
      <OrgnlMsgNmId>pain.001.001.08</OrgnlMsgNmId>
      <GrpSts>ACTC</GrpSts>
    </OrgnlGrpInfAndSts>
  </CstmrPmtStsRpt>
</Document>
```

---

## Step 5: Submitting a Custom/Generic Financial XML Message

You can send custom financial XML payloads. The application detects the custom root element and replies using the same generic standard.

**Endpoint:** `POST /api/messages`
**Content-Type:** `application/xml`
**Headers:** Include Basic Authentication (`financial_agent` / `SecurePassword1`)

### Request XML Payload:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<DirectDebitCollectionRequest>
  <MsgId>DD-REF-007</MsgId>
  <Amount currency="USD">25000.00</Amount>
</DirectDebitCollectionRequest>
```

### Expected Response Payload:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<DirectDebitCollectionRequestResponse>
  <Status>PROCESSED</Status>
  <OriginalMsgId>DD-REF-007</OriginalMsgId>
  <Timestamp>2026-07-18T12:05:00</Timestamp>
</DirectDebitCollectionRequestResponse>
```
