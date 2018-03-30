package com.example.gszzz.attendclass.service_notification;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.widget.Toast;

import com.example.gszzz.attendclass.AttendanceTaking;
import com.example.gszzz.attendclass.R;

public class AutoCheckService extends Service {

    private static final int FOREGROUND_NOTIFICATION_ID = 1334;
    private static final String CHANNEL_ID = "channel1";

    public AutoCheckService() {
    }

    @Override
    public void onCreate() {

        super.onCreate();

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            // Create the NotificationChannel, but only on API 26+ because
//            // the NotificationChannel class is new and not in the support library
//            CharSequence name = getString(R.string.channel_name);
//            String description = getString(R.string.channel_description);
//            int importance = NotificationManagerCompat.IMPORTANCE_DEFAULT;
//            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
//            channel.setDescription(description);
//            // Register the channel with the system
//            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
//            notificationManager.createNotificationChannel(channel);
//        }



    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Intent notificationIntent = new Intent(this, AttendanceTaking.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle("Test Notification")
                .setContentText("Start app...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
//        startForeground(FOREGROUND_NOTIFICATION_ID, builder.build());

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        notificationManager.notify(FOREGROUND_NOTIFICATION_ID, builder.build());
        stopSelf();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
