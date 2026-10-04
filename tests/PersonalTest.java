package br.com.maestro;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

public final class PersonalTest {
 static void check(boolean condition,String name){if(!condition)throw new AssertionError(name);}
 static JSONObject event(long time,int type,String pkg)throws Exception{return new JSONObject().put("time",time).put("type",type).put("package",pkg);}
 static long total(JSONArray sessions)throws Exception{long ms=0;for(int i=0;i<sessions.length();i++){JSONObject s=sessions.getJSONObject(i);ms+=s.getLong("end")-s.getLong("start");}return ms;}
 public static void main(String[] args)throws Exception{
  JSONArray events=new JSONArray().put(event(1000,1,"a")).put(event(2000,1,"a")).put(event(3000,2,"other")).put(event(6000,1,"b")).put(event(10000,16,"android"));
  JSONArray result=UsageTimeline.sessions(events,0,20000);
  check(result.length()==2&&total(result)==9000,"duplicate resumes, irrelevant pauses and screen-off cannot inflate usage");
  check(total(UsageTimeline.sessions(events,4000,8000))==4000,"clip sessions to selected day and observation end");
  JSONArray shortSwitch=new JSONArray().put(event(1000,1,"a")).put(event(3000,2,"a")).put(event(3500,1,"a")).put(event(5000,2,"a"));
  check(UsageTimeline.sessions(shortSwitch,0,6000).length()==1,"merge adjacent activities in same app");
  JSONArray stopped=new JSONArray().put(event(1000,1,"a")).put(event(4000,26,""));
  check(total(UsageTimeline.sessions(stopped,0,300000))==3000,"collection stop does not extend an active session");
  check(UsageTimeline.sessions(new JSONArray().put(event(2,2,"a")),0,100).length()==0,"missing resume cannot invent session");
  Session session=Session.getInstance(new Properties());
  MimeMessage plain=new MimeMessage(session,new ByteArrayInputStream("Content-Type: text/plain; charset=UTF-8\r\n\r\nOlá Edward".getBytes(StandardCharsets.UTF_8)));
  check(GmailReader.plain(plain,0).equals("Olá Edward"),"UTF-8 mail text");
  String raw="MIME-Version: 1.0\r\nContent-Type: multipart/mixed; boundary=x\r\n\r\n--x\r\nContent-Type: multipart/alternative; boundary=y\r\n\r\n--y\r\nContent-Type: text/plain\r\n\r\nVisible text\r\n--y\r\nContent-Type: text/html\r\n\r\n<b>Duplicate html</b>\r\n--y--\r\n--x\r\nContent-Type: text/plain\r\nContent-Disposition: attachment; filename=private.txt\r\n\r\nATTACHMENT_SECRET\r\n--x--\r\n";
  String parsed=GmailReader.plain(new MimeMessage(session,new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8))),0);
  check(parsed.contains("Visible text")&&!parsed.contains("Duplicate")&&!parsed.contains("SECRET"),"prefer plain alternative and skip attachment");
  MimeBodyPart html=new MimeBodyPart(new ByteArrayInputStream("Content-Type: text/html; charset=UTF-8\r\n\r\n<style>hidden</style><script>bad()</script><p>Hello &amp; bye</p><img src='https://example.com/pixel'>".getBytes(StandardCharsets.UTF_8)));
  String htmlText=GmailReader.plain(html,0);check(htmlText.contains("Hello & bye")&&!htmlText.contains("bad()")&&!htmlText.contains("https:"),"HTML is inert text, no tracking pixels");
  MimeBodyPart big=new MimeBodyPart();big.setText("x".repeat(30000));check(GmailReader.plain(big,0).length()==6000,"bounded email body");
  Properties props=GmailReader.properties();check(props.getProperty("mail.imaps.ssl.checkserveridentity").equals("true")&&props.getProperty("mail.imaps.peek").equals("true"),"TLS hostname verification and non-mutating reads");
  System.out.println("PersonalTest: sessions, stop boundaries, MIME, attachment exclusion, HTML, limits and TLS passed");
 }
}
