package br.com.maestro;

import android.app.*;
import android.app.usage.*;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import org.json.*;
import java.util.*;

final class PhoneData {
 static boolean usageAllowed(Context c){AppOpsManager m=(AppOpsManager)c.getSystemService(Context.APP_OPS_SERVICE);return m.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),c.getPackageName())==AppOpsManager.MODE_ALLOWED;}
 static boolean notificationsAllowed(Context c){return ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).isNotificationListenerAccessGranted(new ComponentName(c,PhoneNotifications.class));}
 static long midnight(){Calendar c=Calendar.getInstance();c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);return c.getTimeInMillis();}
 static String appName(Context c,String pkg){try{return c.getPackageManager().getApplicationLabel(c.getPackageManager().getApplicationInfo(pkg,0)).toString();}catch(Exception e){return pkg;}}
 static void setUsageEnabled(Context c,boolean enabled)throws Exception{
  synchronized(PersonalStore.LOCK){
   if(!enabled)collectUsage(c);
   SharedPreferences prefs=c.getSharedPreferences("personal",0);PersonalStore store=new PersonalStore(c);long now=System.currentTimeMillis();
   JSONArray events=store.read("usage");events.put(new JSONObject().put("id",now+":boundary").put("time",now).put("type",26).put("package",""));store.write("usage",events);
   prefs.edit().putBoolean("usage",enabled).putLong("usageSince",now).putLong("usageCursor",now).commit();
  }
 }
 static void collectUsage(Context c)throws Exception{
  synchronized(PersonalStore.LOCK){SharedPreferences p=c.getSharedPreferences("personal",0);if(!p.getBoolean("usage",false)||!usageAllowed(c))return;
   long now=System.currentTimeMillis(),from=Math.max(now-PersonalStore.WEEK,p.getLong("usageCursor",p.getLong("usageSince",now)));
   UsageEvents stream=((UsageStatsManager)c.getSystemService(Context.USAGE_STATS_SERVICE)).queryEvents(from,now);
   if(stream==null)return;
   PersonalStore store=new PersonalStore(c);JSONArray old=store.read("usage");List<JSONObject> list=new ArrayList<>();Set<String> seen=new HashSet<>();
   for(int i=0;i<old.length();i++){JSONObject x=old.getJSONObject(i);if(x.optLong("time")>=now-PersonalStore.WEEK){list.add(x);seen.add(x.optString("id"));}}
   UsageEvents.Event e=new UsageEvents.Event();while(stream.hasNextEvent()){stream.getNextEvent(e);int t=e.getEventType();if(t!=1&&t!=2&&t!=15&&t!=16&&t!=17&&t!=18&&t!=26)continue;
    String pkg=e.getPackageName()==null?"":e.getPackageName(),id=e.getTimeStamp()+":"+t+":"+pkg;
    if(seen.add(id))list.add(new JSONObject().put("id",id).put("time",e.getTimeStamp()).put("type",t).put("package",pkg));
   }
   list.sort(Comparator.comparingLong(x->x.optLong("time")));JSONArray result=new JSONArray();for(int i=Math.max(0,list.size()-10000);i<list.size();i++)result.put(list.get(i));
   store.write("usage",result);p.edit().putLong("usageCursor",Math.max(from,now-1000)).apply();
  }
 }
 static JSONArray calendar(Context c)throws Exception{
  JSONArray out=new JSONArray();if(c.checkSelfPermission(android.Manifest.permission.READ_CALENDAR)!=android.content.pm.PackageManager.PERMISSION_GRANTED)return out;
  long from=midnight(),to=from+PersonalStore.WEEK;Uri.Builder uri=CalendarContract.Instances.CONTENT_URI.buildUpon();ContentUris.appendId(uri,from);ContentUris.appendId(uri,to);
  String[] cols={CalendarContract.Instances.EVENT_ID,CalendarContract.Instances.TITLE,CalendarContract.Instances.BEGIN,CalendarContract.Instances.END,CalendarContract.Instances.EVENT_LOCATION,CalendarContract.Instances.ALL_DAY};
  try(Cursor cur=c.getContentResolver().query(uri.build(),cols,CalendarContract.Instances.VISIBLE+"=1 AND "+CalendarContract.Instances.STATUS+"!=?",new String[]{"2"},CalendarContract.Instances.BEGIN+" ASC")){
   if(cur!=null)while(cur.moveToNext()&&out.length()<100)out.put(new JSONObject().put("id",cur.getLong(0)).put("title",PersonalStore.trim(cur.getString(1),300)).put("start",cur.getLong(2)).put("end",cur.getLong(3)).put("location",PersonalStore.trim(cur.getString(4),300)).put("allDay",cur.getInt(5)==1));
  }return out;
 }
}
