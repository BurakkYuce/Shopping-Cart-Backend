# E-Commerce Analytics Chatbot — Teknik Brifing

> Mülakat, sunum veya teknik tartışma için kapsamlı referans dokümanı.

---

## 1. Proje Özeti

Bu proje, e-ticaret platformuna entegre bir **AI analytics asistanı**dır. Kullanıcılar doğal dil sorularıyla veri tabanını sorgulayabilir, ya da bir ürün fotoğrafı yükleyerek görsel benzerlik araması yapabilir.

**Iki temel özellik:**
1. **Text2SQL chatbot** — "Bu ay en çok satan 10 ürün hangisi?" gibi sorulara yanıt verir
2. **Visual product search** — Bir ürün fotoğrafı yüklendiğinde benzer ürünleri bulur (Google Lens mantığı)

**Stack:**
- **LangGraph** — çok-ajanlı orkestrasyonu
- **Chainlit** — web arayüzü (port 8001)
- **FastAPI** — REST API (port 8002, Spring Boot proxy için)
- **PostgreSQL + pgvector** — veri tabanı + vektör araması
- **CLIP (ViT-B-32)** — görsel embedding modeli
- **OpenAI GPT-4o-mini** — LLM (LangChain üzerinden)
- **Spring Boot** — ana backend, JWT authentication

---

## 2. Sistem Mimarisi

```
┌─────────────────────────────────────────────────────────────────┐
│                         KULLANICI                               │
└───────────────────────┬─────────────────────────────────────────┘
                        │ browser
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│              Chainlit UI  (localhost:8001)                       │
│  1. JWT token yapıştır (Spring Boot /api/auth/login'den alınır) │
│  2. Metin sorusu YA DA resim yükle                              │
└───────┬───────────────────────────────────┬─────────────────────┘
        │ text query                         │ image upload
        ▼                                   ▼
┌───────────────────┐           ┌───────────────────────┐
│  LangGraph Graph  │           │   Visual Search        │
│  (5 agent node)   │           │   CLIP → pgvector      │
└───────┬───────────┘           └───────────┬───────────┘
        │                                   │
        ▼                                   ▼
┌───────────────────────────────────────────────────────────────┐
│                  PostgreSQL (ecommerce DB)                     │
│   • 9 tablo, ~155K satır                                      │
│   • pgvector extension (512-dim CLIP embeddings)              │
│   • 981 ürün image embedding ile yüklü                        │
└───────────────────────────────────────────────────────────────┘
        ▲
        │ JWT proxy (RestTemplate)
┌───────────────────┐
│  Spring Boot       │
│  (localhost:8080)  │
│  POST /api/chat/ask│
└───────────────────┘
```

---

## 3. LangGraph Multi-Agent Pipeline

### 3.1 Graph Topolojisi

```
START
  └─→ [1] GUARDRAILS
          ├─→ off_topic / greeting / clarify / unsafe ──→ END
          └─→ sql_query
                  └─→ [2] SQL GENERATOR
                              ├─→ (başarı) ──→ [4] ANALYSIS ──→ [5] VISUALIZATION ──→ END
                              └─→ (hata)   ──→ [3] ERROR RECOVERY
                                                      └─→ [2] SQL GENERATOR (retry)
                                                              └─→ (max retry) ──→ FATAL HANDLER ──→ END
```

**Maksimum retry sayısı:** 3 (env var `SQL_RETRY_LIMIT` ile değiştirilebilir)

### 3.2 Agent 1 — Guardrails

**Dosya:** `graph/nodes/guardrails.py`

**Görevi:** Kullanıcı sorusunu sınıflandırır. LLM'e JSON döndürmesini söyler:
```json
{"intent": "sql_query", "is_safe": true, "reason": ""}
```

**Intent türleri:**
- `sql_query` → SQL pipeline'ına devam et
- `greeting` → Standart karşılama mesajı döndür, SQL çalıştırma
- `off_topic` → "Sadece e-ticaret verisi hakkında konuşabilirim" de
- `clarify` → "Daha spesifik olabilir misin?" de
- `is_safe: false` → Zararlı/inject içerikli sorgular, hemen sonlandır

