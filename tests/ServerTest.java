package br.com.maestro;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

public final class ServerTest {
 interface Checked {void run()throws Exception;}
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 static void rejects(Checked f)throws Exception{try{f.run();throw new AssertionError("Expected rejection");}catch(java.io.IOException expected){}}
 public static void main(String[] args)throws Exception {
  for(String bad:new String[]{"http://server.test","https://user:secret@server.test","https://server.test/api","https://server.test?token=x","https://server.test/#x","file:///tmp/a","https://server.test:0","https://server.test:99999"})rejects(()->ServerClient.origin(bad));
  check(ServerClient.origin(" https://SERVER.test/ ").equals("https://server.test"),"Canonical origin");
  final String token="a".repeat(40);List<String> requests=new ArrayList<>();List<JSONObject> bodies=new ArrayList<>();
  ServerClient.Transport fixture=(url,key,body)->{
   check(key.equals(token),"Only server token used");String path=url.replace("https://server.test","");requests.add(path);if(body!=null)bodies.add(body);
   switch(path){
    case "/api/maestro":return new JSONObject("{\"service\":\"maestro-opendots\",\"protocol\":1}");
    case "/api/workspace":return new JSONObject("{\"setup\":{\"missing\":[]},\"dots\":[{\"id\":\"dot-1\"}]}");
    case "/api/state":return new JSONObject("{\"settings\":{\"paused\":false,\"researchAllowed\":true},\"tasks\":[]}");
    case "/api/conversations":return new JSONObject("{\"id\":\"thread-1\"}");
    case "/api/tasks":return new JSONObject("{\"id\":\"task-1\",\"status\":\"queued\"}");
    case "/api/tasks/task-1/actions":return new JSONObject().put("status",body.getString("action"));
    default:throw new AssertionError(path);
   }
  };
  ServerClient client=new ServerClient("https://server.test",token,fixture);client.metadata();requests.clear();
  check(client.create("dot-1","Pesquise fontes",86400).getString("status").equals("queued"),"Server receipt");
  check(requests.equals(List.of("/api/workspace","/api/state","/api/conversations","/api/tasks")),"Dependency order");
  check(bodies.get(1).getString("threadId").equals("thread-1"),"Task bound to returned thread");
  check(bodies.get(1).getInt("intervalSeconds")==86400,"Explicit recurrence");
  check(bodies.get(1).length()==3,"No automatic phone data");
  client.action("task-1","pause");
  rejects(()->client.action("../admin","pause"));rejects(()->client.action("task-1","delete"));
  rejects(()->client.create("dot-1","ok",0));rejects(()->client.create("missing","Valid request",0));
  rejects(()->new ServerClient("https://server.test","short",fixture));
  rejects(()->new ServerClient("https://server.test",token+"\nX: injected",fixture));
  ServerClient blocked=new ServerClient("https://server.test",token,(u,k,b)->new JSONObject("{\"setup\":{\"missing\":[\"OPENAI_API_KEY\"]}}"));
  rejects(()->blocked.create("dot-1","Valid request",0));
  ServerClient wrong=new ServerClient("https://server.test",token,(u,k,b)->new JSONObject("{\"service\":\"other\",\"protocol\":1}"));rejects(wrong::metadata);
  int[] attempts={0};ServerClient uncertain=new ServerClient("https://server.test",token,(u,k,b)->{if(u.endsWith("/api/tasks")){attempts[0]++;throw new java.io.IOException("Network lost");}return fixture.request(u,k,b);});
  rejects(()->uncertain.create("dot-1","Run this once",0));check(attempts[0]==1,"Never retry task POST automatically");
  HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);int[] redirected={0};
  server.createContext("/redirect",e->{e.getResponseHeaders().add("Location","/leak");e.sendResponseHeaders(302,-1);e.close();});
  server.createContext("/leak",e->{redirected[0]++;e.sendResponseHeaders(200,2);e.getResponseBody().write("{}".getBytes());e.close();});
  server.createContext("/error",e->{byte[] data=("{\"error\":\"Denied "+token+"\"}").getBytes(StandardCharsets.UTF_8);e.sendResponseHeaders(401,data.length);e.getResponseBody().write(data);e.close();});
  server.start();try{String base="http://127.0.0.1:"+server.getAddress().getPort();
   rejects(()->Net.json(base+"/redirect",token,null,new Net.Job()));check(redirected[0]==0,"Never forward token on redirect");
   try{Net.json(base+"/error",token,null,new Net.Job());throw new AssertionError();}catch(java.io.IOException e){check(e.getMessage().contains("Denied")&&!e.getMessage().contains(token),"Readable errors without token echo");}
  }finally{server.stop(0);}
  System.out.println("ServerTest passed: URL/token boundaries, task contract, recurrence, readiness, no retry, redirect/auth errors.");
 }
}
