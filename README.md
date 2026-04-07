# DataPulse — E-Commerce Analytics Platform

A full-stack e-commerce platform with an AI-powered analytics chatbot, visual product search, centralized logging, and monitoring dashboards.

---

## Features

### Core E-Commerce
- **User management** with JWT authentication and 3 roles (ADMIN / CORPORATE / INDIVIDUAL)
- **Product catalog** with images, stock tracking, brand, rating, search & filtering
- **Shopping cart** with add/update/remove/checkout
- **Orders** with creation, cancellation (with stock restore), and return requests
- **Wishlist / favorites**
- **Address book** with default shipping address
- **Reviews** with star ratings and sentiment tracking
- **Shipments** with tracking status
- **Categories** with hierarchical parent-child relationships
- **Password reset** via token flow

### AI & Analytics
- **Text2SQL chatbot** — ask natural-language questions and get SQL-backed answers with Plotly charts (LangGraph 5-agent pipeline)
- **Visual product search** — upload an image and find similar products (CLIP ViT-B-32 embeddings + pgvector)
- **Analytics dashboards** — sales, customer demographics, product performance
- **Role-based data scoping** — chatbot automatically filters queries by user role via mandatory CTE injection

### Infrastructure & Observability
- **ELK stack** (Elasticsearch + Logstash + Kibana) with authenticated access
- **Kibana dashboards** with 6 visualizations + 3 real-time alerts
- **Structured JSON logging** via Logstash encoder
- **Flyway** database migrations
- **Docker Compose** orchestration for the full stack
- **CORS** configured for Angular dev server
- **Security testing suite** — Schemathesis (API fuzzing) + OWASP ZAP (active scanning)

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Backend** | Spring Boot 3.2.3 (Java 17) |
| **Database** | PostgreSQL 16 + pgvector |
| **Migrations** | Flyway 9.22 |
| **Auth** | JWT (JJWT 0.12) + Spring Security |
| **ORM** | Hibernate / JPA |
| **API Docs** | SpringDoc OpenAPI 2.3 |
| **Frontend** | Angular 21 (TypeScript 5.9) |
| **AI Chatbot** | Python 3.11 + FastAPI + Chainlit + LangGraph + LangChain |
| **LLM** | OpenAI GPT-4o (configurable to Anthropic Claude) |
| **Visual Search** | OpenCLIP (ViT-B-32, 512-dim) + pgvector |
| **Logging** | ELK Stack 8.11 (Elasticsearch, Logstash, Kibana) |
| **Containers** | Docker Compose v3.8 |
| **Build** | Maven 3.9 |
| **Testing** | JUnit 5, Mockito, H2, Schemathesis, OWASP ZAP |

---

## Architecture

```
┌──────────────┐     ┌─────────────────────────────────────┐
│  Angular 21  │────▶│  Spring Boot Backend (:8080)         │
│  Frontend    │     │  ├─ REST API (14 endpoint groups)    │
│  (:4200)     │     │  ├─ JWT + RBAC                       │
│              │     │  ├─ Flyway migrations                │
│  Chat Widget─┼──┐  │  ├─ Structured logging → Logstash    │
└──────────────┘  │  │  └─ CSV DataLoader (on startup)      │
                  │  └──────────┬────────────────────────────┘
                  │             │                    │
                  │     ┌───────▼─────────┐   ┌─────▼─────┐
                  │     │ PostgreSQL      │   │ Logstash  │
                  │     │ + pgvector      │   │ (:5001)   │
                  │     │ (:5432)         │   └─────┬─────┘
                  │     └───────▲─────────┘         │
                  │             │             ┌─────▼──────┐
                  │     ┌───────┴─────────┐   │Elasticsearch│
                  └────▶│ Python Chatbot  │   │  (:9200)   │
                        │ ├─ FastAPI:8002 │   └─────┬──────┘
                        │ ├─ Chainlit:8001│         │
                        │ ├─ LangGraph    │   ┌─────▼──────┐
                        │ └─ CLIP/pgvector│   │ Kibana     │
                        └─────────────────┘   │ (:5601)    │
                                              └────────────┘
```

---

## Getting Started

### Prerequisites
- Docker Desktop
- OpenAI API key (for chatbot LLM)
- 5+ GB free disk space

### Quick Start

```bash
# 1. Clone the repository
git clone https://github.com/BurakkYuce/Shopping-Cart-Backend.git
cd Shopping-Cart-Backend

# 2. Create the .env file
cp .env.example .env
# Edit .env and set your OPENAI_API_KEY

# 3. Start the full stack
docker compose up -d --build

# 4. Wait ~30 seconds for all services to become healthy
docker compose ps

# 5. Verify the backend
curl http://localhost:8080/api/products
```

### Service URLs

