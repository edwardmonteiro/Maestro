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
