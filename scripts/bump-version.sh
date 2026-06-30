#!/usr/bin/env bash
# 用法: ./scripts/bump-version.sh <new-version>
# 示例: ./scripts/bump-version.sh 0.2.0
set -euo pipefail

NEW_VERSION="$1"
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo "Bumping version to ${NEW_VERSION}..."

# 1. 更新 VERSION 文件
echo "${NEW_VERSION}" > "${ROOT_DIR}/VERSION"

# 2. 更新 backend pom.xml（父 POM + 子模块）
cd "${ROOT_DIR}/backend"
mvn versions:set -DnewVersion="${NEW_VERSION}" -DgenerateBackupPoms=false

# 3. 更新 frontend package.json
cd "${ROOT_DIR}/frontend"
npm version "${NEW_VERSION}" --no-git-tag-version

# 4. 更新 ai-service（写入 __version__.py）
echo "__version__ = \"${NEW_VERSION}\"" > "${ROOT_DIR}/ai-service/app/__version__.py"

echo "✅ Version bumped to ${NEW_VERSION} across all components"
echo ""
echo "Next steps:"
echo "  1. Update CHANGELOG.md with the new version's changes"
echo "  2. git add -A && git commit -m 'chore: bump version to v${NEW_VERSION}'"
echo "  3. git tag v${NEW_VERSION}"
echo "  4. git push origin main --tags"
