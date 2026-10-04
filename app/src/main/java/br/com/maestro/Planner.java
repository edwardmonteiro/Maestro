package br.com.maestro;
import org.json.*;
final class Planner {
 static JSONObject openai(String key,String model,String prompt,boolean search,Net.Job job)throws Exception{
  if(key.isEmpty())throw new IllegalStateException("Configure sua chave OpenAI em Motores.");
  JSONObject req=Reasoning.request(prompt,search);
  JSONObject result=Net.json("https://api.openai.com/v1/responses",key,req,job);
  if(!result.optString("status","completed").equals("completed"))throw new IllegalStateException("Resposta interrompida. Tente um pedido menor.");
  StringBuilder out=new StringBuilder();JSONArray citations=new JSONArray();JSONArray items=result.optJSONArray("output");boolean searched=false;
  if(items!=null)for(int i=0;i<items.length();i++){JSONObject item=items.getJSONObject(i);if(item.optString("type").equals("web_search_call")&&item.optString("status").equals("completed"))searched=true;JSONArray parts=item.optJSONArray("content");if(parts==null)continue;for(int k=0;k<parts.length();k++){JSONObject p=parts.getJSONObject(k);if(p.optString("type").equals("refusal"))throw new IllegalStateException(p.optString("refusal","Pedido nao atendido"));out.append(p.optString("text"));JSONArray ann=p.optJSONArray("annotations");if(ann!=null)for(int h=0;h<ann.length();h++){JSONObject an=ann.getJSONObject(h);if(an.optString("type").equals("url_citation"))citations.put(new JSONObject().put("title",an.optString("title","Fonte")).put("url",an.optString("url")));}}}
  JSONObject plan=Plan.parse(out.toString());plan.put("sources",citations).put("searched",searched).put("engine","GPT-6 Astra · raciocínio alto");JSONObject usage=result.optJSONObject("usage");if(usage!=null)plan.put("usage",usage);return plan;
 }
 static JSONObject openclaw(String token,String prompt,String session,Net.Job job)throws Exception{
  if(token.isEmpty())throw new IllegalStateException("Configure o token do OpenClaw em Motores.");
  JSONObject req=new JSONObject().put("model","openclaw").put("user",session).put("stream",false).put("messages",new JSONArray().put(new JSONObject().put("role","user").put("content",Plan.instructions()+"\n\n"+prompt+"\nUse maestro_save_note para salvar um resumo no Maestro. Nao abra aplicativos nem compartilhe nada. Ao terminar, retorne APENAS o JSON do plano.")));
  JSONObject r=Net.json("http://127.0.0.1:18789/v1/chat/completions",token,req,job);String text=r.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");return Plan.parse(text).put("engine","OpenClaw · gateway local").put("searched",false);
 }
 static JSONObject local(String path,String prompt)throws Exception{
  String text=LocalModel.generate(path,"Crie um plano simples e criativo para este pedido. Voce esta offline, nao indique estabelecimentos reais nem precos verificados. Responda em ate 180 palavras. Pedido: "+prompt,420);
  return new JSONObject().put("title","Seu plano local").put("summary",text).put("invite",text).put("options",new JSONArray()).put("sources",new JSONArray()).put("searched",false).put("engine","Qwen3 1.7B · no aparelho");
 }
}