**Neden gerekli?** LLM'e her soruyu SQL'e dönüştürmek yerine önce bir "kapı bekçisi" geçirmek, hem maliyeti düşürür hem prompt injection'a karşı korur.

**Teknik detay:** LLM bazen markdown fence içinde JSON döndürür. Guardrails bunu parse etmeden önce fence'leri temizler (```` ```json ``` ```` bloklarını soyar).

### 3.3 Agent 2 — SQL Generator

**Dosya:** `graph/nodes/sql_generator.py`

**Görevi:** Natural language soruyu çalıştırılabilir PostgreSQL SELECT sorgusuna çevirir.

**Prompt'a enjekte edilenler:**
1. **Schema context** — 9 tablonun açıklaması, FK ilişkileri, JOIN örnekleri (statik, session başında bir kez okunur)
2. **Role-filter CTE** — Role göre zorunlu CTE bloğu (RBAC, bkz. §5)
3. **Conversation history** — Son 3 Q&A çifti (bağlamsal sorgular için)
4. **Original question** — Kullanıcı sorusu

**Error recovery sonrası davranış:** `retry_count > 0` ve `sql_error is None` ise LLM'i çağırmaz — sadece `error_recovery`'nin düzelttiği SQL'i yeniden çalıştırır.

**SQL Executor güvenlik katmanı** (`db/executor.py`):
- Sadece `SELECT` veya `WITH` ile başlayan sorgulara izin verir
- `INSERT, UPDATE, DELETE, DROP, CREATE, GRANT, EXEC` gibi pattern'ları regex ile reddeder
- Sonuç satırı hard-limit: 500 (env `MAX_SQL_ROWS`)
- `password_hash` kolonu schema'ya bile yazılmamış, LLM göremez

### 3.4 Agent 3 — Error Recovery

**Dosya:** `graph/nodes/error_recovery.py`

**Görevi:** SQL execution hatası aldığında LLM'e hatalı SQL + hata mesajını gösterir, düzeltilmiş SQL üretmesini ister.

**Önemli detay:** Düzeltilmiş SQL'i `generated_sql`'e yazar ve `sql_error`'ı `None` yapar. SQL Generator bu durumu görünce LLM'i tekrar çağırmaz, doğrudan execute eder. Bu sayede gereksiz LLM çağrısı önlenir.

**Retry döngüsü:** Her `error_recovery` çağrısında `retry_count += 1`. Limit aşılınca `fatal_sql_handler` devreye girer ve "üzgünüm, soruyu yeniden ifade eder misin?" şeklinde bir response üretir.

### 3.5 Agent 4 — Analysis

**Dosya:** `graph/nodes/analysis.py`

**Görevi:** SQL sonuçlarını (tablo) okunabilir doğal dil analizine çevirir.

- LLM'e maximum 10 satır gösterir (token tasarrufu)
- Sonuç 500 satırdan fazlaysa truncation uyarısı ekler
- `temperature=0.3` — diğer agentlar `0.0`, analysis biraz yaratıcı olabilir

### 3.6 Agent 5 — Visualization

**Dosya:** `graph/nodes/visualization.py`

**Görevi:** Sorgu sonucuna bakarak otomatik grafik türü seçer ve Plotly JSON üretir. **LLM çağrısı yoktur** — tamamen deterministik Python lojiği.

**Grafik karar ağacı:**
| Durum | Grafik |
|-------|--------|
| Tarih kolonu var | Line chart |
| "top/rank/most/best" sorguda var, ≤20 satır | Bar chart |
| 1 kategorik + 1 sayısal kolon, ≤8 satır | Pie chart |
| >20 satır, ≥2 sayısal kolon | Scatter |
| Geri kalan | Bar chart |
| 0 satır | None |

Grafik Chainlit'e `cl.Plotly()` ile inline gönderilir.

---

## 4. Visual Product Search (Google Lens Mantığı)

### 4.1 CLIP Nedir?

OpenAI'nin CLIP modeli (Contrastive Language-Image Pretraining) iki encoder içerir:
- **Image encoder** — Resmi 512 boyutlu vektöre çevirir
- **Text encoder** — Metni 512 boyutlu vektöre çevirir

