# Clinic Appointment Backend (WildFly + EJB3)

Modular REST backend built with Jakarta EE on WildFly, deployed as an EAR (WAR + EJB modules) with MySQL persistence.

## Why this design

- Multi-module Maven project — each domain has its own EJB JAR with its own JPA persistence unit
- Separate MySQL schema per module, shared datasource — multi-clinic support via `clinic_id` on every table
- No cross-module JPA relationships — inter-module communication via service interfaces only
- Soft delete, UTC timestamps, correlation IDs on every error response
- JWT-based auth with role enforcement: `ADMIN`, `USER`, `CUSTOMER`, `DOCTOR`
- Appointment business rules: no double-booking, schedule window validation, lead time, cancellation cutoff, reschedule limit, per-day booking cap

## Tech stack

| Layer | Technology |
|---|---|
| Runtime | WildFly 35.0.1 (Jakarta EE 10) |
| Language | Java 21 (`maven.compiler.release=21`) |
| Business | EJB3 `@Stateless` beans with `@Transactional` |
| REST | JAX-RS (RESTEasy) via `@Path` resources |
| Persistence | JPA/Hibernate 6.2 with Flyway schema migrations |
| Auth | JWT tokens (custom `JwtService` + `JwtAuthenticationFilter`) |
| Database | MySQL 8.4 |
| Packaging | EAR containing WAR + EJB JARs |
| Runtime | Docker Compose |

## Project structure

```
ejb-wildfly-sample/
├── pom.xml                          # Parent POM (multi-module, EJB 3.2)
├── docker-compose.yml               # MySQL + WildFly + Adminer
├── cli/
│   ├── configure-datasource.cli     # WildFly CLI script for datasource setup
│   ├── setup-wildfly.sh             # Shell wrapper for CLI script
│   └── wildfly-entrypoint.sh        # Docker entrypoint: auto-configures datasource + starts WildFly
├── clinic-common/                   # Shared: ApiError, CorrelationIdFilter, AuditEntry
├── clinic-security/                 # JwtService, JwtPrincipal
├── clinic-bom/                      # Bill of materials
├── clinic-audit-ejb/                # Persistent audit log (AuditService)
├── clinic-user-management-ejb/      # User management (login, seed admin)
├── clinic-customer-management-ejb/  # Customer CRUD
├── clinic-doctor-management-ejb/    # Doctor CRUD + specialties
├── clinic-schedule-management-ejb/  # Doctor schedule management
├── clinic-appointment-management-ejb/ # Appointment booking + business rules
├── clinic-api-war/                  # REST layer: resources, DTOs, JWT filter, OpenAPI
└── clinic-app-ear/                  # EAR packaging (deployed to WildFly)
```

### Module communication

| From | To | Mechanism |
|---|---|---|
| API WAR | Any EJB module | CDI `@Inject` (within same EAR) |
| API WAR | ScheduleManagementService (for appointment booking) | Interface injection + method parameters |
| EJB modules | Database | JPA `@PersistenceContext` (each module has its own PU) |

Cross-module JPA `@ManyToOne` / `@OneToMany` relationships are intentionally avoided. Each module owns its own tables and entities.

### JPA persistence units

Each EJB module defines its own `persistence.xml` with a dedicated persistence unit:

| Module | Persistence Unit | Schema |
|---|---|---|
| clinic-user-management-ejb | `userMgmtPU` | `user_mgmt` |
| clinic-customer-management-ejb | `customerMgmtPU` | `customer_mgmt` |
| clinic-doctor-management-ejb | `doctorMgmtPU` | `doctor_mgmt` |
| clinic-schedule-management-ejb | `scheduleMgmtPU` | `schedule_mgmt` |
| clinic-appointment-management-ejb | `appointmentMgmtPU` | `appointment_mgmt` |
| clinic-audit-ejb | `auditMgmtPU` | `audit_mgmt` |

All persistence units share the same `ClinicDS` JNDI datasource (MySQL user has cross-schema privileges), but each entity is mapped to its own real MySQL schema via `@Table(schema = ...)` — the six schemas above are created by `V1__create_schemas.sql`, and every other migration creates its tables directly inside the schema it owns. The `clinicdb` database named in the JDBC URL holds only Flyway's own `flyway_schema_history` bookkeeping table, not any domain data.

## Seed data

On first startup (and on redeploy, idempotently):

| Role | Username | Password | Notes |
|---|---|---|---|
| ADMIN, USER | `admin` | `Admin123` | Primary admin account |
| CUSTOMER | `johndoe` | — | Seeded in `customer_mgmt` |
| CUSTOMER | `janesmith` | — | Seeded in `customer_mgmt` |
| DOCTOR | `alicejohnson` | — | Cardiology, 30 min slots |
| DOCTOR | `bobwilliams` | — | General Practice, 20 min slots |

