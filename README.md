# SecureBank

SecureBank is a full-stack banking project built with Spring Boot, React, PostgreSQL, RabbitMQ, Docker, and a separate notification microservice.

The project is not a real banking system. It is designed to demonstrate realistic backend engineering decisions for a fintech-style application: secure authentication, account ownership checks, money transfers, audit logging, event-driven messaging, email notifications, and retry/DLQ handling.

## Status

The project is feature-complete for the current project scope.

- Backend core is stable and tested.
- Frontend is implemented in `securebank-frontend`.
- Notification service is implemented in `securebank-notification-service`.
- Docker Compose runs backend, PostgreSQL, RabbitMQ, and notification service together.
- Real SMTP transfer emails work when configured.

## Live Deployment

```text
Frontend: https://d1zrdkm958a5xk.cloudfront.net
Backend API base URL: https://api-63-180-21-244.nip.io
```

The production stack runs on AWS with the frontend served through S3 and CloudFront, and the backend stack running on EC2 with Docker Compose.

The API base URL is not a standalone web page. Protected endpoints return `401 Unauthorized` unless a valid JWT is provided.

## Architecture

```text
React Frontend
  -> Spring Boot Backend
     -> PostgreSQL
     -> RabbitMQ
        -> Notification Service
           -> SMTP Provider
           -> notifications table
```

The banking backend is a modular monolith. The notification service is a separate Spring Boot service that consumes transfer events asynchronously through RabbitMQ.

## Tech Stack

- Java 17, Spring Boot 3, Spring Security, Spring Data JPA
- PostgreSQL
- JWT access tokens, HttpOnly refresh-token cookie
- RabbitMQ
- React, TypeScript, Vite
- Docker Compose
- SMTP email provider support

## Main Features

- Register/login/logout
- JWT authentication
- Role-based authorization
- Account creation and listing
- Secure money transfers
- Transaction history
- Saved recipients
- Admin dashboard and admin funding
- Audit logs
- API rate limiting
- RabbitMQ transfer events
- Real email notifications
- Retry + dead-letter queue for failed notifications
- Notification status persistence

## Security Highlights

The project addresses several OWASP Top 10 risk areas:

- Broken Access Control: account ownership checks, admin-only endpoints, and IDOR protection for account-specific data.
- Cryptographic Failures: BCrypt password hashing, hashed refresh tokens, and environment-based secrets.
- Injection: Spring Data JPA repositories, parameterized queries, request validation, and no string-built SQL.
- Identification and Authentication Failures: generic login errors, JWT access tokens, refresh token rotation, logout revocation, and rate limiting.
- Security Misconfiguration: environment-driven CORS, Git-ignored `.env`, and no hardcoded production secrets.
- Security Logging and Monitoring Failures: audit logs for authentication, transfers, account creation, and admin actions.
- Cross-Site Scripting: escaped dynamic values in HTML email templates.
- Software and Data Integrity Failures: Flyway-managed database migrations and Docker-based repeatable local runtime.

## Run Locally

Copy the example environment file:

```powershell
Copy-Item .env.example .env
```

Update local values such as database password, RabbitMQ password, JWT secret, admin password, and SMTP credentials if real email sending is needed.

For local testing, an admin user can be seeded through environment variables:

```text
ADMIN_SEED_ENABLED=true
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=change-this-admin-password
```

After the stack starts, log in with this admin account and use the admin funding screen to add balance to test accounts.

Start the stack:

```powershell
docker compose up -d --build
```

Useful local URLs:

```text
Backend API:  http://localhost:8081
Swagger UI:   http://localhost:8081/swagger-ui/index.html
RabbitMQ UI:  http://localhost:15672
PostgreSQL:   localhost:5433
```

## Tests

Backend:

```powershell
.\mvnw.cmd test
```

Notification service:

```powershell
cd ..\securebank-notification-service
.\mvnw.cmd test
```

Frontend:

```powershell
cd ..\securebank-frontend
npm run lint
npm run build
```

## API Docs

Swagger UI is available while the backend is running:

```text
http://localhost:8081/swagger-ui/index.html
```

Postman collection:

```text
postman/SecureBank.postman_collection.json
```

## AWS Deployment

Current setup:

- Frontend: S3 + CloudFront
- Backend stack: EC2
- Runtime: Docker Compose
- Services on EC2: backend, notification service, PostgreSQL, RabbitMQ
- Reverse proxy: Nginx
- HTTPS: Let's Encrypt
- Backend DNS: `nip.io`

## Related Projects

- `securebank-frontend`: React frontend.
- `securebank-notification-service`: RabbitMQ notification microservice.
