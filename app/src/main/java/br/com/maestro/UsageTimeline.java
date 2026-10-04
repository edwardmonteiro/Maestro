package br.com.maestro;

import java.util.*;
import org.json.*;

/** Conservative foreground sessions. Android events are evidence, not proof of attention. */
final class UsageTimeline {
 static JSONArray sessions(JSONArray events,long begin,long end)throws Exception{
  List<JSONObject> sorted=new ArrayList<>();for(int i=0;i<events.length();i++)sorted.add(events.getJSONObject(i));
  sorted.sort(Comparator.comparingLong(x->x.optLong("time")));
  JSONArray out=new JSONArray();String active="";long since=0;
  for(JSONObject e:sorted){long t=e.optLong("time");if(t>end)break;int type=e.optInt("type");String pkg=e.optString("package");
   boolean close=(type==1&&!pkg.equals(active))||(type==2&&pkg.equals(active))||type==16||type==17||type==26;
   if(close&&!active.isEmpty()){append(out,active,Math.max(begin,since),Math.min(end,t),false);active="";}
   if(type==1&&!pkg.isEmpty()&&!pkg.equals(active)){active=pkg;since=t;}
  }
  if(!active.isEmpty())append(out,active,Math.max(begin,since),end,true);
  return out;
 }
 private static void append(JSONArray out,String pkg,long from,long to,boolean open)throws Exception{
  if(to<=from)return;
  JSONObject last=out.length()==0?null:out.getJSONObject(out.length()-1);
  if(last!=null&&last.optString("package").equals(pkg)&&from-last.optLong("end")<=1500){last.put("end",to).put("open",open);return;}
  out.put(new JSONObject().put("package",pkg).put("start",from).put("end",to).put("open",open));
 }
}
