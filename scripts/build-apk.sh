#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
: "${ANDROID_JAR:?Set ANDROID_JAR to android.jar (API 35+)}"
: "${BUILD_TOOLS:?Set BUILD_TOOLS to Android build-tools directory}"
JAVAC=${JAVAC:-javac}
JAR=${JAR:-jar}
OUT="$ROOT/build"
bash "$ROOT/scripts/mail-deps.sh"
DEPS="$OUT/deps/android-mail.jar:$OUT/deps/android-activation.jar"
mkdir -p "$OUT/classes" "$OUT/dex" "$OUT/apk/lib/arm64-v8a"
mkdir -p "$ROOT/app/src/main/assets/setup/plugin"
cp "$ROOT/openclaw-plugin/"* "$ROOT/app/src/main/assets/setup/plugin/"
"$BUILD_TOOLS/aapt2" compile --dir "$ROOT/app/src/main/res" -o "$OUT/resources.zip"
"$BUILD_TOOLS/aapt2" link -o "$OUT/base.apk" --manifest "$ROOT/app/src/main/AndroidManifest.xml" -I "$ANDROID_JAR" -A "$ROOT/app/src/main/assets" --java "$OUT/generated" "$OUT/resources.zip" --auto-add-overlay
find "$ROOT/app/src/main/java" "$OUT/generated" -name '*.java' > "$OUT/sources.txt"
"$JAVAC" -encoding UTF-8 -source 17 -target 17 -classpath "$ANDROID_JAR:$DEPS" -d "$OUT/classes" @"$OUT/sources.txt"
"$JAR" cf "$OUT/classes.jar" -C "$OUT/classes" .
"$BUILD_TOOLS/d8" --lib "$ANDROID_JAR" --min-api 28 --output "$OUT/dex" "$OUT/classes.jar" "$OUT/deps/android-mail.jar" "$OUT/deps/android-activation.jar"
cp "$OUT/base.apk" "$OUT/unsigned.apk"
cp "$ROOT/app/src/main/jniLibs/arm64-v8a/libmaestro_llm.so" "$OUT/apk/lib/arm64-v8a/"
cp "$OUT/dex/classes.dex" "$OUT/apk/"
python3 - "$OUT" <<'PYCODE'
import sys,zipfile,pathlib
out=pathlib.Path(sys.argv[1])
with zipfile.ZipFile(out/'unsigned.apk','a',zipfile.ZIP_DEFLATED) as apk:
 for dex in (out/'dex').glob('*.dex'): apk.write(dex,dex.name)
 apk.write(out/'apk/lib/arm64-v8a/libmaestro_llm.so','lib/arm64-v8a/libmaestro_llm.so')
 seen=set()
 for name in ['android-mail','android-activation']:
  with zipfile.ZipFile(out/'deps'/f'{name}.jar') as dep:
   for member in dep.namelist():
    if member.startswith(('META-INF/mailcap','META-INF/javamail','META-INF/mimetypes')) and member not in seen:
     apk.writestr(member,dep.read(member));seen.add(member)
   for member in dep.namelist():
    if member.startswith(('META-INF/LICENSE','META-INF/NOTICE')):
     apk.writestr(f'assets/licenses/{name}-{pathlib.Path(member).name}',dep.read(member))
PYCODE
"$BUILD_TOOLS/zipalign" -f -p 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
# Personal test signing. Keep keystore outside git; CI signs with its configured keystore.
KEYSTORE=${MAESTRO_KEYSTORE:-$ROOT/../maestro-test.keystore}
if [ ! -f "$KEYSTORE" ]; then keytool -genkeypair -keystore "$KEYSTORE" -storepass android -keypass android -alias maestro -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=Maestro Personal Test' >/dev/null 2>&1; fi
"$BUILD_TOOLS/apksigner" sign --ks "$KEYSTORE" --ks-pass pass:android --key-pass pass:android --out "$OUT/Maestro-v0.3.0.apk" "$OUT/aligned.apk"
"$BUILD_TOOLS/apksigner" verify --verbose "$OUT/Maestro-v0.3.0.apk"
sha256sum "$OUT/Maestro-v0.3.0.apk" > "$OUT/Maestro-v0.3.0.apk.sha256"
