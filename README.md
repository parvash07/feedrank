# FeedRank

![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-pgvector-4169E1?logo=postgresql&logoColor=white)
![React](https://img.shields.io/badge/React-18-61DAFB?logo=react&logoColor=black)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED?logo=docker&logoColor=white)

A personalized Hacker News feed that **learns what you like**. Every upvote, skip, and
second spent reading feeds a preference model that re-ranks your feed — visibly, within
seconds. The interesting problem here is the feedback loop: the system demonstrably gets
better at surfacing relevant content the more you interact with it.

**Ranking pipeline:** scheduled HN ingestion → interaction tracking → tag-preference
model (MVP) → embedding preference vectors (upgrade) → epsilon-greedy exploration
(anti–echo-chamber layer).

---

## Features

- **Content ingestion** — polls the Hacker News API every 15 minutes (plus on startup),
  normalizes stories, derives topic tags, and deduplicates on `external_id`
- **Interaction tracking** — clicks, upvotes, skips, and time-in-viewport dwell time,
  captured by the UI via `IntersectionObserver`
- **Adaptive tag ranking** — per-user topic weights with time decay, blended with
  recency so fresh stories are never buried
- **Embedding ranking** — item embeddings (Gemini / Groq / offline hash fallback) stored
  in pgvector, per-user preference vector updated as an exponentially-weighted average,
  feed ranked by cosine similarity
- **Explore/exploit** — epsilon-greedy injection of under-explored stories into the top-N,
  badged in the UI; every impression logged with its reason
- **"Why am I seeing this?"** — live debug view of your topic weights, interaction
  count, and embedding status
- **Clean layered backend** — `controller → service → repository → entity`, request/response
  DTOs, MapStruct-style mappers, and centralized exception handling

---

## Tech stack

| Layer    | Technology                                                              |
| -------- | ----------------------------------------------------------------------- |
| Backend  | Java 17, Spring Boot 3.2, Spring Data JPA, Spring Security (JWT), Flyway |
| Database | PostgreSQL 16 + [pgvector](https://github.com/pgvector/pgvector)         |
| Frontend | React 18, Vite 5, Tailwind CSS 3.4                                      |
| Infra    | Docker Compose (db + backend + frontend), Nginx (static + `/api` proxy) |

---

## Getting started

### Prerequisites

- Docker + Docker Compose **or** JDK 17 + Maven + Node 20 + a local PostgreSQL with pgvector

### Option A — Docker (recommended)

```bash
docker compose up -d --build
```

| Service  | URL                   |
| -------- | --------------------- |
| UI       | http://localhost:5173 |
| API      | http://localhost:8080 |
| Database | localhost:5432        |

The backend ingests HN top stories on startup, so the feed is populated within a minute.
Register in the UI, upvote a few cards, and hit **Re-rank my feed**.

```bash
docker compose down      # stop (data is kept in the pgdata volume)
docker compose down -v   # stop and wipe all data for a fresh start
```

### Option B — Local development

```bash
# 1. Database
docker compose up -d db

# 2. Backend (:8080, ingests HN on startup)
cd backend && mvn spring-boot:run

# 3. Frontend (:5173, proxies /api to the backend)
cd frontend && npm install && npm run dev
```

> Don't run Docker and local dev at the same time — they compete for ports
> 5432 / 8080 / 5173.

---

## Configuration

All settings are environment variables (see `application.yml` for defaults):

| Variable              | Default                        | Description                                              |
| --------------------- | ------------------------------ | -------------------------------------------------------- |
| `DATABASE_URL`        | `jdbc:postgresql://localhost:5432/feedrank` | JDBC URL (compose sets this to `db`)     |
| `DATABASE_USER` / `DATABASE_PASSWORD` | `feedrank` / `feedrank` | DB credentials                                  |
| `JWT_SECRET`          | dev-only placeholder           | **Change in production** (min. 32 bytes)                 |
| `RANKING_MODE`        | `tag`                          | `tag` or `embedding`                                     |
| `EMBEDDING_PROVIDER`  | `hash`                         | `hash` (offline), `gemini`, or `groq`                    |
| `GEMINI_API_KEY`      | —                              | Required for `EMBEDDING_PROVIDER=gemini`                 |
| `GROQ_API_KEY`        | —                              | Required for `EMBEDDING_PROVIDER=groq`                   |
| `INGESTION_ENABLED`   | `true`                         | Set `false` to disable HN polling (e.g. in tests)        |

No API key is needed to run the app — the deterministic hash embedding works offline
and the full feedback loop is demoable without one. Add a key only when you want to
compare real semantic ranking against tag ranking (wipe the DB volume afterwards so
items get re-embedded with the new provider).

---

## API reference

All endpoints except `/api/auth/**` require `Authorization: Bearer <token>`.

| Method | Endpoint | Description |
| ------ | -------- | ----------- |
| `POST` | `/api/auth/register` | `{username, password}` → `{token, username, userId}` |
| `POST` | `/api/auth/login` | Same shape; `401` on bad credentials |
| `GET` | `/api/feed?mode=tag\|embedding&limit=20&explore=true` | Ranked feed; exploration picks are flagged |
| `GET` | `/api/feed/latest?limit=20` | Reverse-chronological (ingestion health check) |
| `POST` | `/api/feed/ingest` | Trigger an HN ingestion run now |
| `POST` | `/api/interactions` | `{itemId, interactionType: click\|upvote\|skip\|dwell, dwellTimeMs?}` |
| `GET` | `/api/interactions` | The user's interaction history (newest first) |
| `GET` | `/api/preferences` | Tag weights, vector status, interaction count |
| `GET` | `/api/preferences/vector` | Preference-vector preview + exploration rate |
| `POST` | `/api/preferences/decay?factor=0.98` | Manually decay this user's weights |
| `GET` | `/api/admin/stats` | Item count + recent exploration impressions |
| `GET` | `/api/admin/impressions` | Recent impressions with explore/exploit reasons |

Errors are consistent JSON via a global handler:

```json
{ "error": "unknown item 999999", "status": 404, "timestamp": "2026-09-06T16:19:41Z" }
```

---

## How the feedback loop works

**Tags.** `TagExtractor` (a pure, unit-tested function) derives up to 5 tags per title
using stopword filtering plus an alias map (`llm → ai`, `k8s → kubernetes`, …).

**Weights.** Each interaction nudges the weights of the item's tags:

| Interaction | Delta |
| ----------- | ----- |
| Upvote | `+2.0` |
| Click | `+1.0` |
| Dwell ≥ 30 s / ≥ 10 s | `+1.5` / `+0.5` |
| Skip | `−0.4` |

All weights decay ×0.98 daily (3 AM cron, plus a manual endpoint), so the feed follows
*current* interests instead of ossifying.

**Ranking (tag mode).** `score = Σ tagWeights × (0.35 + 0.65·recency) + hnBoost + recency·0.3`,
with `recency = exp(−ageHours / 48)`. Item scoring lives behind the isolated
`RankingService` interface, so swapping strategies touches nothing in ingestion or
interaction tracking.

**Ranking (embedding mode).** Item vectors are cached in `embedding_text` (plus the
`embedding_vector` pgvector column); the user vector is an EMA (α = 0.15) over positive
engagements and the feed is ordered by cosine similarity, blended with recency for
cold start. Toggle Tags/Vector in the nav to compare the two strategies live.

**Exploration.** With probability ε = 0.1, under-ranked items are injected into the
top-N (`ExplorationService`), badged `✦ For you to explore` in the UI. Every impression
— explore or exploit — is logged with its reason, so you can show the echo-chamber
guard working. A Thompson-sampling Beta helper is included per-topic for further
experimentation.

---

## Project structure

```
backend/src/main/java/com/feedrank/
├── controller/     # thin HTTP layer (auth, feed, interactions, preferences, admin)
├── service/        # business logic (auth, preferences, ingestion, embeddings, admin)
│   └── ranking/    # RankingService + tag / embedding / exploration strategies
├── repository/     # Spring Data JPA interfaces
├── entity/         # JPA entities
├── dto/
│   ├── request/    # validated input records
│   └── response/   # output records
├── mapper/         # entity → response mapping
├── exception/      # GlobalExceptionHandler + typed exceptions
├── security/       # JWT filter, token util, security config
└── config/         # WebClient, app wiring
frontend/src/
├── App.tsx         # feed, hero, debug panel, auth screen
├── FeedCard.tsx    # story card with voting + dwell tracking
├── api.ts          # typed API client        useDwellFix.ts  # viewport dwell hook
├── Logo.tsx        # brand mark              theme.tsx       # light/dark mode hook
```

Dependency rule: `controller → service → repository → entity`. Controllers never touch
repositories; services never touch HTTP; the API never serializes entities.

---

## Testing

```bash
cd backend && mvn test          # TagExtractor, ranking math, cosine similarity, deltas
cd frontend && npm run build    # tsc --noEmit + vite production build
```
