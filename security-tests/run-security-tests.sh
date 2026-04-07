#!/bin/bash
# ═══════════════════════════════════════════════════════════════
# DataPulse Security Test Runner
# Runs Schemathesis (API fuzzing) + OWASP ZAP (security scan)
# ═══════════════════════════════════════════════════════════════

set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "═══════════════════════════════════════════════════════"
echo "  DataPulse Security Testing Suite"
echo "═══════════════════════════════════════════════════════"
echo ""

# Ensure main stack is running
echo "1. Checking main stack..."
cd "$PROJECT_DIR"
if ! docker compose ps backend --format '{{.Status}}' 2>/dev/null | grep -q "Up"; then
  echo "   Backend is not running. Start it with: docker compose up -d"
  exit 1
fi
echo "   ✅ Backend is up"

# Create reports directory
mkdir -p "$SCRIPT_DIR/reports"

# Run security tests
echo ""
echo "2. Starting security test containers..."
cd "$SCRIPT_DIR"

echo ""
echo "─── Running Schemathesis (API Fuzzing) ───"
docker compose -f docker-compose.security.yml run --rm schemathesis 2>&1
SCHEMA_EXIT=$?

echo ""
echo "─── Running OWASP ZAP (Security Scan) ───"
docker compose -f docker-compose.security.yml run --rm zap 2>&1
ZAP_EXIT=$?

# Summary
echo ""
echo "═══════════════════════════════════════════════════════"
echo "  Results Summary"
echo "═══════════════════════════════════════════════════════"
echo ""
echo "Reports saved to: $SCRIPT_DIR/reports/"
echo ""

if [ -f "$SCRIPT_DIR/reports/schemathesis-auth.txt" ]; then
  echo "  Schemathesis (authenticated):"
  tail -5 "$SCRIPT_DIR/reports/schemathesis-auth.txt" | sed 's/^/    /'
fi

if [ -f "$SCRIPT_DIR/reports/zap-api-scan.txt" ]; then
  echo ""
  echo "  OWASP ZAP (API scan):"
  grep -E "^PASS|^WARN|^FAIL|^INFO" "$SCRIPT_DIR/reports/zap-api-scan.txt" | tail -10 | sed 's/^/    /'
fi

echo ""
echo "  HTML Reports:"
echo "    - $SCRIPT_DIR/reports/zap-baseline.html"
echo "    - $SCRIPT_DIR/reports/zap-api-scan.html"
echo ""
echo "  XML Reports (CI/JUnit):"
echo "    - $SCRIPT_DIR/reports/schemathesis-unauth.xml"
echo "    - $SCRIPT_DIR/reports/schemathesis-auth.xml"
echo ""

if [ $SCHEMA_EXIT -ne 0 ] || [ $ZAP_EXIT -ne 0 ]; then
  echo "⚠️  Some tests reported findings. Check reports above."
  exit 1
fi

echo "✅ All security tests passed."
