package psycho.euphoria.v.server;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import android.os.Process;

import java.io.IOException;

import psycho.euphoria.v.MainActivity;
import psycho.euphoria.v.Shared;

public class WebService extends Service {

    public static final String ACTION_DISMISS = "psycho.euphoria.v.server.WebService.ACTION_DISMISS";
    public static final String KP_NOTIFICATION_CHANNEL_ID = "notes_notification_channel";

    public static void createNotification(WebService context) {
        Notification notification = new Builder(context, KP_NOTIFICATION_CHANNEL_ID).setContentTitle("其他")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .addAction(getAction("关闭", PendingIntent.getService(context, 0,
                        new Intent(context, WebService.class).setAction(ACTION_DISMISS), PendingIntent.FLAG_IMMUTABLE)))
                .setContentIntent(getPendingIntent(context))
                .build();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            context.startForeground(1, notification);
        } else {
            context.startForeground(1, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        }
    }

    public static void createNotificationChannel(Context context) {
        NotificationChannel notificationChannel = new NotificationChannel(KP_NOTIFICATION_CHANNEL_ID, "其他", NotificationManager.IMPORTANCE_LOW);
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(notificationChannel);
    }

    public static Notification.Action getAction(String name, PendingIntent piDismiss) {
        return new Notification.Action.Builder(null, name, piDismiss).build();
    }

    public static PendingIntent getPendingIntent(Context context) {
        Intent dismissIntent = new Intent(context, MainActivity.class);
        return PendingIntent.getActivity(context, 0, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel(this);
        String host = Shared.getDeviceIP(this);
        createNotification(this);
        new Thread(() -> {
            WebServer webServer = new WebServer(WebService.this, "0.0.0.0", 9100);
            try {
                webServer.start();
            } catch (IOException e) {
            }
        }).start();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(ACTION_DISMISS)) {
                stopForeground(true);
                stopSelf();
                Process.killProcess(Process.myPid());
                return START_NOT_STICKY;
            }

        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}