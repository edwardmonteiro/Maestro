# Maestro · Android híbrido

Assistente pessoal Android: planejamento com OpenAI, execução local limitada, Qwen3 no aparelho e conexão com um Gateway OpenClaw no mesmo celular.

## O que esta versão faz

- Interface Android nativa em português, com voz pelo reconhecedor do sistema e leitura em voz alta.
- Planejamento de encontros com **Responses API**, raciocínio e pesquisa web opcional, fontes clicáveis, revisão do plano anterior.
- Seleção de opção, mapa, convite editável, compartilhamento Android e notas privadas.
- Registro factual: abrir o compartilhamento **não** confirma envio; solicitar um mapa **não** confirma chegada.
- Qwen3-1.7B Q4_K_M com **llama.cpp compilado no APK**, inferência CPU de 4 threads, contexto 4096 e até 768 tokens de saída.
- Download retomável de **1.107.409.472 bytes**, revisão e SHA-256 fixados em `scripts/deps.json`.
- Conexão HTTP autenticada com OpenClaw em `127.0.0.1:18789`. Ponte Maestro em `127.0.0.1:19421`.
- Plugin OpenClaw com ferramentas reais para status, salvar notas, solicitar ações revisadas e consultar Qwen local.
- Demonstração pré-definida, explicitamente identificada como sem IA, para testar ações antes de configurar chaves.

## Teste pelo celular

1. Instale o APK de teste assinado. Android 9+ ARM64; alvo principal Samsung recente.
2. Abra **Motores**. Configure sua chave pessoal OpenAI e a cidade. O modelo padrão é `gpt-5.4`, editável conforme acesso à API.
3. Escolha **OpenAI**, volte a **Missão** e use “Planejar uma noite com amigos”.
4. Escolha uma opção, consulte o mapa e prepare o convite. A escolha do destinatário e o envio acontecem no aplicativo de compartilhamento.
5. Para testar offline, baixe o Qwen em **Motores**, escolha **Qwen local** e peça uma brincadeira ou roteiro. Não precisa de OpenAI nesse modo.

A configuração da chave utiliza o fluxo seguro do OpenAI Platform disponibilizado na conversa. Não coloque chaves no código, APK ou issues. A API possui cobrança separada do ChatGPT. As credenciais do aplicativo são criptografadas com Android Keystore e excluídas de backup.

## OpenClaw local real

**O runtime OpenClaw/Node.js não está embutido neste APK.** Instale o runtime comunitário [OpenClaw Android](https://github.com/AidanPark/openclaw-android/releases) ou siga seu caminho Termux. Não exige notebook. O projeto comunitário é separado do Maestro e precisa de validação no seu aparelho.

No terminal desse runtime:

```bash
openclaw --profile maestro onboard
git clone https://github.com/edwardmonteiro/Maestro.git
cd Maestro
bash scripts/setup-openclaw.sh
openclaw --profile maestro gateway --port 18789
```

O instalador usa um **perfil separado `maestro`**, exige o token da ponte exibido em Motores e limita as ferramentas a Maestro, pesquisa e leitura web. A configuração do provedor OpenAI do Gateway é feita pelo onboarding. O token do Gateway deve ser inserido no Maestro e validado em “Salvar token e testar conexão”. Mantenha o app aberto. Depois selecione o modo **OpenClaw**.

Exemplo: “Planeje um encontro em São Paulo e salve o resumo no Maestro.” O registro deve mostrar a nota realmente salva pelo plugin. Se o Gateway não tiver provedor de pesquisa configurado, ele não poderá verificar estabelecimentos atuais.

### Híbrido com Qwen

O OpenClaw com raciocínio na nuvem pode chamar `maestro_local_think` para delegar rascunhos e resumos ao Qwen no telefone. O modelo precisa estar baixado e o Maestro aberto. A resposta local pode voltar ao provedor na nuvem como resultado de ferramenta; “inferência local” não significa que a conversa inteira permaneça offline.

O endpoint local Qwen é de texto; não suporta streaming nem chamadas de ferramentas. Não é apresentado como um substituto completo do modelo de raciocínio do OpenClaw. Comandos exatos como “salvar plano”, “abrir mapa”, “ler plano” e “preparar convite” são roteados deterministicamente pelo app, sem chamadas à nuvem.

## Limites reais

- **JEV/Laya não integrado:** o repositório/modelo correspondente ainda não foi fornecido. Não há um “JEV” simulado.
- Não lê WhatsApp, não controla telas arbitrárias, não compra, não reserva e não envia mensagens sozinho.
- O sandbox é o de aplicativos Android, não uma VM ou container Docker. OpenClaw roda no sandbox de seu próprio app/Termux.
- Permissão INTERNET não é bloqueio de exfiltração. No modo OpenAI, pedido/cidade/plano vão para a API. No modo OpenClaw, a política de dados depende também do Gateway.
- Voz offline depende do reconhecedor e idiomas instalados. O botão Falar abre o reconhecedor do sistema; o texto é revisado antes de iniciar o plano.
- Cancelar encerra a conexão/inferência no Maestro. Um run já iniciado no Gateway pode continuar; ações externas do plugin ainda exigem revisão no Registro.
- O Android pode encerrar processos de segundo plano. Não há promessa de agente permanente ou 24×7.
- A disponibilidade do modelo OpenAI depende da conta. Sem uma chave ativa, chamadas reais da API não podem ser verificadas.
- APK assinado para teste pessoal, não distribuição na Play Store. Preserve a chave de assinatura para futuras atualizações.
- O CI gera uma chave de teste nova se não receber `MAESTRO_KEYSTORE`; artefatos de diferentes execuções podem exigir reinstalação. O APK entregue na conversa usa a chave preservada do projeto.

## Compilar

JDK 21, SDK Android 35+, build-tools 35+, NDK r27c, CMake e Ninja:

```bash
export ANDROID_NDK_HOME=/caminho/ndk/27.2.12479018
bash scripts/build-native.sh
export ANDROID_JAR=/caminho/platforms/android-35/android.jar
export BUILD_TOOLS=/caminho/build-tools/35.0.0
bash scripts/build-apk.sh
```

O workflow GitHub Actions compila e disponibiliza um artefato APK. Bibliotecas nativas são geradas a partir de commit fixado do llama.cpp; não entram no Git.

## Fontes e licenças

- [OpenAI Responses / pesquisa web](https://developers.openai.com/api/docs/guides/tools-web-search)
- [OpenClaw HTTP Gateway](https://docs.openclaw.ai/gateway/openai-http-api)
- [OpenClaw tool plugins](https://docs.openclaw.ai/plugins/tool-plugins)
- [Qwen3 original](https://huggingface.co/Qwen/Qwen3-1.7B) · Apache 2.0
- [Quantização Unsloth](https://huggingface.co/unsloth/Qwen3-1.7B-GGUF)
- [llama.cpp](https://github.com/ggml-org/llama.cpp) · MIT

Veja `docs/VALIDATION.md` para evidências e limitações dos testes.
