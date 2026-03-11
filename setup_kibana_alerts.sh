#!/bin/bash
KIBANA="http://localhost:5601"
H=('-H' 'kbn-xsrf: true' '-H' 'Content-Type: application/json')

echo "⏳ Kibana hazır olana kadar bekleniyor..."
until curl -s "$KIBANA/api/status" | python3 -c "import sys,json; s=json.load(sys.stdin); exit(0 if s.get('status',{}).get('overall',{}).get('level')=='available' else 1)" 2>/dev/null; do
  echo -n "."; sleep 3
done
echo -e "\n✅ Kibana hazır\n"

# ─── 1. Email connector (log-only) ───────────────────────────────────────────
echo "🔌 Connector oluşturuluyor (server log)..."
CONNECTOR_ID=$(curl -s -X POST "$KIBANA/api/actions/connector" "${H[@]}" -d '{
  "name": "Ecommerce Log Connector",
  "connector_type_id": ".server-log",
  "config": {}
}' | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('id',''))")
echo "  Connector ID: $CONNECTOR_ID"

# ─── 2. AUTH_FAILED Spike Alert ──────────────────────────────────────────────
echo "🚨 AUTH_FAILED spike alert oluşturuluyor..."
curl -s -X POST "$KIBANA/api/alerting/rule" "${H[@]}" -d "{
  \"name\": \"AUTH_FAILED Spike - Brute Force Detection\",
  \"rule_type_id\": \".es-query\",
  \"consumer\": \"alerts\",
  \"schedule\": {\"interval\": \"1m\"},
  \"params\": {
    \"size\": 100,
    \"timeWindowSize\": 5,
    \"timeWindowUnit\": \"m\",
    \"threshold\": [5],
    \"thresholdComparator\": \">=\",
    \"index\": [\"ecommerce-logs-*\"],
    \"timeField\": \"@timestamp\",
    \"esQuery\": \"{\\\"query\\\":{\\\"bool\\\":{\\\"must\\\":[{\\\"term\\\":{\\\"event_type.keyword\\\":\\\"AUTH_FAILED\\\"}}]}}}\",
    \"excludeHitsFromPreviousRun\": true
  },
  \"actions\": [
    {
      \"id\": \"$CONNECTOR_ID\",
      \"group\": \"query matched\",
      \"params\": {
        \"level\": \"warn\",
        \"message\": \"⚠️ AUTH_FAILED spike detected! {{context.hits}} failed login attempts in the last 5 minutes.\"
      }
    }
  ]
}" | python3 -c "import sys,json; d=json.load(sys.stdin); print('  ✅ Alert:', d.get('name',''), '| ID:', d.get('id','ERROR: '+str(d))[:40])"

# ─── 3. High Response Time Alert ─────────────────────────────────────────────
echo "⏱️  High response time alert oluşturuluyor..."
curl -s -X POST "$KIBANA/api/alerting/rule" "${H[@]}" -d "{
  \"name\": \"High API Response Time (>500ms)\",
  \"rule_type_id\": \".es-query\",
  \"consumer\": \"alerts\",
  \"schedule\": {\"interval\": \"2m\"},
  \"params\": {
    \"size\": 100,
    \"timeWindowSize\": 5,
    \"timeWindowUnit\": \"m\",
    \"threshold\": [3],
    \"thresholdComparator\": \">=\",
    \"index\": [\"ecommerce-logs-*\"],
    \"timeField\": \"@timestamp\",
    \"esQuery\": \"{\\\"query\\\":{\\\"bool\\\":{\\\"must\\\":[{\\\"term\\\":{\\\"event_type.keyword\\\":\\\"API_REQUEST\\\"}},{\\\"range\\\":{\\\"response_time_ms\\\":{\\\"gte\\\":500}}}]}}}\",
    \"excludeHitsFromPreviousRun\": true
  },
  \"actions\": [
    {
      \"id\": \"$CONNECTOR_ID\",
      \"group\": \"query matched\",
      \"params\": {
        \"level\": \"warn\",
        \"message\": \"⏱️ Slow API responses detected! {{context.hits}} requests exceeded 500ms in the last 5 minutes.\"
      }
    }
  ]
}" | python3 -c "import sys,json; d=json.load(sys.stdin); print('  ✅ Alert:', d.get('name',''), '| ID:', d.get('id','ERROR')[:40])"

# ─── 4. New Order Spike Alert ────────────────────────────────────────────────
echo "📦 Order spike alert oluşturuluyor..."
curl -s -X POST "$KIBANA/api/alerting/rule" "${H[@]}" -d "{
  \"name\": \"Order Spike Detection\",
  \"rule_type_id\": \".es-query\",
  \"consumer\": \"alerts\",
  \"schedule\": {\"interval\": \"1m\"},
  \"params\": {
    \"size\": 100,
    \"timeWindowSize\": 1,
    \"timeWindowUnit\": \"m\",
    \"threshold\": [10],
    \"thresholdComparator\": \">=\",
    \"index\": [\"ecommerce-logs-*\"],
    \"timeField\": \"@timestamp\",
    \"esQuery\": \"{\\\"query\\\":{\\\"bool\\\":{\\\"must\\\":[{\\\"term\\\":{\\\"event_type.keyword\\\":\\\"ORDER_PLACED\\\"}}]}}}\",
    \"excludeHitsFromPreviousRun\": true
  },
  \"actions\": [
    {
      \"id\": \"$CONNECTOR_ID\",
      \"group\": \"query matched\",
      \"params\": {
        \"level\": \"info\",
        \"message\": \"📦 Order spike! {{context.hits}} orders placed in the last minute.\"
      }
    }
  ]
}" | python3 -c "import sys,json; d=json.load(sys.stdin); print('  ✅ Alert:', d.get('name',''), '| ID:', d.get('id','ERROR')[:40])"

echo ""
echo "🎉 Alert kurulumu tamamlandı!"
echo "🔗 Alerts: $KIBANA/app/management/insightsAndAlerting/triggersActions/rules"
