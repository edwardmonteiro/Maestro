# Maestro Android · 0.4.0

Assistente pessoal para Android ARM64: GPT-6 Astra com raciocínio alto na OpenAI,
execução OpenClaw no Termux e Qwen3 1.7B opcional para tarefas offline.

## Servidor · novidade 0.4.0

Abra **Servidor · tarefas autônomas** para usar OpenDots hospedado com GPT-6 Astra
e raciocínio alto. Envie tarefas avulsas ou recorrentes, acompanhe resultados e
pause/cancele pelo APK. As tarefas continuam no servidor ao fechar o app.

[Instalar servidor na Render](https://render.com/deploy?repo=https%3A%2F%2Fgithub.com%2Fedwardmonteiro%2FMaestro)
· [Setup completo e Docker](server/README.md)

Requer uma conta de hospedagem e chaves OpenAI/CopilotKit Intelligence. O token
do servidor é gerado pela hospedagem e salvo no Android Keystore. O setup mostra
o custo antes de contratar. Nenhum servidor de produção foi provisionado com este código.

Gmail, notificações e agenda continuam locais. Esta versão não implementa envio
autônomo de e-mail/WhatsApp nem controle de tela; o servidor recebe apenas as tarefas
digitadas na nova central. Não há push de resultados; abra a central para acompanhar.

## Meu celular · novidade 0.3.0

Abra **Meu celular** no topo do app. Esta central é local e não depende do Termux.
Acesso desligado por padrão, com ativação separada para cada fonte:

- **Notificações:** NotificationListenerService guarda apenas novas notificações;
  ignora serviços contínuos, resumos de grupos, notificações próprias e downloads.
  Atualizações da mesma chave substituem o registro. Até 500 itens; filtros por app.
- **Meu dia:** UsageStatsManager recupera eventos desde a ativação, ao abrir/atualizar
  a central. Reconstrói sessões conservadoras, mostra apps, tela e desbloqueios,
  total diário e barras dos cinco apps principais. Sem leitura de telas, GPS ou
  Accessibility. Eventos técnicos de notificações não contam como atividade humana.
- **Agenda:** somente READ_CALENDAR, próximos sete dias dos calendários visíveis e
  sincronizados no Android. Consulta em memória, sem alterar compromissos.
- **Gmail:** IMAP/TLS direto para imap.gmail.com:993, senha de app e verificação em
  duas etapas. Não exige projeto Google. Lista 20 mensagens da INBOX e lê o texto
  sob demanda, sem marcar como lido, sem SMTP e sem baixar anexos. A senha de app
  tem permissões amplas; a restrição de leitura é aplicada pelo código do Maestro.
  Até 6.000 caracteres por corpo. Rascunhos locais abrem o cliente de e-mail para
  revisão/envio manual; não são gravados como rascunhos remotos pelo Maestro.

Notificações, uso e cache de e-mails são criptografados com AES-GCM e chave Android
Keystore; backup Android desativado. Retenção de até sete dias, aplicada durante
uso/coleta, sem promessa de limpeza agendada quando o processo está encerrado.
**Pausar e apagar dados pessoais** limpa as cópias, credencial Gmail e desativa
as três fontes. Permissões do Android e senha de app podem ser revogadas à parte.

A central chama apenas **Qwen3 local**, se já baixado em Motores. Não envia dados
pessoais à OpenAI ou ao gateway OpenClaw. O assistente desta conversa também não
recebe acesso ao celular. Os modos Astra anteriores continuam separados.

Limites: notificações antigas não são recuperadas, conteúdo sensível pode estar
oculto e Samsung pode suspender o listener. O histórico de uso do Android tem
retenção limitada; abra a central regularmente. As durações são estimativas de
primeiro plano, não comprovam atenção e podem divergir em tela dividida.
Sem dados de exemplo disfarçados de dados reais.

## Instalar e usar

Atualize o APK por cima da versão já instalada. A versão fornecida ao usuário usa a mesma chave
assinante; preserva a chave criptografada, as notas e o modelo já baixado.

1. Em **Motores**, informe sua chave uma vez, ou reutilize a chave salva, e toque
   **Conectar e testar Astra**. O app faz uma chamada real à API; a conta precisa
   de acesso ao `gpt-6-astra` e saldo. O uso da API tem cobrança própria.
2. Para OpenClaw, instale/abra o **Termux oficial** pelo link no app. Toque
   **Preparar OpenClaw → Copiar e abrir**, cole e dê Enter uma vez. O comando vale
   por 15 minutos e só pode ser usado uma vez.
3. O instalador prepara Node, OpenClaw, tokens, provedor e plugin, inicia o Gateway
   e mostra o progresso no Maestro. A verificação chama `maestro_status` através
   do Gateway: sucesso exige a ponte Android real.
4. Escolha **Astra + OpenClaw local**. Autorize a integração Termux ao usar
   **Iniciar executor pelo app**; depois, início e reparo podem ser feitos por botão.

A confirmação da instalação do Termux e a primeira colagem não são silenciosas.
Sem OpenClaw, **Astra + ações Android** já pode pesquisar e planejar. Qwen pode ser
baixado depois; seu arquivo de 1,11 GB não bloqueia os outros modos.

## Correção 0.2.1

- A permissão Android não implica `allow-external-apps=true`. A primeira execução
  sempre exige colagem no Termux. Automação só é liberada após confirmação do
  instalador autenticado; há opção manual para recuperação.
- O comando copiado executa atualização completa via `apt-get` antes de usar
  `curl`. Corrige instalações parciais e inclui reinstalação de curl/libcurl/OpenSSL
  caso o executável continue quebrado. Não usa `pkg` durante esse reparo, pois
  `pkg` depende do próprio curl. A atualização afeta os pacotes do Termux.
- Comandos automáticos possuem callback privado para receber falhas do Termux.
  Mensagens brutas, stdout e tokens não são persistidos pelo callback.
- Na atualização, gere um novo comando em Preparar OpenClaw; comandos anteriores
  têm pareamento temporário e não incluem o reparo.

## O que mudou

- Modelo fixado em `gpt-6-astra`, `reasoning.effort=high`, API Responses, pesquisa
  web e saída estruturada. Não há fallback silencioso para um modelo inferior.
- Orçamento de até 25.000 tokens de saída/raciocínio e timeout de cinco minutos.
  O teste de conexão usa esforço `low` para verificar acesso com custo menor.
- Migração automática do modelo da 0.1; sem preencher nomes de modelos ou tokens.
- Pareamento de uso único/15 minutos, restrito ao endpoint do instalador.
- Instalador incluído no APK, perfil `maestro` separado, backup antes de reconfigurar,
  arquivos privados e tokens criados no aparelho. A chave é copiada para o perfil
  privado do Termux para o OpenClaw chamar a OpenAI diretamente.
- Processo do Maestro mantido por serviço em primeiro plano durante o setup.
  Android/Samsung ainda pode encerrar processos; o app não promete execução 24/7.

## Limites e segurança

O runtime é separado, dentro do sandbox de aplicativo do Termux; não é Docker,
nem uma VM, nem isolamento por tarefa. A política do agente permite apenas as
quatro ferramentas Maestro. Sem shell livre, root, Accessibility ou envio autônomo.
Mapas e compartilhamentos passam por revisão no Maestro. O bridge escuta apenas
`127.0.0.1`, exige token e rejeita requisições com Origin de navegador.

Qwen faz inferência de verdade com llama.cpp; não pesquisa a web. JEV não foi
integrado: sua implementação não foi fornecida. OpenClaw usa Astra via Responses,
mas a comunicação app→Gateway usa o endpoint local de Chat Completions do próprio
OpenClaw, não o endpoint Chat Completions da OpenAI.

Apagar a chave no Maestro não apaga a cópia do Termux; revogue-a na OpenAI para
invalidar ambas. Nenhuma chave de usuário é incluída no código ou no APK distribuído.

## Desenvolvimento

- `bash scripts/test.sh`: contratos, ponte HTTP real, pareamento, configuração e
  requisições de raciocínio. Requer JDK, Node e curl.
- `bash scripts/build-native.sh`: compila llama.cpp ARM64 conforme deps.json.
- `ANDROID_JAR=... BUILD_TOOLS=... bash scripts/build-apk.sh`: APK Android.
- `tests/GatewayIntegration.mjs` valida o Gateway e o plugin reais com ponte
  Android e provedor simulados, incluindo modelo e esforço enviados à API.

O build inclui o plugin automaticamente. Não versione APKs, keystores ou segredos.
A CI usa chave de teste própria: seu APK não substitui uma instalação assinada
com outra chave. Veja [VALIDATION.md](docs/VALIDATION.md) para evidências e limites.

Runtime comunitário: AidanPark/openclaw-android, commit
`c8ceeff0238199a38485fbaf34e49539dd46ce4d`, tarball verificado por SHA-256.
O par fixado pelo upstream é OpenClaw `2026.7.35` e Node `22.23.3`.