Kritik özellik: **her ikisi de aynı vektör uzayına** map eder. Yani "gaming mouse" metni ile bir gaming mouse fotoğrafının vektörleri birbirine yakındır. Bu cross-modal arama sağlar.

### 4.2 Veri Yükleme Pipeline'ı

**Script:** `visual_search/load_amazon_products.py`

Adımlar:
1. Amazon ürün CSV'ini oku (yalnızca `image_url` dolu olanları)
2. 8 thread ile paralel resim indir (`ThreadPoolExecutor`)
3. Her resim için CLIP image embedding üret (512-dim float array)
4. PostgreSQL `products` tablosuna `embedding` kolonu olarak kaydet
5. `ivfflat` vector index oluştur (cosine ops, `lists=20`)

**Sonuç:** 981 Amazon ürünü embedding ile yüklendi.

**Teknik notlar:**
- `sku` kolonu Amazon ASIN olarak kullanıldı (idempotency için)
- `store_id = "amazon-marketplace"` (dummy store, NOT NULL constraint için)
- `title[:255]` truncation (VARCHAR(255) limiti)
- `csv.field_size_limit(10**7)` — Amazon CSV'inde çok uzun alanlar var

### 4.3 Arama Algoritması

**Dosya:** `visual_search/searcher.py`

**Saf resim araması:**
```
Yüklenen resim → CLIP image encoder → 512-dim vektör
                                            ↓
PostgreSQL: ORDER BY embedding <=> CAST(:vec AS vector) LIMIT 8
                                            ↓
En benzer 8 ürün (cosine similarity ile)
```

**Resim + metin araması (blended):**
```
Resim → image embedding (512-dim)  ─┐
Metin → text embedding (512-dim)   ─┤ weighted avg (70% resim, 30% metin)
                                    ↓
           normalize(0.7 × img_emb + 0.3 × txt_emb)
                                    ↓
                         pgvector cosine search
```

Örnek: "Bu mouse'un kırmızı versiyonunu bul" → resim geometrik benzerliği korur, "kırmızı" metni sonuçları kırmızı ürünlere doğru iter.

**SQL cast sorunu çözümü:** SQLAlchemy `:vec::vector` syntax'ını yanlış parse ediyor çünkü `:` parametreleri başlatıyor. Çözüm: `CAST(:vec AS vector)`.

### 4.4 pgvector Kurulumu

pgvector PostgreSQL extension'dır. `CREATE EXTENSION vector` superuser gerektirir:
```bash
psql postgres -c "CREATE EXTENSION IF NOT EXISTS vector;" -d ecommerce
```

`ivfflat` index (Inverted File Flat):
- Approximate nearest neighbor — exact search'ten ~10x hızlı
- `lists=20` — kaç cluster (küçük dataset için yeterli, büyük dataset için artırılmalı)
- `ivfflat.probes=10` — arama sırasında kaç cluster taranacak (accuracy/speed tradeoff)

---

## 5. RBAC — Role-Based Access Control

**Dosya:** `rbac/sql_filter.py`

### Sorun
LLM SQL üretirken tüm tablolara erişebilir. `CORPORATE` kullanıcısı başka firmanın verisini sorgulayabilir. Bunu nasıl önlüyoruz?

### Çözüm: Mandatory Leading CTE

Her kullanıcı için session başında bir CTE bloğu üretilir. LLM'e "Bu CTE'yi AYNEN kullanmak zorundasın" talimatı verilir.

**ADMIN:** CTE yok, tüm tablolara tam erişim.

**CORPORATE:**
```sql
WITH _allowed_stores AS (
    SELECT id AS store_id FROM stores WHERE owner_id = 'user_id_here'
),
_allowed_orders AS (
    SELECT o.* FROM orders o JOIN _allowed_stores s ON o.store_id = s.store_id
),
_allowed_products AS (
    SELECT p.* FROM products p JOIN _allowed_stores s ON p.store_id = s.store_id
)
```
LLM'e: "`orders` yerine `_allowed_orders` kullan, `products` yerine `_allowed_products` kullan."

