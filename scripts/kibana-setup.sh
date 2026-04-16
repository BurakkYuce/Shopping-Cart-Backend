#!/usr/bin/env bash
# ============================================================
# Kibana Data View + Saved Search Setup
# Kullanım: ./scripts/kibana-setup.sh
# ============================================================
set -euo pipefail

KIBANA_URL="http://localhost:5601"
ES_PASS="${ELASTIC_PASSWORD:-elastic}"
AUTH="elastic:${ES_PASS}"

GREEN='\033[0;32m'
NC='\033[0m'

echo -e "${GREEN}[1/3] Data View oluşturuluyor...${NC}"
curl -s -u "${AUTH}" -X POST "${KIBANA_URL}/api/data_views/data_view" \
  -H "kbn-xsrf: true" \
  -H "Content-Type: application/json" \
  -d '{
    "data_view": {
      "title": "ecommerce-logs-*",
      "timeFieldName": "@timestamp",
      "name": "E-Commerce Logs"
    }
  }' | python3 -c "
import sys, json
d = json.load(sys.stdin)
dv = d.get('data_view', {})
print(f'  Data View ID: {dv.get(\"id\", \"already exists\")}')
print(f'  Title: {dv.get(\"title\", \"\")}')
" 2>/dev/null || echo "  Data view zaten mevcut (OK)"

echo -e "\n${GREEN}[2/3] Kibana'ya örnek Discover aramaları...${NC}"
echo "  Kibana Discover'da kullanabileceğin KQL filtreleri:"
echo ""
echo "  ┌──────────────────────────────────────────────────────────────┐"
echo "  │ Filtre                          │ Ne gösterir               │"
echo "  ├──────────────────────────────────┼───────────────────────────┤"
echo "  │ event_type: USER_LOGIN          │ Login olayları            │"
echo "  │ event_type: ORDER_PLACED        │ Sipariş olayları          │"
echo "  │ event_type: PRODUCT_VIEWED      │ Ürün görüntüleme          │"
echo "  │ event_type: AUTH_FAILED         │ Başarısız login denemeleri │"
echo "  │ event_type: CHATBOT_SQL_EXEC    │ Chatbot SQL sorguları     │"
echo "  │ event_type: CHATBOT_SQL_REJECTED│ Reddedilen SQL'ler        │"
echo "  │ level: ERROR                    │ Tüm hatalar               │"
echo "  │ level: WARN                     │ Tüm uyarılar              │"
echo "  │ status_code >= 400              │ HTTP hata yanıtları       │"
echo "  │ response_time_ms > 1000         │ Yavaş istekler (>1s)      │"
echo "  └──────────────────────────────────┴───────────────────────────┘"

echo -e "\n${GREEN}[3/3] Dev Tools örnek sorguları...${NC}"
echo "  Kibana > Dev Tools'a yapıştır:"
echo ""
cat << 'DEVTOOLS'
  # --- Toplam log sayısı ---
  GET ecommerce-logs-*/_count

  # --- Event type dağılımı ---
  GET ecommerce-logs-*/_search
  {
    "size": 0,
    "aggs": {
      "events": {
        "terms": { "field": "event_type.keyword", "size": 20 }
      }
    }
  }

  # --- Son 10 hata ---
  GET ecommerce-logs-*/_search
  {
    "size": 10,
    "sort": [{"@timestamp": "desc"}],
    "query": { "match": { "level": "ERROR" } }
  }

  # --- Saatlik log hacmi ---
  GET ecommerce-logs-*/_search
  {
    "size": 0,
    "aggs": {
      "hourly": {
        "date_histogram": {
          "field": "@timestamp",
          "calendar_interval": "hour"
        }
      }
    }
  }

  # --- User bazlı event sayısı ---
  GET ecommerce-logs-*/_search
  {
    "size": 0,
    "query": { "exists": { "field": "user_id" } },
    "aggs": {
      "users": {
        "terms": { "field": "user_id.keyword", "size": 10 }
      }
    }
  }
DEVTOOLS

echo ""
echo -e "${GREEN}Hazır! Kibana'yı aç: ${KIBANA_URL}${NC}"
echo "  Login: elastic / ${ES_PASS}"
