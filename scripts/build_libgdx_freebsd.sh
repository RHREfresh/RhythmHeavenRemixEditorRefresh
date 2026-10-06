#!/usr/bin/env bash

# Compilation of libGDX needs git, bash, unzip, cc and c++ (preinstalled), fetch (preinstalled), and specifically JDK 17 because it's used in RHREfresh
# What this script does briefly is it detects libGDX version used from build.gradle, finds if JDK17 exists (it's in /usr/local/openjdk17 on FreeBSD) and 
# prepares libGDX for FreeBSD building by getting the necessary files (Mainly freetype iirc),
# stripping android from the building (because it errors out on FreeBSD, I know this part is overengineered but it worksTM) and compiling the .so files
# The compilation mostly happens inside ~/libgdx and /tmp/gdx-build, debug symbols are also stripped (afaik)
# It copies the .so files to distribution/freebsd_natives
# This is mostly made using a LLM (GLM), I tried to rewrite as much as I could by hand (I suck at bash :p)
# - Saraistupid

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_BUILD_GRADLE="$SCRIPT_DIR/../build.gradle"

GDX_DIR="${GDX_DIR:-$HOME/libgdx}"
BUILD_DIR="${BUILD_DIR:-/tmp/gdx-build}"
NATIVES_DIR="${NATIVES_DIR:-$SCRIPT_DIR/../distribution/freebsd_natives}"
JDK_HOME="${JDK_HOME:-/usr/local/openjdk17}"
FREETYPE_VERSION="${FREETYPE_VERSION:-2.13.2}"
FREETYPE_URL="https://downloads.sourceforge.net/project/freetype/freetype2/${FREETYPE_VERSION}/freetype-${FREETYPE_VERSION}.tar.gz"

JNI_INC="-I$JDK_HOME/include -I$JDK_HOME/include/freebsd"

if [[ -f "$PROJECT_BUILD_GRADLE" ]]; then
    v="$(grep -oE "gdxVersion[[:space:]]*=[[:space:]]*'[^']+'" "$PROJECT_BUILD_GRADLE" 2>/dev/null \
        | head -1 | sed "s/.*=[[:space:]]*'//; s/'$//")"
    GDX_VERSION="${GDX_VERSION:-${v:-1.12.1}}"
    echo "Detected gdxVersion=$GDX_VERSION from $PROJECT_BUILD_GRADLE"
else
    GDX_VERSION="${GDX_VERSION:-1.12.1}"
    echo "No build.gradle found at $PROJECT_BUILD_GRADLE, using gdxVersion=$GDX_VERSION"
fi

