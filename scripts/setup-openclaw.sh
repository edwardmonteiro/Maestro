#!/usr/bin/env bash
set -euo pipefail
umask 077
ROOT=$(cd "$(dirname "$0")/.." && pwd)
if ! command -v openclaw >/dev/null; then
 echo 'Instale primeiro o runtime: https://github.com/AidanPark/openclaw-android/releases'
 exit 1
fi
# A dedicated profile avoids modifying the user's default OpenClaw environment.
oc() { openclaw --profile maestro "$@"; }
echo 'Este perfil usa somente a ponte Maestro e pesquisa web. Sem shell livre.'
echo 'Se ainda não configurou o provedor deste perfil, execute: openclaw --profile maestro onboard'
read -r -s -p 'Token da ponte (Maestro > Motores > Copiar token): ' MAESTRO_BRIDGE_TOKEN
printf '\n'
if [ ${#MAESTRO_BRIDGE_TOKEN} -lt 32 ]; then echo 'Token inválido'; exit 1; fi
oc plugins install "$ROOT/openclaw-plugin"
oc config set plugins.entries.maestro-phone.enabled true
# Pass only the app-specific token; no OpenAI credential is read or printed.
oc config set plugins.entries.maestro-phone.config.bridgeToken "$MAESTRO_BRIDGE_TOKEN"
unset MAESTRO_BRIDGE_TOKEN
oc config set gateway.mode local
oc config set gateway.bind loopback
oc config set gateway.port 18789
oc config set gateway.http.endpoints.chatCompletions.enabled true
oc config set gateway.auth.mode token
oc config set tools.allow '["maestro_status","maestro_save_note","maestro_request_action","maestro_local_think","web_search","web_fetch"]'
oc config set tools.elevated.enabled false
oc plugins inspect maestro-phone --runtime
printf '\nConfigure o provedor: openclaw --profile maestro onboard\n'
printf 'Copie o token do Gateway para o Maestro: openclaw --profile maestro config get gateway.auth.token\n'
printf 'Inicie em uma aba separada: openclaw --profile maestro gateway --port 18789\n'
printf 'Mantenha o Maestro aberto. Teste a conexão em Motores.\n'
