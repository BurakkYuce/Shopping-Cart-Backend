#!/usr/bin/env bash
# ============================================================
# ELK stack — tek komut konfigürasyon
# - Index template (ecommerce-logs-*)
# - ILM policy (7 gün retention, günlük rollover)
# - Kibana data view + dashboard + visualizations (ndjson import)
#
# Kullanım: ./scripts/elk-setup.sh
# Idempotent — istediğin kadar çalıştırabilirsin.
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

ES_URL="${ES_URL:-http://localhost:9200}"
KIBANA_URL="${KIBANA_URL:-http://localhost:5601}"
ES_USER="${ES_USER:-elastic}"
ES_PASS="${ELASTIC_PASSWORD:-elastic}"
AUTH="${ES_USER}:${ES_PASS}"

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
ok()   { echo -e "  ${GREEN}✓${NC} $*"; }
warn() { echo -e "  ${YELLOW}!${NC} $*"; }
fail() { echo -e "  ${RED}✗${NC} $*"; exit 1; }

wait_for() {
  local name=$1 url=$2 user=${3:-} pass=${4:-}
  local authopt=()
  [ -n "$user" ] && authopt=(-u "$user:$pass")
  local i=0
  until curl -sf "${authopt[@]}" "$url" >/dev/null 2>&1; do
    i=$((i+1))
    [ $i -gt 60 ] && fail "$name hazır değil: $url"
    sleep 2
  done
  ok "$name hazır"
}

echo -e "${GREEN}== ELK setup ==${NC}"
wait_for "Elasticsearch" "${ES_URL}/_cluster/health" "$ES_USER" "$ES_PASS"
wait_for "Kibana"        "${KIBANA_URL}/api/status"   "$ES_USER" "$ES_PASS"

echo -e "\n${GREEN}[1/3] ILM policy${NC}"
curl -sf -u "$AUTH" -X PUT "${ES_URL}/_ilm/policy/ecommerce-logs-policy" \
  -H "Content-Type: application/json" \
  -d @"$REPO_ROOT/elk/elasticsearch/ilm/ecommerce-logs-policy.json" >/dev/null \
  && ok "ecommerce-logs-policy aktif" || fail "ILM policy apply başarısız"

echo -e "\n${GREEN}[2/3] Index template${NC}"
curl -sf -u "$AUTH" -X PUT "${ES_URL}/_index_template/ecommerce-logs" \
  -H "Content-Type: application/json" \
  -d @"$REPO_ROOT/elk/elasticsearch/templates/ecommerce-logs-template.json" >/dev/null \
  && ok "ecommerce-logs template aktif" || fail "Index template apply başarısız"

echo -e "\n${GREEN}[3/3] Kibana saved objects${NC}"
BUNDLE="$REPO_ROOT/elk/kibana/exports/datapulse-dashboard.ndjson"
if [ ! -f "$BUNDLE" ]; then
  warn "Bundle yok: $BUNDLE — skip"
else
  RESP=$(curl -s -u "$AUTH" -X POST "${KIBANA_URL}/api/saved_objects/_import?overwrite=true" \
    -H "kbn-xsrf: true" \
    --form "file=@${BUNDLE}")
  SUCCESS=$(echo "$RESP" | python3 -c "import json,sys;print(json.load(sys.stdin).get('success',False))" 2>/dev/null || echo "false")
  COUNT=$(echo "$RESP" | python3 -c "import json,sys;print(json.load(sys.stdin).get('successCount',0))" 2>/dev/null || echo "0")
  if [ "$SUCCESS" = "True" ] || [ "$SUCCESS" = "true" ]; then
    ok "Import OK — $COUNT saved object yüklendi"
  else
    echo "$RESP" | python3 -m json.tool 2>/dev/null | head -30 || echo "$RESP"
    fail "Kibana import başarısız"
  fi
fi

echo
echo -e "${GREEN}Hazır!${NC}"
echo "  Kibana:    ${KIBANA_URL}"
echo "  Login:     ${ES_USER} / ${ES_PASS}"
echo "  Dashboard: Analytics → Dashboard → 'E-Commerce Analytics Dashboard'"