**INDIVIDUAL:**
```sql
WITH _allowed_orders AS (SELECT * FROM orders WHERE user_id = 'user_id_here'),
     _allowed_reviews AS (SELECT * FROM reviews WHERE user_id = 'user_id_here'),
     _allowed_profile AS (SELECT * FROM customer_profiles WHERE user_id = 'user_id_here')
```

**Güvenlik garantisi:** CTE'ler veritabanı katmanında satır filtresi uygular. LLM ne üretirse üretsin, `_allowed_orders` yalnızca o kullanıcının siparişlerini içerir.

---

## 6. JWT Authentication

**Dosya:** `auth/jwt_validator.py`

Spring Boot ile **tam uyum** sağlanmıştır:

```python
# Spring Boot JwtUtil.java:
# Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))
raw_key = base64.b64decode(os.environ["JWT_SECRET"])
payload = jwt.decode(token, raw_key, algorithms=["HS256"])
```

JWT payload'ındaki claim'ler:
- `sub` → email
- `userId` → kullanıcı ID'si
- `role` → `ADMIN | CORPORATE | INDIVIDUAL`
- `type` → `refresh` token'lar reddedilir (sadece access token kabul edilir)

**Spring Boot entegrasyonu:** `JwtAuthenticationFilter.java`, JWT token'ı `UsernamePasswordAuthenticationToken`'ın credentials alanında saklar. `ChatService.java` bu token'ı `Authorization: Bearer <token>` header'ı olarak Python chatbot'una iletir.

---

## 7. Spring Boot Entegrasyonu

### ChatService.java

```
Kullanıcı → POST /api/chat/ask (Spring Boot, port 8080)
                 ↓
    JWT token alınır (SecurityContext'ten)
                 ↓
    RestTemplate → POST http://chatbot:8002/chat/ask
                 ↓
    Python FastAPI response döner
                 ↓
    ChatResponse (answer + plotlyJson) döner
```

Spring Boot chatbot'a proxy gibi davranır. Frontend yalnızca Spring Boot ile konuşur, Python servisi doğrudan expose edilmez (production güvenlik pratiği).

---

## 8. Chainlit UI Detayları

**Dosya:** `chainlit_app.py`

### Session lifecycle

1. `on_chat_start` → JWT token iste
2. Token decode et → `UserContext` (userId, email, role)
3. `role_filter_cte` ve `schema_context` session'a kaydet (her soru için tekrar üretme)
4. `on_message` → her mesajda:
   - Resim var mı? → `_handle_visual_search()`
   - Metin mi? → LangGraph graph'ı çalıştır

### Conversation Memory
Son 20 mesaj session'da tutulur (`message_history[-20:]`). SQL Generator'a son 6 mesaj (3 Q&A çifti) gönderilir. Bu sayede "bir önceki sorgunun sonuçlarını karşılaştır" gibi follow-up sorular çalışır.

### Chainlit 2.x File Upload
Chainlit 2.x'te dosya yüklemeleri için storage backend gerekir. Built-in local storage yok, custom `LocalStorageClient` implement ettik (`BaseStorageClient`'ı extend ederek). Upload edilen dosyalar `/tmp/chainlit-uploads/` dizinine `aiofiles` ile asenkron yazılır.

---

## 9. Veritabanı Şeması

```
users (1,853 kayıt)
  └─→ stores (4 kayıt: 3 normal + 1 "amazon-marketplace")
        └─→ products (4,981 kayıt, 981'inde CLIP embedding)
              └─→ order_items (126,483 kayıt)
  └─→ orders (7,000 kayıt)
        └─→ order_items
        └─→ shipments (10,999 kayıt — order başına birden fazla olabilir)
  └─→ reviews (5,000 kayıt)
  └─→ customer_profiles
```

**Önemli notlar:**
- `shipments` tablosunda order başına birden fazla satır olabilir. JOIN'lerde `findFirstByOrderId` veya agregasyon gerekir
- `products.embedding` kolonu `vector(512)` tipinde (pgvector)
- `password_hash` kolonu schema context'e yazılmadı, LLM asla döndüremez
- Tüm CSV kullanıcılarının şifresi `SEED_PASSWORD` ortam değişkeniyle belirlenir (varsayılan: `changeme`). BCrypt, import sırasında bir kez encode edilip reuse edilir — performans optimizasyonu

