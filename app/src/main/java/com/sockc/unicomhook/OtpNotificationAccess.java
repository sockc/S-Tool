package com.sockc.unicomhook;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

final class OtpNotificationAccess {

    private OtpNotificationAccess() {
    }

    static boolean isEnabled(
            Context context
    ) {
        if (context == null) {
            return false;
        }

        ComponentName component =
                new ComponentName(
                        context,
                        OtpNotificationListenerService.class
                );

        if (Build.VERSION.SDK_INT >= 27) {
            try {
                NotificationManager manager =
                        (NotificationManager)
                                context.getSystemService(
                                        Context.NOTIFICATION_SERVICE
                                );

                if (manager != null
                        && manager
                        .isNotificationListenerAccessGranted(
                                component
                        )) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }

        try {
            String enabled =
                    Settings.Secure.getString(
                            context.getContentResolver(),
                            "enabled_notification_listeners"
                    );

            return enabled != null
                    && enabled.contains(
                    context.getPackageName()
            );
        } catch (Throwable ignored) {
            return false;
        }
    }
}
