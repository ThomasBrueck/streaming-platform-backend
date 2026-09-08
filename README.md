# IRL Streaming Platform — Backend

> A Twitch-style **live streaming platform** built on an **event-driven microservices architecture**.
> Real-time video (WebRTC), real-time chat (WebSocket + Kafka), stateless JWT security and a fully containerized stack.

**This repository contains the BACKEND** (microservices, messaging, databases and infrastructure).
👉 **Frontend (React + TypeScript) lives here:** [`irl-streaming-front`](../irl-streaming-front) · _replace with your GitHub URL, e.g._ `https://github.com/<user>/irl-streaming-front`

---

## 📌 Table of Contents
- [Overview](#-overview)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [Microservices](#-microservices)
- [Event-Driven Communication (Kafka)](#-event-driven-communication-kafka)
- [Security](#-security)
- [Data Layer](#-data-layer)
- [Getting Started](#-getting-started)
- [API Reference](#-api-reference)
- [Design Decisions & Trade-offs](#-design-decisions--trade-offs)
- [Roadmap](#-roadmap)

---

## 🎯 Overview

This project reproduces the core of a live streaming platform (à la Twitch): users register, create a channel/stream, go **LIVE** broadcasting their camera and microphone over **WebRTC**, and viewers watch in real time while participating in a **live chat**.

Rather than a monolith, it is deliberately built as a **distributed system** to practice the patterns real streaming platforms rely on: independent deployability, asynchronous decoupling through an event bus, per-service data ownership, centralized authentication and horizontal scalability of the most demanding services (video and chat).

**Highlights**
- 🧩 **6 independent microservices** coordinated by an API Gateway + Service Discovery.
- 📨 **Asynchronous, event-driven communication** via Apache Kafka (no direct inter-service HTTP coupling for state changes).
- 🔐 **Stateless JWT authentication** enforced centrally at the gateway.
- 🎥 **Real-time video** through WebRTC (LiveKit SFU).
- 💬 **Real-time chat** over WebSocket/STOMP, fanned out through Kafka.
- 🐘 **Database-per-service** with PostgreSQL and Flyway-versioned schemas.
- 🐳 **One-command startup** — the entire stack (11 containers) runs with Docker Compose.

---

## 🏗 Architecture

# **SOON...**

**Request lifecycle (example — creating a stream):**
1. The client sends `POST /api/streams` with a `Bearer <JWT>` to the **API Gateway**.
2. The gateway's global **`JwtAuthFilter`** validates the signature, extracts `user_id` / `role`, strips the token and injects trusted headers downstream.
3. It resolves `stream-service` through **Eureka** (`lb://STREAM-SERVICE`) and load-balances the request.
4. `stream-service` persists the stream in its **own PostgreSQL** database and returns the created resource.

---

## 🧰 Tech Stack

| Layer | Technologies |
|---|---|
| **Language / Runtime** | Java 21 |
| **Framework** | Spring Boot 4, Spring Web, Spring WebFlux, Spring Data JPA, Spring WebSocket, Spring Security Crypto |
| **Microservices** | Spring Cloud Gateway (reactive), Netflix Eureka (service discovery) |
| **Messaging** | Apache Kafka (KRaft mode — no ZooKeeper) |
| **Database** | PostgreSQL 17, Flyway (schema migrations), Hibernate |
| **Real-time media** | LiveKit (WebRTC SFU) |
| **Real-time chat** | WebSocket + STOMP |
| **Security** | JWT (JJWT, HS256), BCrypt password hashing, role-based authorization |
| **Build & Infra** | Maven, Docker, Docker Compose |
| **Tooling** | Kafka UI, Postman collection |

---

## 🧩 Microservices

| Service | Port | Responsibility | Data / Messaging |
|---|---|---|---|
| **discovery-server** | 8761 | Service registry (Eureka) — every service registers and is discovered by logical name | — |
| **api-gateway** | 8080 | Single entry point. Routing, **global JWT validation**, CORS, header propagation | Reactive (WebFlux) |
| **auth-service** | 8083 | Registration, login, password hashing (BCrypt), **JWT issuing** | `auth-db` · produces `auth-events`, consumes `user-events` |
| **user-service** | 8081 | User profiles & lifecycle. Created **reactively** from auth events | `user-db` · consumes `auth-events`, produces `user-events` |
| **stream-service** | 8082 | Streams CRUD, stream-key generation, `LIVE/OFFLINE` status, viewer count | `stream-db` · consumes `user-events` |
| **chat-service** | 8084 | Real-time chat via WebSocket/STOMP; Kafka-backed message fan-out | consumes/produces `chat-messages` |

Each service follows a clean, layered structure: **Controller → Service → Repository**, with **DTOs**, **Mappers**, dedicated **enums/entities**, and a **global exception handler** returning consistent error responses.

---

## 📨 Event-Driven Communication (Kafka)

Services **never call each other directly to mutate state** — they publish domain events and react to them. This keeps services decoupled and resilient: if a consumer is temporarily down, events are processed once it recovers.


| Topic | Producer | Consumer(s) | Event | Purpose |
|---|---|---|---|---|
| `auth-events` | auth-service | user-service | `UserCreatedEvent` | Provision a user profile after registration |
| `user-events` | user-service | auth-service, stream-service | `UserDeletedEvent` | Cascade account deletion (remove credentials + user's streams) |
| `chat-messages` | chat-service | chat-service | `ChatMessage` | Fan-out chat messages to all subscribers/instances |

> **Why fan-out chat through Kafka?** It decouples message ingestion from delivery and lets the chat service scale to multiple instances while every WebSocket subscriber of a stream still receives every message.

---

## 🔐 Security

- **Stateless JWT** (HS256) issued by `auth-service` on login; tokens carry `sub` (user id), `username` and `role`, and expire in 3 hours.
- **Centralized enforcement at the gateway** — a single global `JwtAuthFilter` validates every request. Downstream services trust the `user_id` / `user_role` headers the gateway injects, so they don't re-implement auth.
- **Public routes** are explicitly whitelisted: `POST /api/auth/register`, `POST /api/auth/login`, and `GET /api/streams/**` (public catalog).
- **Passwords** are hashed with **BCrypt** (`spring-security-crypto`) — never stored in plaintext.
- **Role-based authorization** (`USER` / `ADMIN`) — e.g. only the owner (or an admin) can delete an account or change a stream's status.

---

## 🗄 Data Layer

**Database-per-service** pattern — each service owns and isolates its schema; no cross-service database access.

| Database | Owner | Port | Key tables |
|---|---|---|---|
| `authdb` | auth-service | 5434 | `auth_user` (credentials, role) |
| `userdb` | user-service | 5432 | `users` (public profile) |
| `streamdb` | stream-service | 5433 | `stream` (title, category, status, stream_key, viewer_count) |

Schemas are versioned and applied automatically with **Flyway** (`src/main/resources/db/migration`). Hibernate runs in `ddl-auto=validate`, so the code never silently mutates the schema — migrations are the single source of truth.

---

## 🚀 Getting Started

### Prerequisites
- Docker & Docker Compose
- (For local, non-Docker runs) JDK 21 & Maven

### 1. Configure environment
Create a `.env` file at the repository root:

```env
JWT_SECRET=change-me-with-a-long-random-secret-at-least-32-chars
```

### 2. Run the whole stack
```bash
docker compose up --build
```

This starts **11 containers**: 3× PostgreSQL, Kafka + Kafka UI, LiveKit, the discovery server, the API gateway and the 4 business services.

### 3. Useful endpoints
| Service | URL |
|---|---|
| API Gateway | http://localhost:8080 |
| Eureka Dashboard | http://localhost:8761 |
| Kafka UI | http://localhost:8090 |
| LiveKit | ws://localhost:7880 |

> A **Postman collection** is included in [`/postman`](./postman) to exercise the API end-to-end.

---

## 📡 API Reference

All requests go through the gateway (`http://localhost:8080`). Protected routes require `Authorization: Bearer <token>`.

### Auth — `auth-service`
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Register a new account |
| `POST` | `/api/auth/login` | Public | Authenticate, returns a JWT |

### Users — `user-service`
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/users` | JWT | List users |
| `GET` | `/api/users/{id}` | JWT | Get a user |
| `DELETE` | `/api/users/{id}` | JWT (owner/admin) | Delete account (cascades via `user-events`) |

### Streams — `stream-service`
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/streams` | Public | Public catalog of streams |
| `GET` | `/api/streams/{id}` | Public | Stream detail |
| `GET` | `/api/streams/user/{userId}` | Public | Stream by user |
| `POST` | `/api/streams` | JWT | Create a stream |
| `PATCH` | `/api/streams/{id}/status` | JWT (owner) | Toggle `LIVE` / `OFFLINE` |

### Chat — `chat-service`
| Channel | Destination | Description |
|---|---|---|
| STOMP endpoint | `/ws/chat` | WebSocket handshake |
| Publish | `/app/chat/{streamId}` | Send a message |
| Subscribe | `/topic/stream/{streamId}` | Receive live messages |

---

## 🧠 Design Decisions & Trade-offs

- **Microservices over a monolith** — chosen to isolate the workloads with very different scaling profiles (video signaling and chat vs. CRUD). The trade-off is higher operational complexity, addressed with Docker Compose + Eureka + a gateway.
- **Kafka over synchronous REST for state propagation** — favors decoupling and resilience (eventual consistency) at the cost of not having strong immediate consistency across services.
- **JWT validated once, at the gateway** — avoids duplicating security logic in every service and keeps downstream services simple and stateless.
- **Database-per-service** — enforces bounded contexts and independent deployability; the price is coordinating changes through events instead of foreign keys.
- **LiveKit (SFU) for media** — an SFU scales to many viewers far better than a mesh/P2P topology, which is the realistic model for streaming.

---

## 🗺 Roadmap

Honest view of what's next (this project is actively evolving):

- [ ] **Move LiveKit token minting to the backend** (`stream-service`) so media credentials are never exposed to the client.
- [ ] **Externalize all secrets** (currently some dev defaults live in config) via env vars / a secrets manager.
- [ ] **Automated tests** — unit tests (JUnit + Mockito) for the service layer and integration tests with Testcontainers.
- [ ] **Persist chat history** and add moderation.
- [ ] **Observability** — centralized logging, metrics (Micrometer/Prometheus) and distributed tracing.
- [ ] **CI/CD pipeline** and Kubernetes manifests for cloud deployment.

---

## 👤 Author

Built by **Thomas Brück** as an in-depth exploration of distributed systems, event-driven design and real-time media.

- 🔗 **Frontend repository:** [`irl-streaming-front`](../irl-streaming-front)