strip_android() {
    local ROOT="$1"
    local AWK_PROG
    read -r -d '' AWK_PROG <<'AWK' || true
BEGIN { SQ = sprintf("%c", 39); DQ = sprintf("%c", 34) }

function brace_delta(s,   i, n, ch, o, c, sq, dq) {
    o = 0; c = 0; sq = 0; dq = 0
    n = length(s)
    for (i = 1; i <= n; i++) {
        ch = substr(s, i, 1)
        if (sq) { if (ch == "\\") { i++; continue } if (ch == SQ) sq = 0; continue }
        if (dq) { if (ch == "\\") { i++; continue } if (ch == DQ) dq = 0; continue }
        if (ch == SQ) { sq = 1; continue }
        if (ch == DQ) { dq = 1; continue }
        if (ch == "/" && substr(s, i+1, 1) == "/") break
        if (ch == "{") o++
        else if (ch == "}") c++
    }
    return o - c
}

{
    line = $0

    if (in_block) {
        print (/^[[:space:]]*\/\// ? line : "// " line)
        depth += brace_delta(line)
        if (depth <= 0) in_block = 0
        next
    }

    if (/^[[:space:]]*\/\//) { print line; next }

    if (/^[[:space:]]*configure[[:space:]]*\(/ && /-android/) {
        match(line, /^[[:space:]]*/)
        print substr(line, 1, RLENGTH) "configure(subprojects) {"
        next
    }

    if (/^[[:space:]]*include[[:space:]]/ && /-android/) { print "// " line; next }

    if (/^[[:space:]]*project[[:space:]]*\(/ && /-android/) {
        print "// " line
        depth = brace_delta(line)
        if (depth > 0) in_block = 1
        next
    }

    if (line ~ ("[" SQ DQ "]:[^" SQ DQ "]*-android[" SQ DQ "]")) { print "// " line; next }

    print line
}
AWK

    cd "$ROOT"

    [[ -f settings.gradle && -f build.gradle ]] || {
        echo "error: no settings.gradle/build.gradle in $(pwd)" >&2
        return 1
    }
    echo "strip-android: working in $(pwd)"

    local -a FILES
    mapfile -t FILES < <(find . \
        -type d \( -name build -o -name .gradle -o -name .git -o -name out \) -prune -o \
        -type f -name '*.gradle' -print)
    echo "Found ${#FILES[@]} .gradle file(s)."

    local CHANGED=0 f out
    for f in "${FILES[@]}"; do
        out="$(mktemp)"
        awk "$AWK_PROG" "$f" > "$out"
        if cmp -s "$f" "$out"; then
            rm -f "$out"
            continue
        fi
        mv "$out" "$f"
        CHANGED=$((CHANGED + 1))
    done
    echo "strip-android: modified $CHANGED file(s)."
}

if [ ! -d "$GDX_DIR" ]; then
    git clone --depth 1 --branch "$GDX_VERSION" https://github.com/libgdx/libgdx.git "$GDX_DIR"
fi
CURRENT=$(git -C "$GDX_DIR" describe --tags 2>/dev/null || git -C "$GDX_DIR" rev-parse --short HEAD)
echo "libGDX checkout at $GDX_DIR (version $GDX_VERSION, resolved: $CURRENT)"

strip_android "$GDX_DIR"

if [ ! -f "$GDX_DIR/gdx/jni/com_badlogic_gdx_graphics_g2d_Gdx2DPixmap.cpp" ] || \
   [ ! -f "$GDX_DIR/extensions/gdx-freetype/jni/com_badlogic_gdx_graphics_g2d_freetype_FreeType.cpp" ]; then
    echo "Running jnigen (may take several minutes on first run)"
    ( cd "$GDX_DIR" && ./gradlew gdx:jnigen :extensions:gdx-freetype:jnigen )
fi

mkdir -p "$BUILD_DIR" "$NATIVES_DIR"
cd "$BUILD_DIR"
rm -f ./*.o libgdx.so libgdx-freetype.so

check_symbol() {
    local SYMS
    SYMS="$(nm -D "$1")"
    echo "$SYMS" | grep -q "$2" || { echo "ERROR: $2 symbol missing" >&2; exit 1; }
}

echo "Building libgdx.so"
GDX_JNI="$GDX_DIR/gdx/jni"

cc -c -fPIC -O2 $JNI_INC -I"$GDX_JNI" -I"$GDX_JNI/gdx2d" \
   $(find "$GDX_JNI" -name '*.c')

for w in "$GDX_JNI"/*.cpp; do
    c++ -c -fPIC -O2 $JNI_INC -I"$GDX_JNI" -I"$GDX_JNI/gdx2d" "$w"
done

c++ -shared -fPIC -o "$BUILD_DIR/libgdx.so" "$BUILD_DIR"/*.o -lm

check_symbol "$BUILD_DIR/libgdx.so" Java_com_badlogic_gdx_graphics_g2d_Gdx2DPixmap_load
check_symbol "$BUILD_DIR/libgdx.so" Java_com_badlogic_gdx_math_Matrix4_prj

echo "Building libgdx-freetype.so"
FT_JNI="$GDX_DIR/extensions/gdx-freetype/jni"
FT_SRC="$FT_JNI/freetype"

if [ ! -d "$FT_SRC/src" ]; then
    echo "    fetching freetype $FREETYPE_VERSION"
    mkdir -p "$FT_SRC"
    fetch "$FREETYPE_URL" -o "$BUILD_DIR/freetype.tar.gz"
    tar xzf "$BUILD_DIR/freetype.tar.gz" -C "$FT_SRC" --strip-components 1
fi

cd "$BUILD_DIR"
rm -f ./*.o
FTCFLAGS="-fPIC -O2 -w -DFT2_BUILD_LIBRARY $JNI_INC -I$FT_SRC/include -I$FT_JNI -I$FT_JNI/jni-headers/linux"

grep -o '"freetype/src/[^"]*\.c"' "$GDX_DIR/extensions/gdx-freetype/build.gradle" \
  | tr -d '"' | sed "s|^|$FT_JNI/|" | while read -r f; do [ -f "$f" ] && echo "$f"; done > "$BUILD_DIR/files.txt"

cc -c $FTCFLAGS $(cat "$BUILD_DIR/files.txt") "$FT_JNI/memcpy_wrap.c"
c++ -c $FTCFLAGS "$FT_JNI/com_badlogic_gdx_graphics_g2d_freetype_FreeType.cpp"
c++ -shared -fPIC -o "$BUILD_DIR/libgdx-freetype.so" "$BUILD_DIR"/*.o -lm -lz

check_symbol "$BUILD_DIR/libgdx-freetype.so" Java_com_badlogic_gdx_graphics_g2d_freetype_FreeType_initFreeTypeJni

strip --strip-debug "$BUILD_DIR"/libgdx.so "$BUILD_DIR"/libgdx-freetype.so
cp "$BUILD_DIR"/libgdx.so "$BUILD_DIR"/libgdx-freetype.so "$NATIVES_DIR/"

echo
echo "Success. Natives in $NATIVES_DIR:"
ls -la "$NATIVES_DIR"
file "$NATIVES_DIR"/*.so