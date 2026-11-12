#!/usr/bin/env bash
# Build the pinned upstream Kodi Android engine for ARM64.
# This is the native-engine stage; APK embedding into Fermata is handled separately.
set -Eeuo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KODI_SRC="$ROOT/kodi"
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT to the Android SDK directory}"

if [[ ! -f "$KODI_SRC/CMakeLists.txt" || ! -f "$KODI_SRC/tools/depends/Makefile.include.in" ]]; then
  echo "Kodi submodule is missing or incomplete. Run: git submodule update --init --recursive kodi" >&2
  exit 2
fi

# Kodi's current Android build guide specifies NDK r28c.
NDK_VERSION="28.2.13676358"
NDK_PATH="$ANDROID_SDK_ROOT/ndk/$NDK_VERSION"
if [[ ! -d "$NDK_PATH" ]]; then
  echo "Required Kodi NDK not found: $NDK_PATH" >&2
  exit 2
fi

JOBS="${KODI_BUILD_JOBS:-$(getconf _NPROCESSORS_ONLN)}"
# Avoid exhausting hosted runners by default.
if (( JOBS > 6 )); then JOBS=6; fi

BUILD_ROOT="${KODI_BUILD_ROOT:-$ROOT/.kodi-build}"
TARBALLS="$BUILD_ROOT/tarballs"
PREFIX="$BUILD_ROOT/depends"
BUILD_DIR="$BUILD_ROOT/cmake"

mkdir -p "$TARBALLS" "$PREFIX" "$BUILD_DIR"
cd "$KODI_SRC"

echo "== Kodi source =="
git -C "$KODI_SRC" rev-parse HEAD
echo "== Android ARM64 dependencies =="
cd tools/depends
./bootstrap
./configure \
  --with-tarballs="$TARBALLS" \
  --host=aarch64-linux-android \
  --with-sdk-path="$ANDROID_SDK_ROOT" \
  --with-ndk-path="$NDK_PATH" \
  --prefix="$PREFIX"
make -j"$JOBS"

echo "== Configure Kodi CMake =="
cd "$KODI_SRC"
make -C tools/depends/target/cmakebuildsys \
  BUILD_DIR="$BUILD_DIR" \
  CMAKE_EXTRA_ARGUMENTS="-DCMAKE_INSTALL_PREFIX=$PREFIX"

echo "== Compile Kodi native engine =="
cmake --build "$BUILD_DIR" --parallel "$JOBS"

echo "== Package upstream Kodi Android APK (engine validation artifact) =="
# Kodi creates the apk target in the generated build directory; run make apk there.
make -C "$BUILD_DIR" -j"$JOBS" apk

APK="$(find "$KODI_SRC" -maxdepth 1 -type f -name '*arm64*release*.apk' -o -name '*aarch64*release*.apk' | head -n 1)"
if [[ -z "$APK" ]]; then
  APK="$(find "$KODI_SRC" -maxdepth 1 -type f -name '*.apk' | head -n 1)"
fi
if [[ -z "$APK" || ! -s "$APK" ]]; then
  echo "Kodi APK packaging target completed but no APK was found." >&2
  exit 3
fi

echo "Kodi ARM64 build artifact: $APK"
ls -lh "$APK"
