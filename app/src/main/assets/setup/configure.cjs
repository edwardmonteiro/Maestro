'use strict';
const fs = require('fs'), path = require('path');
function config(bundle, home) {
 const root = path.join(home, '.maestro');
 return {
  gateway: {mode:'local', bind:'loopback', port:18789, auth:{mode:'token',token:bundle.gateway}, http:{endpoints:{chatCompletions:{enabled:true}}}},
  models:{mode:'merge',providers:{openai:{agentRuntime:{id:'openclaw'},baseUrl:'https://api.openai.com/v1',api:'openai-responses',apiKey:bundle.key,models:[{id:'gpt-6-astra',name:'GPT-6 Astra',reasoning:true,input:['text'],contextWindow:1048576,maxTokens:25000}]}}},
  agents:{defaults:{maxConcurrent:1,model:{primary:'openai/gpt-6-astra',fallbacks:[]},thinkingDefault:'high',workspace:path.join(root,'workspace')}},
  plugins:{allow:['openai','maestro-phone'],load:{paths:[path.join(root,'plugin')]},entries:{'maestro-phone':{enabled:true,config:{bridgeToken:bundle.bridge}}}},
  tools:{allow:['maestro_status','maestro_save_note','maestro_request_action','maestro_local_think'],elevated:{enabled:false}},
  logging:{redactSensitive:'tools'}
 };
}
function install(bundle, home) {
 const root=path.join(home,'.maestro'), state=path.join(home,'.openclaw-maestro');
 for(const dir of [root,state,path.join(root,'plugin'),path.join(root,'workspace')]) {fs.mkdirSync(dir,{recursive:true,mode:0o700});fs.chmodSync(dir,0o700);}
 for(const name of ['index.js','package.json','openclaw.plugin.json']) fs.writeFileSync(path.join(root,'plugin',name),bundle.plugin[name],{mode:0o600});
 const target=path.join(state,'openclaw.json');
 if(fs.existsSync(target)){const backup=target+'.backup-'+Date.now();fs.copyFileSync(target,backup);fs.chmodSync(backup,0o600);}
 fs.writeFileSync(target+'.new',JSON.stringify(config(bundle,home),null,2),{mode:0o600});fs.renameSync(target+'.new',target);fs.chmodSync(target,0o600);
 // Commands are fixed application code. No user-provided value is interpolated into shell.
 fs.writeFileSync(path.join(root,'start.sh'),`#!/data/data/com.termux/files/usr/bin/bash
set -e
export PATH="$HOME/.openclaw-android/bin:$HOME/.openclaw-android/node/bin:$HOME/.local/bin:$PATH"
export TMPDIR="$PREFIX/tmp" TMP="$PREFIX/tmp" TEMP="$PREFIX/tmp" OA_GLIBC=1
if [ -f "$HOME/.bashrc" ]; then source "$HOME/.bashrc" || true; fi
exec openclaw --profile maestro gateway --port 18789
`,{mode:0o700});
 const props=path.join(home,'.termux','termux.properties');fs.mkdirSync(path.dirname(props),{recursive:true});
 const prior=fs.existsSync(props)?fs.readFileSync(props,'utf8'):'';
 fs.writeFileSync(props,prior.replace(/^\s*allow-external-apps\s*=.*$/mg,'')+'\nallow-external-apps=true\n',{mode:0o600});
}
module.exports={config,install};
if(require.main===module){const file=process.argv[2];const bundle=JSON.parse(fs.readFileSync(file,'utf8'));install(bundle,process.env.HOME);fs.writeFileSync(file,'');}
