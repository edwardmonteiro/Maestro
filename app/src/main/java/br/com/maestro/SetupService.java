package br.com.maestro;
import android.app.*;import android.content.*;import android.os.*;
/** Keeps the app process available while the user is in Termux during setup. */
public final class SetupService extends Service {
 private final Handler timer=new Handler(Looper.getMainLooper());
 @Override public int onStartCommand(Intent i,int flags,int id){
  NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("setup","Preparação do Maestro",NotificationManager.IMPORTANCE_LOW));
  PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  startForeground(20,new Notification.Builder(this,"setup").setSmallIcon(br.com.maestro.R.drawable.ic_maestro).setContentTitle("Maestro · preparando executor").setContentText("Toque para acompanhar a instalação no celular.").setContentIntent(open).setOngoing(true).build());
  timer.removeCallbacksAndMessages(null);timer.postDelayed(this::stopSelf,45*60*1000L);return START_NOT_STICKY;
 }
 @Override public IBinder onBind(Intent i){return null;}
 @Override public void onDestroy(){timer.removeCallbacksAndMessages(null);super.onDestroy();}
}
