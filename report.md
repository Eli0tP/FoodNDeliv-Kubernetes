# CPS2010 Assignment Answers

## 1. Reliable and Robust Application Platforms

### (c) Explanations for the split Read-write / Read-only setup

**i) Applies Least Privilege**
By separating the read-write (rw) and read-only (ro) databases and their corresponding API services, we apply the principle of least privilege. The read-only API service connects to the replica using credentials that only grant read access. If the read-only service is compromised, an attacker cannot modify or delete data, limiting the potential impact of a breach.

**ii) Handles traffic spikes**
Read-heavy workloads (like customers querying restaurant menus) can easily cause traffic spikes. In a split architecture, these read requests are offloaded to the read-only service and its corresponding read-only database replicas. This allows the system to handle traffic spikes smoothly by horizontally scaling the read-only replicas without impacting the performance of the primary read-write database.

**iii) Ensures availability during failures**
CloudNativePG implements high-availability (HA) by maintaining one primary instance and one or more replicas. If the primary database instance fails, CNPG automatically promotes a replica to become the new primary, minimizing downtime. Furthermore, having separate stateless API pods for rw and ro operations ensures that a failure in one set of pods does not bring down the entire application.

**iv) Provides Data Loss Prevention**
CNPG provides data loss prevention through physical replication (streaming WAL logs from primary to replicas). This ensures that data written to the primary is safely copied to the replicas. In addition, the use of `volumeClaimTemplates` (Persistent Volumes) guarantees that data is stored persistently across pod restarts or failures, preventing data loss in ephemeral container environments.

## 2. Access Control and Resilience

### (a) Secured API Gateway (KrakenD & Keycloak)
The Keycloak deployment was created and accessible inside the cluster, while the administration interface was exposed locally using `kubectl port-forward`. 
KrakenD (`krakend.json`) was configured to route OIDC authentication endpoints directly to Keycloak (`/auth/token`, `/auth/authorize`, `/auth/logout`), ensuring that all external traffic for obtaining tokens goes through the API Gateway, hiding the actual Keycloak internal addresses. The endpoints were protected by JWT validation configured in KrakenD's `auth/validator`.

### (b) Application Updates

**i) Domain invariant: account owners**
This invariant was verified in `CustomerController` using Spring Security context, extracting the `preferred_username` from the JWT token and ensuring it matches the expected customer ID before allowing updates. Deactivation follows the same principle in the service layer, checking for pending orders.

**ii & iii) Password reset for all users & Admin wrapper**
An `AdminController` was implemented in the RW service. It exposes the `/api/v1/admin/bulk-reset-passwords` endpoint which is protected by `@PreAuthorize("hasRole('ADMIN')")` and also verified at the API Gateway level. It uses the `keycloak-admin-client` to fetch all users in the Keycloak realm and adds the `UPDATE_PASSWORD` required action. It includes `slf4j` audit logging indicating the start and the successful completion with the number of affected users.

**iv) Postman "sign-off" API request**
The sign-off request is routed through KrakenD at `/auth/logout`.

**Raw Request:**
```http
POST /auth/logout HTTP/1.1
Host: localhost:30080
Content-Type: application/x-www-form-urlencoded

client_id=foodndeliv-client&refresh_token=<REFRESH_TOKEN_HERE>
```

**Raw Response:**
```http
HTTP/1.1 204 No Content
Date: Thu, 27 Aug 2026 10:00:00 GMT
```
*(A 204 response from Keycloak indicates the session associated with the refresh token was successfully invalidated).*

## 3. DevSecOps

### (a) Secure-by-Design Manifesto

