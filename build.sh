#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_DIR="$PROJECT_DIR/app/src/main"
BUILD_DIR="$PROJECT_DIR/build"
DIST_DIR="$PROJECT_DIR/dist"
ANDROID_SDK="${ANDROID_HOME:-/usr/lib/android-sdk}"
ANDROID_JAR="$ANDROID_SDK/platforms/android-34/android.jar"
BUILD_TOOLS="$ANDROID_SDK/build-tools/34.0.0"
AAPT="$BUILD_TOOLS/aapt"
D8="$BUILD_TOOLS/d8"
ZIPALIGN="$BUILD_TOOLS/zipalign"

if [[ ! -f "$ANDROID_JAR" ]]; then
  echo "Android API 34 SDK를 찾지 못했습니다: $ANDROID_JAR" >&2
  exit 1
fi

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/generated" "$BUILD_DIR/classes" "$BUILD_DIR/dex" "$DIST_DIR"

"$AAPT" package -f -m -J "$BUILD_DIR/generated" -M "$APP_DIR/AndroidManifest.xml" -S "$APP_DIR/res" -I "$ANDROID_JAR"
mapfile -t JAVA_SOURCES < <(find "$APP_DIR/java" "$BUILD_DIR/generated" -name '*.java' -type f | sort)
javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options -bootclasspath "$ANDROID_JAR" -d "$BUILD_DIR/classes" "${JAVA_SOURCES[@]}"
jar cf "$BUILD_DIR/classes.jar" -C "$BUILD_DIR/classes" .
"$D8" --min-api 23 --lib "$ANDROID_JAR" --output "$BUILD_DIR/dex" "$BUILD_DIR/classes.jar"
"$AAPT" package -f -M "$APP_DIR/AndroidManifest.xml" -S "$APP_DIR/res" -I "$ANDROID_JAR" -F "$BUILD_DIR/unsigned.apk"
(cd "$BUILD_DIR/dex" && "$AAPT" add "$BUILD_DIR/unsigned.apk" classes.dex)
"$ZIPALIGN" -f 4 "$BUILD_DIR/unsigned.apk" "$DIST_DIR/MyRecord-unsigned.apk"
echo "Unsigned APK 생성 완료: $DIST_DIR/MyRecord-unsigned.apk"
echo "공개 저장소에는 서명키를 포함하지 않습니다. 배포용 서명은 개인 키로 별도 수행하세요."
