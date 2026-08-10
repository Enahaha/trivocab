#!/usr/bin/env bash
set -euo pipefail

# Build the macOS app bundle, DMG and ZIP for one architecture.
# Usage: package-macos.sh <arm64|x64>
ARCH="${1:?usage: package-macos.sh arm64|x64}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR="$ROOT/target/ielts-vocabulary-0.1.0-SNAPSHOT.jar"
ICON="$ROOT/release/TrVocab.icns"
VERSION="1.1.0"

case "$ARCH" in
  arm64)
    JAVA_HOME="/Users/zhouzihao/Library/Java/JavaVirtualMachines/openjdk-24/Contents/Home"
    APP_DIR="$ROOT/release/TrVocab.app"
    ;;
  x64)
    JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-26.jdk/Contents/Home"
    APP_DIR="/tmp/trivocab-x64/TrVocab.app"
    ;;
  *)
    echo "Unknown arch: $ARCH" >&2
    exit 2
    ;;
esac

export JAVA_HOME
JPACKAGE="$JAVA_HOME/bin/jpackage"

COMMON_OPTS=(
  --name TrVocab
  --app-version "$VERSION"
  --icon "$ICON"
  --input "$ROOT/target"
  --main-jar "$(basename "$JAR")"
  --main-class org.springframework.boot.loader.launch.JarLauncher
  --java-options '-Dspring.datasource.url=jdbc:h2:file:~/TrVocab/trivocab;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_ON_EXIT=FALSE'
  --java-options '-Dapp.open-browser-on-start=true'
  --java-options '-Dapp.public-url=http://localhost:8090'
  --java-options '-Dserver.port=8090'
  --java-options '-Dapp.auth.expose-reset-code=false'
  --java-options '-Dapp.allow-shutdown=true'
)

rm -rf "$APP_DIR"
"$JPACKAGE" --type app-image "${COMMON_OPTS[@]}" --dest "$(dirname "$APP_DIR")"

# Headless server app: hide Dock icon so it never bounces while waiting for a window.
PYTHON_BIN="/Users/zhouzihao/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3"
PLIST="$APP_DIR/Contents/Info.plist"
"$PYTHON_BIN" - "$PLIST" <<'PY'
import plistlib, sys
path = sys.argv[1]
with open(path, "rb") as handle:
    data = plistlib.load(handle)
data["LSUIElement"] = True
with open(path, "wb") as handle:
    plistlib.dump(data, handle)
PY

codesign --force --deep -s - "$APP_DIR" >/dev/null 2>&1 || true

"$JPACKAGE" --type dmg --app-image "$APP_DIR" --name TrVocab --app-version "$VERSION" --dest "$ROOT/release" >/dev/null
mv -f "$ROOT/release/TrVocab-$VERSION.dmg" "$ROOT/release/TrVocab-$VERSION-$ARCH.dmg"
ditto -c -k --sequesterRsrc --keepParent "$APP_DIR" "$ROOT/release/TrVocab-$VERSION-$ARCH.zip"

echo "Built release/TrVocab-$VERSION-$ARCH.dmg and .zip"
