package br.com.maestro;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.concurrent.*;

/** Native task console. Requests continue on the server when this Activity closes. */
public final class ServerActivity extends Activity {
 private static final int BG=0xff101d2a,CARD=0xff1a2a3a,INK=0xfff5f0e8,MUTED=0xff9eafbe,GOLD=0xffe3b678;
 private static final String DEPLOY="https://render.com/deploy?repo=https%3A%2F%2Fgithub.com%2Fedwardmonteiro%2FMaestro";
 private Vault vault;private LinearLayout body,tasks;private TextView status;
 private EditText urlInput,tokenInput,prompt;private Spinner dotChoice,frequency;
 private String url="",token="",draft="",detailId="";private int selectedDot=0,selectedFrequency=0;
 private JSONObject metadata,workspace,state,detail;private boolean busy=false,destroyed=false,resumed=false;
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final Handler handler=new Handler(Looper.getMainLooper());private Net.Job job;
 private final Runnable poll=()->{if(resumed&&!busy&&!token.isEmpty())refresh(false);};
 interface Work{void run()throws Exception;}
 @Override public void onCreate(Bundle saved){super.onCreate(saved);vault=new Vault(this);
  try{JSONObject connection=new JSONObject(vault.get("serverConnection"));url=connection.getString("url");token=connection.getString("token");}catch(Exception ignored){}
  if(saved!=null){draft=saved.getString("draft","");selectedDot=saved.getInt("dot",0);selectedFrequency=saved.getInt("frequency",0);detailId=saved.getString("detail","");}
  render();if(!token.isEmpty())refresh(true);
 }
 private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
 private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
 private void add(LinearLayout p,View v){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(10);p.addView(v,lp);}
 private TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setTextIsSelectable(true);t.setPadding(0,dp(3),0,dp(3));return t;}
 private Button button(String value,Runnable action){Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setTextColor(BG);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(GOLD));b.setOnClickListener(v->{if(!busy)action.run();else toast("Aguarde a operação atual.");});return b;}
 private EditText field(String hint,boolean secret){EditText e=new EditText(this);e.setTextColor(INK);e.setHintTextColor(MUTED);e.setHint(hint);e.setTextSize(16);e.setInputType(secret?InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);e.setSingleLine(true);e.setSaveEnabled(false);e.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);return e;}
 private LinearLayout card(LinearLayout parent){LinearLayout c=column();c.setPadding(dp(16),dp(12),dp(16),dp(16));GradientDrawable b=new GradientDrawable();b.setColor(CARD);b.setCornerRadius(dp(16));c.setBackground(b);add(parent,c);return c;}
 private void remember(){if(prompt!=null){draft=prompt.getText().toString();selectedDot=dotChoice.getSelectedItemPosition();selectedFrequency=frequency.getSelectedItemPosition();}}
 private void render(){if(destroyed)return;remember();prompt=null;if(token.isEmpty())getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(20),dp(16),dp(20),dp(16));root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(dp(20),dp(16)+i.getSystemWindowInsetTop(),dp(20),dp(16)+i.getSystemWindowInsetBottom());return i;});setContentView(root);root.requestApplyInsets();
  add(root,text("MAESTRO / SERVIDOR",15,GOLD));add(root,button("Voltar ao Maestro",this::finish));
  status=text(busy?"Conectando…":token.isEmpty()?"Configure seu servidor":"Conexão salva · verificando status",14,MUTED);add(root,status);
  ScrollView scroll=new ScrollView(this);body=column();scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  if(token.isEmpty()){setup();return;}
  LinearLayout connection=card(body);add(connection,text(url,14,MUTED));
  if(metadata!=null)add(connection,text(metadata.optString("model","Modelo não configurado")+" · raciocínio "+metadata.optString("reasoning"),17,INK));
  add(connection,button("Abrir painel completo",()->open(url)));
  add(connection,button("Atualizar tarefas",()->refresh(true)));
  add(connection,button("Desconectar este celular",()->new AlertDialog.Builder(this).setTitle("Desconectar?").setMessage("As tarefas continuam no servidor. Para interrompê-las, pause ou cancele antes de desconectar.").setNegativeButton("Voltar",null).setPositiveButton("Desconectar",(d,w)->disconnect()).show()));
  if(workspace!=null){JSONArray missing=workspace.optJSONObject("setup").optJSONArray("missing");
   if(missing!=null&&missing.length()>0){LinearLayout c=card(body);add(c,text("Configuração pendente no servidor",20,INK));add(c,text(missing.toString(),14,MUTED));add(c,text("Cadastre as chaves no painel da hospedagem e atualize esta tela.",15,MUTED));}
   else composer();
  }
  tasks=column();add(body,tasks);renderTasks();
 }
 private void setup(){LinearLayout c=card(body);add(c,text("Trabalha enquanto você sai do app.",27,INK));
  add(c,text("Envie pesquisas e tarefas para o OpenDots. O servidor guarda o histórico e executa o trabalho com OpenAI e CopilotKit Intelligence.",16,MUTED));
  add(c,button("1. Instalar servidor pela Render",()->open(DEPLOY)));
  add(c,text("A Render mostrará o custo da hospedagem com disco persistente antes da contratação. Você precisará das chaves OpenAI e CopilotKit Intelligence. O consumo das APIs é separado.",14,MUTED));
  add(c,button("Ver instruções e opção Docker",()->open("https://github.com/edwardmonteiro/Maestro/blob/main/server/README.md")));
  LinearLayout form=card(body);add(form,text("2. Conectar ao servidor",22,INK));
  urlInput=field("https://seu-servidor.onrender.com",false);urlInput.setText(url);add(form,urlInput);
  tokenInput=field("OWNER_TOKEN gerado na hospedagem",true);add(form,tokenInput);
  add(form,text("Use o token de acesso do servidor, não a chave OpenAI. Ele dá acesso ao seu painel e será guardado criptografado neste celular.",14,MUTED));
  add(form,button("Conectar e verificar",this::connect));
  add(form,text("Somente o texto que você enviar nesta tela vai para o servidor. Gmail, agenda e notificações não são sincronizados por esta conexão.",14,MUTED));
 }
 private void connect(){final String u=urlInput.getText().toString().trim(),t=tokenInput.getText().toString().trim();
  execute("Verificando servidor…",()->{
   ServerClient client=client(u,t);JSONObject m=client.metadata(),w=client.workspace(),s=client.state();
   vault.put("serverConnection",new JSONObject().put("url",client.origin).put("token",t).toString());
   ui(()->{url=client.origin;token=t;metadata=m;workspace=w;state=s;render();status.setText("Conectado. Veja abaixo se há configuração pendente.");});
  },false);
 }
 private void disconnect(){handler.removeCallbacks(poll);try{vault.put("serverConnection","");token="";metadata=null;workspace=null;state=null;detail=null;detailId="";render();}catch(Exception e){toast("Não foi possível remover a conexão.");}}
 private ServerClient client(String u,String t)throws Exception{return new ServerClient(u,t,(endpoint,key,payload)->Net.json(endpoint,key,payload,job,30000));}
 private void composer(){LinearLayout c=card(body);add(c,text("Nova tarefa",22,INK));
  add(c,text("O texto será processado pelos serviços configurados no servidor. Pesquisas web podem enviar consultas e URLs ao Parallel.",14,MUTED));
  JSONArray dots=workspace.optJSONArray("dots");if(dots==null||dots.length()==0){add(c,text("Crie um assistente no painel completo.",16,MUTED));return;}
  String[] names=new String[dots.length()];for(int i=0;i<names.length;i++)names[i]=dots.optJSONObject(i).optString("name","Assistente");
  dotChoice=spinner(names);dotChoice.setSelection(Math.max(0,Math.min(selectedDot,names.length-1)));add(c,dotChoice);
  prompt=field("O que você quer que eu faça?",false);prompt.setSingleLine(false);prompt.setMinLines(3);prompt.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);prompt.setText(draft);add(c,prompt);
  frequency=spinner(new String[]{"Uma vez","Repetir a cada hora","Repetir a cada 24 horas"});frequency.setSelection(Math.max(0,Math.min(selectedFrequency,2)));add(c,frequency);
  add(c,text("Repetições começam agora e contam o intervalo após cada conclusão. Cada execução pode consumir créditos da API.",13,MUTED));
  add(c,button("Enviar tarefa ao servidor",this::submit));
  add(c,button("Exemplo: pesquisa para os amigos",()->prompt.setText("Pesquise 3 programas para fazer com amigos em São Paulo neste fim de semana, até R$100 por pessoa. Compare preços, horários e localização, cite as fontes e prepare um convite. Não faça reservas nem envie mensagens.")));
 }
 private Spinner spinner(String[] choices){Spinner s=new Spinner(this);ArrayAdapter<String>a=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,choices);a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);s.setAdapter(a);return s;}
 private void submit(){remember();String p=draft.trim();if(p.length()<3||p.length()>4000){toast("Escreva entre 3 e 4.000 caracteres.");return;}
  final String dot=workspace.optJSONArray("dots").optJSONObject(dotChoice.getSelectedItemPosition()).optString("id");final int seconds=new int[]{0,3600,86400}[selectedFrequency];
  new AlertDialog.Builder(this).setTitle(seconds==0?"Enviar esta tarefa?":"Ativar tarefa recorrente?").setMessage(p+"\n\n"+(seconds==0?"Uma execução no servidor.":"Executar agora e repetir a cada "+(seconds/3600)+" hora(s), até você pausar ou cancelar.")+" O uso das APIs pode ser cobrado.").setNegativeButton("Editar",null).setPositiveButton("Enviar",(d,w)->execute("Enviando tarefa…",()->{
   JSONObject task=client(url,token).create(dot,p,seconds);
   JSONObject s=client(url,token).state();ui(()->{draft="";if(prompt!=null)prompt.setText("");state=s;detailId=task.optString("id");detail=null;renderTasks();status.setText("Tarefa aceita pelo servidor. Você pode sair do app.");});
  },true)).show();
 }
 private void refresh(boolean full){if(token.isEmpty()||busy)return;final String id=detailId;
  execute("Atualizando…",()->{ServerClient c=client(url,token);JSONObject m=full?c.metadata():metadata,w=full?c.workspace():workspace,s=c.state(),d=id.isEmpty()?null:c.detail(id);
   ui(()->{metadata=m;workspace=w;state=s;detail=d;if(full)render();else renderTasks();status.setText("Atualizado · tarefas continuam no servidor ao sair");});
  },false);
 }
 private void renderTasks(){if(tasks==null)return;tasks.removeAllViews();if(state==null)return;
  if(state.optJSONObject("settings").optBoolean("paused"))add(tasks,text("Servidor pausado. Retome pelo painel completo.",16,GOLD));
  if(!state.optJSONObject("settings").optBoolean("researchAllowed"))add(tasks,text("Pesquisas desativadas no painel completo.",16,GOLD));
  add(tasks,text("Tarefas e resultados",23,INK));JSONArray list=state.optJSONArray("tasks");if(list==null||list.length()==0)add(tasks,text("Nenhuma tarefa no servidor ainda.",16,MUTED));
  if(list!=null)for(int i=0;i<Math.min(100,list.length());i++){JSONObject task=list.optJSONObject(i);if(task==null)continue;String id=task.optString("id"),phase=task.optString("status");LinearLayout c=card(tasks);
   add(c,text(ServerClient.status(phase)+(task.optInt("intervalSeconds")>0?" · recorrente":""),14,GOLD));add(c,text(task.optString("prompt"),16,INK));
   if(!task.isNull("error"))add(c,text(task.optString("error"),14,MUTED));
   if(task.optLong("nextRunAt")>0)add(c,text("Próxima: "+java.text.DateFormat.getDateTimeInstance().format(new java.util.Date(task.optLong("nextRunAt"))),14,MUTED));
   add(c,button(id.equals(detailId)?"Atualizar resultado":"Ver resultado e execução",()->{detailId=id;detail=null;refresh(false);}));
   if(!phase.equals("cancelled")){add(c,button("Pausar",()->action(id,"pause")));add(c,button("Cancelar tarefa",()->action(id,"cancel")));}
   if(id.equals(detailId)&&detail!=null)renderDetail(c);
  }
 }
 private void renderDetail(LinearLayout c){JSONArray runs=detail.optJSONArray("runs");boolean resultFound=false;
  if(runs!=null)for(int i=0;i<Math.min(runs.length(),3);i++){JSONObject run=runs.optJSONObject(i),result=run.optJSONObject("result");
   if(result!=null){String value=result.optString("text");add(c,text((result.optBoolean("sample")?"Demonstração · ":"")+"Resultado",18,GOLD));add(c,text(value.length()>20000?value.substring(0,20000)+"\nAbra o painel para ler o restante.":value,16,INK));resultFound=true;}
   if(!run.isNull("error"))add(c,text(run.optString("error"),14,MUTED));
  }
  if(!resultFound)add(c,text("Ainda não há resultado concluído.",14,MUTED));
  JSONArray events=detail.optJSONArray("events");if(events!=null)for(int i=Math.max(0,events.length()-6);i<events.length();i++)add(c,text(events.optJSONObject(i).optString("text"),13,MUTED));
 }
 private void action(String id,String action){new AlertDialog.Builder(this).setTitle(action.equals("pause")?"Pausar tarefa?":"Cancelar tarefa?").setMessage("Interrompe esta tarefa e suas repetições no servidor. Resultados anteriores continuam disponíveis.").setNegativeButton("Voltar",null).setPositiveButton("Confirmar",(d,w)->execute("Atualizando tarefa…",()->{ServerClient c=client(url,token);c.action(id,action);JSONObject s=c.state();ui(()->{state=s;detail=null;renderTasks();status.setText("Tarefa "+(action.equals("pause")?"pausada":"cancelada")+".");});},false)).show();}
 private void execute(String progress,Work work,boolean mutation){if(busy||destroyed)return;busy=true;handler.removeCallbacks(poll);status.setText(progress);job=new Net.Job();worker.execute(()->{try{work.run();}catch(Exception e){ui(()->{String message=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();if(!token.isEmpty())message=message.replace(token,"[token oculto]");status.setText(message+(mutation?"\nSe a conexão caiu, atualize a lista antes de reenviar: a tarefa pode ter sido aceita.":""));});}finally{ui(()->{busy=false;schedule();});}});}
 private void schedule(){handler.removeCallbacks(poll);if(resumed&&!token.isEmpty())handler.postDelayed(poll,15000);}
 private void ui(Runnable r){runOnUiThread(()->{if(!destroyed)r.run();});}
 private void open(String address){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(address)));}catch(Exception e){toast("Não foi possível abrir o navegador.");}}
 private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_LONG).show();}
 @Override protected void onResume(){super.onResume();resumed=true;schedule();}
 @Override protected void onPause(){resumed=false;handler.removeCallbacks(poll);super.onPause();}
 @Override protected void onSaveInstanceState(Bundle b){remember();b.putString("draft",draft);b.putInt("dot",selectedDot);b.putInt("frequency",selectedFrequency);b.putString("detail",detailId);super.onSaveInstanceState(b);}
 @Override protected void onDestroy(){destroyed=true;handler.removeCallbacks(poll);if(job!=null)job.cancel();worker.shutdownNow();super.onDestroy();}
}
