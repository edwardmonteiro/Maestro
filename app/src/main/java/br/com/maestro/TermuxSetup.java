package br.com.maestro;
final class TermuxSetup {
 static boolean canAutomate(boolean permission,boolean bootstrapConfirmed){return permission&&bootstrapConfirmed;}
 static String command(String preflight,String ticket){
  if(!ticket.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("Invalid pairing ticket");
  return "(\nset -euo pipefail\n"+preflight+"\ncurl --max-time 20 -fsS -H 'Authorization: Bearer "+ticket+"' http://127.0.0.1:19421/setup/install.sh | bash\n)";
 }
}
