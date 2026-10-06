import { readFileSync, writeFileSync, copyFileSync } from 'node:fs';
import { resolve } from 'node:path';

// Deliberately fail if the pinned upstream contract changes.
const root = resolve(process.argv[2] || '.');
function replace(file, before, after) {
  const path = resolve(root, file);
  const source = readFileSync(path, 'utf8');
  if (source.split(before).length !== 2) throw new Error(`Unexpected upstream content: ${file}`);
  writeFileSync(path, source.replace(before, after));
}
replace('src/server/dot-agent.ts',
  '        const adapter = openaiCompatibleText(this.config.model, {',
  "        const useResponses = /^gpt-6/.test(this.config.model);\n        const adapter = openaiCompatibleText(this.config.model, {");
replace('src/server/dot-agent.ts', "api: 'chat-completions',", "api: useResponses ? 'responses' : 'chat-completions',");
replace('src/server/dot-agent.ts',
  'modelOptions: { max_completion_tokens: 2200 },',
  "modelOptions: useResponses ? { max_output_tokens: 25000, reasoning: { effort: 'high' }, store: false, include: ['reasoning.encrypted_content'] } : { max_completion_tokens: 2200 },");
copyFileSync(new URL('./responses.test.ts', import.meta.url), resolve(root, 'tests/maestro-responses.test.ts'));
replace('src/server/runner.ts',
  "import { Store } from './store.js';",
  "import { Store } from './store.js';\nconst taskTimeout = Math.max(90_000, Math.min(900_000, Number(process.env.MAESTRO_TASK_TIMEOUT_MS) || 90_000));");
replace('src/server/runner.ts', "'Research exceeded the 90 second time limit.'", "'A tarefa excedeu o limite de tempo configurado no servidor.'");
replace('src/server/runner.ts', '      90_000,', '      taskTimeout,');
replace('src/server/store.ts', 'now + 180_000', 'now + Math.max(90_000, Math.min(900_000, Number(process.env.MAESTRO_TASK_TIMEOUT_MS) || 90_000)) + 90_000');
replace('src/server/workspace.ts', "'Everyday',", "'Meu espaço',");
replace('src/server/workspace.ts', "'A little space for your day.',", "'Pesquisas, tarefas e resultados do Maestro.',");
replace('src/server/workspace.ts', "        'Dot',", "        'Maestro',");
replace('src/server/workspace.ts',
  "'Be thoughtful, practical, and concise. Help the user think clearly and follow through.',",
  "'Responda em português brasileiro. Pesquise, compare e conclua tarefas com as ferramentas disponíveis. Cite fontes. Distinga resultados confirmados de sugestões. Você não tem acesso automático ao Gmail, às notificações nem aos apps do celular. Para mensagens, compras ou outras ações externas, exija autorização explícita e nunca invente execução.',");
replace('src/server/app.ts', '  const app = new Hono();',
  "  const app = new Hono();\n  app.get('/healthz', (c) => c.json({ ok: true }));");
replace('src/server/app.ts', "  if (platform) app.route('/api', computerRoutes(platform.computers));",
  "  app.get('/api/maestro', (c) => c.json({ protocol: 1, service: 'maestro-opendots', model: config.model ?? null, reasoning: /^gpt-6/.test(config.model ?? '') ? 'high' : 'provider_default', upstream: 'c2569bb6a13a22e565cf3eb791c62267d06babb1' }));\n  if (platform) app.route('/api', computerRoutes(platform.computers));");
console.log('Maestro: OpenDots patch applied.');
