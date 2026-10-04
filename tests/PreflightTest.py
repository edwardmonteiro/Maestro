"""Exercise dependency repair in a shell with broken-curl fixtures; never runs host apt."""
from pathlib import Path
import os, subprocess, tempfile
script=(Path(__file__).resolve().parents[1]/'app/src/main/assets/setup/preflight.sh').read_text()
for scenario in ('partial_upgrade','needs_reinstall','apt_failure'):
 with tempfile.TemporaryDirectory(prefix='maestro-preflight-') as folder:
  root=Path(folder); trace=root/'trace'; ready=root/'curl-ready'
  apt=root/'apt-get';apt.write_text('''#!/bin/bash
printf 'apt %s\\n' "$*" >> "$TRACE_FILE"
if [ "$SCENARIO" = apt_failure ]; then exit 19; fi
case "$*" in
 *--reinstall*) touch "$READY_FILE";;
 *dist-upgrade*) if [ "$SCENARIO" != needs_reinstall ]; then touch "$READY_FILE"; fi;;
esac
exit 0
''');apt.chmod(0o755)
  curl=root/'curl';curl.write_text('''#!/bin/bash
printf 'curl %s\\n' "$*" >> "$TRACE_FILE"
test -f "$READY_FILE"
''');curl.chmod(0o755)
  env={**os.environ,'PATH':folder+':/usr/bin:/bin','PREFIX':'/data/data/com.termux/files/usr','TRACE_FILE':str(trace),'READY_FILE':str(ready),'SCENARIO':scenario}
  result=subprocess.run(['/bin/bash','-c','(\nset -euo pipefail\n'+script+'\nprintf "download\\n" >> "$TRACE_FILE"\n)'],env=env,capture_output=True,text=True)
  lines=trace.read_text().splitlines()
  if scenario=='apt_failure':
   assert result.returncode==19,(result.returncode,result.stderr)
   assert lines==['apt update'],lines
  else:
   assert result.returncode==0,result.stderr
   upgrade=next(i for i,s in enumerate(lines) if 'dist-upgrade' in s)
   firstcurl=next(i for i,s in enumerate(lines) if s.startswith('curl '))
   assert upgrade<firstcurl,lines
   assert lines[-1]=='download',lines
   assert any('--reinstall' in s for s in lines)==(scenario=='needs_reinstall'),lines
print('PASS: broken curl repaired before fetch, reinstall fallback, apt failure stops download')
