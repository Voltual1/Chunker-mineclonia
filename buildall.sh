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
# 运行 NDK 准备脚本
chmod +x rust-build/pre_build_mc2mt_android.sh
chmod +x rust-build/setenv-android.sh
./rust-build/pre_build_mc2mt_android.sh

# 导入 NDK 环境变量
export ANDROID_NDK="$BASEDIR/android-ndk-r26d"
. ./rust-build/setenv-android.sh

# 在 rust 目录下临时配置 NDK 链接器（修正为无 API 版本号的 clang 路径）
cd rust
mkdir -p .cargo
cat > .cargo/config.toml << EOF
[target.aarch64-linux-android]
linker = "${ANDROID_TOOLCHAIN_DIR}/bin/aarch64-linux-android-clang"

[target.armv7-linux-androideabi]
linker = "${ANDROID_TOOLCHAIN_DIR}/bin/armv7a-linux-androideabi-clang"
EOF

echo "=== 3. Exporting C Cross-Compiler Envs for Sqlite3 ==="
# 显式导出编译器和归档器（修正为无 API 版本号的 clang 路径）
export CC_aarch64_linux_android="${ANDROID_TOOLCHAIN_DIR}/bin/aarch64-linux-android-clang"
export AR_aarch64_linux_android="${ANDROID_TOOLCHAIN_DIR}/bin/llvm-ar"
export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="${ANDROID_TOOLCHAIN_DIR}/bin/aarch64-linux-android-clang"

export CC_armv7_linux_androideabi="${ANDROID_TOOLCHAIN_DIR}/bin/armv7a-linux-androideabi-clang"
export AR_armv7_linux_androideabi="${ANDROID_TOOLCHAIN_DIR}/bin/llvm-ar"
export CARGO_TARGET_ARMV7_LINUX_ANDROIDEABI_LINKER="${ANDROID_TOOLCHAIN_DIR}/bin/armv7a-linux-androideabi-clang"

echo "=== 4. Building Termux compatible CLI (aarch64-linux-android) ==="
# 使用 aarch64-linux-android 目标进行编译
$BASEDIR/tools/.cargo/bin/rustup target add aarch64-linux-android
$BASEDIR/tools/.cargo/bin/cargo build --bin mc2mt-cli --target aarch64-linux-android --release

echo "=== Compilation finished! ==="
ls -l target/release/mc2mt-cli
ls -l target/aarch64-linux-android/release/mc2mt-cli