#!/bin/bash
set -e

BASEDIR=$(dirname "$0")
cd "$BASEDIR"
BASEDIR=$(pwd)

# 临时将 Cargo 工具链加入 PATH
export CARGO_HOME="$BASEDIR/tools/.cargo"
export RUSTUP_HOME="$BASEDIR/tools/.rustup"
export PATH="$CARGO_HOME/bin:$PATH"

echo "=== 1. Setting up Rust Toolchain ==="
if [ ! -f "tools/rustup.sh" ]; then
    mkdir -p tools/.cargo tools/.rustup
    curl https://sh.rustup.rs -sSf > tools/rustup.sh
    chmod +x tools/rustup.sh
    sh tools/rustup.sh --no-modify-path -y
fi
rustup default stable
# 同时添加 v8a (arm64) 和 v7a (arm) 的 Target
rustup target add aarch64-linux-android
rustup target add armv7-linux-androideabi

echo "=== 2. Configuring Android NDK using cargo-ndk ==="
# 安装最新版的 cargo-ndk
cargo install cargo-ndk

# 清理可能引起冲突的旧配置
rm -f rust/.cargo/config.toml

# 确定 NDK 路径
if [ -z "$ANDROID_NDK_HOME" ]; then
    if [ -n "$ANDROID_NDK" ]; then
        export ANDROID_NDK_HOME="$ANDROID_NDK"
    elif [ -n "$ANDROID_NDK_LATEST_HOME" ]; then
        export ANDROID_NDK_HOME="$ANDROID_NDK_LATEST_HOME"
    else
        echo "Error: ANDROID_NDK_HOME is not set. Please set it to your NDK path."
        exit 1
    fi
fi
echo "Using NDK at: $ANDROID_NDK_HOME"

echo "=== 3. Building Rust JNI Shared Library ==="
cd rust

# --- 编译 arm64-v8a ---
echo "Building for arm64-v8a..."
cargo ndk -t arm64-v8a --platform 30 build --release

# --- 编译 armeabi-v7a (新增) ---
echo "Building for armeabi-v7a..."
cargo ndk -t armeabi-v7a --platform 30 build --release


echo "=== 4. Moving compiled .so to Android jniLibs ==="
# --- 拷贝 arm64-v8a ---
JNILIBS_V8A_DIR="$BASEDIR/android/src/main/jniLibs/arm64-v8a"
mkdir -p "$JNILIBS_V8A_DIR"
cp target/aarch64-linux-android/release/libmc2mt.so "$JNILIBS_V8A_DIR/"

# --- 拷贝 armeabi-v7a (新增) ---
JNILIBS_V7A_DIR="$BASEDIR/android/src/main/jniLibs/armeabi-v7a"
mkdir -p "$JNILIBS_V7A_DIR"
cp target/armv7-linux-androideabi/release/libmc2mt.so "$JNILIBS_V7A_DIR/"


echo "=== Compilation finished! ==="
echo "=== arm64-v8a: ==="
ls -l "$JNILIBS_V8A_DIR/libmc2mt.so"
echo "=== armeabi-v7a: ==="
ls -l "$JNILIBS_V7A_DIR/libmc2mt.so"