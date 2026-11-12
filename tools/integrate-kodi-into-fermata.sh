#!/usr/bin/env bash
# Embed the already-built Kodi ARM64 payload and upstream Android Java bridge
# into the Fermata auto flavor. The APK remains a single application package.
set -Eeuo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENGINE_APK="${1:?Usage: integrate-kodi-into-fermata.sh <Kodi-engine.apk> <Kodi-package-id>}"
KODI_PACKAGE="${2:?Kodi package id must be read from the upstream engine APK}"
KODI_SRC="$ROOT/kodi"
AUTO="$ROOT/fermata/src/auto"
JAVA_ROOT="$AUTO/java"
RES_ROOT="$AUTO/res"
ASSET_ROOT="$AUTO/assets"
LIB_ROOT="$AUTO/jniLibs/arm64-v8a"
PKG_PATH="${KODI_PACKAGE//./\/}"
TEMPLATE_ROOT="$KODI_SRC/tools/android/packaging/xbmc"

test -s "$ENGINE_APK"
test -f "$TEMPLATE_ROOT/src/Main.java.in"
test -f "$TEMPLATE_ROOT/strings.xml.in"

echo "Embedding Kodi package namespace: $KODI_PACKAGE"
mkdir -p "$ASSET_ROOT" "$LIB_ROOT" "$JAVA_ROOT/$PKG_PATH" "$RES_ROOT"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

unzip -q "$ENGINE_APK" 'assets/*' 'lib/arm64-v8a/*' -d "$TMP"
test -d "$TMP/assets"
test -s "$TMP/lib/arm64-v8a/libkodi.so"
cp -a "$TMP/assets/." "$ASSET_ROOT/"
cp -a "$TMP/lib/arm64-v8a/." "$LIB_ROOT/"

python3 - "$TEMPLATE_ROOT" "$JAVA_ROOT" "$RES_ROOT" "$KODI_PACKAGE" <<'PY'
from pathlib import Path
import re, sys, shutil

template = Path(sys.argv[1])
java_root = Path(sys.argv[2])
res_root = Path(sys.argv[3])
package = sys.argv[4]
pkg_path = Path(*package.split("."))

# Copy all upstream Kodi Android Java sources while keeping their JNI package.
src_root = template / "src"
for source in src_root.rglob("*.java.in"):
    rel = source.relative_to(src_root)
    target = java_root / pkg_path / rel.with_suffix("").with_suffix(".java")
    target.parent.mkdir(parents=True, exist_ok=True)
    code = source.read_text(encoding="utf-8")
    code = code.replace("@APP_PACKAGE@", package)
    code = code.replace("@APP_NAME@", "Kodi Auto Fermata")
    code = code.replace("@APP_NAME_LC@", "kodi")
    code = code.replace("import " + package + ".R;", "import me.aap.fermata.R;")
    code = code.replace("Manifest.permission.ACCESS_LOCAL_NETWORK", '"android.permission.ACCESS_LOCAL_NETWORK"')
    code = re.sub(r"(?<!android\.)\bR\.(string|layout|id|drawable|xml|array|style|styleable|mipmap|color)\.([A-Za-z0-9_]+)",
                  lambda m: "R.%s.kodi_%s" % (m.group(1), m.group(2)), code)
    if "R." in code and "import me.aap.fermata.R;" not in code:
        code = re.sub(r"^(package [^;]+;\s*)", r"\1\nimport me.aap.fermata.R;\n", code, count=1, flags=re.M)
    target.write_text(code, encoding="utf-8")

# Namespace every Kodi resource to avoid collisions with Fermata's resource table.
def prefixed_refs(text):
    for kind in ("string", "layout", "drawable", "xml", "id", "array", "style", "mipmap", "color"):
        text = re.sub(r"(@(?:\+)?%s/)([A-Za-z0-9_]+)" % kind,
                      lambda m: m.group(1) + "kodi_" + m.group(2), text)
    return text

# Copy layouts and drawables used by the upstream Java bridge.
for folder in ("layout", "drawable"):
    src = template / "res" / folder
    if src.exists():
        dst = res_root / folder
        dst.mkdir(parents=True, exist_ok=True)
        for item in src.iterdir():
            if item.is_file() and item.name != ".gitkeep":
                name = "kodi_" + item.name
                content = item.read_text(encoding="utf-8") if item.suffix in (".xml",) else None
                target = dst / name
                if content is None:
                    shutil.copy2(item, target)
                else:
                    target.write_text(prefixed_refs(content), encoding="utf-8")

# Upstream default strings, all namespaced so Fermata's app_name remains the launcher label.
strings = (template / "strings.xml.in").read_text(encoding="utf-8")
strings = strings.replace("@APP_NAME@", "Kodi Auto Fermata")
strings = re.sub(r'name="([^"]+)"', lambda m: 'name="kodi_' + m.group(1) + '"', strings)
strings = prefixed_refs(strings)
values = res_root / "values"
values.mkdir(parents=True, exist_ok=True)
(values / "kodi_strings.xml").write_text(strings, encoding="utf-8")

# Kodi colors and theme resources are separate from strings.xml.in.
colors_template = template / "colors.xml.in"
if colors_template.exists():
    colors = colors_template.read_text(encoding="utf-8")
    colors = re.sub(r'name="([^"]+)"', lambda m: 'name="' + (m.group(1) if m.group(1).startswith("android:") else "kodi_" + m.group(1)) + '"', colors)
    colors = prefixed_refs(colors)
    (values / "kodi_colors.xml").write_text(colors, encoding="utf-8")

# Required Kodi layouts and provider/search metadata.
for name in ("activity_main.xml", "activity_splash.xml", "result.xml", "search_result.xml"):
    src = template / "res" / "layout" / name
    if src.exists():
        content = prefixed_refs(src.read_text(encoding="utf-8"))
        if name == "activity_splash.xml":
            content = content.replace("@drawable/kodi_applaunch_screen", "@android:color/black")
        (res_root / "layout" / ("kodi_" + name)).write_text(content, encoding="utf-8")

for name in ("file_paths.xml",):
    src = template / "res" / "xml" / name
    if src.exists():
        dst = res_root / "xml"
        dst.mkdir(parents=True, exist_ok=True)
        (dst / ("kodi_" + name)).write_text(prefixed_refs(src.read_text(encoding="utf-8")), encoding="utf-8")

searchable = template / "searchable.xml.in"
if searchable.exists():
    dst = res_root / "xml"
    dst.mkdir(parents=True, exist_ok=True)
    content = searchable.read_text(encoding="utf-8").replace("@APP_NAME@", "Kodi Auto Fermata")
    (dst / "kodi_searchable.xml").write_text(prefixed_refs(content), encoding="utf-8")

# Kodi splash uses only private app storage; it must not request MANAGE_EXTERNAL_STORAGE.
splash = java_root / pkg_path / "Splash.java"
if splash.exists():
    code = splash.read_text(encoding="utf-8")
    code = re.sub(r"private boolean CheckPermissions\(\)\s*\{.*?\n  \}",
                  "private boolean CheckPermissions()\n  {\n    return true; // Kodi payload and profile are kept in app-private storage.\n  }",
                  code, count=1, flags=re.S)
    splash.write_text(code, encoding="utf-8")

print("Kodi Android Java bridge and resources prepared.")
PY

echo "Embedded native libraries:"
find "$LIB_ROOT" -maxdepth 1 -type f -name '*.so' | wc -l
echo "Embedded Kodi assets:"
find "$ASSET_ROOT" -type f | wc -l
test -s "$LIB_ROOT/libkodi.so"
test -d "$ASSET_ROOT/addons"
