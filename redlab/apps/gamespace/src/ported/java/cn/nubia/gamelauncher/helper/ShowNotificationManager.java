package dev.redlab.gamespace.helper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public class ShowNotificationManager {
    public static final String TAG = "ShowNotificationManager";
    private Notification mNotification;
    private NotificationManager mNotificationManager;

    public ShowNotificationManager(Context context) {
        this.mNotificationManager = (NotificationManager) context.getSystemService("notification");
    }

    public void showAppMoveToGameSpace(int i, String str, Context context) {
        if (TextUtils.isEmpty(str)) {
            return;
        }
        NotificationChannel notificationChannel = new NotificationChannel("channelID", context.getString(R.string.has_add_gamespace_name), 3);
        notificationChannel.setSound(null, null);
        notificationChannel.setShowBadge(true);
        this.mNotificationManager.createNotificationChannel(notificationChannel);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "channelID");
        builder.setContentTitle(context.getString(R.string.game_space_app_name));
        builder.setContentText(str + " " + context.getString(R.string.has_add_gamespace));
        builder.setSmallIcon(R.mipmap.ic_launcher_notif);
        builder.setWhen(System.currentTimeMillis());
        builder.setDefaults(-1);
        builder.setAutoCancel(true);
        if (this.mNotification == null) {
            Notification notification = builder.getNotification();
            this.mNotification = notification;
            notification.flags = 16;
        }
        this.mNotificationManager.notify(i, builder.build());
        Log.d(TAG, str + " has add in GameSpace");
    }
}
