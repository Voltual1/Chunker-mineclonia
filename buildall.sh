#!/bin/bash
set -e

echo "=== Building host native CLI (x86_64-linux-gnu) ==="
cd rust
cargo build --bin mc2mt-cli --release

echo "=== Installing cross compilation tools for aarch64 ==="
sudo apt-get update && sudo apt-get install -y gcc-aarch64-linux-gnu

# 配置 aarch64 交叉链接器
mkdir -p .cargo
cat > .cargo/config.toml << EOF
[target.aarch64-unknown-linux-gnu]
linker = "aarch64-linux-gnu-gcc"
EOF

echo "=== Building Termux native CLI (aarch64-unknown-linux-gnu) ==="
rustup target add aarch64-unknown-linux-gnu
cargo build --bin mc2mt-cli --target aarch64-unknown-linux-gnu --release

echo "=== Compilation finished! ==="
ls -l target/release/mc2mt-cli
ls -l target/aarch64-unknown-linux-gnu/release/mc2mt-cli