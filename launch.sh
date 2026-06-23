#!/bin/sh
set -e

if ! command -v java >/dev/null 2>&1; then
    echo "[ОШИБКА] Java не найдена. Установите Java 21+."
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

java \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  --add-opens java.base/java.nio=ALL-UNNAMED \
  -Xmx256m \
  -jar "$SCRIPT_DIR/build/libs/voidrp-launcher-1.0.0.jar"