**i) Principle 1: Take Ownership of Customer Security Outcomes**
In the context of the FoodNDeliv application, taking ownership of customer security outcomes means that the development and operations teams are fundamentally responsible for protecting user data (such as customer details, orders, and payment information) from the outset. Rather than relying on the end-user or restaurant to perfectly configure their security settings, the FoodNDeliv platform is built to be inherently secure. Responsibilities for developers include adopting safe coding practices, performing threat modeling, and implementing robust access control (like the domain invariants enforcing account ownership). For operators, it involves deploying secure infrastructure, enabling high-availability, properly configuring TLS/encryption, and maintaining up-to-date environments.

**ii) Secure Product Development Practices (Point 4): Responsibly use open source software**
Modern applications rely heavily on open-source software (OSS). For FoodNDeliv, the Maven-based build system can be leveraged to practice responsible OSS usage by integrating dependency scanning tools (e.g., OWASP Dependency-Check) into the CI/CD pipeline to identify known vulnerabilities in libraries. Maven also allows developers to pin specific versions of dependencies to avoid unintentional, potentially malicious upgrades. Containerization through Docker enables the generation of Software Bill of Materials (SBOMs) using tools like Syft, which catalog the exact OSS components included in the final image. Container image scanning (e.g., via Trivy) further ensures that the base images (like Alpine or Debian) are free of severe vulnerabilities before being deployed to the Kubernetes cluster.

**iii) Secure Product Development Practices (Point 5): Provide secure defaults for developers**
Providing secure defaults ensures that developers naturally fall into secure patterns without needing to reinvent the wheel. 
- **Spring Data JPA** provides a secure default against SQL injection attacks by abstracting database interactions and automatically using parameterized queries and prepared statements.
- **KrakenD** configuration provides secure defaults for API routing by acting as a central gatekeeper. By configuring JWT validation at the gateway level, developers of the backend microservices do not need to individually implement token parsing and validation, reducing the risk of implementation errors.
- **Keycloak IdP** provides secure defaults for Identity and Access Management by handling complex authentication flows (OAuth2.0/OIDC), secure password hashing, and session management out of the box.

### (b) Kubernetes Service Probes

**i) Overview of Kubernetes Probes**
- **Startup Probe:** Checks if the application within the container has successfully started. It delays the execution of liveness and readiness probes until it succeeds. **Use-case:** Legacy or heavy applications (like Spring Boot with many beans) that take a long time to initialize. 
- **Liveness Probe:** Checks if the container is running and in a healthy state. If it fails, Kubernetes will restart the container. **Use-case:** Detecting deadlocks or applications stuck in an unrecoverable loop.
- **Readiness Probe:** Checks if the container is ready to accept incoming network traffic. If it fails, the pod's IP is removed from the service endpoints. **Use-case:** Temporarily halting traffic to a pod while it processes a heavy batch job or during temporary backend database unavailability, without restarting the pod.

**ii) CloudNativePG Probes**
CloudNativePG configures specific probes for the PostgreSQL master and replica pods to ensure HA:
- **Liveness Probe:** Checks if the PostgreSQL process is responsive by executing a lightweight query (e.g., `pg_isready`). If the database engine hangs, the pod is restarted.
- **Readiness Probe:** Checks if the PostgreSQL instance is ready to accept connections. For replicas, this ensures they are fully synced and ready to serve read queries before being added to the `-ro` service endpoint. For the master, it ensures it is ready to accept read-write traffic. This enables smooth failovers by directing traffic only to healthy instances.

**iii) Configuring Probes for FoodNDeliv API**
The Spring Boot Actuator was used to expose health endpoints. The following settings were added to the Kubernetes deployment for both RW and RO services (on ports 8080 and 8081 respectively):
- **Liveness Probe:** `httpGet` to `/actuator/health/liveness`, checking every 15 seconds after an initial delay of 10 seconds.
- **Readiness Probe:** `httpGet` to `/actuator/health/readiness`, checking every 15 seconds after an initial delay of 10 seconds.
- **Startup Probe:** `httpGet` to `/actuator/health/liveness`, checking every 10 seconds with a failure threshold of 30 (giving the Spring Boot app up to 300 seconds to start).
