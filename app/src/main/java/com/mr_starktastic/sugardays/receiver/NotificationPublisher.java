package com.mr_starktastic.sugardays.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationPublisher extends BroadcastReceiver {
    public static String NOTIFICATION_ID = "com.mr_starktastic.sugardays.NOTIFICATION_ID";
    public static String NOTIFICATION = "com.mr_starktastic.sugardays.NOTIFICATION";

    @Override
    public void onReceive(Context context, Intent intent) {
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(intent.getIntExtra(NOTIFICATION_ID, 0),
                        (Notification) intent.getParcelableExtra(NOTIFICATION));
    }
}
