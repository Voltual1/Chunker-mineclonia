#!/bin/bash
set -e

BASEDIR=$(dirname "$0")
cd "$BASEDIR"
BASEDIR=$(pwd)

echo "=== 1. Building host native CLI (x86_64-linux-gnu) ==="
cd rust
cargo build --bin mc2mt-cli --release
cd "$BASEDIR"

echo "=== 2. Configuring Android NDK Toolchain ==="
# 运行我们之前写好的 NDK 准备脚本
chmod +x rust-build/pre_build_mc2mt_android.sh
chmod +x rust-build/setenv-android.sh
./rust-build/pre_build_mc2mt_android.sh

# 导入 NDK 环境变量
export ANDROID_NDK="$BASEDIR/android-ndk-r26d"
. ./rust-build/setenv-android.sh

# 在 rust 目录下临时配置 NDK 链接器
cd rust
mkdir -p .cargo
cat > .cargo/config.toml << EOF
[target.aarch64-linux-android]
linker = "${ANDROID_TOOLCHAIN_DIR}/bin/aarch64-linux-android30-clang"

[target.armv7-linux-androideabi]
linker = "${ANDROID_TOOLCHAIN_DIR}/bin/armv7a-linux-androideabi30-clang"
EOF

echo "=== 3. Building Termux compatible CLI (aarch64-linux-android) ==="
# 使用 aarch64-linux-android 目标进行编译
$BASEDIR/tools/.cargo/bin/rustup target add aarch64-linux-android
$BASEDIR/tools/.cargo/bin/cargo build --bin mc2mt-cli --target aarch64-linux-android --release

echo "=== Compilation finished! ==="
ls -l target/release/mc2mt-cli
ls -l target/aarch64-linux-android/release/mc2mt-cli