#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")" && pwd)"
JDK_INSTALL_BASE="/tmp/jdks"
JDK_URL="https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.5%2B11/OpenJDK21U-jdk_x64_linux_hotspot_21.0.5_11.tar.gz"
JDK_DIR_NAME="jdk-21.0.5+11"

find_java_home() {
  if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
    "$JAVA_HOME/bin/java" -version >/dev/null 2>&1 || true
    echo "$JAVA_HOME"
    return
  fi

  for base in "$REPO_ROOT/.jdks" "/tmp/jdks" "$HOME/.jdks" "/usr/lib/jvm"; do
    [[ -d "$base" ]] || continue
    for candidate in "$base"/*; do
      [[ -x "$candidate/bin/java" ]] || continue
      if "$candidate/bin/java" -version 2>&1 | grep -q '21'; then
        echo "$candidate"
        return
      fi
    done
  done

  if command -v java >/dev/null 2>&1; then
    if java -version 2>&1 | grep -q '21'; then
      command -v java | xargs readlink -f | xargs dirname | xargs dirname
      return
    fi
  fi

  return 1
}

download_java() {
  local archive="$JDK_INSTALL_BASE/temurin21.tar.gz"
  local target="$JDK_INSTALL_BASE/$JDK_DIR_NAME"

  mkdir -p "$JDK_INSTALL_BASE"
  echo "Downloading Temurin JDK 21 to $JDK_INSTALL_BASE..." >&2
  if ! curl -L -o "$archive" "$JDK_URL"; then
    echo "ERROR: Failed to download JDK from $JDK_URL" >&2
    return 1
  fi

  if ! tar -xzf "$archive" -C "$JDK_INSTALL_BASE"; then
    echo "ERROR: Failed to extract JDK archive" >&2
    return 1
  fi
  rm -f "$archive"

  if [[ -x "$target/bin/java" ]]; then
    echo "$target"
    return 0
  fi

  echo "ERROR: Extracted JDK missing expected path $target" >&2
  return 1
}

select_action() {
  while true; do
    printf 'Choose action (run=1, build=2):\n  1) Run client\n  2) Build loader\n' >&2
    read -rp "Select run(1) or build(2): " choice
    case "$choice" in
      1) echo "run"; return ;;
      2) echo "build"; return ;;
      *) printf 'Invalid selection, try again.\n' >&2 ;;
    esac
  done
}

select_loader() {
  while true; do
    printf 'Choose loader to run (fabric=1, neoforge=2):\n  1) Fabric\n  2) NeoForge\n' >&2
    read -rp "Select fabric(1) or neoforge(2): " choice
    case "$choice" in
      1) echo "fabric"; return ;;
      2) echo "neoforge"; return ;;
      *) printf 'Invalid selection, try again.\n' >&2 ;;
    esac
  done
}

JAVA_HOME_PATH="$(find_java_home || true)"
if [[ -z "$JAVA_HOME_PATH" ]]; then
  JAVA_HOME_PATH="$(download_java || true)"
fi
if [[ -z "$JAVA_HOME_PATH" ]]; then
  echo "ERROR: Could not find or install a Java 21 runtime (set JAVA_HOME or install one under ~/.jdks, /tmp/jdks, or /usr/lib/jvm)." >&2
  exit 1
fi

ACTION="$(select_action)"
LOADER="$(select_loader)"

cd "$REPO_ROOT"

if [[ "$ACTION" == "run" ]]; then
  TARGET_DIR="$REPO_ROOT/$LOADER/run"
  rm -rf "$TARGET_DIR"
  mkdir -p "$TARGET_DIR"
  echo "Using Java at $JAVA_HOME_PATH"
  echo "Launching $LOADER client (run folder cleaned: $TARGET_DIR)..."
  env JAVA_HOME="$JAVA_HOME_PATH" ./gradlew ":$LOADER:runClient"
else
  echo "Using Java at $JAVA_HOME_PATH"
  echo "Building $LOADER loader..."
  env JAVA_HOME="$JAVA_HOME_PATH" ./gradlew ":$LOADER:build"
  echo "Build completed for $LOADER."
  echo "Built artifacts live in $REPO_ROOT/$LOADER/build/libs/"
fi
