#!/usr/bin/env bash
# ============================================================
# Continuous Log Generator — arka planda calisir, surekli log uretir
# Kullanim: ./scripts/log-generator.sh &
# Durdurmak: kill %1  veya  pkill -f log-generator
# ============================================================
set -uo pipefail

API="http://localhost:8080/api"
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}[Log Generator] Baslatiliyor...${NC}"

# ── Login ve token al ─────────────────────────────────────────
login() {
  local email=$1 pass=$2
  curl -sf "${API}/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"${email}\",\"password\":\"${pass}\"}" \
    | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null
}

ADMIN_TOKEN=$(login "admin@datapulse.com" "changeme")
CORP_TOKEN=$(login "corporate@datapulse.com" "changeme")
INDV_TOKEN=$(login "individual@datapulse.com" "changeme")

if [ -z "$ADMIN_TOKEN" ]; then
  echo -e "${RED}[Log Generator] Backend'e baglanilamadi! Once backend'i baslat.${NC}"
  exit 1
fi

echo -e "${GREEN}[Log Generator] 3 kullanici login oldu. Dongu basliyor...${NC}"
echo -e "${YELLOW}[Log Generator] Durdurmak icin: Ctrl+C veya kill %1${NC}"
echo ""

# ── Urun ID'lerini al ────────────────────────────────────────
PRODUCT_IDS=$(curl -sf "${API}/products?size=50" \
  | python3 -c "
import sys, json
data = json.load(sys.stdin)
ids = [p['id'] for p in data.get('content', [])]
print(' '.join(ids))
" 2>/dev/null)

IDS_ARR=($PRODUCT_IDS)
TOTAL_PRODUCTS=${#IDS_ARR[@]}

if [ "$TOTAL_PRODUCTS" -eq 0 ]; then
  echo -e "${RED}[Log Generator] Urun bulunamadi!${NC}"
  exit 1
fi

echo -e "${CYAN}[Log Generator] ${TOTAL_PRODUCTS} urun bulundu. Her 2-5 saniyede istek atiliyor...${NC}"
echo ""

COUNT=0

# ── Sonsuz dongu ──────────────────────────────────────────────
while true; do
  COUNT=$((COUNT + 1))
  ACTION=$((RANDOM % 10))
  RAND_IDX=$((RANDOM % TOTAL_PRODUCTS))
  PID=${IDS_ARR[$RAND_IDX]}
  TIMESTAMP=$(date '+%H:%M:%S')

  case $ACTION in
    0|1|2)
      # Urun goruntuleme (en sik)
      curl -sf "${API}/products/${PID}" > /dev/null 2>&1
      echo -e "${CYAN}[${TIMESTAMP}] #${COUNT} PRODUCT_VIEWED  pid=${PID:0:8}...${NC}"
      ;;
    3|4)
      # Urun listesi / arama
      QUERIES=("phone" "laptop" "shirt" "watch" "camera" "shoe" "bag" "headphone")
      Q=${QUERIES[$((RANDOM % ${#QUERIES[@]}))]}
      curl -sf "${API}/products?q=${Q}&size=5" > /dev/null 2>&1
      echo -e "${CYAN}[${TIMESTAMP}] #${COUNT} PRODUCT_SEARCH  q=${Q}${NC}"
      ;;
    5)
      # Login (farkli kullanicilar)
      USERS=("admin@datapulse.com" "corporate@datapulse.com" "individual@datapulse.com")
      U=${USERS[$((RANDOM % 3))]}
      login "$U" "changeme" > /dev/null 2>&1
      echo -e "${GREEN}[${TIMESTAMP}] #${COUNT} USER_LOGIN      user=${U}${NC}"
      ;;
    6)
      # Basarisiz login (AUTH_FAILED event)
      curl -sf "${API}/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"email":"hacker@test.com","password":"wrongpass"}' > /dev/null 2>&1
      echo -e "${RED}[${TIMESTAMP}] #${COUNT} AUTH_FAILED     user=hacker@test.com${NC}"
      ;;
    7)
      # Sepete ekle
      curl -sf "${API}/cart" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer ${INDV_TOKEN}" \
        -d "{\"productId\":\"${PID}\",\"quantity\":1}" > /dev/null 2>&1
      echo -e "${YELLOW}[${TIMESTAMP}] #${COUNT} CART_ADD        pid=${PID:0:8}...${NC}"
      ;;
    8)
      # Siparis listesi
      curl -sf "${API}/orders?size=3" \
        -H "Authorization: Bearer ${INDV_TOKEN}" > /dev/null 2>&1
      echo -e "${CYAN}[${TIMESTAMP}] #${COUNT} ORDER_LIST      user=individual${NC}"
      ;;
    9)
      # Analytics (admin)
      ENDPOINTS=("sales-summary" "customer" "platform-overview")
      EP=${ENDPOINTS[$((RANDOM % ${#ENDPOINTS[@]}))]}
      curl -sf "${API}/analytics/${EP}" \
        -H "Authorization: Bearer ${ADMIN_TOKEN}" > /dev/null 2>&1
      echo -e "${CYAN}[${TIMESTAMP}] #${COUNT} ANALYTICS       endpoint=${EP}${NC}"
      ;;
  esac

  # 2-5 saniye arasi rastgele bekle
  WAIT=$((RANDOM % 4 + 2))
  sleep $WAIT
done
