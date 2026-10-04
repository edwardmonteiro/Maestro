# Bundled installer, delivered once over the authenticated loopback bridge.
set -euo pipefail
umask 077
[ "${PREFIX:-}" = /data/data/com.termux/files/usr ] || { echo 'Abra no Termux oficial.'; exit 1; }
[ "$(uname -m)" = aarch64 ] || { echo 'Este instalador requer Android ARM64.'; exit 1; }
MAESTRO_DIR="$HOME/.maestro"
mkdir -p "$MAESTRO_DIR"
chmod 700 "$MAESTRO_DIR"
progress() {
 echo "Maestro: $1"
 curl --max-time 5 -fsS -H "Authorization: Bearer $MAESTRO_BRIDGE" -H 'Content-Type: application/json' -d "{\"phase\":\"$1\"}" http://127.0.0.1:19421/setup/status >/dev/null 2>&1 || true
}
trap '[ ! -f "$MAESTRO_DIR/pairing.json" ] || : > "$MAESTRO_DIR/pairing.json"' EXIT
trap 'progress "Falha na instalação. Veja a mensagem no Termux e tente novamente."' ERR
progress 'Preparando dependências'
command -v termux-wake-lock >/dev/null && termux-wake-lock || true
export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get -y -o Dpkg::Options::=--force-confdef -o Dpkg::Options::=--force-confold dist-upgrade
apt-get -y -o Dpkg::Options::=--force-confdef -o Dpkg::Options::=--force-confold install git curl python tar
curl --version >/dev/null
printf '%s' "$MAESTRO_BUNDLE" | base64 -d > "$MAESTRO_DIR/pairing.json"
unset MAESTRO_BUNDLE
python - "$MAESTRO_DIR" <<'PY'
import json,sys,pathlib
root=pathlib.Path(sys.argv[1]);bundle=json.loads((root/'pairing.json').read_text())
(root/'configure.cjs').write_text(bundle['configure'])
PY
export PATH="$HOME/.openclaw-android/bin:$HOME/.openclaw-android/node/bin:$HOME/.local/bin:$PATH"
export TMPDIR="$PREFIX/tmp" TMP="$PREFIX/tmp" TEMP="$PREFIX/tmp" OA_GLIBC=1
if ! command -v openclaw >/dev/null; then
 progress 'Baixando runtime Android'
 runtime_dir=$(mktemp -d "$MAESTRO_DIR/runtime.XXXXXX")
 curl --retry 3 --connect-timeout 20 -fL https://codeload.github.com/AidanPark/openclaw-android/tar.gz/c8ceeff0238199a38485fbaf34e49539dd46ce4d -o "$runtime_dir/source.tar.gz"
 echo "bcc02b9a1722230e3843db55549e89a2b6223d76acd70b9ff2f7f0e9b76138cb  $runtime_dir/source.tar.gz" | sha256sum -c -
 tar -xzf "$runtime_dir/source.tar.gz" -C "$runtime_dir"
 src="$runtime_dir/openclaw-android-c8ceeff0238199a38485fbaf34e49539dd46ce4d"
 bash "$src/scripts/check-env.sh"
 bash "$src/scripts/setup-paths.sh"
 progress 'Instalando Node e bibliotecas'
 bash "$src/scripts/install-glibc.sh"
 bash "$src/scripts/install-nodejs.sh" 22.23.3
 bash "$src/scripts/install-build-tools.sh"
 progress 'Instalando OpenClaw'
 bash "$src/platforms/openclaw/install.sh"
 printf 'openclaw\n' > "$HOME/.openclaw-android/.platform"
 bash "$src/scripts/setup-env.sh"
fi
if [ -f "$HOME/.bashrc" ]; then source "$HOME/.bashrc" || true; fi
progress 'Configurando Astra e ferramentas locais'
node "$MAESTRO_DIR/configure.cjs" "$MAESTRO_DIR/pairing.json"
command -v termux-reload-settings >/dev/null && termux-reload-settings || true
curl --max-time 5 -fsS -H "Authorization: Bearer $MAESTRO_BRIDGE" -H 'Content-Type: application/json' -d '{"phase":"Integração Termux preparada","bootstrap_complete":true}' http://127.0.0.1:19421/setup/status >/dev/null
openclaw --profile maestro config validate
progress 'Iniciando Gateway'
# Check for an already-running, authenticated Maestro gateway; never kill unrelated services.
if ! curl --max-time 8 -fsS -H "Authorization: Bearer $MAESTRO_GATEWAY" -H 'Content-Type: application/json' -d '{"tool":"maestro_status","args":{}}' http://127.0.0.1:18789/tools/invoke | python -c 'import json,sys; assert json.load(sys.stdin).get("ok") is True' 2>/dev/null; then
 nohup bash "$MAESTRO_DIR/start.sh" > "$MAESTRO_DIR/gateway.log" 2>&1 < /dev/null &
fi
for attempt in $(seq 1 90); do
 if curl --max-time 4 -fsS -H "Authorization: Bearer $MAESTRO_GATEWAY" -H 'Content-Type: application/json' -d '{"tool":"maestro_status","args":{}}' http://127.0.0.1:18789/tools/invoke | python -c 'import json,sys; assert json.load(sys.stdin).get("ok") is True' 2>/dev/null; then
  progress 'Gateway pronto'
  echo 'Volte ao Maestro. A conexão será verificada no app.'
  exit 0
 fi
 sleep 2
done
progress 'Gateway não respondeu. Consulte ~/.maestro/gateway.log no Termux.'
exit 1
