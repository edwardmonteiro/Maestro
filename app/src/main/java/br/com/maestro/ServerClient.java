package br.com.maestro;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/** OpenDots REST contract. No device data or provider credentials are read here. */
final class ServerClient {
 interface Transport { JSONObject request(String url,String token,JSONObject body)throws Exception; }
 final String origin;
 private final String token;
 private final Transport transport;
 ServerClient(String url,String token,Transport transport)throws Exception {
  this.origin=origin(url);this.token=token.trim();this.transport=transport;
  if(this.token.length()<24||this.token.length()>512||!this.token.matches("[!-~]+"))
   throw new IOException("Use o OWNER_TOKEN do servidor, com pelo menos 24 caracteres.");
 }
 static String origin(String value)throws Exception {
  URI uri=new URI(value.trim());
  if(!"https".equalsIgnoreCase(uri.getScheme())||uri.getHost()==null||uri.getRawUserInfo()!=null||
     uri.getRawQuery()!=null||uri.getRawFragment()!=null||
     !(uri.getPath()==null||uri.getPath().isEmpty()||uri.getPath().equals("/"))||
     uri.getPort()==0||uri.getPort()>65535)
   throw new IOException("Informe apenas o endereço HTTPS do servidor, sem /api, senha ou parâmetros.");
  return new URI("https",null,uri.getHost().toLowerCase(Locale.ROOT),uri.getPort(),null,null,null).toASCIIString();
 }
 private JSONObject call(String path,JSONObject body)throws Exception {
  return transport.request(origin+path,token,body);
 }
 JSONObject metadata()throws Exception {
  JSONObject m=call("/api/maestro",null);
  if(!"maestro-opendots".equals(m.optString("service"))||m.optInt("protocol")!=1)
   throw new IOException("Este endereço não é um servidor Maestro compatível.");
  return m;
 }
 JSONObject workspace()throws Exception {return call("/api/workspace",null);}
 JSONObject state()throws Exception {return call("/api/state",null);}
 JSONObject detail(String id)throws Exception{return call("/api/tasks/"+id(id),null);}
 JSONObject action(String id,String action)throws Exception {
  if(!action.equals("pause")&&!action.equals("cancel")&&!action.equals("run"))throw new IOException("Ação inválida.");
  return call("/api/tasks/"+id(id)+"/actions",new JSONObject().put("action",action));
 }
 JSONObject create(String dotId,String prompt,int intervalSeconds)throws Exception {
  String p=prompt.trim();
  if(p.length()<3||p.length()>4000)throw new IOException("Escreva uma tarefa entre 3 e 4.000 caracteres.");
  if(intervalSeconds!=0&&intervalSeconds!=3600&&intervalSeconds!=86400)throw new IOException("Intervalo inválido.");
  JSONObject w=workspace();
  JSONArray missing=w.getJSONObject("setup").getJSONArray("missing");
  if(missing.length()>0)throw new IOException("Falta configurar no servidor: "+missing.toString());
  JSONObject settings=state().getJSONObject("settings");
  if(settings.optBoolean("paused")||!settings.optBoolean("researchAllowed"))throw new IOException("O servidor está pausado ou com pesquisas desativadas. Confira o painel.");
  boolean found=false;JSONArray dots=w.getJSONArray("dots");
  for(int i=0;i<dots.length();i++)if(dotId.equals(dots.getJSONObject(i).optString("id")))found=true;
  if(!found)throw new IOException("Atualize a lista de assistentes.");
  JSONObject conversation=call("/api/conversations",new JSONObject().put("dotId",dotId).put("title",p.substring(0,Math.min(100,p.length()))));
  return call("/api/tasks",new JSONObject().put("prompt",p).put("threadId",conversation.getString("id"))
    .put("intervalSeconds",intervalSeconds==0?JSONObject.NULL:intervalSeconds));
 }
 private static String id(String value)throws IOException {
  if(value==null||!value.matches("[a-zA-Z0-9_-]{1,128}"))throw new IOException("Identificador inválido.");
  return value;
 }
 static String status(String value){
  switch(value){case "queued":return "Na fila";case "running":return "Executando";case "paused":return "Pausada";
   case "completed":return "Concluída";case "failed":return "Falhou";case "cancelled":return "Cancelada";default:return value;}
 }
}
