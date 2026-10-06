#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
mkdir -p "$ROOT/build/tests"
curl -fsSL https://repo.maven.apache.org/maven2/org/json/json/20240303/json-20240303.jar -o "$ROOT/build/tests/json.jar"
echo "3cf6cd6892e32e2b4c1c39e0f52f5248a2f5b37646fdfbb79a66b46b618414ed  $ROOT/build/tests/json.jar" | sha256sum -c -
javac -cp "$ROOT/build/tests/json.jar" -d "$ROOT/build/tests" "$ROOT/app/src/main/java/br/com/maestro/Bridge.java" "$ROOT/app/src/main/java/br/com/maestro/Plan.java" "$ROOT/app/src/main/java/br/com/maestro/Pairing.java" "$ROOT/app/src/main/java/br/com/maestro/Reasoning.java" "$ROOT/app/src/main/java/br/com/maestro/TermuxSetup.java" "$ROOT/tests/CoreTest.java"
java -Dsun.net.http.allowRestrictedHeaders=true -cp "$ROOT/build/tests:$ROOT/build/tests/json.jar" br.com.maestro.CoreTest
node "$ROOT/tests/PluginTest.mjs"

node "$ROOT/tests/SetupTest.mjs"
bash -n "$ROOT/app/src/main/assets/setup/bootstrap.sh"

bash -n "$ROOT/app/src/main/assets/setup/preflight.sh"
python3 "$ROOT/tests/PreflightTest.py"
bash "$ROOT/scripts/mail-deps.sh"
PERSONAL_CP="$ROOT/build/tests/json.jar:$ROOT/build/deps/android-mail.jar:$ROOT/build/deps/android-activation.jar"
javac -cp "$PERSONAL_CP" -d "$ROOT/build/tests" "$ROOT/app/src/main/java/br/com/maestro/UsageTimeline.java" "$ROOT/app/src/main/java/br/com/maestro/GmailReader.java" "$ROOT/tests/PersonalTest.java"
java -cp "$ROOT/build/tests:$PERSONAL_CP" br.com.maestro.PersonalTest
javac -cp "$ROOT/build/tests/json.jar" -d "$ROOT/build/tests" "$ROOT/app/src/main/java/br/com/maestro/ServerClient.java" "$ROOT/app/src/main/java/br/com/maestro/Net.java" "$ROOT/tests/ServerTest.java"
java -cp "$ROOT/build/tests:$ROOT/build/tests/json.jar" br.com.maestro.ServerTest
