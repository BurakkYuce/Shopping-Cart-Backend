Run a full parallel audit of the DataPulse e-commerce platform using all 5 specialist subagents.

## Environment

- **Frontend (Angular 21)**: http://localhost:4200
- **Backend (Spring Boot)**: http://localhost:8080/api
- **Chatbot (Python)**: http://localhost:8001 (in docker, reachable via `docker exec` or the frontend chat widget)
- **Postgres**: container `shopping-cart-backend-postgres-1`, db `ecommerce`, user `datapulse`
- **Seeded credentials** (all share password `changeme`):
  - Admin: `admin@datapulse.com`
  - Corporate sellers: `techstore@datapulse.com`, `fashionhub@datapulse.com`, `homegoods@datapulse.com`
  - Individual: `customer@datapulse.com` + `retail_<hex>@ecommerce.com` (×1851)
- **Catalog state**: 12,617 products (cleaned — no duplicate names, no $10k price cluster), 99.8% have CLIP embeddings

## CRITICAL: parallel execution rule

You MUST launch all 5 Agent tool calls **in a single assistant message** (one response block with 5 tool_use blocks). Do NOT send them sequentially across multiple turns — that defeats the parallel workflow. The user explicitly asked for parallel fan-out.

## Fan-out — 5 subagents, one message

Launch these 5 Agent tool calls together:

### 1. `ui-ux-critic`
Task: Audit the live DataPulse frontend at http://localhost:4200 across all 6 evaluation dimensions from your definition (Navigation, PLP, PDP, Checkout, Mobile, Conversion). Compare against Trendyol / Hepsiburada / N11 patterns. Log in as `customer@datapulse.com` / `changeme` to reach authenticated views. Emit structured findings per dimension plus the final score card.

### 2. `qa-tester`
Task: Execute the TC-001…TC-032 test suite against the live stack. Use the frontend at http://localhost:4200 for UI flows and the backend API at http://localhost:8080/api for functional/integration checks. Use seeded credentials (`customer@datapulse.com` / `changeme` for user flows, `admin@datapulse.com` / `changeme` for admin flows). Emit one JSON entry per TC plus the summary (totals, critical FAILs, top 3 fixes).

### 3. `ai-tester`
Task: Evaluate the chatbot and visual search features. For the chatbot, run all AI-001…AI-023 scenarios against the chat widget on the frontend (or directly against the Python service at http://localhost:8001). For visual search, run VS-001…VS-023 against the `/api/visual-search` endpoint (check `visual_search/` module for the exact route). Emit structured JSON per test.

### 4. `product-bot`
Task: Act in character as the pre-purchase product assistant and answer these 5 representative queries, **citing real product data from `GET http://localhost:8080/api/products`**:
- "Bu sitede Samsung telefon var mı? En ucuzu kaç lira?"
- "XL beden bir tişört arıyorum, hangi ürünleri önerirsin?"
- "250 TL altı oyuncak var mı?"
- "Bir laptop için mAh bilgisi verebilir misin?"
- "Bu site hangi markaları satıyor?"

For each, output: query, your in-character reply (under 150 words), and any catalog issues you hit (missing fields, broken image URLs, empty stock, etc.).

### 5. `support-bot`
Task: Act in character as the post-purchase support agent and handle these 5 representative scenarios, **using real order data from `GET http://localhost:8080/api/orders` (auth as `admin@datapulse.com` / `changeme`)**:
- "Siparişim nerede? Kargo takip kodu var mı?"
- "Ürünü iade etmek istiyorum, 14 gün geçmedi."
- "Yanlış ürün geldi, ne yapmalıyım?"
- "Siparişimi iptal edebilir miyim? Durum: Hazırlanıyor."
- "Yetkili ile konuşmak istiyorum."

For each, output: scenario, your in-character reply (structured: Acknowledge → Action → Resolution → Confirm), escalation decision (yes/no + reason), and any data issues you hit.

## Synthesis (Phase 2 — after all 5 return)

Once all 5 subagent reports come back, emit a single unified report in the orchestrator JSON schema defined in `.claude/commands/CLAUDE.md`:

```json
{
  "routed_to": ["ui-ux-critic", "qa-tester", "ai-tester", "product-bot", "support-bot"],
  "summary": "One-paragraph synthesis of the most important findings across all 5 specialists",
  "findings": [
    {
      "agent": "ui-ux-critic",
      "key_issues": ["...", "..."],
      "recommendations": ["...", "..."]
    },
    { "agent": "qa-tester", "key_issues": [...], "recommendations": [...] },
    { "agent": "ai-tester", "key_issues": [...], "recommendations": [...] },
    { "agent": "product-bot", "key_issues": [...], "recommendations": [...] },
    { "agent": "support-bot", "key_issues": [...], "recommendations": [...] }
  ],
  "priority_actions": [
    { "action": "...", "owner": "frontend | backend | chatbot | data", "priority": "P1" },
    { "action": "...", "owner": "...", "priority": "P2" },
    { "action": "...", "owner": "...", "priority": "P3" }
  ]
}
```

After the JSON, include a short human-readable executive summary (≤150 words) highlighting the top 3 P1 items.

## Rules

- **ONE message, 5 Agent calls** — not 5 separate turns.
- Each subagent starts with no context from this conversation, so its prompt must be self-contained.
- Do not fabricate results — subagents must actually inspect the live system.
- If a subagent can't reach a service (connection refused, timeout), it should report that as a blocker in its output and continue with whatever it can test.
- Treat `commands/CLAUDE.md` as the authoritative spec for the final output format.
