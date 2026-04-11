# Task Manager

A full-stack, multi-organization task management platform. Teams can create organizations, invite members, manage tasks on a Kanban board, and receive real-time notifications — all with role-based access control.

---

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Quick Start (Docker)](#quick-start-docker)
- [Local Development](#local-development)
- [Environment Variables](#environment-variables)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)
- [Known Issues / Roadmap](#known-issues--roadmap)

---

## Features

| Area | What's supported |
|---|---|
| **Auth** | JWT-based signup / login / logout, token blacklisting via Redis, rate limiting on login (5 req/min per IP) |
| **Organizations** | Create orgs, invite members (pending or force-add), accept/decline invitations, remove members, change roles |
| **Tasks** | Create, update, delete tasks; Kanban board with drag-and-drop status change; assign to members; filter by assignee |
| **Comments** | Add / delete comments on tasks (owner only can delete their own) |
| **Notifications** | Real-time in-app notifications via WebSocket (STOMP); org and task events; mark read / mark all read |
| **Audit Logs** | AOP-based audit trail for every mutating action; old/new state captured; viewable by ADMIN |
| **Dashboard** | Tasks-by-status bar chart, tasks-by-priority pie chart, "Assigned to Me" quick stats |
| **RBAC** | Three org-level roles: `ADMIN`, `MANAGER`, `MEMBER` with enforced access at service layer |
| **Caching** | Redis-backed cache for task lists, org members, and user organizations (5–10 min TTL) |

---

## Architecture

```
Browser
  │
  │  HTTP / WebSocket
  ▼
┌─────────────────────────┐
│   Nginx (port 80)       │  ← serves React SPA static files
│   /api  → backend:8080  │  ← proxies REST calls
│   /ws   → backend:8080  │  ← proxies WebSocket (STOMP)
└─────────────────────────┘
              │
              ▼
┌─────────────────────────┐
│  Spring Boot (port 8080) │
│  ┌────────┐ ┌─────────┐ │
│  │  REST  │ │  WS/    │ │
│  │  API   │ │  STOMP  │ │
│  └────────┘ └─────────┘ │
│  ┌──────────────────────┐│
│  │  Service Layer       ││  ← business logic + RBAC
│  └──────────────────────┘│
│  ┌─────────┐ ┌─────────┐ │
│  │  Redis  │ │  Kafka  │ │
│  │  Cache/ │ │  Events │ │
│  │  Token  │ │  (async)│ │
│  │  Blklst │ └─────────┘ │
│  └─────────┘             │
│  ┌──────────────────────┐│
│  │     PostgreSQL       ││
│  └──────────────────────┘│
└──────────────────────────┘
```

**Event flow for notifications:**
```
Task action (create/update/assign/status) 
  → NotificationProducer (@Async) 
  → Kafka topic "task-events" 
  → NotificationConsumer 
  → Notification persisted to DB 
  → SimpMessagingTemplate pushes to /user/{id}/queue/notifications (WebSocket)
```

---

## Tech Stack

### Backend
| Technology | Version | Purpose |
|---|---|---|
| Java | 17 | Language |
| Spring Boot | 3.2 | Application framework |
| Spring Security | 6 | Auth + RBAC |
| Spring Data JPA / Hibernate | — | ORM |
| PostgreSQL | 16 | Primary database |
| Redis | 7 | JWT blacklist, response caching, rate limiting |
| Apache Kafka | 3.6 (Confluent 7.6) | Async notification events, audit events |
| Spring WebSocket (STOMP) | — | Real-time push notifications |
| JJWT | 0.12 | JWT generation + validation |
| Bucket4j | — | Local rate-limit fallback |
| Lombok | — | Boilerplate reduction |
| Springdoc OpenAPI | — | Swagger UI |

### Frontend
| Technology | Version | Purpose |
|---|---|---|
| React | 18 | UI framework |
| TypeScript | 5 | Type safety |
| Redux Toolkit | 2 | State management |
| React Router | 6 | Client-side routing |
| Axios | 1 | HTTP client |
| @stomp/stompjs + SockJS | — | WebSocket client |
| @hello-pangea/dnd | — | Drag-and-drop Kanban |
| Recharts | 2 | Dashboard charts |
| React Hook Form | 7 | Form management |
| Tailwind CSS | 3 | Styling |
| Vite | 5 | Dev server + bundler |
| Vitest | 1 | Unit testing |

---

## Quick Start (Docker)

### Prerequisites
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (includes Docker Compose v2)

### Steps

```bash
# 1. Clone the repository
git clone <repo-url>
cd taskmanager

# 2. Create your environment file
cp .env.example .env

# 3. Set a strong JWT secret in .env (REQUIRED)
#    Generate one with: openssl rand -hex 32
#    Edit .env and set:   JWT_SECRET=<your-secret>

# 4. Build and start all services
docker compose up --build -d

# 5. Open the app
open http://localhost        # macOS
start http://localhost       # Windows
```

**What starts:**
| Container | Port | Description |
|---|---|---|
| `tm-frontend` | **80** | React SPA served by Nginx |
| `tm-backend` | 8080 | Spring Boot REST + WebSocket API |
| `tm-postgres` | 5432 | PostgreSQL database |
| `tm-redis` | 6379 | Redis cache + token store |
| `tm-kafka` | 9092 | Kafka broker |
| `tm-zookeeper` | — | Kafka coordination (internal) |

### Stopping

```bash
docker compose down          # stop containers (data persisted)
docker compose down -v       # stop + delete all volumes (full reset)
```

### Rebuilding after code changes

```bash
docker compose up --build -d
```

---

## Local Development

### Prerequisites
- Java 17+
- Maven 3.9+
- Node.js 20+
- Docker (for infrastructure services only)

### 1. Start infrastructure

```bash
# Start just postgres, redis, and kafka
docker compose -f task-manager-backend/docker-compose.yml up -d postgres redis zookeeper kafka
```

### 2. Start the backend

```bash
cd task-manager-backend
./mvnw spring-boot:run
# Backend available at http://localhost:8080
# Swagger UI at http://localhost:8080/swagger-ui/index.html
```

### 3. Start the frontend

```bash
cd task-manager-frontend
npm install
npm run dev
# Frontend available at http://localhost:3000
# Vite dev proxy forwards /api and /ws to http://localhost:8080
```

### Running tests

```bash
# Backend unit tests
cd task-manager-backend
./mvnw test

# Frontend unit tests
cd task-manager-frontend
npm test
```

---

## Environment Variables

All variables live in `.env` at the repository root (copy from `.env.example`).

| Variable | Required | Default | Description |
|---|---|---|---|
| `JWT_SECRET` | **Yes** | — | JWT signing key, minimum 32 characters. Use `openssl rand -hex 32`. |
| `JWT_EXPIRATION_MS` | No | `86400000` | Token lifetime in ms (default 24 h) |
| `DB_NAME` | No | `taskmanager` | PostgreSQL database name |
| `DB_USER` | No | `taskuser` | PostgreSQL username |
| `DB_PASSWORD` | No | `taskpassword` | PostgreSQL password |
| `VITE_API_BASE_URL` | No | `` (empty) | Backend base URL baked into frontend at build time. Leave empty when Nginx proxies `/api` (default Docker Compose). Set to `https://api.yourdomain.com` when running on separate hosts. |
| `VITE_WS_URL` | No | `` (empty) | WebSocket base URL. Leave empty when Nginx proxies `/ws` (default). |
| `FRONTEND_PORT` | No | `80` | Host port for the Nginx frontend container. |

> **Security:** Never commit a `.env` file with real secrets. The `.gitignore` excludes it by convention — verify this before pushing.

---

## API Documentation

Swagger UI is available at **[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)** when the backend is running.

### Endpoint summary

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/signup` | Public | Register a new user |
| POST | `/api/auth/login` | Public | Login and receive JWT (rate-limited: 5/min per IP) |
| POST | `/api/auth/logout` | JWT | Logout and blacklist token |
| GET | `/api/orgs/mine` | JWT | List organizations I belong to |
| POST | `/api/orgs` | JWT | Create organization |
| GET | `/api/orgs/{id}/members` | JWT | List ACTIVE members |
| POST | `/api/orgs/{id}/invite` | ADMIN | Invite user (pending or force-add) |
| POST | `/api/orgs/{id}/invitations/accept` | JWT | Accept pending invitation |
| POST | `/api/orgs/{id}/invitations/decline` | JWT | Decline pending invitation |
| DELETE | `/api/orgs/{id}/members/{memberId}` | ADMIN/MANAGER | Remove member |
| PUT | `/api/orgs/{id}/members/{memberId}/role` | ADMIN | Change member role |
| GET | `/api/tasks` | JWT | List tasks for org (supports status/assignedTo filters + pagination) |
| POST | `/api/tasks` | JWT | Create task |
| PUT | `/api/tasks/{id}` | JWT | Update task (ADMIN/MANAGER or creator/assignee) |
| PATCH | `/api/tasks/{id}/status` | JWT | Change task status |
| PATCH | `/api/tasks/{id}/assign` | ADMIN/MANAGER | Assign/unassign task |
| DELETE | `/api/tasks/{id}` | ADMIN/MANAGER | Delete task |
| GET | `/api/tasks/{id}/comments` | JWT | List comments on task |
| POST | `/api/tasks/{id}/comments` | JWT | Add comment |
| DELETE | `/api/comments/{id}` | JWT (owner) | Delete own comment |
| GET | `/api/notifications` | JWT | Get notifications (paginated) |
| GET | `/api/notifications/unread-count` | JWT | Unread notification count |
| PATCH | `/api/notifications/{id}/read` | JWT | Mark notification read |
| PATCH | `/api/notifications/read-all` | JWT | Mark all notifications read |
| GET | `/api/audit` | ADMIN | Paginated audit log |
| GET | `/actuator/health` | Public | Health check |

### WebSocket

Connect via STOMP over SockJS at `/ws` with an `Authorization: Bearer <token>` connect header.

Subscribe to `/user/queue/notifications` to receive real-time notification payloads.

---

## Project Structure

```
taskmanager/
├── docker-compose.yml              ← Full-stack deployment (all services)
├── .env.example                    ← Environment variable template
├── task-manager-backend/
│   ├── Dockerfile                  ← Multi-stage Maven → JRE image
│   ├── docker-compose.yml          ← Infrastructure-only (for local dev)
│   ├── pom.xml
│   └── src/main/java/com/taskmanager/
│       ├── auth/                   ← JWT auth, signup/login/logout, rate limiting
│       ├── user/                   ← User entity + repository
│       ├── organization/           ← Org CRUD, membership, invitations
│       ├── task/                   ← Task CRUD, Kanban status, assignment
│       ├── comment/                ← Task comments
│       ├── notification/           ← Kafka consumer, WebSocket push, REST API
│       ├── audit/                  ← AOP audit aspect, Kafka producer, REST API
│       └── config/                 ← Security, Redis, Kafka, WebSocket config
└── task-manager-frontend/
    ├── Dockerfile                  ← Multi-stage Node build → Nginx image
    ├── nginx.conf                  ← Serves SPA + proxies /api and /ws
    ├── src/
    │   ├── api/                    ← Axios client + typed API modules
    │   ├── app/                    ← Redux store + hooks
    │   ├── features/               ← Feature slices (auth, tasks, orgs, notifications, audit, dashboard)
    │   ├── components/             ← Shared UI components (Button, Modal, Badge)
    │   ├── hooks/                  ← useWebSocket, useAuth
    │   ├── router/                 ← React Router config + ProtectedRoute
    │   └── types/                  ← Shared TypeScript interfaces
    └── package.json
```

---

## Known Issues / Roadmap

### Currently not supported
- **Password reset / forgot password** — no email integration exists yet
- **Refresh tokens** — `refresh-expiration-ms` is configured but the endpoint is not implemented; users must re-login when their 24h token expires
- **Email notifications** — only in-app (WebSocket); no SMTP integration
- **Member removal orphans task assignments** — removing a member from an org does not unassign their tasks
- **Organization delete** — no delete endpoint exists
- **Only 2 backend unit test classes** — OrganizationService, CommentService, AuditService, NotificationService have no tests
- **No frontend unit tests** — component tests not yet written

### Production deployment checklist
- [ ] Set a strong `JWT_SECRET` (min 32 chars, generated with `openssl rand -hex 32`)
- [ ] Change default database password (`DB_PASSWORD`)
- [ ] Set `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` after first boot (schema is stable)
- [ ] Configure `server.forward-headers-strategy=native` in `application-prod.yml` if deploying behind a known reverse proxy (for accurate client IP in rate limiting)
- [ ] Update `corsConfigurationSource()` in `SecurityConfig` to restrict allowed origins to your production domain
- [ ] Set up TLS termination at your load balancer or Nginx in front of the Docker stack
- [ ] Add Kafka topic replication factor > 1 for multi-broker production setup
