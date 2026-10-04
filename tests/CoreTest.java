package br.com.maestro;
import org.json.*;import java.net.*;import java.io.*;import java.util.concurrent.atomic.AtomicInteger;
public class CoreTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static int request(String token,String origin,String path,String payload)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL("http://127.0.0.1:19421"+path).openConnection();c.setRequestProperty("Authorization","Bearer "+token);if(origin!=null)c.setRequestProperty("Origin",origin);if(payload!=null){c.setDoOutput(true);c.setRequestMethod("POST");c.getOutputStream().write(payload.getBytes("UTF-8"));}int status=c.getResponseCode();c.disconnect();return status;}
 public static void main(String[]a)throws Exception{
  JSONObject plan=Plan.parse("```json\n{\"title\":\"Teste\",\"summary\":\"Resumo\",\"options\":[]}\n```");check(plan.getString("invite").equals("Resumo"),"invite fallback");
  boolean rejected=false;try{Plan.parse("{\"title\":\"x\",\"summary\":\"y\",\"options\":[{\"name\":\"Lugar\"}]}");}catch(Exception e){rejected=true;}check(rejected,"invalid option rejected");
  check(!Plan.schema().getBoolean("additionalProperties"),"strict schema");AtomicInteger calls=new AtomicInteger();
  try(Bridge bridge=new Bridge("secret-token",(path,body)->{calls.incrementAndGet();return new JSONObject().put("ok",true);})){check(request("wrong",null,"/health",null)==403,"bad token");check(calls.get()==0,"handler not called for bad token");check(request("secret-token","https://malicious.example","/health",null)==403,"browser origin rejected");check(request("secret-token",null,"/health",null)==200,"valid token");check(request("secret-token",null,"/note","{\"text\":\"hello\"}")==200,"valid body");check(calls.get()==2,"only authorized requests reached handler");}
  java.util.concurrent.atomic.AtomicLong now=new java.util.concurrent.atomic.AtomicLong(1000);
  Pairing pair=new Pairing(now::get);String first=pair.begin();check(pair.accepts("/setup/install.sh","Bearer "+first),"fresh capability");check(!pair.accepts("/note","Bearer "+first),"capability cannot write notes");now.addAndGet(900001);check(!pair.accepts("/setup/install.sh","Bearer "+first),"expiry");
  String ticket=pair.begin();
  try(Bridge b=new Bridge("permanent",pair,(path,body)->{if(path.equals("/setup/install.sh")){pair.consume();return new JSONObject().put("script","echo fixture");}return new JSONObject().put("ok",true);})){check(request(ticket,null,"/health",null)==403,"setup credential scoped");check(request(ticket,"https://example.com","/setup/install.sh",null)==403,"setup disallows browser");check(request(ticket,null,"/setup/install.sh",null)==200,"one-time delivery");check(request(ticket,null,"/setup/install.sh",null)==403,"replay rejected");check(request("permanent",null,"/health",null)==200,"bridge survives pairing");}
  JSONObject reasoning=Reasoning.request("Teste",true);check(reasoning.getString("model").equals("gpt-6-astra"),"current model");check(reasoning.getJSONObject("reasoning").getString("effort").equals("high"),"high reasoning");check(!reasoning.has("temperature")&&!reasoning.has("top_p"),"no incompatible parameters");check(reasoning.getJSONArray("tools").getJSONObject(0).getString("type").equals("web_search"),"web tool");check(!Reasoning.request("offline",false).has("tools"),"search disabled");
  rejected=false;try{Reasoning.output(new JSONObject().put("status","incomplete"));}catch(Exception e){rejected=true;}check(rejected,"incomplete response rejected");
  System.out.println("PASS: plan validation, real HTTP bridge, scoped one-time pairing, replay/origin rejection, reasoning request");
 }
}
