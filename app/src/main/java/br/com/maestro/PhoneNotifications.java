package br.com.maestro;

import android.app.Notification;
import android.content.*;
import android.os.Bundle;
import android.service.notification.*;
import org.json.JSONObject;
import java.util.*;
import java.util.concurrent.*;

public final class PhoneNotifications extends NotificationListenerService {
 private final ExecutorService writer=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(100),new ThreadPoolExecutor.DiscardOldestPolicy());
 private SharedPreferences prefs(){return getSharedPreferences("personal",0);}
 @Override public void onListenerConnected(){prefs().edit().putBoolean("listenerConnected",true).apply();}
 @Override public void onListenerDisconnected(){prefs().edit().putBoolean("listenerConnected",false).apply();}
 @Override public void onNotificationPosted(StatusBarNotification sbn){
  if(sbn==null||!prefs().getBoolean("notifications",false))return;
  final String pkg=sbn.getPackageName();Notification n=sbn.getNotification();
  if(pkg.equals(getPackageName())||pkg.equals("android")||pkg.equals("com.android.providers.downloads")||n==null||sbn.isOngoing()||(n.flags&Notification.FLAG_GROUP_SUMMARY)!=0)return;
  if(prefs().getStringSet("excluded",Collections.emptySet()).contains(pkg))return;
  try{
   Bundle b=n.extras;String title=String.valueOf(b.getCharSequence(Notification.EXTRA_TITLE,""));
   CharSequence big=b.getCharSequence(Notification.EXTRA_BIG_TEXT);CharSequence normal=b.getCharSequence(Notification.EXTRA_TEXT);
   String content=big!=null?big.toString():normal==null?"":normal.toString();
   if(content.isEmpty()){CharSequence[] lines=b.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);if(lines!=null)content=android.text.TextUtils.join("\n",lines);}
   String label=pkg;try{label=getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0)).toString();}catch(Exception ignored){}
   final JSONObject item=new JSONObject().put("id",sbn.getKey()).put("package",pkg).put("app",label).put("title",PersonalStore.trim(title,240)).put("text",PersonalStore.trim(content,2000)).put("time",System.currentTimeMillis());
   final long epoch=prefs().getLong("epoch",0);
   writer.execute(()->{try{synchronized(PersonalStore.LOCK){if(prefs().getBoolean("notifications",false)&&prefs().getLong("epoch",0)==epoch&&!prefs().getStringSet("excluded",Collections.emptySet()).contains(pkg))new PersonalStore(this).upsertNotification(item);}}catch(Exception e){prefs().edit().putBoolean("storageError",true).apply();}});
  }catch(Exception ignored){}
 }
 @Override public void onDestroy(){prefs().edit().putBoolean("listenerConnected",false).apply();writer.shutdownNow();super.onDestroy();}
}
