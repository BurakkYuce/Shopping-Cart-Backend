#!/bin/bash
KIBANA="http://localhost:5601"
ELASTIC_PASSWORD="${ELASTIC_PASSWORD:-elastic}"
AUTH=('-u' "elastic:$ELASTIC_PASSWORD")

echo "⏳ Kibana hazır olana kadar bekleniyor..."
until curl -s "${AUTH[@]}" "$KIBANA/api/status" | python3 -c "import sys,json; s=json.load(sys.stdin); exit(0 if s.get('status',{}).get('overall',{}).get('level')=='available' else 1)" 2>/dev/null; do
  echo -n "."
  sleep 3
done
echo -e "\n✅ Kibana hazır\n"

HEADERS=('-H' 'kbn-xsrf: true' '-H' 'Content-Type: application/json')

# ─── 1. Data View ────────────────────────────────────────────────────────────
echo "📌 Data view oluşturuluyor..."
curl -s "${AUTH[@]}" -X POST "$KIBANA/api/data_views/data_view" "${HEADERS[@]}" -d '{
  "data_view": {
    "id": "ecommerce-logs",
    "title": "ecommerce-logs-*",
    "timeFieldName": "@timestamp",
    "name": "E-Commerce Logs"
  }
}' | python3 -c "import sys,json; d=json.load(sys.stdin); print('  ✅ Data view:', d.get('data_view',{}).get('id','already exists'))" 2>/dev/null

# ─── 2. Saved Objects (Visualizations + Dashboard) ───────────────────────────
echo "📊 Visualization'lar ve dashboard oluşturuluyor..."

