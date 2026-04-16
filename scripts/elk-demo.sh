#!/usr/bin/env bash
# ============================================================
# ELK Stack Demo Script — DataPulse E-Commerce
# Hocaya göstermek için: ./scripts/elk-demo.sh
# ============================================================
set -euo pipefail

ES_URL="http://localhost:9200"
ES_USER="elastic"
ES_PASS="${ELASTIC_PASSWORD:-elastic}"
KIBANA_URL="http://localhost:5601"
AUTH="-u ${ES_USER}:${ES_PASS}"

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
NC='\033[0m'

header() { echo -e "\n${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"; echo -e "${GREEN}  $1${NC}"; echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"; }

# ──────────────────────────────────────────────────
header "1. ELASTICSEARCH — Cluster Health"
# ──────────────────────────────────────────────────
curl -s ${AUTH} "${ES_URL}/_cluster/health?pretty" | python3 -c "
import sys, json
d = json.load(sys.stdin)
print(f'  Cluster:  {d[\"cluster_name\"]}')
print(f'  Status:   {d[\"status\"]}')
print(f'  Nodes:    {d[\"number_of_nodes\"]}')
print(f'  Indices:  {d[\"active_primary_shards\"]} primary shards')
"

# ──────────────────────────────────────────────────
header "2. ELASTICSEARCH — Log Indices"
# ──────────────────────────────────────────────────
echo "  Günlük index'ler (ecommerce-logs-YYYY.MM.DD):"
curl -s ${AUTH} "${ES_URL}/_cat/indices/ecommerce-logs-*?v&h=index,docs.count,store.size&s=index:desc" 2>/dev/null | head -10

# ──────────────────────────────────────────────────
header "3. ELASTICSEARCH — Toplam Log Sayısı"
# ──────────────────────────────────────────────────
TOTAL=$(curl -s ${AUTH} "${ES_URL}/ecommerce-logs-*/_count" | python3 -c "import sys,json; print(json.load(sys.stdin)['count'])")
echo "  Toplam log kaydı: ${TOTAL}"

# ──────────────────────────────────────────────────
header "4. LOGSTASH — Event Type Dağılımı"
# ──────────────────────────────────────────────────
curl -s ${AUTH} "${ES_URL}/ecommerce-logs-*/_search" -H "Content-Type: application/json" -d '{
  "size": 0,
  "aggs": {
    "event_types": {
      "terms": { "field": "event_type.keyword", "size": 20 }
    }
  }
}' | python3 -c "
import sys, json
d = json.load(sys.stdin)
buckets = d['aggregations']['event_types']['buckets']
print(f'  {\"Event Type\":<30} {\"Count\":>8}')
print(f'  {\"─\"*30} {\"─\"*8}')
for b in buckets:
    print(f'  {b[\"key\"]:<30} {b[\"doc_count\"]:>8}')
"

# ──────────────────────────────────────────────────
header "5. LOGSTASH — Son 5 Business Event"
# ──────────────────────────────────────────────────
curl -s ${AUTH} "${ES_URL}/ecommerce-logs-*/_search" -H "Content-Type: application/json" -d '{
  "size": 5,
  "sort": [{"@timestamp": "desc"}],
  "query": {"exists": {"field": "event_type"}}
}' | python3 -c "
import sys, json
d = json.load(sys.stdin)
for h in d['hits']['hits']:
    s = h['_source']
    ts = s.get('@timestamp','')[:19]
    evt = s.get('event_type','?')
    uid = s.get('user_id','anon')
    det = json.dumps(s.get('details',{}), ensure_ascii=False)[:60]
    print(f'  {ts}  [{evt:<20}]  user={uid:<10}  {det}')
"

# ──────────────────────────────────────────────────
header "6. LOGSTASH — HTTP Endpoint İstatistikleri"
# ──────────────────────────────────────────────────
curl -s ${AUTH} "${ES_URL}/ecommerce-logs-*/_search" -H "Content-Type: application/json" -d '{
  "size": 0,
  "query": {"exists": {"field": "endpoint"}},
  "aggs": {
    "endpoints": {
      "terms": { "field": "endpoint.keyword", "size": 10, "order": {"_count": "desc"} }
    }
  }
}' | python3 -c "
import sys, json
d = json.load(sys.stdin)
buckets = d['aggregations']['endpoints']['buckets']
if buckets:
    print(f'  {\"Endpoint\":<40} {\"Hits\":>8}')
    print(f'  {\"─\"*40} {\"─\"*8}')
    for b in buckets:
        print(f'  {b[\"key\"]:<40} {b[\"doc_count\"]:>8}')
else:
    print('  (endpoint field henüz loglanmamış — login/order gibi işlemler yapınca dolar)')
"

