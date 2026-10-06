# Maestro + OpenDots

O APK 0.4.0 inclui uma central nativa para conectar a este servidor, enviar tarefas,
consultar resultados e pausar/cancelar execuções. As tarefas rodam no servidor mesmo
quando o app fecha. Não há sincronização automática de Gmail, agenda ou notificações.

## Instalar pelo celular, usando GitHub

[Instalar na Render](https://render.com/deploy?repo=https%3A%2F%2Fgithub.com%2Fedwardmonteiro%2FMaestro)

1. Entre na Render e conecte o repositório. O formulário lê `render.yaml`.
2. Confira o plano e o preço apresentados pelo provedor **antes de contratar**.
   O arquivo solicita uma instância de 1 CPU/2 GB com disco persistente de 1 GB.
   Este setup não usa o plano gratuito que dorme: precisa executar tarefas e guardar SQLite.
3. Preencha `OPENAI_API_KEY` e `INTELLIGENCE_API_KEY` no formulário seguro da Render.
   Nunca coloque essas chaves em commits, issues ou mensagens de chat.
4. Aguarde o deploy ficar saudável. Em **Environment**, copie o `OWNER_TOKEN`,
   gerado automaticamente. Copie também o endereço HTTPS do serviço.
5. No APK, abra **Servidor · tarefas autônomas**, cole endereço e `OWNER_TOKEN`
   e toque em **Conectar e verificar**. Esta tela informa chaves pendentes.
6. Envie uma pesquisa pequena e acompanhe **Ver resultado e execução**. Ver a chave
   configurada não prova acesso ao modelo: a primeira tarefa confirma a integração real.

Para o painel completo no navegador, use o mesmo `OWNER_TOKEN` no login do OpenDots.
O app não coloca tokens em URLs nem transfere automaticamente a sessão ao navegador.

### Chaves necessárias

- **OpenAI:** chave de API com acesso ao modelo `gpt-6-astra`. O servidor usa a API
  Responses (necessária para ferramentas com Astra), esforço
  de raciocínio `high`, limite de saída de 25.000 tokens por chamada e até 10 minutos
  por execução. As credenciais OpenAI que você salvou no celular não são enviadas
  automaticamente ao servidor. Configure a chave na hospedagem.
- **CopilotKit Intelligence:** chave de projeto para conversas persistentes.
  Siga a [configuração oficial](https://docs.copilotkit.ai/intelligence/quickstart).
  A CLI oficial usa `npx copilotkit@latest login` e `npx copilotkit@latest project select`;
  ela grava `CPK_INTELLIGENCE_API_KEY` no `.env`. Copie seu valor para a variável
  `INTELLIGENCE_API_KEY` da hospedagem. Caso use a CLI pelo Termux, faça isso em uma
  pasta privada separada do Maestro; compatibilidade da CLI com seu aparelho não foi testada.
- **Parallel:** busca pública habilitada. `PARALLEL_API_KEY` é opcional para limites
  maiores; o endpoint anônimo pode impor limites. Consultas, URLs e objetivos de
  pesquisa são compartilhados com esse serviço. Desative com `WEB_SEARCH_PROVIDER=disabled`.

As contas e custos de hospedagem, modelo e serviços são independentes. O APK não
contrata um servidor, não cria contas externas e não contém chaves embutidas.

## O que esta versão faz

- Tarefas avulsas e repetição a cada hora ou 24 horas, com confirmação do texto e do intervalo.
- Lista de execução, resultados, erros, pausa/cancelamento e consulta a cada 15 segundos
  enquanto a tela está aberta. Não há push de resultados no Android nesta versão.
- Painel web original do OpenDots: conversas, páginas, memória e configurações.
- Uma instância e um proprietário. O token de acesso é administrativo; não compartilhe.
- A tarefa recorrente executa imediatamente e repete o intervalo após uma conclusão
  bem-sucedida. Falhas precisam de revisão/retomada no painel; não é um cron de horário fixo.
- Se o envio perder a conexão, consulte a lista antes de reenviar. Não há retry automático
  de POST nem garantia de exatamente uma execução em caso de falha/crash do servidor.

## Limites atuais

Este pacote não implementa envio de Gmail/WhatsApp, leitura remota das notificações,
controle da tela Android, voz ou computadores OpenBot. O Gmail existente no APK
continua local e somente leitura, com respostas preparadas para revisão no app de e-mail.
Recursos de computador do OpenDots exigem uma implantação separada com Docker supervisor;
a instalação Render acima não os provisiona. Consulte a documentação upstream antes
de habilitá-los: [COMPUTERS.md](https://github.com/CopilotKit/OpenDots/blob/c2569bb6a13a22e565cf3eb791c62267d06babb1/docs/COMPUTERS.md).

## Servidor próprio com Docker

Em um host Linux com Docker Engine e Compose v2:

```sh
git clone https://github.com/edwardmonteiro/Maestro.git
cd Maestro/server
cp .env.example .env
chmod 600 .env
# Preencha .env com suas chaves e APP_ORIGIN. Gere OWNER_TOKEN, por exemplo:
openssl rand -hex 32
docker compose up --build -d
docker compose logs --tail=60
```

Configure seu proxy HTTPS para `127.0.0.1:4310` e defina `APP_ORIGIN` com a origem
HTTPS exata. A porta do Compose fica apenas no loopback; o app recusa HTTP remoto.
Não exponha o socket Docker ou a ponte Termux do celular.

Faça backup do volume `maestro-data`. As conversas são armazenadas no projeto
Intelligence: copiar apenas SQLite não copia o histórico de conversas. Preserve
`OWNER_ID`, as chaves e o projeto ao migrar. Rotacione `OWNER_TOKEN` na hospedagem
para revogar acesso; desconectar o celular apenas apaga a conexão local.

## Origem, alterações e validação

Fonte: [CopilotKit/OpenDots](https://github.com/CopilotKit/OpenDots), licença MIT,
commit fixado `c2569bb6a13a22e565cf3eb791c62267d06babb1`. O build mantém o LICENSE
original em `/app/UPSTREAM-LICENSE` e usa o package-lock desse commit.

`patch-opendots.mjs` faz alterações verificadas e falha se o conteúdo não corresponde:
API Responses para GPT-6, raciocínio alto, orçamento de saída, timeout e lease compatíveis,
assistente inicial em português, `/healthz` público mínimo e `/api/maestro` autenticado.
O frontend original não é reescrito. Credenciais continuam apenas no servidor.

Para reproduzir a validação de código sem Docker:

```sh
git clone https://github.com/CopilotKit/OpenDots.git /tmp/maestro-opendots
git -C /tmp/maestro-opendots checkout c2569bb6a13a22e565cf3eb791c62267d06babb1
node server/patch-opendots.mjs /tmp/maestro-opendots
cd /tmp/maestro-opendots
npm ci
npm run build
npm test
```

O teste `node server/smoke.mjs /tmp/maestro-opendots`, executado na raiz do Maestro,
inicia o servidor compilado em loopback com banco temporário, verifica autenticação,
setup incompleto, persistência após reinício e frontend, sem chamar modelos.

Use Node.js 24. O teste de produção com contas reais deve confirmar o primeiro resultado,
a persistência após reinício e o funcionamento de pausa/cancelamento. Não confunda
healthcheck HTTP com uma tarefa validada no modelo.