| Service | URL | Credentials |
|---|---|---|
| **Swagger UI** | http://localhost:8080/swagger-ui.html | — |
| **Backend API** | http://localhost:8080/api | JWT (see auth flow) |
| **Chainlit UI** | http://localhost:8001 | JWT token paste |
| **Chatbot API** | http://localhost:8002 | JWT |
| **Kibana** | http://localhost:5601 | `elastic` / `elastic` |
| **Elasticsearch** | http://localhost:9200 | `elastic` / `elastic` |
| **PostgreSQL** | localhost:5432 | `datapulse` / `changeme` |

### Default Application Users

After the CSV DataLoader runs on first startup, the following users are available:

| Email | Password | Role |
|---|---|---|
| `admin@datapulse.com` | `changeme` | ADMIN |
| _any CSV-seeded user_ | `changeme` | INDIVIDUAL / CORPORATE |

The password is configurable via `SEED_PASSWORD` in `.env`.

---

## Project Structure

```
Shopping-Cart-Backend/
├── backend/                      # Spring Boot API
│   ├── src/main/java/com/datapulse/
│   │   ├── config/               # AppConfig, DataLoader, OpenApiConfig
│   │   ├── controller/           # 14 REST controllers
│   │   ├── service/              # Business logic
│   │   ├── repository/           # JPA repositories
│   │   ├── model/                # 12 JPA entities
│   │   ├── dto/                  # Request/response DTOs
│   │   ├── security/             # JWT + RBAC
│   │   ├── exception/            # Global exception handler
│   │   └── logging/              # Structured log publisher
│   ├── src/main/resources/
│   │   ├── db/migration/         # Flyway SQL migrations (V1-V8)
│   │   └── application*.yml      # Config per profile
│   └── pom.xml
│
├── chatbot/                      # Python AI service
│   ├── chainlit_app.py           # UI entry (:8001)
│   ├── fastapi_app.py            # REST entry (:8002)
│   ├── graph/                    # LangGraph 5-agent pipeline
│   │   └── nodes/                # guardrails, sql_generator, error_recovery, analysis, visualization
│   ├── rbac/                     # Role-based SQL CTE injection
│   ├── visual_search/            # CLIP + pgvector
│   ├── auth/                     # JWT validation (Spring-compatible)
│   └── prompts/                  # LLM system prompts
│
├── frontend/                     # Angular 21 SPA
│
├── elk/                          # ELK stack configs
│   ├── elasticsearch/elasticsearch.yml
│   ├── kibana/kibana.yml
│   └── logstash/pipeline/logstash.conf
│
├── db-init/                      # PostgreSQL init scripts
│   ├── 00_pgvector.sql           # Install pgvector extension
│   └── 01_chatbot_reader.sql     # Read-only role for chatbot
│
├── security-tests/               # Automated security testing
│   ├── docker-compose.security.yml
│   ├── run-security-tests.sh
│   └── reports/
│
├── docker-compose.yml            # Main stack orchestration
├── .env.example                  # Environment template
├── setup_kibana.sh               # Provision dashboards
├── setup_kibana_alerts.sh        # Provision alerts
├── generate_traffic.sh           # Synthetic traffic generator
├── FRONTEND_API_MANUAL.md        # Complete API reference
└── README.md                     # This file
```

---

## API Documentation

- **Interactive Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs
- **Frontend Integration Manual**: [`FRONTEND_API_MANUAL.md`](./FRONTEND_API_MANUAL.md) — 1600+ lines covering every endpoint, Angular service skeletons, interceptors, and guards

### Endpoint Groups

| Prefix | Description |
|---|---|
| `/api/auth` | Login, register, refresh, forgot/reset password |
| `/api/products` | Product CRUD + search & filter (`q`, `categoryId`, `minPrice`, `maxPrice`) |
| `/api/cart` | Shopping cart + checkout |
| `/api/orders` | Order CRUD + cancel + return |
| `/api/wishlist` | Favorites |
| `/api/addresses` | User addresses |
| `/api/categories` | Hierarchical categories |
| `/api/stores` | Multi-vendor stores |
| `/api/reviews` | Product reviews |
| `/api/shipments` | Shipment tracking |
| `/api/users` | User profiles |
| `/api/customer-profiles` | Extended customer data |
| `/api/analytics` | Sales / customer / product analytics |
| `/api/chat` | AI chatbot proxy |

---

## Database

9 entities with 12 foreign key constraints, managed via Flyway migrations:

| Migration | Description |
|---|---|
| V1 | Initial schema (users, products, orders, etc.) |
| V2 | Add product embedding column (pgvector, 512-dim) |
| V3 | Cart items table |
| V4 | Product image URL |
| V5 | Product stock quantity |
| V6 | Addresses table |
| V7 | Password reset tokens + wishlist |
| V8 | Product brand, rating, retail price |

### Seeded Data
- 1,852 users
- 4,000 products (with CSV data)
- 981 Amazon products with CLIP image embeddings (for visual search)
- 7,000 orders
- 126,485 order items
- 5,000 reviews
- 10,999 shipments

---

## AI Chatbot Pipeline

The chatbot uses a 5-agent LangGraph pipeline:

