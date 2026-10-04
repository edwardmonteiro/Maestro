// Optional integration: OPENCLAW_CLI=/path/to/openclaw.mjs node tests/GatewayIntegration.mjs
// Real OpenClaw + real plugin, fake Android bridge; no live OpenAI request.
import assert from 'node:assert/strict';
import {createRequire} from 'node:module';
import {createServer} from 'node:http';
import {spawn,spawnSync} from 'node:child_process';
import {mkdtempSync,readFileSync,writeFileSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join,resolve} from 'node:path';
import {fileURLToPath} from 'node:url';
const root=fileURLToPath(new URL('../',import.meta.url));
const cli=process.env.OPENCLAW_CLI;if(!cli)throw Error('Set OPENCLAW_CLI to the real CLI path');
const {install}=createRequire(import.meta.url)('../app/src/main/assets/setup/configure.cjs');
const home=mkdtempSync(join(tmpdir(),'maestro-gateway-'));
const bundle={key:'fixture-only-not-a-key',gateway:'gateway-fixture-for-integration',bridge:'bridge-fixture-at-least-thirty-two-characters',plugin:{}};
for(const f of ['index.js','package.json','openclaw.plugin.json'])bundle.plugin[f]=readFileSync(join(root,'openclaw-plugin',f),'utf8');
install(bundle,home);
let capturedRequest;
const provider=createServer(async(req,res)=>{let raw='';for await(const chunk of req)raw+=chunk;capturedRequest={path:req.url,body:JSON.parse(raw)};res.writeHead(400,{'Content-Type':'application/json'});res.end(JSON.stringify({error:{message:'Intentional fixture stop: no live model call',type:'invalid_request_error',code:'fixture_stop'}}));});
await new Promise(r=>provider.listen(0,'127.0.0.1',r));
const cfgPath=join(home,'.openclaw-maestro','openclaw.json');const cfg=JSON.parse(readFileSync(cfgPath));
cfg.models.providers.openai.baseUrl='http://127.0.0.1:'+provider.address().port+'/v1';writeFileSync(cfgPath,JSON.stringify(cfg));
const env={...process.env,OPENCLAW_STATE_DIR:join(home,'.openclaw-maestro'),OPENCLAW_CONFIG_PATH:join(home,'.openclaw-maestro','openclaw.json'),OPENCLAW_NO_AUTO_UPDATE:'1'};
const validation=spawnSync(process.execPath,[resolve(cli),'--profile','maestro','config','validate'],{env,encoding:'utf8',timeout:60000});
if(validation.status!==0){provider.close();throw Error(validation.stdout+'\n'+validation.stderr);}
console.log('PASS: real OpenClaw accepts generated config');
let bridgeCalls=0,proc,logs='';
const bridge=createServer((req,res)=>{if(req.headers.authorization!=='Bearer '+bundle.bridge){res.writeHead(403);res.end('{}');return;}assert.equal(req.url,'/health');bridgeCalls++;res.setHeader('Content-Type','application/json');res.end(JSON.stringify({ok:true,service:'maestro',version:'fixture',model_ready:false}));});
await new Promise(r=>bridge.listen(19421,'127.0.0.1',r));
try{
 proc=spawn(process.execPath,[resolve(cli),'--profile','maestro','gateway','--port','18789'],{env,stdio:['ignore','pipe','pipe']});
 for(const stream of [proc.stdout,proc.stderr])stream.on('data',b=>logs=(logs+b).slice(-16000));
 const invoke=async(tool,token=bundle.gateway)=>fetch('http://127.0.0.1:18789/tools/invoke',{method:'POST',headers:{Authorization:'Bearer '+token,'Content-Type':'application/json'},body:JSON.stringify({tool,args:{}}),signal:AbortSignal.timeout(3000)});
 let result,lastResponse='';
 for(let i=0;i<120;i++){if(proc.exitCode!==null)throw Error('Gateway exited: '+logs);try{const r=await invoke('maestro_status');if(r.ok){result=await r.json();break;}lastResponse=r.status+': '+await r.text();}catch(e){lastResponse=e.message;}if(i%20===19)console.log('Waiting for Gateway: '+lastResponse+' '+logs.slice(-1500));await new Promise(r=>setTimeout(r,1000));}
 assert(result,'Gateway/plugin never became ready: '+lastResponse+' '+logs);
 assert.equal(result.ok,true);assert.equal(result.result.details.service,'maestro');assert(bridgeCalls>0);
 assert.equal((await invoke('maestro_status','wrong-fixture')).status,401);
 assert([403,404].includes((await invoke('exec')).status),'shell tool must be unavailable');
 console.log('PASS: real Gateway authenticates, loads plugin, calls bridge and blocks shell');
 try{await fetch('http://127.0.0.1:18789/v1/chat/completions',{method:'POST',headers:{Authorization:'Bearer '+bundle.gateway,'Content-Type':'application/json'},body:JSON.stringify({model:'openclaw',stream:false,messages:[{role:'user',content:'Fixture probe, say hello.'}]}),signal:AbortSignal.timeout(25000)});}catch{}
 assert(capturedRequest,'No provider request emitted: '+logs);
 assert.equal(capturedRequest.path,'/v1/responses');assert.equal(capturedRequest.body.model,'gpt-6-astra');assert.equal(capturedRequest.body.reasoning.effort,'high');
 console.log('PASS: real agent emits Responses request with gpt-6-astra and reasoning high (mock provider rejects intentionally)');
}finally{if(proc&&proc.exitCode===null){proc.kill('SIGTERM');await Promise.race([new Promise(r=>proc.once('exit',r)),new Promise(r=>setTimeout(r,4000))]);if(proc.exitCode===null)proc.kill('SIGKILL');}bridge.close();provider.close();}
