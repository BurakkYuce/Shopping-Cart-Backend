#!/bin/bash
BASE="http://localhost:8080"

echo "=== Fetching tokens ==="

# Admin login
ADMIN_TOKEN=$(curl -s -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@datapulse.com","password":"hashed_pw_123"}' \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('accessToken',''))")

# Pick a real individual user from the DB (first one)
IND_EMAIL=$(curl -s "$BASE/api/users" -H "Authorization: Bearer $ADMIN_TOKEN" \
  | python3 -c "import sys,json; users=json.load(sys.stdin); ind=[u for u in users if u['roleType']=='INDIVIDUAL']; print(ind[0]['email'] if ind else '')")

IND_TOKEN=$(curl -s -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$IND_EMAIL\",\"password\":\"hashed_pw_123\"}" \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('accessToken',''))")

# Register a fresh corporate user
CORP_EMAIL="corp_$(date +%s)@test.com"
CORP_TOKEN=$(curl -s -X POST "$BASE/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$CORP_EMAIL\",\"password\":\"Pass1234!\",\"roleType\":\"CORPORATE\"}" \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('accessToken',''))")

echo "Admin:      ${ADMIN_TOKEN:0:20}..."
echo "Individual: ${IND_TOKEN:0:20}..."
echo "Corporate:  ${CORP_TOKEN:0:20}..."

# Grab some product IDs
PRODUCT_IDS=$(curl -s "$BASE/api/products" \
  | python3 -c "import sys,json; ps=json.load(sys.stdin); print('\n'.join([p['id'] for p in ps[:20]]))")
P1=$(echo "$PRODUCT_IDS" | sed -n '1p')
P2=$(echo "$PRODUCT_IDS" | sed -n '2p')
P3=$(echo "$PRODUCT_IDS" | sed -n '3p')

# Grab a store ID
STORE_ID=$(curl -s "$BASE/api/stores" -H "Authorization: Bearer $ADMIN_TOKEN" \
  | python3 -c "import sys,json; ss=json.load(sys.stdin); print(ss[0]['id'] if ss else '')")

echo ""
echo "=== Generating traffic (Ctrl+C to stop) ==="

COUNT=0
while true; do
  COUNT=$((COUNT + 1))
  echo -n "[$COUNT] "

  # Public product browse
  curl -s "$BASE/api/products" > /dev/null
  echo -n "browse "

  # View specific products
  for PID in $P1 $P2 $P3; do
    curl -s "$BASE/api/products/$PID" > /dev/null
  done
  echo -n "product-views "

  # Sales analytics (admin)
  curl -s "$BASE/api/analytics/sales" -H "Authorization: Bearer $ADMIN_TOKEN" > /dev/null
  echo -n "analytics-sales "

  # Customer analytics (admin)
  curl -s "$BASE/api/analytics/customers" -H "Authorization: Bearer $ADMIN_TOKEN" > /dev/null
  echo -n "analytics-customers "

  # Product analytics
  curl -s "$BASE/api/analytics/products" -H "Authorization: Bearer $ADMIN_TOKEN" > /dev/null
  echo -n "analytics-products "

  # Individual user — view own orders
  curl -s "$BASE/api/orders" -H "Authorization: Bearer $IND_TOKEN" > /dev/null
  echo -n "orders "

  # Individual user — place an order
  if [ -n "$STORE_ID" ] && [ -n "$P1" ]; then
    ORDER_RESP=$(curl -s -X POST "$BASE/api/orders" \
      -H "Authorization: Bearer $IND_TOKEN" \
      -H "Content-Type: application/json" \
      -d "{\"storeId\":\"$STORE_ID\",\"paymentMethod\":\"card\",\"items\":[{\"productId\":\"$P1\",\"quantity\":2},{\"productId\":\"$P2\",\"quantity\":1}]}")
    ORDER_ID=$(echo "$ORDER_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('id',''))" 2>/dev/null)
    echo -n "order-placed "

    # Admin updates order status
    if [ -n "$ORDER_ID" ]; then
      curl -s -X PATCH "$BASE/api/orders/$ORDER_ID/status" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"status":"shipped"}' > /dev/null
      echo -n "status-updated "
    fi
  fi

  # Chat query
  QUESTIONS=("What are the top products?" "Show me revenue trends" "Which stores perform best?")
  Q="${QUESTIONS[$((RANDOM % 3))]}"
  curl -s -X POST "$BASE/api/chat/ask" \
    -H "Authorization: Bearer $IND_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"message\":\"$Q\"}" > /dev/null
  echo -n "chat "

  # Failed auth attempt (generates AUTH_FAILED log)
  curl -s -X POST "$BASE/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"hacker@evil.com","password":"wrongpassword"}' > /dev/null
  echo -n "auth-fail "

  # Unauthorized access attempt (no token)
  curl -s "$BASE/api/analytics/customers" > /dev/null
  echo -n "unauth "

  echo "✓"
  sleep 3
done
