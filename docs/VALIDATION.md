# Correção 0.2.1

Relatos recebidos: RUN_COMMAND negado por allow-external-apps ausente; curl com
símbolo OpenSSL não encontrado após atualização parcial de dependências.

Validação da correção:
- Regressão JVM: permissão concedida + bootstrap incompleto continua no fluxo manual.
- Shell com apt/curl simulados: curl inicialmente quebrado, reparo por upgrade,
  alternativa de reinstalação e falha do apt impedindo o download.
- APK com versionCode 3, mesmo certificado da 0.2.0; testes anteriores mantidos.
- Não executado no Samsung físico. Testes de shell simulam a falha de dependências;
  não afirmam ter atualizado o Termux do usuário neste ambiente.

Fontes oficiais:
- https://github.com/termux/termux-app/wiki/RUN_COMMAND-Intent
- https://github.com/termux/termux-packages/wiki/Termux-execution-environment

# Maestro 0.2.0 — validação

## Verificado neste ambiente

- Compilação Java/DEX, recursos e APK ARM64, Android API 36, target SDK 35.
- Assinatura APK v3 com a chave da versão 0.1.0; versionCode incrementado para 2.
- Testes JVM: plano estrito, servidor HTTP real com autenticação, bloqueio de
  Origin, escopo/expiração/uso único do pareamento, bloqueio de replay e
  configuração GPT-6 Astra/high/Responses sem parâmetros incompatíveis.
- Testes Node: contratos do plugin e ações limitadas; geração do perfil isolado,
  permissão 0600 da configuração, backup e repetição sem alterar o perfil padrão.
- Sintaxe do instalador Bash. Runtime Android fixado por commit e SHA-256.

## Integração OpenClaw real

Testado com OpenClaw 2026.7.35, Node 24.19.0 no Linux e o plugin real do projeto:

- A CLI aceitou a configuração gerada.
- O Gateway iniciou, carregou o plugin, autenticou o pedido e chamou a ponte
  simulada por HTTP. Token incorreto foi recusado e a ferramenta de shell ficou
  indisponível pela política.
- Uma missão enviada ao Gateway produziu uma requisição `/v1/responses` com
  `model=gpt-6-astra` e `reasoning.effort=high`, capturada por um servidor de teste.
  O servidor recusou a geração intencionalmente; não foi resposta de IA real.
- Corrigido o startup que tentava instalar o harness Codex automaticamente:
  `models.providers.openai.agentRuntime.id=openclaw`, com plugins explícitos.

Reprodução: instale o pacote em uma pasta de teste com npm, execute o postinstall
oficial e rode `OPENCLAW_CLI=/caminho/openclaw.mjs node tests/GatewayIntegration.mjs`.

## Ainda depende do Samsung e da conta

Não houve execução desta versão em um Samsung físico nem teste end-to-end do
instalador Termux no Android. Não foi usada uma chave OpenAI do usuário nos testes
deste ambiente. O teste de conexão dentro do app verifica acesso real ao modelo,
e o teste de Gateway exige a execução de maestro_status através do OpenClaw.

A política de bateria/segundo plano do Samsung pode interromper instalação ou
Gateway. Sem alegação de desempenho, autonomia ou estabilidade medida no aparelho.
A biblioteca llama.cpp ARM64 foi reaproveitada, sem alterações, do APK 0.1.0.
