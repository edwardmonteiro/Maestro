package br.com.maestro;
import android.app.Activity;import android.content.*;import android.os.Bundle;
/** Explicit, one-shot PendingIntent receiver: never persists stdout or secret-bearing command text. */
public final class TermuxResultReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context context,Intent intent){
  Bundle result=intent.getBundleExtra("result");if(result==null)return;
  int error=result.getInt("err",Activity.RESULT_OK),exit=result.getInt("exitCode",0);
  if(error==Activity.RESULT_OK&&exit==0)return;
  String message=error!=Activity.RESULT_OK?"Termux bloqueou o comando. Toque em Preparar OpenClaw e cole o novo comando manualmente.":"O comando do Termux falhou. Toque em Preparar OpenClaw → Colar manualmente para ver o erro.";
  context.getSharedPreferences("maestro",0).edit().putBoolean("termuxBootstrapReady",false).putBoolean("gatewayVerified",false).putBoolean("termuxCommandFailed",true).putString("setupPhase",message).apply();
  context.stopService(new Intent(context,SetupService.class));
 }
}