# ──────────────────────────────────────────────────
header "7. LOGSTASH — Response Time Ortalaması"
# ──────────────────────────────────────────────────
curl -s ${AUTH} "${ES_URL}/ecommerce-logs-*/_search" -H "Content-Type: application/json" -d '{
  "size": 0,
  "query": {"exists": {"field": "response_time_ms"}},
  "aggs": {
    "avg_response": { "avg": { "field": "response_time_ms" } },
    "max_response": { "max": { "field": "response_time_ms" } },
    "p95_response": { "percentiles": { "field": "response_time_ms", "percents": [95] } }
  }
}' | python3 -c "
import sys, json
d = json.load(sys.stdin)
total = d['hits']['total']['value']
if total > 0:
    aggs = d['aggregations']
    print(f'  Toplam request: {total}')
    print(f'  Ortalama:       {aggs[\"avg_response\"][\"value\"]:.1f} ms')
    print(f'  Max:            {aggs[\"max_response\"][\"value\"]:.1f} ms')
    p95 = list(aggs['p95_response']['values'].values())[0]
    print(f'  P95:            {p95:.1f} ms')
else:
    print('  (response_time_ms field henüz loglanmamış)')
"

# ──────────────────────────────────────────────────
header "8. LOGSTASH — Log Level Dağılımı"
# ──────────────────────────────────────────────────
curl -s ${AUTH} "${ES_URL}/ecommerce-logs-*/_search" -H "Content-Type: application/json" -d '{
  "size": 0,
  "aggs": {
    "levels": {
      "terms": { "field": "level.keyword", "size": 10 }
    }
  }
}' | python3 -c "
import sys, json
d = json.load(sys.stdin)
for b in d['aggregations']['levels']['buckets']:
    bar = '█' * (b['doc_count'] // 50)
    print(f'  {b[\"key\"]:<8} {b[\"doc_count\"]:>6}  {bar}')
"

# ──────────────────────────────────────────────────
header "9. KIBANA — Erişim Bilgileri"
# ──────────────────────────────────────────────────
echo -e "  ${YELLOW}URL:${NC}      ${KIBANA_URL}"
echo -e "  ${YELLOW}User:${NC}     elastic"
echo -e "  ${YELLOW}Password:${NC} ${ES_PASS}"
echo ""
echo "  Kibana'da gösterilecek ekranlar:"
echo "  ─────────────────────────────────────────────"
echo "  1. Discover       → Sol menü > Analytics > Discover"
echo "                       Index pattern: ecommerce-logs-*"
echo "                       Tüm logları filtrele, ara, zaman aralığı seç"
echo ""
echo "  2. Dashboard      → Sol menü > Analytics > Dashboard > Create"
echo "                       Hazır vizualizasyon ekle (aşağıdaki script ile)"
echo ""
echo "  3. Dev Tools      → Sol menü > Management > Dev Tools"
echo "                       ES query'lerini canlı çalıştır"
echo ""
echo "  4. Index Mgmt     → Sol menü > Stack Management > Index Management"
echo "                       ecommerce-logs-* index'leri gör"

# ──────────────────────────────────────────────────
header "10. Canlı Log Üretme (Login + Product View)"
# ──────────────────────────────────────────────────
echo "  Backend'e istek atarak yeni log üretiyorum..."

# Login → USER_LOGIN event
LOGIN_RESP=$(curl -s http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"email":"admin@datapulse.com","password":"changeme"}' 2>/dev/null)
TOKEN=$(echo "$LOGIN_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null || echo "")

if [ -n "$TOKEN" ]; then
    echo "  ✓ Login event üretildi (USER_LOGIN)"

    # Product view → PRODUCT_VIEWED event
    PROD_ID=$(curl -s "http://localhost:8080/api/products?size=1" | python3 -c "import sys,json; c=json.load(sys.stdin).get('content',[]); print(c[0]['id'] if c else '')" 2>/dev/null)
    if [ -n "$PROD_ID" ]; then
        curl -s "http://localhost:8080/api/products/${PROD_ID}" > /dev/null 2>&1
        echo "  ✓ Product view event üretildi (PRODUCT_VIEWED)"
    fi

    # Order list → API_REQUEST event
    curl -s "http://localhost:8080/api/orders?size=1" -H "Authorization: Bearer $TOKEN" > /dev/null 2>&1
    echo "  ✓ API request event üretildi (API_REQUEST)"
else
    echo "  ⚠ Backend'e bağlanılamadı — loglar elle üretilecek"
fi

echo ""
echo -e "  ${GREEN}3-5 saniye bekleyin, sonra Kibana Discover'da yeni logları göreceksiniz.${NC}"
echo -e "  ${GREEN}Refresh butonuna (🔄) basın veya zaman aralığını 'Last 15 minutes' yapın.${NC}"

header "Demo tamamlandı!"
echo "  Kibana:  http://localhost:5601"
echo "  ES API:  http://localhost:9200"
echo ""