Doctor schedules are seeded for tomorrow and the day after (09:00–12:00 and 13:00–17:00 windows, capacity 5).

## Roles

| Role | Permissions |
|---|---|
| `ADMIN` | Full access: create users, doctors, customers; manage schedules; view/delete appointments |
| `USER` | Staff: create customers, view/update appointments |
| `CUSTOMER` | Book/view/cancel their own appointments |
| `DOCTOR` | View their appointments, update status |

Roles are enforced via `@RolesAllowed` on JAX-RS methods and validated against the JWT claims.

## Endpoints

Base URL: `http://localhost:8080/clinic-api/api`

### Auth (unauthenticated)

- `POST /auth/login` — Login, returns JWT token

### Health (unauthenticated)

- `GET /health` — Liveness check
- `GET /ready` — Readiness check
- `GET /openapi` — OpenAPI 3.0 specification

### Users (ADMIN)

- `GET /users?clinicId=` — List users
- `POST /users` — Create user

### Customers (USER, ADMIN)

- `GET /customers?clinicId=` — List customers
- `POST /customers` — Create customer

### Doctors

- `GET /doctors?clinicId=` — List doctors (all authenticated)
- `POST /doctors` — Create doctor (ADMIN)
- `GET /doctors/{doctorId}/schedules?clinicId=` — List schedules (all authenticated)
- `POST /doctors/{doctorId}/schedules` — Create schedule (ADMIN, USER)

### Appointments

- `GET /appointments?clinicId=` — List all (USER, ADMIN)
- `GET /appointments/{id}?clinicId=` — Get one (CUSTOMER, USER, ADMIN)
- `GET /appointments/customer/{customerId}?clinicId=` — By customer (CUSTOMER, USER, ADMIN)
- `GET /appointments/doctor/{doctorId}?clinicId=` — By doctor (DOCTOR, USER, ADMIN)
- `POST /appointments` — Book (CUSTOMER, USER, ADMIN)
- `PUT /appointments/{id}/reschedule?clinicId=` — Reschedule (CUSTOMER, USER, ADMIN)
- `PUT /appointments/{id}/cancel?clinicId=` — Cancel (CUSTOMER, USER, ADMIN)
- `PUT /appointments/{id}/status?clinicId=` — Update status (DOCTOR, USER, ADMIN)
- `DELETE /appointments/{id}?clinicId=` — Soft delete (ADMIN)

### Booking Wizard (step-by-step)

The booking wizard provides a multi-step appointment booking flow using server-side stateful sessions:

- `POST /bookings` — Start a booking session (returns `sessionId`)
- `GET /bookings/{sessionId}` — Get booking summary (current state)
- `PUT /bookings/{sessionId}/doctor` — Select a doctor
- `PUT /bookings/{sessionId}/schedule` — Select a schedule
- `PUT /bookings/{sessionId}/time` — Select an appointment time
- `PUT /bookings/{sessionId}/notes` — Add optional notes
- `POST /bookings/{sessionId}/confirm` — Confirm and create the appointment
- `DELETE /bookings/{sessionId}` — Cancel the booking session

The wizard uses a `@Stateful` EJB (`BookingSessionBean`) stored in a `@Singleton` registry. Sessions are in-memory and not persisted — they expire when the server restarts.

### Audit log (ADMIN)

- `GET /audit?clinicId=&entityType=&entityId=&actor=&limit=` — Query the persistent audit trail (`entityType`/`entityId`/`actor`/`limit` are optional filters, capped at 200 rows)

Every create/update/delete/activate/deactivate across users, customers, doctors, and schedules is recorded synchronously by the REST layer. Appointment lifecycle changes (book/cancel/reschedule) are recorded asynchronously by `AppointmentEventMDB` off the JMS queue below; status updates and deletes are recorded synchronously since they don't publish an event. Audit writes run in their own transaction (`clinic-audit-ejb`, `AuditService.record`, `REQUIRES_NEW`) so a failure to write an audit row never rolls back the business operation that triggered it.

### JMS Event Processing

Appointment state changes (book, cancel, reschedule) publish events to the `java:/jms/queue/AppointmentEvents` JMS queue. An `AppointmentEventMDB` consumes these events and persists them as audit log entries.

Events are JSON-serialized `AppointmentEvent` records containing: `eventType`, `appointmentId`, `clinicId`, `customerId`, `doctorId`, `scheduleId`, `appointmentDate`, `appointmentTime`, `status`, `actor`, `correlationId`, and `timestamp`.

### Appointment business rules

