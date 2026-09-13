#!/usr/bin/env bash
# Run script (Linux / macOS). Requires JDK 11+ on the PATH.
set -euo pipefail

cd "$(dirname "$0")"

if ! command -v javac >/dev/null 2>&1; then
    echo "[ERROR] javac not found. Install a JDK 11+ (e.g. 'brew install openjdk' or 'apt install default-jdk')." >&2
    exit 1
fi

mkdir -p build
echo "Compiling..."
javac -encoding UTF-8 -d build SnakeGame.java

echo "Starting game..."
java -cp build SnakeGame
