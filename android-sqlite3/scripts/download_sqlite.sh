#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=/dev/null
source "$ROOT_DIR/sqlite-version.env"

DOWNLOAD_DIR="$ROOT_DIR/downloads"
BUILD_DIR="$ROOT_DIR/build"

mkdir -p "$DOWNLOAD_DIR" "$BUILD_DIR"

# ==================================================
# 1. 下载并校验 SQLite Amalgamation 包
# ==================================================
ZIP_NAME="sqlite-amalgamation-${SQLITE_VERSION_NUMBER}.zip"
URL="https://www.sqlite.org/${SQLITE_YEAR}/${ZIP_NAME}"
ZIP_PATH="$DOWNLOAD_DIR/$ZIP_NAME"

if [[ ! -f "$ZIP_PATH" ]]; then
  echo "Downloading SQLite Amalgamation..."
  curl -L --fail --show-error -o "$ZIP_PATH" "$URL"
fi

echo "Verifying SHA3-256 for $ZIP_NAME..."
ACTUAL_SHA3="$(openssl dgst -sha3-256 "$ZIP_PATH" | awk '{print $NF}')"
if [[ "$ACTUAL_SHA3" != "$SQLITE_AMALGAMATION_SHA3_256" ]]; then
  echo "SHA3 mismatch for $ZIP_NAME" >&2
  echo "expected: $SQLITE_AMALGAMATION_SHA3_256" >&2
  echo "actual:   $ACTUAL_SHA3" >&2
  exit 1
fi

echo "Extracting Amalgamation..."
unzip -q -o "$ZIP_PATH" -d "$BUILD_DIR"

# ==================================================
# 2. 下载并校验 SQLite 完整源码包 (用于 recover 扩展)
# ==================================================
SRC_ZIP_NAME="sqlite-src-${SQLITE_VERSION_NUMBER}.zip"
SRC_URL="https://www.sqlite.org/${SQLITE_YEAR}/${SRC_ZIP_NAME}"
SRC_ZIP_PATH="$DOWNLOAD_DIR/$SRC_ZIP_NAME"

if [[ ! -f "$SRC_ZIP_PATH" ]]; then
  echo "Downloading SQLite Complete Source..."
  curl -L --fail --show-error -o "$SRC_ZIP_PATH" "$SRC_URL"
fi

echo "Verifying SHA3-256 for $SRC_ZIP_NAME..."
ACTUAL_SRC_SHA3="$(openssl dgst -sha3-256 "$SRC_ZIP_PATH" | awk '{print $NF}')"
if [[ "$ACTUAL_SRC_SHA3" != "$SQLITE_SRC_SHA3_256" ]]; then
  echo "SHA3 mismatch for $SRC_ZIP_NAME" >&2
  echo "expected: $SQLITE_SRC_SHA3_256" >&2
  echo "actual:   $ACTUAL_SRC_SHA3" >&2
  exit 1
fi

echo "Extracting Complete Source..."
unzip -q -o "$SRC_ZIP_PATH" -d "$BUILD_DIR"

echo "--------------------------------------------------"
echo "SQLite resources are ready."
echo "Amalgamation path: $BUILD_DIR/sqlite-amalgamation-${SQLITE_VERSION_NUMBER}"
echo "Source code path:  $BUILD_DIR/sqlite-src-${SQLITE_VERSION_NUMBER}"
echo "--------------------------------------------------"