# Interview Prep — Task Manager Project

> **Your stance going in:** You are a Spring Boot developer. You built the entire backend — architecture, security, async messaging, caching, RBAC, WebSocket, everything. The React frontend was scaffolded with AI assistance; you understand its structure and the APIs it calls, but your depth and passion is in the backend. Be upfront and confident about this.

---

## Table of Contents

- [How to Introduce the Project](#how-to-introduce-the-project)
- [Architecture Questions](#architecture-questions)
- [Spring Boot & Java Questions](#spring-boot--java-questions)
- [Security & JWT Questions](#security--jwt-questions)
- [Kafka & Async Messaging Questions](#kafka--async-messaging-questions)
- [Redis Questions](#redis-questions)
- [JPA / Database Questions](#jpa--database-questions)
- [WebSocket / Real-Time Questions](#websocket--real-time-questions)
- [RBAC & Multi-Tenancy Questions](#rbac--multi-tenancy-questions)
- [AOP & Audit Logging Questions](#aop--audit-logging-questions)
- [Docker & Deployment Questions](#docker--deployment-questions)
- [Frontend — Honest Answers](#frontend--honest-answers)
- [Behavioral / Situational Questions](#behavioral--situational-questions)
- [Questions YOU Should Ask](#questions-you-should-ask)

---

## How to Introduce the Project

**When asked: "Tell me about a project you've built."**

> "I built a full-stack task management platform — think of it as a team-based tool where multiple organizations can manage tasks, assign work, and collaborate. My focus was entirely on the backend. It's built with Spring Boot 3.2, Java 17, PostgreSQL, Redis, and Kafka.
>
> What I'm most proud of is the real-time notification system — when a task is assigned or a status changes, the action hits a Kafka topic asynchronously, a consumer processes it, persists the notification, and pushes it to the user's browser over WebSocket using STOMP. All of this is decoupled from the main request thread so latency is unaffected.
>
> I also implemented JWT authentication with token blacklisting via Redis, a login rate limiter using Bucket4j with Redis-backed distributed buckets, org-level RBAC with three roles enforced at the service layer, and an AOP-based audit trail that captures every mutating operation without touching business logic.
>
> For the frontend, I used a React + TypeScript scaffold since my primary interest and depth is in backend systems — I understand the full request lifecycle end to end, but the backend is where I genuinely enjoy solving problems."

---

## Architecture Questions

**Q: Walk me through the architecture of your system.**

> "The entry point is Nginx on port 80 — it serves the React SPA for all non-API routes and reverse-proxies `/api` to the Spring Boot backend on port 8080. WebSocket connections at `/ws` are also proxied with proper Upgrade headers.
>
> The backend has a standard layered architecture: Controller → Service → Repository. Security sits as a filter chain — JWT is validated on every request before it reaches a controller. RBAC checks happen in the service layer so they can't be bypassed by any route misconfiguration.
>
> For async operations — specifically notifications and audit events — I produce to Kafka topics instead of doing synchronous work during the HTTP request. This keeps response times fast and the concerns separated."

**Q: Why did you choose a monolith over microservices?**

> "For a project of this scope it would be over-engineering. The codebase is split into clearly bounded packages — auth, organization, task, notification, audit — each with its own controller, service, and repository. If any one of them needed to scale independently, I could extract it. The Kafka integration is already the first step in that direction — notifications are already fully decoupled from the main request flow."

**Q: How does the notification flow work end to end?**

> "When a task-related action happens — create, assign, status change — the service calls `NotificationProducer.publishEvent()`. That method is annotated with `@Async` so it runs off the request thread. It builds a `TaskNotificationEvent` object and calls `kafkaTemplate.send()` to the `task-events` topic using the task ID as the partition key so events for the same task land on the same partition in order.
>
> The Kafka consumer receives it in `NotificationConsumer.handleTaskEvent()`, uses a strategy pattern to route to the right handler (task vs. org events), persists a `Notification` entity to PostgreSQL, then pushes it over WebSocket to the specific user via `/user/{userId}/queue/notifications`. The user only receives their own notifications — Spring's `convertAndSendToUser` handles the routing."

---

## Spring Boot & Java Questions

**Q: How does Spring Security's filter chain work in your app?**

> "Every request passes through `SecurityFilterChain`. I have several filters in order: `LoginRateLimitFilter` runs before authentication and checks Redis to enforce 5 login attempts per minute per IP. Then `JwtAuthenticationFilter` intercepts every request, extracts the Bearer token, validates signature and expiry, checks Redis to confirm it isn't blacklisted, and if valid, populates the `SecurityContext` with a `UsernamePasswordAuthenticationToken`.
>
> Certain paths are permitted without authentication — `/api/auth/signup`, `/api/auth/login`, `/actuator/health`, `/error`, and the WebSocket handshake endpoint `/ws`. Everything else requires a valid JWT."

**Q: How did you implement rate limiting?**

> "I use Bucket4j with a Redis-backed `ProxyManager` as the primary store. Each bucket is keyed by the client's IP (`request.getRemoteAddr()` — not `X-Forwarded-For`, to prevent spoofing). The bucket allows 5 tokens per 60-second window. If the bucket is empty, the filter returns a 429 with a `Retry-After` header.
>
> There's a local in-memory fallback using `ConcurrentHashMap` in case Redis is unavailable. I added a bounded eviction on the fallback — if the map exceeds 10,000 entries, I remove entries older than 10 minutes to prevent the memory from growing unboundedly."

**Q: What is `@Transactional` and where did you use it?**

> "It tells Spring to wrap the method in a database transaction — begin on entry, commit on success, rollback on any `RuntimeException`. I annotate service methods that do multiple DB writes that must be atomic. For example, when accepting an org invitation I update the `UserOrganization` status AND the invitation record — if either fails, both roll back. For read-only queries I use `@Transactional(readOnly = true)` which allows Hibernate to skip dirty checking and gives the DB driver a hint to optimize for reads."

**Q: What is the N+1 problem and how did you solve it?**

> "N+1 happens when you load a collection of N entities and then issue one additional query per entity to fetch an association. In my `TaskService.toResponse()` I was originally calling `userRepository.findById(task.getAssigneeId())` inside a loop. For 50 tasks that's 51 queries.
>
> I fixed it by collecting all unique user IDs from the task list into a `Set<UUID>`, calling `userRepository.findAllById(Set)` once to get all users in a single `IN (...)` query, then building a `Map<UUID, String>` of `userId → name` for O(1) lookup during the mapping loop. One query instead of N."

**Q: How does `@Cacheable` work and where do you use it?**

> "Spring intercepts the method call, generates a cache key from the parameters, and checks Redis. If the key exists it returns the cached value without executing the method. If not, it executes the method, stores the result in Redis with the configured TTL, and returns it.
>
> I cache task lists keyed by `orgId`, org members keyed by `orgId`, and user organizations keyed by `userId`. TTL is 5–10 minutes. I use `@CacheEvict` on mutating operations — when a task is created or updated, the relevant cache entry is evicted so the next read is fresh."

**Q: How does `@Async` work? What do you need to configure?**

> "You annotate a method with `@Async` and annotate a configuration class with `@EnableAsync`. Spring wraps the method in a proxy — when called, it submits the work to a thread pool and returns immediately to the caller. The caller gets back a `void` or `Future` instantly while the work runs asynchronously.
>
> I use it on `NotificationProducer.publishEvent()` so Kafka publishing doesn't block the HTTP request thread. The default thread pool is fine for this use case, but in production you'd configure a `ThreadPoolTaskExecutor` with explicit core/max pool sizes."

---

## Security & JWT Questions

**Q: How does JWT authentication work in your implementation?**

> "On login, `AuthService` authenticates the credentials, then calls `JwtUtil.generateToken()` which creates a signed JWT with the user's ID and email as claims, an issued-at timestamp, and an expiry (default 24 hours). The token is signed with HMAC-SHA256 using a secret key loaded from an environment variable.
>
> On every subsequent request, `JwtAuthenticationFilter` extracts the token from the `Authorization: Bearer` header, calls `JwtUtil.validateToken()` which checks the signature and expiry, then checks Redis to ensure the token isn't blacklisted. If all pass, it calls `UserDetailsService` to load the user and sets the `SecurityContext`.
>
> On logout, the token is stored in Redis with a TTL equal to its remaining validity. Any future request with that token gets rejected at the blacklist check."

**Q: Why store JWT in Redis for blacklisting instead of short-lived tokens?**

> "Without blacklisting, a JWT is valid until it expires — you can't revoke it. If a user logs out or their account is compromised, the token is still usable for up to 24 hours. Storing the token in Redis on logout gives us instant revocation. The TTL on the Redis key matches the token's remaining lifetime so we don't store stale entries forever."

**Q: What's the risk of trusting X-Forwarded-For for rate limiting?**

> "If you read `X-Forwarded-For` from the request directly, any client can set that header to any IP — effectively bypassing the rate limiter by rotating fake IPs. I deliberately use `request.getRemoteAddr()` which is the actual TCP connection IP that the server observed. The caveat is that behind a load balancer or proxy, `getRemoteAddr()` would return the proxy's IP, making rate limiting per-proxy rather than per-user. The correct fix is to only trust `X-Forwarded-For` when the request arrives from a known, trusted proxy — configured via `forward-headers-strategy: native` in Spring, not by blindly reading the header."

**Q: How did you store the JWT secret securely?**

> "The secret is injected via environment variable — `${JWT_SECRET}` — and never hardcoded in source code. The `.env` file is excluded from version control via `.gitignore`. In a production setup this would come from a secrets manager like AWS Secrets Manager or HashiCorp Vault injected as an env var into the container at runtime."

---

## Kafka & Async Messaging Questions

**Q: Why use Kafka instead of a simple in-memory queue or `@Async`?**

> "Three reasons: durability, decoupling, and scalability. `@Async` runs in the same JVM — if the application crashes mid-processing the event is lost. Kafka persists messages to disk. With Kafka, the notification consumer could be a separate service entirely. And Kafka supports consumer groups so you can scale consumers horizontally — multiple consumer instances share the load across partitions."

**Q: What happens if Kafka is down when a task is created?**

> "The producer will block up to `max.block.ms` (10 seconds in my config) trying to reach the broker. If it can't connect it throws an exception. Since `publishEvent()` is `@Async`, this failure is caught in the async context and logged — it does not propagate back to the HTTP response. The task is still created successfully. The notification is lost.
>
> A production solution would use an outbox pattern — write the event to a DB table in the same transaction as the task creation, then have a separate process poll the outbox and publish to Kafka. That guarantees at-least-once delivery."

**Q: What is a consumer group and how does it relate to your setup?**

> "All consumers with the same `group-id` collectively consume a topic — each partition is assigned to exactly one consumer in the group. This enables parallel processing and horizontal scaling. In my setup there's one consumer instance with `group-id: task-manager-group`. If I ran two instances of the backend, Kafka would distribute the partitions between them, doubling throughput without duplicate processing."

**Q: What is the partition key and why does it matter?**

> "I use the task ID as the partition key when publishing task events. Kafka hashes the key to determine which partition the message goes to. Because all events for the same task hash to the same partition, they are ordered — consumer processes them in the sequence they were produced. Without a key, messages go round-robin and could be consumed out of order."

---

## Redis Questions

**Q: What are the different ways you use Redis in this project?**

> "Three distinct use cases:
> 1. **Token blacklist** — invalidated JWTs are stored as keys with TTL = remaining token lifetime. Fast O(1) lookup on every authenticated request.
> 2. **Response cache** — `@Cacheable` stores task lists and org members. Reduces DB load for reads. Evicted on writes with `@CacheEvict`.
> 3. **Rate limiter buckets** — Bucket4j's `ProxyManager` stores the token bucket state per IP in Redis, enabling distributed rate limiting across multiple backend instances."

**Q: What happens if Redis goes down?**

> "The rate limiter falls back to an in-memory `ConcurrentHashMap`. The cache miss path leads directly to the DB — no data loss, just slower responses. The JWT blacklist is the risky one — if Redis is completely unavailable, blacklisted tokens might pass validation until Redis comes back. A more robust approach would be to fail-open for cache misses but fail-closed for auth (reject all authenticated requests if the blacklist store is unreachable), depending on security requirements."

---

## JPA / Database Questions

**Q: What is `ddl-auto: update` and should you use it in production?**

> "It tells Hibernate to compare the entity model against the actual DB schema on startup and run ALTER statements to sync them. In development it's convenient. In production it's dangerous — it can't drop columns or handle complex migrations and it runs schema changes at startup without a review process. Production should use `ddl-auto: validate` (fail if schema doesn't match) combined with a migration tool like Flyway or Liquibase for controlled, versioned schema changes."

**Q: What is `@ManyToOne` lazy loading and when does it cause problems?**

> "By default JPA associations are `EAGER` for `@ManyToOne` — the related entity is fetched in the same query via a JOIN. If set to `LAZY`, the proxy is only initialized when you first access the field. Lazy loading causes a problem outside a transaction — the 'LazyInitializationException'. It also contributes to N+1 if you loop over a collection and access a lazy field on each element, triggering one query per element."

**Q: What is an `@Index` annotation and did you use it?**

> "A database index speeds up lookups on non-primary-key columns. I add indexes on columns commonly used in WHERE clauses — `task.orgId`, `task.status`, `task.assigneeId`, `notification.userId`, `userOrganization.orgId`. Without them, every filter query is a full table scan. With them, the DB can jump directly to matching rows."

---

## WebSocket / Real-Time Questions

**Q: How do WebSockets work and how did you secure yours?**

> "WebSocket starts as an HTTP request with an `Upgrade: websocket` header. On handshake success the connection upgrades to a persistent full-duplex TCP connection. I use STOMP as a messaging protocol on top of WebSocket (via SockJS for fallback).
>
> Security: the STOMP `CONNECT` frame includes an `Authorization: Bearer <token>` header. I intercept this in a `ChannelInterceptor` that validates the token before allowing the connection. Once connected, each user is subscribed to their own personal channel `/user/{userId}/queue/notifications` — Spring's `convertAndSendToUser()` ensures messages are only delivered to the correct user's session."

**Q: What is SockJS and why use it?**

> "SockJS is a browser JavaScript library that provides a WebSocket-like API but falls back to HTTP long-polling if WebSockets are blocked (e.g. by corporate proxies or older browsers). It ensures the real-time connection works in environments where raw WebSocket might not."

---

## RBAC & Multi-Tenancy Questions

**Q: How is RBAC implemented? Where are permissions enforced?**

> "Roles are per-organization — a user can be ADMIN in one org and MEMBER in another. The `UserOrganization` join table stores `userId`, `orgId`, and `role`. Role checks happen in the service layer via helper methods like `assertAdminInOrg(userId, orgId)` and `isManagerOrAdminInOrg(userId, orgId)`. I deliberately don't rely solely on Spring Security's method-level `@PreAuthorize` annotations for org-scoped checks because those work on global roles — org-scoped permissions require a DB lookup."

**Q: What is the difference between authentication and authorization?**

> "Authentication is verifying identity — 'who are you?' — handled by the JWT filter. Authorization is verifying permission — 'are you allowed to do this?' — handled by the RBAC checks in the service layer and Spring Security path-level rules for coarse access."

**Q: How do you prevent a user in Org A from accessing data in Org B?**

> "Every task, member list, and audit log retrieval is scoped to an `orgId`. Before operating on any org resource, the service verifies that the requesting user has an active `UserOrganization` record for that org. Without that record they get a 403. Even if someone guesses a valid task UUID that belongs to another org, the org membership check blocks access."

---

## AOP & Audit Logging Questions

**Q: What is AOP and how did you use it?**

> "Aspect-Oriented Programming lets you inject cross-cutting behavior — like logging, security, or metrics — without modifying the target business method. I define a `@Around` advice on my `@AuditLog` annotation. Any method annotated with `@AuditLog` gets wrapped: before execution I capture the method args (representing old state), after execution I capture the return value (new state), then I publish an `AuditEvent` to Kafka asynchronously.
>
> The benefit is zero pollution of business logic — the service methods don't know auditing exists."

**Q: What is the difference between `@Before`, `@After`, and `@Around` advice?**

> "`@Before` runs before the method. `@After` runs after (regardless of success/failure). `@Around` fully wraps the method — you control when it executes via `joinPoint.proceed()` and you can access both the input and the output. I use `@Around` for audit logging because I need both what was passed in and what was returned."

---

## Docker & Deployment Questions

**Q: What does a multi-stage Dockerfile do?**

> "It uses multiple `FROM` instructions. The first stage (the 'build' stage) uses a heavy image with build tools — Maven or Node — to compile the application. The second stage uses a minimal runtime image — just a JRE or Nginx — and copies only the compiled artifact. The result is a production image that's much smaller and has no build toolchain, reducing attack surface."

**Q: What is a Docker healthcheck and why does it matter for `depends_on`?**

> "`HEALTHCHECK` runs a command periodically inside the container. If it exits 0, the container is healthy; non-zero is unhealthy. Docker Compose `depends_on: condition: service_healthy` waits until the dependency's healthcheck passes before starting the dependent service. Without this, a service might start before its database is ready, causing connection failures on startup."

**Q: What is the difference between `EXPOSE` and publishing a port in Docker Compose?**

> "`EXPOSE` in a Dockerfile is documentation — it declares which port the process inside the container listens on. It doesn't actually open anything. `ports: - '8080:8080'` in Compose actually binds the host port to the container port, making it accessible from outside the Docker network."

---

## Frontend — Honest Answers

> **Strategy:** Be honest, concise, and redirect to your depth. Don't pretend expertise you don't have.

**Q: Walk me through the frontend architecture.**

> "The frontend is a React 18 + TypeScript SPA built with Vite. State is managed with Redux Toolkit — there are slices for auth, tasks, organizations, notifications, and dashboard data. Routing is React Router 6 with a `ProtectedRoute` wrapper. The Axios client has a request interceptor to attach the JWT and a response interceptor for 401 handling. I understand how it all fits together and how it talks to my API, but I want to be transparent — I used AI tooling to scaffold the frontend code. My primary expertise and the work I'm most proud of is the backend."

**Q: What is Redux and why use it?**

> "Redux is a predictable state container. In Redux Toolkit, you define slices with reducers and async thunks. Components dispatch actions; reducers update the store; components re-render from selectors. It's useful when multiple components need the same data — for example, the unread notification count in the navbar and the notification dropdown both read from the same store slice. I understand the pattern even if React isn't my focus."

**Q: How does the WebSocket connection work on the frontend?**

> "The custom `useWebSocket` hook connects using `@stomp/stompjs` over SockJS to `/ws`. On connection it sends the JWT in the STOMP connect headers for authentication. It subscribes to `/user/queue/notifications` and on each message dispatches a Redux action to add the notification to the store and increment the unread count. The hook tears down and reconnects when the auth token changes."

---

## Behavioral / Situational Questions

**Q: What was the hardest technical problem you solved in this project?**

> "The notification system. Getting the full async pipeline right — from producing events off the request thread, to Kafka ordering via partition keys, to the consumer persisting and pushing over WebSocket to the right user's session — required understanding all the moving parts. I hit a bug early where org invitation events had a null task ID, which caused a NPE when building the partition key. I fixed it with a null-safe fallback to the org ID. Small bug, but it taught me to think carefully about which fields are guaranteed to be present at each stage of an event's lifecycle."

**Q: What would you improve if you had more time?**

> "A few things:
> 1. Replace `ddl-auto: update` with Flyway migrations for safe, versioned schema changes.
> 2. Implement the outbox pattern for Kafka publishing — right now a notification can be lost if Kafka is down at the moment a task is created.
> 3. Add refresh tokens with a rotation strategy instead of just long-lived JWTs.
> 4. Proper integration tests with Testcontainers — spinning up real PostgreSQL, Redis, and Kafka containers in tests rather than mocking them.
> 5. Extract the notification consumer into a separate service to demonstrate the microservice extraction path."

**Q: How did you test this project?**

> "The backend has unit tests covering `AuthService` and `TaskService` — the two most business-critical services. I mock the repositories and test happy paths and key failure cases like duplicate email registration and task permission checks. What I'd add is Testcontainers-based integration tests that exercise the full stack — actual DB, actual Redis, actual Kafka — because many bugs only surface when the real infrastructure is involved."

**Q: How do you handle errors in your API?**

> "A `@RestControllerAdvice` class — `GlobalExceptionHandler` — catches exceptions centrally. I have handlers for `ResourceNotFoundException` (404), a custom `ForbiddenException` (403), `MethodArgumentNotValidException` for Bean Validation failures (400 with field-level error details), and a catch-all for unexpected exceptions (500). Every response uses a consistent error envelope so clients have predictable error parsing."

---

## Questions YOU Should Ask

Ask these to show depth of thinking:

- "How do you handle database schema migrations at scale — do you use Flyway or Liquibase?"
- "What's your strategy for secrets management in production — Vault, AWS Secrets Manager, Kubernetes secrets?"
- "Are services deployed as containers? Kubernetes or ECS?"
- "How do you approach distributed tracing across services — do you use OpenTelemetry or something similar?"
- "What's the on-call process like when a Kafka consumer falls behind on a partition?"
