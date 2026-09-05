#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=/dev/null
source "$ROOT_DIR/sqlite-version.env"

API_LEVEL="${API_LEVEL:-23}"
SOURCE_DIR="${SOURCE_DIR:-$ROOT_DIR/build/sqlite-amalgamation-${SQLITE_VERSION_NUMBER}}"
DIST_DIR="${DIST_DIR:-$ROOT_DIR/dist}"
NDK_HOME="${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-}}"

# 将其指向 build 目录中自动下载并解压的源码目录
SQLITE_SRC_DIR="${SQLITE_SRC_DIR:-$ROOT_DIR/build/sqlite-src-${SQLITE_VERSION_NUMBER}}"
RECOVER_DIR="$SQLITE_SRC_DIR/ext/recover"

if [[ -z "$NDK_HOME" ]]; then
  for candidate in \
    "$HOME/Library/Android/sdk/ndk/29.0.14206865" \
    "/Library/android/SDK/ndk/29.0.14206865" \
    "$HOME/Library/Android/sdk/ndk/27.1.12297006" \
    "/Library/android/SDK/ndk/27.1.12297006"; do
    if [[ -d "$candidate" ]]; then
      NDK_HOME="$candidate"
      break
    fi
  done
fi

if [[ -z "$NDK_HOME" || ! -d "$NDK_HOME" ]]; then
  echo "ANDROID_NDK_HOME or ANDROID_NDK_ROOT must point to an Android NDK." >&2
  exit 1
fi

case "$(uname -s)" in
  Darwin) HOST_TAG="darwin-x86_64" ;;
  Linux) HOST_TAG="linux-x86_64" ;;
  *) echo "Unsupported host: $(uname -s)" >&2; exit 1 ;;
esac

TOOLCHAIN="$NDK_HOME/toolchains/llvm/prebuilt/$HOST_TAG/bin"
if [[ ! -d "$TOOLCHAIN" ]]; then
  echo "Cannot find NDK LLVM toolchain: $TOOLCHAIN" >&2
  exit 1
fi

# 检查 Amalgamation 核心文件
if [[ ! -f "$SOURCE_DIR/sqlite3.c" || ! -f "$SOURCE_DIR/shell.c" ]]; then
  echo "SQLite amalgamation not found. Run scripts/download_sqlite.sh first." >&2
  exit 1
fi

# 检查 recover 核心扩展源文件
if [[ ! -f "$RECOVER_DIR/sqlite3recover.c" || ! -f "$RECOVER_DIR/dbdata.c" ]]; then
  echo "Error: Recover extension files not found at $RECOVER_DIR" >&2
  echo "Please verify sqlite-src-${SQLITE_VERSION_NUMBER} exists under build directory." >&2
  exit 1
fi

COMMON_CFLAGS=(
  -Os
  -DSQLITE_THREADSAFE=1
  -DSQLITE_ENABLE_DBPAGE_VTAB
  -DSQLITE_ENABLE_COLUMN_METADATA
)

build_one() {
  local abi="$1"
  local compiler="$2"
  local out_dir="$DIST_DIR/$abi"
  mkdir -p "$out_dir"

  echo "--------------------------------------------------"
  echo "Building for ABI: $abi"
  echo "--------------------------------------------------"

  # ==================================================
  # 1. 编译 sqlite3 命令行工具 (PIE Executable)
  # ==================================================
  echo "-> Compiling sqlite3 CLI executable..."
  "$TOOLCHAIN/$compiler" \
    "${COMMON_CFLAGS[@]}" \
    -fPIE \
    "$SOURCE_DIR/shell.c" \
    "$SOURCE_DIR/sqlite3.c" \
    -o "$out_dir/sqlite3" \
    -pie -ldl -lm -lz

  "$TOOLCHAIN/llvm-strip" "$out_dir/sqlite3"
  chmod 0755 "$out_dir/sqlite3"

  # ==================================================
  # 2. 编译 libsqlite3.so 共享库 (包含 Recover API)
  # ==================================================
  echo "-> Compiling libsqlite3.so (Shared Library with Recover API)..."
  "$TOOLCHAIN/$compiler" \
    "${COMMON_CFLAGS[@]}" \
    -fPIC \
    -DSQLITE_CORE \
    -I"$SOURCE_DIR" \
    -I"$RECOVER_DIR" \
    "$SOURCE_DIR/sqlite3.c" \
    "$RECOVER_DIR/dbdata.c" \
    "$RECOVER_DIR/sqlite3recover.c" \
    -o "$out_dir/libsqlite3.so" \
    -shared -ldl -lm -lz

  "$TOOLCHAIN/llvm-strip" "$out_dir/libsqlite3.so"
  chmod 0755 "$out_dir/libsqlite3.so"
}

rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"

build_one "arm64-v8a" "aarch64-linux-android${API_LEVEL}-clang"
build_one "armeabi-v7a" "armv7a-linux-androideabi${API_LEVEL}-clang"
build_one "x86_64" "x86_64-linux-android${API_LEVEL}-clang"

# 打包发布版
(
  cd "$DIST_DIR"
  zip -qr "sqlite3-android-complete-${SQLITE_VERSION}.zip" arm64-v8a armeabi-v7a x86_64
)

echo "=================================================="
echo "Build complete! Output artifacts:"
find "$DIST_DIR" -maxdepth 2 -type f -print 