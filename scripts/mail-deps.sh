#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
mkdir -p "$ROOT/build/deps"
fetch() {
 local name="$1" expected="$2" target="$ROOT/build/deps/$1.jar"
 if [ ! -f "$target" ] || [ "$(sha256sum "$target" | cut -d' ' -f1)" != "$expected" ]; then
  curl --connect-timeout 20 --max-time 120 -fsSL "https://repo.maven.apache.org/maven2/com/sun/mail/$name/1.6.7/$name-1.6.7.jar" -o "$target"
 fi
 echo "$expected  $target" | sha256sum -c -
}
fetch android-mail 12c4240aa3a4ccda37c49b4e6b880a797a21e03a4f8c7421a1b51e0db552b79c
fetch android-activation 285713b2b549f382f26a57919ecddc4c83d9755737969fb7c006c905322bb253
