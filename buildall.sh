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
rustup target add aarch64-linux-android

echo "=== 2. Building host native CLI (x86_64-linux-gnu) ==="
cd rust
cargo build --bin mc2mt-cli --release
cd "$BASEDIR"

echo "=== 3. Configuring Android NDK using cargo-ndk ==="
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

echo "=== 4. Building Termux compatible CLI (aarch64-linux-android) ==="
cd rust
# 【修复点】：使用 --platform 替代 -p，避免与 Cargo 的 --package 冲突！
cargo ndk -t arm64-v8a --platform 30 build --bin mc2mt-cli --release

echo "=== Compilation finished! ==="
ls -l target/release/mc2mt-cli
ls -l target/aarch64-linux-android/release/mc2mt-cli