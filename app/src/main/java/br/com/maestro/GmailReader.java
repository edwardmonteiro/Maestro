package br.com.maestro;

import javax.mail.*;
import javax.mail.internet.*;
import com.sun.mail.imap.*;
import org.json.*;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

/** IMAP only. No SMTP implementation and no mutating folder operations. */
final class GmailReader implements AutoCloseable {
 private final Store store;private final IMAPFolder inbox;
 static Properties properties(){Properties p=new Properties();
  p.setProperty("mail.imaps.ssl.enable","true");p.setProperty("mail.imaps.ssl.checkserveridentity","true");
  p.setProperty("mail.imaps.connectiontimeout","15000");p.setProperty("mail.imaps.timeout","20000");p.setProperty("mail.imaps.writetimeout","20000");
  p.setProperty("mail.imaps.peek","true");p.setProperty("mail.imaps.partialfetch","true");p.setProperty("mail.imaps.fetchsize","8192");
  p.setProperty("mail.imaps.connectionpoolsize","1");return p;
 }
 GmailReader(String email,String password)throws Exception{
  if(!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")||!password.matches("[A-Za-z0-9]{16}"))throw new IllegalArgumentException("Informe o e-mail e a senha de app de 16 caracteres.");
  Session session=Session.getInstance(properties());session.setDebug(false);
  store=new IMAPSSLStore(session,new URLName("imaps", "imap.gmail.com",993,null,null,null));
  try{store.connect("imap.gmail.com",993,email,password);inbox=(IMAPFolder)store.getFolder("INBOX");inbox.open(Folder.READ_ONLY);}catch(Exception e){try{store.close();}catch(Exception ignored){}throw e;}
 }
 JSONArray latest()throws Exception{
  JSONArray out=new JSONArray();int count=inbox.getMessageCount();if(count==0)return out;
  Message[] messages=inbox.getMessages(Math.max(1,count-19),count);FetchProfile profile=new FetchProfile();profile.add(FetchProfile.Item.ENVELOPE);profile.add(FetchProfile.Item.FLAGS);profile.add(UIDFolder.FetchProfileItem.UID);inbox.fetch(messages,profile);
  for(int i=messages.length-1;i>=0;i--){Message m=messages[i];Address[] from=m.getFrom();Date date=m.getReceivedDate();if(date==null)date=m.getSentDate();
   out.put(new JSONObject().put("id",inbox.getUID(m)).put("validity",inbox.getUIDValidity()).put("subject",limit(m.getSubject(),300)).put("from",limit(from==null||from.length==0?"":from[0].toString(),300)).put("replyTo",replyTo(m)).put("time",date==null?0:date.getTime()).put("cachedAt",System.currentTimeMillis()).put("unread",!m.isSet(Flags.Flag.SEEN)));
  }return out;
 }
 JSONObject read(long uid,long validity)throws Exception{
  if(inbox.getUIDValidity()!=validity)throw new IOException("A caixa mudou. Atualize a lista antes de abrir.");
  Message m=inbox.getMessageByUID(uid);if(m==null)throw new IOException("Mensagem não está mais na caixa de entrada.");
  return new JSONObject().put("text",plain(m,0)).put("replyTo",replyTo(m));
 }
 static String replyTo(Message m)throws Exception{Address[] a=m.getReplyTo();return a!=null&&a.length>0&&a[0] instanceof InternetAddress?((InternetAddress)a[0]).getAddress():"";}
 static String limit(String s,int n){return s==null?"":s.substring(0,Math.min(n,s.length()));}
 static String plain(Part part,int depth)throws Exception{
  if(depth>6||Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()))return "";
  if(part.isMimeType("text/plain")||part.isMimeType("text/html")){
   Charset charset=Charset.forName("UTF-8");try{String name=new ContentType(part.getContentType()).getParameter("charset");if(name!=null)charset=Charset.forName(name);}catch(Exception ignored){}
   ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=part.getInputStream()){byte[] b=new byte[2048];int n;while(bytes.size()<12000&&(n=in.read(b,0,Math.min(b.length,12000-bytes.size())))!=-1)bytes.write(b,0,n);}
   String s=new String(bytes.toByteArray(),charset);if(part.isMimeType("text/html"))s=s.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>","").replaceAll("(?i)<br\\s*/?>|</p>|</div>","\n").replaceAll("<[^>]+>"," ").replace("&nbsp;"," ").replace("&lt;","<").replace("&gt;",">").replace("&amp;","&").replace("&quot;","\"");
   return limit(s,6000);
  }
  if(part.isMimeType("multipart/*")){
   Multipart mp=(Multipart)part.getContent();int count=Math.min(mp.getCount(),12);
   if(part.isMimeType("multipart/alternative")){for(int i=0;i<count;i++)if(mp.getBodyPart(i).isMimeType("text/plain"))return plain(mp.getBodyPart(i),depth+1);}
   StringBuilder out=new StringBuilder();for(int i=0;i<count&&out.length()<6000;i++){String s=plain(mp.getBodyPart(i),depth+1);if(!s.isEmpty()){out.append(s).append('\n');if(part.isMimeType("multipart/alternative"))break;}}return limit(out.toString(),6000);
  }return "";
 }
 @Override public void close(){try{if(inbox.isOpen())inbox.close(false);}catch(Exception ignored){}try{store.close();}catch(Exception ignored){}}
}
