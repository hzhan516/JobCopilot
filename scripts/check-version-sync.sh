#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
VERSION="$(tr -d '\r\n' < "${ROOT_DIR}/VERSION")"

fail() {
  echo "Version drift: $1" >&2
  exit 1
}

FRONTEND_VERSION="$(cd "${ROOT_DIR}/frontend" && node -p "require('./package.json').version")"
LOCK_VERSION="$(cd "${ROOT_DIR}/frontend" && node -p "require('./package-lock.json').version")"
LOCK_PACKAGE_VERSION="$(cd "${ROOT_DIR}/frontend" && node -p "require('./package-lock.json').packages[''].version")"

[[ "${FRONTEND_VERSION}" == "${VERSION}" ]] || fail "frontend/package.json"
[[ "${LOCK_VERSION}" == "${VERSION}" ]] || fail "frontend/package-lock.json root"
[[ "${LOCK_PACKAGE_VERSION}" == "${VERSION}" ]] || fail "frontend/package-lock.json package"
grep -q "__version__ = \"${VERSION}\"" "${ROOT_DIR}/ai-service/app/__version__.py" || fail "ai-service"
grep -q "version: ${VERSION}" "${ROOT_DIR}/docs/deployment/k8s/helm/jobcopilot/Chart.yaml" || fail "Helm chart version"
grep -q "appVersion: \"${VERSION}\"" "${ROOT_DIR}/docs/deployment/k8s/helm/jobcopilot/Chart.yaml" || fail "Helm appVersion"

while IFS= read -r pom; do
  grep -q "<version>${VERSION}</version>" "${pom}" || fail "${pom}"
done < <(find "${ROOT_DIR}/backend" -maxdepth 2 -name pom.xml -print)

COUNT="$(grep -c "APP_VERSION:${VERSION}" "${ROOT_DIR}/backend/app/src/main/resources/application.yml")"
[[ "${COUNT}" == "2" ]] || fail "backend application fallback"

echo "Version sources are aligned at ${VERSION}"