---

## 10. Sık Karşılaşılan Hatalar ve Çözümleri

| Hata | Sebep | Çözüm |
|------|-------|-------|
| `Cannot load driver class: com.mysql.cj.jdbc.Driver` | Eski `application.properties` (dream-shops projesi) `application.yml`'ı override ediyordu | `application.properties` silindi + `mvn clean` |
| `store_id NOT NULL constraint` | Amazon ürünlerinin store'u yoktu | `"amazon-marketplace"` dummy store oluşturuldu |
| `StringDataRightTruncation` | Amazon başlıkları 255 karakteri aşıyordu | `title[:255]` truncation eklendi |
| `field larger than field limit` | CSV'de çok uzun alanlar | `csv.field_size_limit(10**7)` |
| `CREATE EXTENSION vector permission denied` | pgvector superuser gerektirir | `psql postgres -c "CREATE EXTENSION..."` |
| `syntax error at or near ":"` | SQLAlchemy `:vec::vector` cast'ini yanlış parse ediyor | `CAST(:vec AS vector)` kullanıldı |
| `No module named 'visual_search'` | Background process'te CWD farklı | `sys.path.insert(0, __file__'ın dizini)` |
| `Upload failed` (Chainlit 2.x) | Storage client yok | Custom `LocalStorageClient` implement edildi |
| `asyncio.get_event_loop().run_until_complete()` hatası | Zaten çalışan event loop'ta başka loop çalıştırılamaz | Sync helper async'e çevrildi |

---

## 11. Mülakat Soruları ve Cevapları

### "Neden LangGraph kullandınız, basit bir LLM çağrısı yeterli olmaz mıydı?"

Tek LLM çağrısı ile Text2SQL yapılabilir ama birkaç sorunu çözmez:
1. **Hata recovery** — SQL hata verirse ne olur? LangGraph retry döngüsü bunu yönetir
2. **Separation of concerns** — Guardrails ayrı, SQL üretimi ayrı, analiz ayrı. Her biri bağımsız test edilebilir ve geliştirilebilir
3. **State yönetimi** — `AgentState` TypedDict tüm pipeline boyunca tutarlı bir state taşır
4. **Conditional routing** — "Bu greeting mi yoksa SQL sorusu mu?" kararı bir ajanın işi, SQL üretimi başka ajanın. Tek prompt'ta bunu yapmak çok karmaşık olurdu

### "RBAC'ı neden uygulama katmanında yaptınız, veritabanı seviyesinde Row Level Security (RLS) daha güvenli olmaz mıydı?"

İkisi de geçerli yaklaşım. CTE tercihinin sebebi:
- LLM'in ürettiği SQL'in içine güvenlik katmanı "embed" ediliyor — LLM yanlış tablo kullansa bile CTE filtresi devrede
- PostgreSQL RLS, Spring Boot gibi birden fazla backend'in bağlandığı sistemlerde yönetimi zorlaştırır
- CTE yaklaşımı audit edilmesi kolay: üretilen SQL'e bakarak hangi veriye erişildiği görülüyor

### "CLIP embedding'i neden image'a uygulayıp text'e değil, tüm ürünlere text embedding yapamaz mıydı?"

Aynı CLIP modeli hem resim hem metin encode eder ve **aynı vektör uzayına** map eder. Yani:
- Image embedding: kullanıcının yüklediği resim
- Text embedding: veritabanındaki ürün adı/açıklaması

İkisi cosine similarity ile karşılaştırılabilir. Ancak text-only ürünlerde "resim aramak" semantik olarak kayıp içerir — CLIP metin/resim hizalaması mükemmel değildir.

### "512 boyutlu vektörü neden 512 boyutlu, neden daha fazla/az değil?"

CLIP ViT-B-32'nin çıktı boyutu 512. Bu OpenAI'nin eğitim sırasında belirlediği boyut. Daha büyük boyut (CLIP ViT-L-14 ile 768) daha doğru ama daha yavaş ve daha fazla disk alanı. Trade-off: doğruluk vs. hız/depolama.

### "ivfflat index nedir, exact search yapmıyor musunuz?"

`ivfflat` (Inverted File with Flat compression) **approximate nearest neighbor** (yaklaşık en yakın komşu) algoritmasıdır. 981 vektör için exact search de hızlı ama büyük veri setlerinde (milyonlarca) exact search O(n) olur, ivfflat bunu O(√n)'e indirir. `lists=20` cluster sayısı, `probes=10` arama sırasında taranan cluster sayısıdır.

### "Chainlit session'da state tutmak yeterli mi? Kullanıcı sayfayı yenilerse ne olur?"

Chainlit session bellek tabanlıdır. Sayfa yenilenince session kaybolur. Production'da bu session'ı Redis veya database'de persist etmek gerekir. Mevcut implementasyon MVP/demo için yeterli.

### "Error recovery'de neden LLM'e sadece hata mesajını değil SQL'i de gösteriyorsunuz?"

Hata recovery LLM'in doğru düzeltme yapabilmesi için hem hatayı hem hatalı SQL'i görmesi gerekir. Sadece hata mesajı ile LLM kontekst olmadan yanlış SQL üretebilir. Ayrıca CTE bloğunu da tekrar gösteriyoruz çünkü LLM bağlamı kaybedip CTE'yi unutabilir.

---

## 12. Dosya Yapısı

```
chatbot/
├── chainlit_app.py          # Chainlit UI entry point
├── rest_api.py              # FastAPI REST (Spring Boot proxy için)
├── requirements.txt
├── brief.md                 # Bu dosya
│
├── auth/
│   └── jwt_validator.py     # PyJWT, Spring Boot uyumlu base64 decode
│
├── db/
│   ├── engine.py            # SQLAlchemy engine (singleton)
│   ├── executor.py          # SELECT-only güvenli executor, 500 row limit
│   └── schema_context.py    # LLM için curated schema string
│
├── graph/
│   ├── state.py             # AgentState TypedDict
│   ├── graph_builder.py     # LangGraph StateGraph assembly
│   └── nodes/
│       ├── guardrails.py    # Agent 1: intent classification
│       ├── sql_generator.py # Agent 2: NL→SQL + execute
│       ├── error_recovery.py# Agent 3: SQL error fix
│       ├── analysis.py      # Agent 4: result narration
│       └── visualization.py # Agent 5: Plotly chart (no LLM)
│
├── llm/
│   └── provider.py          # LangChain LLM factory (OpenAI/Anthropic)
│
├── prompts/
│   ├── guardrails_system.md
│   ├── sql_generator_system.md
│   ├── error_recovery_system.md
│   └── analysis_system.md
│
├── rbac/
│   └── sql_filter.py        # Role-based CTE builder
│
├── visual_search/
│   ├── searcher.py          # CLIP encode + pgvector search
│   └── load_amazon_products.py  # One-time data loader
│
└── tests/
    ├── test_jwt_validator.py
    ├── test_sql_filter.py
    └── test_visualization.py
```

---

## 13. Ortam Değişkenleri

```bash
JWT_SECRET=ZGV2LXNlY3JldC1rZXktMzJjaGFycy1taW5pbXVtLXJlcXVpcmVk  # Base64 encoded
OPENAI_API_KEY=sk-...
DB_HOST=localhost
DB_USER=datapulse
DB_PASS=changeme
DB_NAME=ecommerce
SQL_RETRY_LIMIT=3          # opsiyonel, default 3
MAX_SQL_ROWS=500           # opsiyonel, default 500
```

## 14. Başlatma

```bash
# Local
export JWT_SECRET=... OPENAI_API_KEY=... DB_HOST=localhost DB_USER=datapulse DB_PASS=changeme DB_NAME=ecommerce
cd chatbot/
chainlit run chainlit_app.py --port 8001

# Docker (tüm stack)
cd Shopping-Cart-Backend/
docker-compose up -d --build
```

**Test kullanıcısı:**
- Email: `admin@datapulse.com`
- Şifre: `SEED_PASSWORD` ortam değişkeninin değeri (varsayılan: `changeme`)
- Swagger: `http://localhost:8080/swagger-ui.html` → `POST /api/auth/login` → token kopyala → Chainlit'e yapıştır
