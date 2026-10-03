package br.com.maestro;
import java.net.*;import java.io.*;import org.json.*;import java.util.concurrent.atomic.AtomicBoolean;
final class Net {
 static final class Job {final AtomicBoolean cancelled=new AtomicBoolean(false);volatile HttpURLConnection active; void cancel(){cancelled.set(true);if(active!=null)active.disconnect();}void check()throws IOException{if(cancelled.get())throw new IOException("Cancelado");}}
 static JSONObject json(String url,String token,JSONObject body,Job job)throws Exception{
  job.check();URL u=new URL(url);boolean local=u.getHost().equals("127.0.0.1")||u.getHost().equals("localhost");if(!u.getProtocol().equals("https")&&!local)throw new IOException("Conexao insegura bloqueada");
  HttpURLConnection c=(HttpURLConnection)u.openConnection();job.active=c;c.setConnectTimeout(15000);c.setReadTimeout(120000);c.setInstanceFollowRedirects(false);c.setRequestMethod(body==null?"GET":"POST");c.setRequestProperty("Accept","application/json");if(!token.isEmpty())c.setRequestProperty("Authorization","Bearer "+token);
  try{if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(OutputStream o=c.getOutputStream()){o.write(body.toString().getBytes("UTF-8"));}}
  int code=c.getResponseCode();InputStream input=code<400?c.getInputStream():c.getErrorStream();String s=read(input,2_000_000);job.check();if(code>=400){String message="";try{message=new JSONObject(s).optJSONObject("error").optString("message");}catch(Exception ignored){}if(message.length()>350)message=message.substring(0,350);throw new IOException("HTTP "+code+". "+message);}return new JSONObject(s);
  }finally{c.disconnect();job.active=null;}
 }
 static String read(InputStream i,int cap)throws IOException{if(i==null)return "";try(InputStream in=i;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[]b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>cap)throw new IOException("Resposta excedeu limite");out.write(b,0,n);}return out.toString("UTF-8");}}
}
