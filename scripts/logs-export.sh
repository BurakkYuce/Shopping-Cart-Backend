#!/usr/bin/env bash
# ============================================================
# ES log export — local'de biriken logları JSON dump'a çevir
# Çıktı: elk/backups/ecommerce-logs.json (+ mappings.json)
#
# Kullanım: ./scripts/logs-export.sh
# EC2'ye aktarmak için: scp -i key.pem elk/backups ec2:~/Shopping-Cart-Backend/elk/
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKUP_DIR="$REPO_ROOT/elk/backups"

ES_PASS="${ELASTIC_PASSWORD:-elastic}"
ES_URL="${ES_URL:-http://localhost:9200}"
ES_HOST_IN_COMPOSE="http://elastic:${ES_PASS}@elasticsearch:9200"
INDEX_PATTERN="ecommerce-logs-*"

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'

mkdir -p "$BACKUP_DIR"
cd "$REPO_ROOT"

echo -e "${GREEN}== Log export ==${NC}"

# Compose network'ünü elasticsearch container'ına bakarak tespit et
ES_CONT=$(docker ps --filter "name=elasticsearch" --format '{{.Names}}' | head -1)
[ -n "$ES_CONT" ] || { echo "Elasticsearch container bulunamadı. docker compose up -d"; exit 1; }
NET=$(docker inspect "$ES_CONT" --format '{{range $k,$v := .NetworkSettings.Networks}}{{$k}} {{end}}' | tr ' ' '\n' | grep '_elk-net$' | head -1)
[ -n "$NET" ] || { echo "$ES_CONT'ın elk-net'i yok"; exit 1; }
echo "  Network:     $NET"
echo "  ES conteiner: $ES_CONT"

DOC_COUNT=$(curl -sf -u "elastic:${ES_PASS}" "${ES_URL}/${INDEX_PATTERN}/_count" | python3 -c 'import json,sys;print(json.load(sys.stdin)["count"])' 2>/dev/null || echo 0)
echo "  Kaynak: $DOC_COUNT doküman ($INDEX_PATTERN)"

if [ "$DOC_COUNT" -eq 0 ]; then
  echo -e "${YELLOW}Uyarı: henüz log yok. log-generator.sh çalıştır, sonra tekrar dene.${NC}"
  exit 0
fi

echo -e "\n[1/2] Mapping export..."
docker run --rm --network "$NET" \
  -v "$BACKUP_DIR:/data" \
  elasticdump/elasticsearch-dump:latest \
  --input="${ES_HOST_IN_COMPOSE}/${INDEX_PATTERN}" \
  --output=/data/ecommerce-logs-mapping.json \
  --type=mapping \
  --limit=1000 2>&1 | tail -3

echo -e "\n[2/2] Data export..."
docker run --rm --network "$NET" \
  -v "$BACKUP_DIR:/data" \
  elasticdump/elasticsearch-dump:latest \
  --input="${ES_HOST_IN_COMPOSE}/${INDEX_PATTERN}" \
  --output=/data/ecommerce-logs.json \
  --type=data \
  --limit=1000 2>&1 | tail -3

echo
echo -e "${GREEN}OK.${NC}"
ls -lh "$BACKUP_DIR"/
echo
echo "EC2'ye aktarmak için:"
echo "  scp -i datapulse-key.pem -r $BACKUP_DIR ubuntu@<EC2-IP>:~/Shopping-Cart-Backend/elk/backups"
echo "  EC2'de: ./scripts/logs-import.sh"
