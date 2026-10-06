// Local integration check against the actual compiled server, no provider accounts.
import assert from 'node:assert/strict';
import { randomBytes } from 'node:crypto';
import { spawn } from 'node:child_process';
import { once } from 'node:events';
import { mkdtemp, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { createServer } from 'node:net';
import { setTimeout as delay } from 'node:timers/promises';

const root = resolve(process.argv[2]);
const folder = await mkdtemp(join(tmpdir(), 'maestro-server-smoke-'));
const allocator = createServer();
allocator.listen(0, '127.0.0.1');
await once(allocator, 'listening');
const port = allocator.address().port;
await new Promise((resolve) => allocator.close(resolve));
const base = `http://127.0.0.1:${port}`;
const token = randomBytes(32).toString('hex');
let child;
async function start() {
  child = spawn(process.execPath, ['dist/server/server/index.js'], {
    cwd: root,
    env: { PATH: process.env.PATH, HOST: '127.0.0.1', PORT: String(port), NODE_ENV: 'production',
      DATABASE_PATH: join(folder, 'test.sqlite'), OWNER_TOKEN: token, OPENAI_MODEL: 'gpt-6-astra' },
    stdio: ['ignore', 'ignore', 'pipe'],
  });
  let error = '';
  child.stderr.on('data', (chunk) => { error += chunk; });
  for (let i = 0; i < 100; i++) {
    if (child.exitCode !== null) throw new Error(`Server failed to start: ${error.slice(0, 500)}`);
    try { if ((await fetch(`${base}/healthz`)).ok) return; } catch {}
    await delay(100);
  }
  throw new Error('Server startup timed out.');
}
async function stop() {
  if (!child || child.exitCode !== null) return;
  const done = once(child, 'exit'); child.kill('SIGTERM');
  const timer = setTimeout(() => child.kill('SIGKILL'), 5000);
  await done; clearTimeout(timer);
}
function call(path, body, extra = {}) {
  return fetch(base + path, { method: body === undefined ? 'GET' : 'POST',
    headers: { Authorization: `Bearer ${token}`, ...(body === undefined ? {} : {'Content-Type':'application/json'}), ...extra },
    ...(body === undefined ? {} : {body: JSON.stringify(body)}) });
}
try {
  await start();
  assert.equal((await fetch(`${base}/api/maestro`)).status, 401);
  assert.equal((await fetch(`${base}/api/state`)).status, 401);
  const metadata = await (await call('/api/maestro')).json();
  assert.equal(metadata.service, 'maestro-opendots'); assert.equal(metadata.reasoning, 'high');
  const workspace = await (await call('/api/workspace')).json();
  assert.deepEqual(workspace.setup.missing, ['INTELLIGENCE_API_KEY', 'OPENAI_API_KEY']);
  assert.equal(workspace.dots[0].name, 'Maestro');
  assert.equal((await call('/api/tasks', {prompt:'A task without credentials'})).status, 503);
  assert.equal((await call('/api/memories', {text:'Test'}, {Origin:'https://other.example'})).status, 403);
  assert.equal((await call('/api/memories', {text:'Integration fixture'})).status, 201);
  await stop(); await start();
  const state = await (await call('/api/state')).json();
  assert.equal(state.memories[0].text, 'Integration fixture');
  assert.equal(state.tasks.length, 0);
  assert.equal((await fetch(base)).status, 200);
  console.log('Smoke passed: real HTTP authentication, setup gating, cross-origin rejection, SQLite restart persistence and frontend serving. No model calls.');
} finally { await stop(); await rm(folder, {recursive:true, force:true}); }
