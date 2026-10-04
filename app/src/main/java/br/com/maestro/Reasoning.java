package br.com.maestro;
import org.json.*;
final class Reasoning {
 static final String MODEL="gpt-6-astra", EFFORT="high";
 static JSONObject request(String prompt,boolean search)throws Exception {
  JSONObject r=new JSONObject().put("model",MODEL).put("store",false).put("reasoning",new JSONObject().put("effort",EFFORT)).put("max_output_tokens",25000).put("instructions",Plan.instructions()).put("input",prompt).put("text",new JSONObject().put("format",new JSONObject().put("type","json_schema").put("name","maestro_plan").put("strict",true).put("schema",Plan.schema())));
  if(search)r.put("tools",new JSONArray().put(new JSONObject().put("type","web_search"))).put("tool_choice","required");
  return r;
 }
 static JSONObject probe()throws Exception{return new JSONObject().put("model",MODEL).put("store",false).put("reasoning",new JSONObject().put("effort","low")).put("max_output_tokens",2048).put("input","Responda apenas MAESTRO_OK.");}
 static String output(JSONObject r)throws Exception{
  if(!r.optString("status","completed").equals("completed"))throw new IllegalStateException("Resposta incompleta. Nenhum plano parcial foi executado.");
  StringBuilder s=new StringBuilder();JSONArray out=r.optJSONArray("output");if(out!=null)for(int i=0;i<out.length();i++){JSONArray c=out.getJSONObject(i).optJSONArray("content");if(c!=null)for(int j=0;j<c.length();j++)if(c.getJSONObject(j).optString("type").equals("output_text"))s.append(c.getJSONObject(j).optString("text"));}return s.toString();
 }
}