curl -s "${AUTH[@]}" -X POST "$KIBANA/api/saved_objects/_bulk_create?overwrite=true" "${HEADERS[@]}" -d '[
  {
    "type": "visualization",
    "id": "event-type-donut",
    "attributes": {
      "title": "Event Type Dağılımı",
      "visState": "{\"title\":\"Event Type Dağılımı\",\"type\":\"pie\",\"aggs\":[{\"id\":\"1\",\"enabled\":true,\"type\":\"count\",\"schema\":\"metric\",\"params\":{}},{\"id\":\"2\",\"enabled\":true,\"type\":\"terms\",\"schema\":\"segment\",\"params\":{\"field\":\"event_type.keyword\",\"size\":12,\"order\":\"desc\",\"orderBy\":\"1\"}}],\"params\":{\"addLegend\":true,\"addTooltip\":true,\"isDonut\":true,\"legendPosition\":\"right\",\"type\":\"pie\"}}",
      "uiStateJSON": "{}",
      "description": "",
      "kibanaSavedObjectMeta": {
        "searchSourceJSON": "{\"index\":\"ecommerce-logs\",\"query\":{\"query\":\"\",\"language\":\"kuery\"},\"filter\":[]}"
      }
    }
  },
  {
    "type": "visualization",
    "id": "requests-over-time",
    "attributes": {
      "title": "API İstekleri (Zaman)",
      "visState": "{\"title\":\"API İstekleri (Zaman)\",\"type\":\"histogram\",\"aggs\":[{\"id\":\"1\",\"enabled\":true,\"type\":\"count\",\"schema\":\"metric\",\"params\":{}},{\"id\":\"2\",\"enabled\":true,\"type\":\"date_histogram\",\"schema\":\"segment\",\"params\":{\"field\":\"@timestamp\",\"interval\":\"auto\",\"min_doc_count\":1}}],\"params\":{\"addLegend\":true,\"addTimeMarker\":false,\"addTooltip\":true,\"categoryAxes\":[{\"id\":\"CategoryAxis-1\",\"labels\":{\"show\":true,\"truncate\":100},\"position\":\"bottom\",\"scale\":{\"type\":\"linear\"},\"show\":true,\"style\":{},\"title\":{},\"type\":\"category\"}],\"grid\":{\"categoryLines\":false},\"legendPosition\":\"right\",\"seriesParams\":[{\"data\":{\"id\":\"1\",\"label\":\"Count\"},\"drawLinesBetweenPoints\":true,\"mode\":\"stacked\",\"show\":\"true\",\"showCircles\":true,\"type\":\"histogram\",\"valueAxis\":\"ValueAxis-1\"}],\"times\":[],\"type\":\"histogram\",\"valueAxes\":[{\"id\":\"ValueAxis-1\",\"labels\":{\"filter\":false,\"rotate\":0,\"show\":true,\"truncate\":100},\"name\":\"LeftAxis-1\",\"position\":\"left\",\"scale\":{\"mode\":\"normal\",\"type\":\"linear\"},\"show\":true,\"style\":{},\"title\":{\"text\":\"Count\"},\"type\":\"value\"}]}}",
      "uiStateJSON": "{}",
      "description": "",
      "kibanaSavedObjectMeta": {
        "searchSourceJSON": "{\"index\":\"ecommerce-logs\",\"query\":{\"query\":\"\",\"language\":\"kuery\"},\"filter\":[]}"
      }
    }
  },
  {
    "type": "visualization",
    "id": "user-role-breakdown",
    "attributes": {
      "title": "Kullanıcı Rolü Dağılımı",
      "visState": "{\"title\":\"Kullanıcı Rolü Dağılımı\",\"type\":\"pie\",\"aggs\":[{\"id\":\"1\",\"enabled\":true,\"type\":\"count\",\"schema\":\"metric\",\"params\":{}},{\"id\":\"2\",\"enabled\":true,\"type\":\"terms\",\"schema\":\"segment\",\"params\":{\"field\":\"user_role.keyword\",\"size\":5,\"order\":\"desc\",\"orderBy\":\"1\",\"missingBucket\":true,\"missingBucketLabel\":\"anonymous\"}}],\"params\":{\"addLegend\":true,\"addTooltip\":true,\"isDonut\":false,\"legendPosition\":\"right\",\"type\":\"pie\"}}",
      "uiStateJSON": "{}",
      "description": "",
      "kibanaSavedObjectMeta": {
        "searchSourceJSON": "{\"index\":\"ecommerce-logs\",\"query\":{\"query\":\"\",\"language\":\"kuery\"},\"filter\":[]}"
      }
    }
  },
  {
    "type": "visualization",
    "id": "auth-failures",
    "attributes": {
      "title": "Auth Hataları",
      "visState": "{\"title\":\"Auth Hataları\",\"type\":\"metric\",\"aggs\":[{\"id\":\"1\",\"enabled\":true,\"type\":\"count\",\"schema\":\"metric\",\"params\":{}}],\"params\":{\"addLegend\":false,\"addTooltip\":true,\"metric\":{\"colorSchema\":\"Green to Red\",\"colorsRange\":[{\"from\":0,\"to\":10},{\"from\":10,\"to\":50},{\"from\":50,\"to\":1000}],\"invertColors\":false,\"labels\":{\"show\":true},\"metricColorMode\":\"Labels\",\"style\":{\"bgColor\":false,\"bgFill\":\"#000\",\"fontSize\":60,\"labelColor\":false,\"subText\":\"\"},\"useRanges\":true},\"type\":\"metric\"}}",
      "uiStateJSON": "{}",
      "description": "",
      "kibanaSavedObjectMeta": {
        "searchSourceJSON": "{\"index\":\"ecommerce-logs\",\"query\":{\"query\":\"event_type: AUTH_FAILED\",\"language\":\"kuery\"},\"filter\":[]}"
      }
    }
  },
  {
    "type": "visualization",
    "id": "orders-placed",
    "attributes": {
      "title": "Verilen Sipariş Sayısı",
      "visState": "{\"title\":\"Verilen Sipariş Sayısı\",\"type\":\"metric\",\"aggs\":[{\"id\":\"1\",\"enabled\":true,\"type\":\"count\",\"schema\":\"metric\",\"params\":{}}],\"params\":{\"addLegend\":false,\"addTooltip\":true,\"metric\":{\"colorSchema\":\"Green to Red\",\"colorsRange\":[{\"from\":0,\"to\":100}],\"invertColors\":false,\"labels\":{\"show\":true},\"metricColorMode\":\"None\",\"style\":{\"bgColor\":false,\"bgFill\":\"#000\",\"fontSize\":60,\"labelColor\":false,\"subText\":\"\"},\"useRanges\":false},\"type\":\"metric\"}}",
      "uiStateJSON": "{}",
      "description": "",
      "kibanaSavedObjectMeta": {
        "searchSourceJSON": "{\"index\":\"ecommerce-logs\",\"query\":{\"query\":\"event_type: ORDER_PLACED\",\"language\":\"kuery\"},\"filter\":[]}"
      }
    }
  },
  {
    "type": "visualization",
    "id": "response-time-avg",
    "attributes": {
      "title": "Ortalama Response Time (ms)",
      "visState": "{\"title\":\"Ortalama Response Time (ms)\",\"type\":\"histogram\",\"aggs\":[{\"id\":\"1\",\"enabled\":true,\"type\":\"avg\",\"schema\":\"metric\",\"params\":{\"field\":\"request.response_time_ms\"}},{\"id\":\"2\",\"enabled\":true,\"type\":\"date_histogram\",\"schema\":\"segment\",\"params\":{\"field\":\"@timestamp\",\"interval\":\"auto\",\"min_doc_count\":1}}],\"params\":{\"addLegend\":true,\"addTooltip\":true,\"categoryAxes\":[{\"id\":\"CategoryAxis-1\",\"labels\":{\"show\":true},\"position\":\"bottom\",\"scale\":{\"type\":\"linear\"},\"show\":true,\"title\":{},\"type\":\"category\"}],\"legendPosition\":\"right\",\"seriesParams\":[{\"data\":{\"id\":\"1\",\"label\":\"Average response_time_ms\"},\"drawLinesBetweenPoints\":true,\"mode\":\"normal\",\"show\":true,\"showCircles\":true,\"type\":\"line\",\"valueAxis\":\"ValueAxis-1\"}],\"times\":[],\"type\":\"histogram\",\"valueAxes\":[{\"id\":\"ValueAxis-1\",\"labels\":{\"filter\":false,\"rotate\":0,\"show\":true},\"name\":\"LeftAxis-1\",\"position\":\"left\",\"scale\":{\"mode\":\"normal\",\"type\":\"linear\"},\"show\":true,\"title\":{\"text\":\"Average response_time_ms\"},\"type\":\"value\"}]}}",
      "uiStateJSON": "{}",
      "description": "",
      "kibanaSavedObjectMeta": {
        "searchSourceJSON": "{\"index\":\"ecommerce-logs\",\"query\":{\"query\":\"event_type: API_REQUEST\",\"language\":\"kuery\"},\"filter\":[]}"
      }
    }
  },
  {
    "type": "dashboard",
    "id": "ecommerce-main-dashboard",
    "attributes": {
      "title": "E-Commerce Analytics Dashboard",
      "hits": 0,
      "description": "Real-time e-commerce platform monitoring",
      "panelsJSON": "[{\"version\":\"8.18.8\",\"type\":\"visualization\",\"gridData\":{\"x\":0,\"y\":0,\"w\":24,\"h\":15,\"i\":\"1\"},\"panelIndex\":\"1\",\"embeddableConfig\":{},\"panelRefName\":\"panel_1\"},{\"version\":\"8.18.8\",\"type\":\"visualization\",\"gridData\":{\"x\":24,\"y\":0,\"w\":24,\"h\":15,\"i\":\"2\"},\"panelIndex\":\"2\",\"embeddableConfig\":{},\"panelRefName\":\"panel_2\"},{\"version\":\"8.18.8\",\"type\":\"visualization\",\"gridData\":{\"x\":0,\"y\":15,\"w\":16,\"h\":10,\"i\":\"3\"},\"panelIndex\":\"3\",\"embeddableConfig\":{},\"panelRefName\":\"panel_3\"},{\"version\":\"8.18.8\",\"type\":\"visualization\",\"gridData\":{\"x\":16,\"y\":15,\"w\":16,\"h\":10,\"i\":\"4\"},\"panelIndex\":\"4\",\"embeddableConfig\":{},\"panelRefName\":\"panel_4\"},{\"version\":\"8.18.8\",\"type\":\"visualization\",\"gridData\":{\"x\":32,\"y\":15,\"w\":16,\"h\":10,\"i\":\"5\"},\"panelIndex\":\"5\",\"embeddableConfig\":{},\"panelRefName\":\"panel_5\"},{\"version\":\"8.18.8\",\"type\":\"visualization\",\"gridData\":{\"x\":0,\"y\":25,\"w\":48,\"h\":12,\"i\":\"6\"},\"panelIndex\":\"6\",\"embeddableConfig\":{},\"panelRefName\":\"panel_6\"}]",
      "optionsJSON": "{\"hidePanelTitles\":false,\"useMargins\":true}",
      "timeRestore": false,
      "kibanaSavedObjectMeta": {
        "searchSourceJSON": "{\"query\":{\"query\":\"\",\"language\":\"kuery\"},\"filter\":[]}"
      }
    },
    "references": [
      {"name": "panel_1", "type": "visualization", "id": "requests-over-time"},
      {"name": "panel_2", "type": "visualization", "id": "event-type-donut"},
      {"name": "panel_3", "type": "visualization", "id": "user-role-breakdown"},
      {"name": "panel_4", "type": "visualization", "id": "auth-failures"},
      {"name": "panel_5", "type": "visualization", "id": "orders-placed"},
      {"name": "panel_6", "type": "visualization", "id": "response-time-avg"}
    ]
  }
]' | python3 -c "
import sys, json
results = json.load(sys.stdin)
for item in results.get('saved_objects', []):
    status = '✅' if not item.get('error') else '❌'
    print(f'  {status} {item[\"type\"]}: {item[\"attributes\"][\"title\"]}')
" 2>/dev/null

echo ""
echo "🎉 Kurulum tamamlandı!"
echo ""
echo "🔗 Dashboard: http://localhost:5601/app/dashboards#/view/ecommerce-main-dashboard"
echo "🔗 Discover:  http://localhost:5601/app/discover"
