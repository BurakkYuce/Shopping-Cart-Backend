#!/usr/bin/env bash
# ============================================================
# ES log import — JSON dump'tan logları geri yükle
# Giriş: elk/backups/ecommerce-logs.json
#
# Kullanım (EC2'de): ./scripts/logs-import.sh
# Önce ELK stack ayakta olmalı (docker compose up -d)
# Önce elk-setup (index template + ILM) uygulanmış olmalı
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKUP_DIR="$REPO_ROOT/elk/backups"
DATA_FILE="$BACKUP_DIR/ecommerce-logs.json"

ES_PASS="${ELASTIC_PASSWORD:-elastic}"
TARGET_INDEX="${TARGET_INDEX:-ecommerce-logs-imported}"
ES_HOST_IN_COMPOSE="http://elastic:${ES_PASS}@elasticsearch:9200"

GREEN='\033[0;32m'; RED='\033[0;31m'; NC='\033[0m'

echo -e "${GREEN}== Log import ==${NC}"

[ -f "$DATA_FILE" ] || { echo -e "${RED}$DATA_FILE yok. Önce logs-export.sh çalıştır.${NC}"; exit 1; }

ES_CONT=$(docker ps --filter "name=elasticsearch" --format '{{.Names}}' | head -1)
[ -n "$ES_CONT" ] || { echo -e "${RED}Elasticsearch container bulunamadı. docker compose up -d${NC}"; exit 1; }
NET=$(docker inspect "$ES_CONT" --format '{{range $k,$v := .NetworkSettings.Networks}}{{$k}} {{end}}' | tr ' ' '\n' | grep '_elk-net$' | head -1)
[ -n "$NET" ] || { echo -e "${RED}$ES_CONT'ın elk-net'i yok${NC}"; exit 1; }
echo "  Network:      $NET"
echo "  Kaynak:       $DATA_FILE ($(du -h "$DATA_FILE" | cut -f1))"
echo "  Hedef index:  $TARGET_INDEX"

docker run --rm --network "$NET" \
  -v "$BACKUP_DIR:/data:ro" \
  elasticdump/elasticsearch-dump:latest \
  --input=/data/ecommerce-logs.json \
  --output="${ES_HOST_IN_COMPOSE}/${TARGET_INDEX}" \
  --type=data \
  --limit=1000 2>&1 | tail -3

echo
COUNT=$(curl -sf -u "elastic:${ES_PASS}" "http://localhost:9200/${TARGET_INDEX}/_count" | python3 -c 'import json,sys;print(json.load(sys.stdin)["count"])' 2>/dev/null || echo "?")
echo -e "${GREEN}OK. Hedef index doküman sayısı: ${COUNT}${NC}"
echo
echo "Kibana dashboard'ı açtığında loglar görünür. Data view 'ecommerce-logs-*' bu index'i de kapsar."
