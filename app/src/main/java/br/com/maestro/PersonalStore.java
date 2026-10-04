package br.com.maestro;

import android.content.Context;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Private encrypted stores; never accessible from the HTTP bridge. */
final class PersonalStore {
 static final long WEEK=7L*24*60*60*1000;
 static final Object LOCK=new Object();
 private final Context context;
 PersonalStore(Context c){context=c.getApplicationContext();}
 private AtomicFile file(String name){
  if(!name.matches("notifications|usage|mail"))throw new IllegalArgumentException("Unknown personal store");
  return new AtomicFile(new File(context.getFilesDir(),"personal-"+name+".enc"));
 }
 JSONArray read(String name)throws Exception{synchronized(LOCK){
  AtomicFile f=file(name);if(!f.getBaseFile().exists())return new JSONArray();
  String encoded=new String(f.readFully(),StandardCharsets.UTF_8);
  return new JSONArray(new Vault(context).unseal(encoded));
 }}
 void write(String name,JSONArray data)throws Exception{synchronized(LOCK){
  byte[] bytes=new Vault(context).seal(data.toString()).getBytes(StandardCharsets.UTF_8);
  AtomicFile f=file(name);FileOutputStream out=null;
  try{out=f.startWrite();out.write(bytes);f.finishWrite(out);}catch(Exception e){if(out!=null)f.failWrite(out);throw e;}
 }}
 void upsertNotification(JSONObject value)throws Exception{synchronized(LOCK){
  JSONArray old=read("notifications"),next=new JSONArray();next.put(value);
  for(int i=0;i<old.length()&&next.length()<500;i++){JSONObject x=old.getJSONObject(i);
   if(x.optLong("time")>=System.currentTimeMillis()-WEEK&&!x.optString("id").equals(value.optString("id")))next.put(x);
  }write("notifications",next);
 }}
 void prune()throws Exception{synchronized(LOCK){for(String name:new String[]{"notifications","usage","mail"}){
  JSONArray old=read(name),keep=new JSONArray();long cutoff=System.currentTimeMillis()-WEEK;
  for(int i=0;i<old.length();i++){JSONObject x=old.getJSONObject(i);if(x.optLong("cachedAt",x.optLong("time"))>=cutoff)keep.put(x);}
  if(keep.length()!=old.length())write(name,keep);
 }}}
 void removeApp(String pkg)throws Exception{synchronized(LOCK){JSONArray old=read("notifications"),next=new JSONArray();for(int i=0;i<old.length();i++)if(!pkg.equals(old.getJSONObject(i).optString("package")))next.put(old.getJSONObject(i));write("notifications",next);}}
 void clear(String name){synchronized(LOCK){file(name).delete();}}
 static String trim(String s,int limit){return s==null?"":s.substring(0,Math.min(limit,s.length()));}
}
