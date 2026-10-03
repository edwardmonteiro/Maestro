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
  System.out.println("PASS: strict plan validation and real HTTP bridge authentication/origin enforcement");
 }
}
