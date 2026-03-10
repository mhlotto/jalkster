#!/usr/bin/env bash
# path: /Users/arr/repos/mhlotto/jalkster-git/tools/make_build_zip.sh
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${ROOT_DIR}/dist"
OUT_NAME="jalkster-build-src.zip"

mkdir -p "${OUT_DIR}"

cd "${ROOT_DIR}"

zip -r "${OUT_DIR}/${OUT_NAME}" \
  app \
  gradle \
  gradlew \
  gradlew.bat \
  build.gradle \
  settings.gradle \
  gradle.properties \
  -x "app/build/*" \
     "app/.cxx/*" \
     "app/.gradle/*" \
     "build/*" \
     "out/*" \
     ".gradle/*" \
     ".idea/*" \
     "*.iml" \
     "local.properties" \
     "dist/*"