- `appointmentTime` must be within the schedule's `startTime`–`endTime` window
- Appointment must be at least 60 minutes in the future (lead time)
- No double-booking: same doctor + same time slot + same schedule → rejected
- Cancel: only `BOOKED` appointments, and only before the 120-minute cutoff
- Reschedule: only `BOOKED` appointments, max 3 reschedules per appointment
- Per-day booking limit: max 5 appointments per customer per day

## Docker setup

### Prerequisites

- Docker and Docker Compose v2
- Maven 3.9+
- Java 21+

### Start services

```bash
docker compose up -d
```

This starts MySQL → Flyway → WildFly in sequence:

| Service | Port | Description |
|---|---|---|
| `clinic-mysql` | 3306 | MySQL 8.4 with `clinicdb` database |
| `clinic-flyway` | — | Flyway schema migrations (runs once at startup) |
| `clinic-wildfly` | 8080 (HTTP), 9990 (admin) | WildFly 35.0.1 |
| `clinic-adminer` | 8081 | DB admin UI (`--profile tools` to start) |

### WildFly datasource configuration

**Fully automated.** The entrypoint script (`cli/wildfly-entrypoint.sh`) runs on first WildFly startup:

1. Downloads the MySQL JDBC driver from Maven Central (if not cached)
2. Installs it as a WildFly module (`com.mysql.mysql-connector-j`) with a `module.xml` descriptor
3. Registers the `ClinicDS` datasource via an embed-server CLI script
4. Cleans stale deployment markers
5. Starts WildFly in the foreground with `standalone-full.xml` (required for JMS/MDB support)

No manual CLI steps needed. On subsequent restarts, the script detects the existing driver and datasource and skips reconfiguration.

If you need to reconfigure manually:

```bash
docker exec -i clinic-wildfly /opt/jboss/wildfly/bin/jboss-cli.sh --connect --command="/subsystem=datasources/data-source=ClinicDS:test-connection-in-pool"
```

### Build and deploy

```bash
# Build all modules
mvn clean package -DskipTests

# The EAR is built to: clinic-app-ear/target/clinic-app.ear
# It is volume-mounted into WildFly's deployments/ directory.
# Trigger deployment:
touch clinic-app-ear/target/clinic-app.ear.dodeploy

# Or if already deployed, force redeploy:
rm -f clinic-app-ear/target/clinic-app.ear.deployed
touch clinic-app-ear/target/clinic-app.ear.dodeploy
```

WildFly's deployment scanner picks up the `.dodeploy` marker and deploys the EAR automatically.

### Useful commands

```bash
# Check deployment status
docker exec clinic-wildfly ls /opt/jboss/wildfly/standalone/deployments/

# WildFly logs
docker logs -f clinic-wildfly

# Reconfigure datasource (if needed)
docker exec -i clinic-wildfly /opt/jboss/wildfly/bin/jboss-cli.sh --connect \
  --command="/subsystem=datasources/data-source=ClinicDS:test-connection-in-pool"

# Connect to MySQL
docker exec -it clinic-mysql mysql -uroot -proot123

# Query tables in a given module's schema
docker exec clinic-mysql mysql -uroot -proot123 -e "SHOW TABLES FROM appointment_mgmt;"

# List all clinic schemas
docker exec clinic-mysql mysql -uroot -proot123 -e "SHOW SCHEMAS;"

# Open Adminer (DB admin UI)
# Available at http://localhost:8081 when started with --profile tools
docker compose --profile tools up -d adminer

# Run smoke tests (32 tests)
./smoke-tests.sh
```

## Smoke tests

See `smoke-tests.sh` for the full test script, or run manually:

```bash
# Login and get token
TOKEN=$(curl -s -X POST http://localhost:8080/clinic-api/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"clinicId":1,"username":"admin","password":"Admin123"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

# Health check
curl -s http://localhost:8080/clinic-api/api/health

# List doctors
curl -s -H "Authorization: Bearer $TOKEN" "http://localhost:8080/clinic-api/api/doctors?clinicId=1"

# Book appointment
curl -s -X POST "http://localhost:8080/clinic-api/api/appointments?clinicId=1" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"clinicId":1,"customerId":1,"doctorId":1,"scheduleId":1,"appointmentTime":"09:30","notes":"Test"}'
```

## Error responses

All errors return a consistent JSON structure with a correlation ID for debugging:

```json
{
  "code": "REQUEST_ERROR",
  "message": "doctor is already booked for this time slot",
  "correlationId": "4a26cfaf-090e-47c4-83ab-13ed0d57f381"
}
```

| Code | HTTP Status | Meaning |
|---|---|---|
| `REQUEST_ERROR` | 400/404 | Invalid input, not found, business rule violation |
| `INTERNAL_ERROR` | 500 | Unexpected server error |
| `UNAUTHORIZED` | 401 | Missing or invalid JWT token |
