#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
MAVEN_VERSION="3.9.14"
TOOLS_DIR="$ROOT/.tools"
MAVEN_DIR="$TOOLS_DIR/apache-maven-$MAVEN_VERSION"
ARCHIVE="$TOOLS_DIR/apache-maven-$MAVEN_VERSION-bin.tar.gz"
MAVEN_BIN="$MAVEN_DIR/bin/mvn"

if [[ ! -x "$MAVEN_BIN" ]]; then
  mkdir -p "$TOOLS_DIR"
  URL="https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz"
  echo "Downloading Maven $MAVEN_VERSION..."
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$URL" -o "$ARCHIVE"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ARCHIVE" "$URL"
  else
    echo "Need curl or wget to download Maven." >&2
    exit 1
  fi
  tar -xzf "$ARCHIVE" -C "$TOOLS_DIR"
fi

"$MAVEN_BIN" -q -DskipTests package

echo "Built jar: target/DonutCombatLog-3.1.2-custom.jar"
