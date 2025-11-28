#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")" && pwd)"
JDK_INSTALL_BASE="/tmp/jdks"
JDK_URL="https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.5%2B11/OpenJDK21U-jdk_x64_linux_hotspot_21.0.5_11.tar.gz"
JDK_DIR_NAME="jdk-21.0.5+11"

find_java_home() {
  if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
    "$JAVA_HOME/bin/java" -version >/dev/null 2>&1 || true
    if "$JAVA_HOME/bin/java" -version 2>&1 | grep -q '21'; then
      echo "$JAVA_HOME"
      return
    fi
  fi

  for base in "$REPO_ROOT/.jdks" "$JDK_INSTALL_BASE" "$HOME/.jdks" "/usr/lib/jvm"; do
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
    candidate="$(command -v java | xargs readlink -f | xargs dirname | xargs dirname)"
    if [[ -x "$candidate/bin/java" ]] && "$candidate/bin/java" -version 2>&1 | grep -q '21'; then
      echo "$candidate"
      return
    fi
  fi

  return 1
}

download_java() {
  local archive="$JDK_INSTALL_BASE/temurin21.tar.gz"
  local target="$JDK_INSTALL_BASE/$JDK_DIR_NAME"

  mkdir -p "$JDK_INSTALL_BASE"
  echo "Downloading Temurin JDK 21..." >&2
  curl -L -o "$archive" "$JDK_URL"
  tar -xzf "$archive" -C "$JDK_INSTALL_BASE"
  rm -f "$archive"
  [[ -x "$target/bin/java" ]] && echo "$target" && return 0
  echo "ERROR: extracted JDK missing $target" >&2
  return 1
}

select_loader() {
  while true; do
    printf 'Build loader (fabric=1, neoforge=2):\n  1) Fabric\n  2) NeoForge\n' >&2
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
  echo "ERROR: Failed to locate or download Java 21." >&2
  exit 1
fi

LOADER="$(select_loader)"
BUILD_TASK=":$LOADER:build"

cat <<EOF
Using Java at $JAVA_HOME_PATH
Running Gradle build for loader '$LOADER'
EOF

cd "$REPO_ROOT"
env JAVA_HOME="$JAVA_HOME_PATH" ./gradlew "$BUILD_TASK"

echo "Build completed: $BUILD_TASK"
