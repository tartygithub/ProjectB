# User Instructions and Run Guide

Follow these steps to build, run, and test the Financial Message Processing Application.

---

## 1. Prerequisites
- **Java**: JDK 21
- **Maven**: Version 3.8+
- **Docker** & **Docker Compose** (for multi-container tests)
- **Kubernetes Client / Cluster** (optional, for deployment)

---

## 2. Configuration Settings (`src/main/resources/application.yml`)
The security and operational parameters are configured inside `application.yml`:

- `app.security.ldap-enabled`: Set to `true` to enable LDAP authentication; set to `false` (default) to fallback to Database authentication.
- `app.security.basic-validation-regex`: Custom Regex specifying password requirements (Defaults to at least 6 characters, 1 letter, and 1 number).

---

## 3. Running with Maven (Local Profile)
To quickly run the application locally on port `8080` (utilizing the in-memory H2 database simulation):

```bash
mvn clean spring-boot:run -Dspring-boot.run.profiles=test
```

The server starts up and outputs logs in your terminal. You can visit `http://localhost:8080/swagger-ui.html` immediately.

---

## 4. Running unit & integration tests
Run standard Junit test suites via Maven:

```bash
mvn clean test
```

---

## 5. Running with Docker & Compose
To compile the application inside a multi-stage Docker file and start the integrated stack (Spring Boot container and Oracle Database):

1. **Build the image**:
   ```bash
   docker build -t financial-message-app:1.0.0 .
   ```

2. **Start Docker Compose Stack**:
   We have provided Kubernetes configuration specs to configure and spin up deployments inside a Kubernetes cluster as well.

---

## 6. Running on Kubernetes
Apply the deployment manifests located in the `k8s` directory:

```bash
# Deploy Oracle DB StatefulSet
kubectl apply -f k8s/oracle-db-statefulset.yaml

# Deploy Spring Boot Web Application
kubectl apply -f k8s/app-deployment.yaml
```

To monitor the deployments:
```bash
kubectl get pods
kubectl get services
```

---

## 7. Endpoint References

### 7.1 Registration
- **URL**: `POST /api/auth/register`
- **Body**: JSON with username and password.

### 7.2 Login
- **URL**: `POST /api/auth/login`
- **Body**: JSON with username and password.

### 7.3 Financial Messages Ingestion
- **URL**: `POST /api/messages`
- **Auth**: Basic Authentication
- **Content-Type**: `application/xml`
- **Body**: XML standard payload.
- **Returns**: Matching response standard.
