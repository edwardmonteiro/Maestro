package br.com.maestro;
import java.net.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.security.MessageDigest;import java.util.*;import java.util.concurrent.*;import org.json.*;
final class Bridge implements AutoCloseable {
 interface Handler{JSONObject handle(String path,JSONObject payload)throws Exception;}
 private final ServerSocket server;private volatile boolean running=true;private final ExecutorService clients=Executors.newFixedThreadPool(2);private final Semaphore slots=new Semaphore(2);
 Bridge(String token,Handler handler)throws IOException{this(token,new Pairing(),handler);}
 Bridge(String token,Pairing pairing,Handler handler)throws IOException{
  server=new ServerSocket();server.setReuseAddress(true);server.bind(new InetSocketAddress("127.0.0.1",19421));
  Thread t=new Thread(()->{while(running){try{Socket s=server.accept();if(!slots.tryAcquire()){s.close();continue;}clients.execute(()->{try{serve(s,token,pairing,handler);}finally{slots.release();}});}catch(Exception e){if(!running)break;}}},"maestro-bridge");t.setDaemon(true);t.start();
 }
 static String line(InputStream in)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();int v;while((v=in.read())!=-1){if(v=='\n')break;if(v!='\r')b.write(v);if(b.size()>8192)throw new IOException("header too long");}return b.toString("UTF-8");}
 private void serve(Socket socket,String token,Pairing pairing,Handler handler){try(Socket s=socket){s.setSoTimeout(10000);InputStream in=s.getInputStream();String[] request=line(in).split(" ");if(request.length!=3)return;Map<String,String> headers=new HashMap<>();String l;int h=0;while(!(l=line(in)).isEmpty()){if(++h>32)throw new IOException("headers");int colon=l.indexOf(':');if(colon>0)headers.put(l.substring(0,colon).toLowerCase(Locale.ROOT),l.substring(colon+1).trim());}
  if(headers.containsKey("origin")||(!pairing.accepts(request[1],headers.getOrDefault("authorization",""))&&!MessageDigest.isEqual(("Bearer "+token).getBytes(StandardCharsets.UTF_8),headers.getOrDefault("authorization","").getBytes(StandardCharsets.UTF_8)))){reply(s,403,new JSONObject().put("error","unauthorized"));return;}
  if(!request[0].equals("GET")&&!request[0].equals("POST")){reply(s,405,new JSONObject().put("error","method"));return;}
  int n=Integer.parseInt(headers.getOrDefault("content-length","0"));if(n<0||n>65536||headers.containsKey("transfer-encoding")){reply(s,413,new JSONObject().put("error","size"));return;}
  byte[] bytes=new byte[n];new DataInputStream(in).readFully(bytes);JSONObject body=n==0?new JSONObject():new JSONObject(new String(bytes,StandardCharsets.UTF_8));
  try{JSONObject result=handler.handle(request[1],body);if(request[1].equals("/setup/install.sh"))raw(s,200,result.getString("script"),"text/plain; charset=utf-8");else reply(s,200,result);}catch(Exception e){reply(s,400,new JSONObject().put("error",e.getMessage()==null?"request failed":e.getMessage()));}
 }catch(Exception ignored){} }
 static void reply(Socket s,int code,JSONObject object)throws IOException{raw(s,code,object.toString(),"application/json; charset=utf-8");}
 static void raw(Socket s,int code,String body,String type)throws IOException{byte[] b=body.getBytes(StandardCharsets.UTF_8);OutputStream out=s.getOutputStream();out.write(("HTTP/1.1 "+code+" Result\r\nContent-Type: "+type+"\r\nCache-Control: no-store\r\nConnection: close\r\nContent-Length: "+b.length+"\r\n\r\n").getBytes(StandardCharsets.US_ASCII));out.write(b);out.flush();}
 public void close(){running=false;try{server.close();}catch(IOException ignored){}clients.shutdownNow();}
}