```
User Question
     │
     ▼
┌─────────────┐   off-topic/greeting/unsafe   ┌─────┐
│ Guardrails  │──────────────────────────────▶│ END │
│ (classify + │                                └─────┘
│  safety)    │   sql_query intent
└─────────────┘──────────────┐
                             ▼
                    ┌────────────────┐
                    │ SQL Generator  │◀──────────────┐
                    │ (NL → SELECT)  │               │
                    └───────┬────────┘               │
                      ok  ╱    ╲  error              │
                         ╱      ╲                    │
                        ▼        ▼                   │
              ┌──────────┐  ┌────────────────┐       │
              │ Analysis │  │ Error Recovery │───────┘
              │ (explain │  │ (fix + retry)  │  (max 3 retries)
              └────┬─────┘  └────────────────┘
                   ▼
            ┌──────────────┐
            │Visualization │   (deterministic chart selection)
            │ (Plotly JSON)│
            └──────┬───────┘
                   ▼
                 END
```

**Security features:**
- SQL executor allows only `SELECT` / `WITH` queries
- Regex rejection of `INSERT`, `UPDATE`, `DELETE`, `DROP`, `CREATE`, `GRANT`
- Hard row limit (500) per query
- Mandatory leading CTE for role-based data scoping
- `password_hash` column hidden from schema context

---

## Visual Search

Upload an image to the Chainlit UI (`http://localhost:8001`) to find visually similar products.

**How it works:**
1. Image → CLIP ViT-B-32 image encoder → 512-dim vector
2. PostgreSQL `ORDER BY embedding <=> <query_vector>` (pgvector cosine)
3. Returns top-K similar products

**Blended search** (image + text):
- 70% image embedding + 30% text embedding
- Example: "find a red version of this mouse"

---

## Observability

### Kibana Dashboards

Access: http://localhost:5601 (login: `elastic` / `elastic`)

**Pre-configured visualizations:**
1. Event Type Distribution (donut chart)
2. API Requests Over Time (histogram)
3. User Role Breakdown (pie chart)
4. Auth Failures Counter
5. Orders Placed Counter
6. Average Response Time (line chart)

### Alerts

- **AUTH_FAILED spike** — ≥5 failed logins in 5 minutes (brute-force detection)
- **High response time** — ≥3 requests >500ms in 5 minutes
- **Order spike** — ≥10 orders placed in 1 minute

### Setup

```bash
# After the stack is running
./setup_kibana.sh          # Create data view + dashboard
./setup_kibana_alerts.sh   # Create alerts
./generate_traffic.sh      # Generate synthetic traffic
```

---

## Testing

### Unit & Integration Tests (Java)

```bash
cd backend
./mvnw test
```

- 26 tests across JwtUtil, Auth/Order/Analytics services, Auth/Product controllers, and a full-context integration test

### Python Chatbot Tests

```bash
cd chatbot
pytest
```

- 19 tests covering JWT validation, RBAC CTE generation, and visualization logic

### Security Tests (Schemathesis + OWASP ZAP)

```bash
cd security-tests
./run-security-tests.sh
```

Or manually:

```bash
docker compose -f docker-compose.security.yml run --rm schemathesis
docker compose -f docker-compose.security.yml run --rm zap
```

Reports are saved to `security-tests/reports/` as HTML, JSON, and TXT.

---

## Environment Variables

See `.env.example` for the full list. Key variables:

| Variable | Default | Description |
|---|---|---|
| `POSTGRES_USER` | `datapulse` | Database user |
| `POSTGRES_PASSWORD` | `changeme` | Database password |
| `JWT_SECRET` | _(base64)_ | JWT signing key (shared between backend and chatbot) |
| `SEED_PASSWORD` | `changeme` | Password for CSV-seeded users |
| `ELASTIC_PASSWORD` | `elastic` | Elasticsearch superuser password |
| `OPENAI_API_KEY` | — | **Required** for chatbot |
| `LLM_PROVIDER` | `openai` | `openai` or `anthropic` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | Comma-separated |
| `DATASETS_PATH` | `/datasets/output/` | CSV datasets path |

---

## Development

### Running Backend Locally (without Docker)

```bash
# Requires: Java 17, PostgreSQL 16 with pgvector, Maven 3.9
cd backend

export DB_USER=datapulse
export DB_PASS=changeme
export JWT_SECRET=your-base64-secret
export DATALOADER_ENABLED=false

./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Running Chatbot Locally

```bash
cd chatbot
pip install -r requirements.txt

export OPENAI_API_KEY=sk-...
export DB_HOST=localhost
export JWT_SECRET=<same as backend>

# Run both services
supervisord -c supervisord.conf

# Or individually:
chainlit run chainlit_app.py --port 8001
uvicorn fastapi_app:app --port 8002
```

### Running Frontend Locally

```bash
cd frontend
npm install
npx ng serve
# → http://localhost:4200
```

---

## License

See [`backend/LICENSE`](./backend/LICENSE).

---

## Contributing

This is a university term project. For questions or issues, open a GitHub issue.
