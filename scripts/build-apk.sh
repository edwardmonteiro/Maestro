#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
: "${ANDROID_JAR:?Set ANDROID_JAR to android.jar (API 35+)}"
: "${BUILD_TOOLS:?Set BUILD_TOOLS to Android build-tools directory}"
JAVAC=${JAVAC:-javac}
JAR=${JAR:-jar}
OUT="$ROOT/build"
mkdir -p "$OUT/classes" "$OUT/dex" "$OUT/apk/lib/arm64-v8a"
"$BUILD_TOOLS/aapt2" compile --dir "$ROOT/app/src/main/res" -o "$OUT/resources.zip"
"$BUILD_TOOLS/aapt2" link -o "$OUT/base.apk" --manifest "$ROOT/app/src/main/AndroidManifest.xml" -I "$ANDROID_JAR" -A "$ROOT/app/src/main/assets" --java "$OUT/generated" "$OUT/resources.zip" --auto-add-overlay
find "$ROOT/app/src/main/java" "$OUT/generated" -name '*.java' > "$OUT/sources.txt"
"$JAVAC" -encoding UTF-8 -source 17 -target 17 -classpath "$ANDROID_JAR" -d "$OUT/classes" @"$OUT/sources.txt"
"$JAR" cf "$OUT/classes.jar" -C "$OUT/classes" .
"$BUILD_TOOLS/d8" --lib "$ANDROID_JAR" --min-api 28 --output "$OUT/dex" "$OUT/classes.jar"
cp "$OUT/base.apk" "$OUT/unsigned.apk"
cp "$ROOT/app/src/main/jniLibs/arm64-v8a/libmaestro_llm.so" "$OUT/apk/lib/arm64-v8a/"
cp "$OUT/dex/classes.dex" "$OUT/apk/"
(cd "$OUT/apk" && zip -q -r "$OUT/unsigned.apk" classes.dex lib)
"$BUILD_TOOLS/zipalign" -f -p 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
# Personal test signing. Keep keystore outside git; CI signs with its configured keystore.
KEYSTORE=${MAESTRO_KEYSTORE:-$ROOT/../maestro-test.keystore}
if [ ! -f "$KEYSTORE" ]; then keytool -genkeypair -keystore "$KEYSTORE" -storepass android -keypass android -alias maestro -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=Maestro Personal Test' >/dev/null 2>&1; fi
"$BUILD_TOOLS/apksigner" sign --ks "$KEYSTORE" --ks-pass pass:android --key-pass pass:android --out "$OUT/Maestro-v0.1.0.apk" "$OUT/aligned.apk"
"$BUILD_TOOLS/apksigner" verify --verbose "$OUT/Maestro-v0.1.0.apk"
sha256sum "$OUT/Maestro-v0.1.0.apk" > "$OUT/Maestro-v0.1.0.apk.sha256"
