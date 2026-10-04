#!/usr/bin/env bash
set -euo pipefail
if [ -f "$HOME/.maestro/start.sh" ]; then
 exec bash "$HOME/.maestro/start.sh"
fi
cat <<'HELP'
No Maestro 0.2, abra Motores > Conectar e testar Astra > Preparar OpenClaw.
O aplicativo gera o instalador com um pareamento de uso único.
Cole o comando no Termux oficial uma vez. Provedor, modelo, tokens e plugin
são configurados automaticamente. Não use o onboarding manual antigo.
HELP
