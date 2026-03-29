# Advisor Platform Backend

Spring Boot 3 / Java 21 backend for the Advisor Platform.

## Prerequisites

- Java 21
- Maven 3.9+
- Docker (for Postgres)

## Local Setup

### 1. Start Postgres

```bash
docker-compose up -d
```

### 2. Configure environment

```bash
cp .env.example .env
```

Edit `.env` and fill in your `ANTHROPIC_API_KEY`. The other values default to the local Docker Postgres.

```
ANTHROPIC_API_KEY=sk-ant-...
DB_URL=jdbc:postgresql://localhost:5432/advisorplatform
DB_USER=advisorplatform
DB_PASS=advisorplatform
CORS_ORIGINS=http://localhost:5173
```

### 3. Run the backend

```bash
mvn spring-boot:run
```

### 4. Verify

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET  | `/actuator/health` | Health check |
| POST | `/api/visitor/identify` | Find-or-create visitor by browser token |
| POST | `/api/session` | Create a new AI planning session |
| GET  | `/api/visitor/{visitorId}/sessions` | List sessions for a visitor |
| POST | `/api/chat/{sessionId}` | Non-streaming chat |
| POST | `/api/chat/{sessionId}/stream` | Streaming SSE chat |
| POST | `/api/message` | Create a message thread |
| GET  | `/api/visitor/{visitorId}/threads` | List threads for a visitor |
| GET  | `/api/thread/{threadId}/messages` | List messages in a thread |
