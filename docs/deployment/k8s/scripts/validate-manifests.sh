#!/bin/bash
# =============================================================================
# validate-manifests.sh — Validate Helm manifests
# validate-manifests.sh — 验证 Helm 清单
# =============================================================================
set -e

CHART_DIR="$(dirname "$0")/../helm/resume-assistant"

echo "========================================"
echo "Helm Lint / Helm 语法检查"
echo "========================================"
helm lint "$CHART_DIR"

echo ""
echo "========================================"
echo "Template Validation — Default Values"
echo "模板验证 — 默认值"
echo "========================================"
helm template resume-assistant "$CHART_DIR" > /dev/null

echo ""
echo "========================================"
echo "Template Validation — Production Values"
echo "模板验证 — 生产值"
echo "========================================"
helm template resume-assistant "$CHART_DIR" \
  -f "$CHART_DIR/values.yaml" \
  -f "$CHART_DIR/values-production.yaml" > /dev/null

echo ""
echo "========================================"
echo "Template Validation — Minimal Values"
echo "模板验证 — 最小值"
echo "========================================"
helm template resume-assistant "$CHART_DIR" \
  -f "$CHART_DIR/values.yaml" \
  -f "$CHART_DIR/values-minimal.yaml" > /dev/null

echo ""
echo "All validations passed! / 所有验证通过！"
