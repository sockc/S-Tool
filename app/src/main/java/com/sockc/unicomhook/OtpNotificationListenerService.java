package com.sockc.unicomhook;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import com.sockc.unicomhook.compat.XposedBridge;

public final class OtpNotificationListenerService
        extends NotificationListenerService {

    private static final String TAG =
            "S-Tool/OtpNotification: ";

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();

        XposedBridge.log(
                TAG
                        + "通知监听已连接"
        );
    }

    @Override
    public void onListenerDisconnected() {
        XposedBridge.log(
                TAG
                        + "通知监听已断开"
        );

        super.onListenerDisconnected();
    }

    @Override
    public void onNotificationPosted(
            StatusBarNotification sbn
    ) {
        if (sbn == null) {
            return;
        }

        FeaturePrefs.OpenResult openResult =
                FeaturePrefs.open(this);

        if (!FeaturePrefs.isEnabled(
                openResult.preferences,
                "sms_code"
        )) {
            return;
        }

        Notification notification =
                sbn.getNotification();

        if (notification == null) {
            return;
        }

        String text =
                collectText(
                        notification.extras
                );

        if (TextUtils.isEmpty(text)) {
            return;
        }

        boolean copied =
                SmsCodeAutoCopy.handleExternalText(
                        this,
                        "notification:"
                                + sbn.getPackageName(),
                        text
                );

        if (copied) {
            XposedBridge.log(
                    TAG
                            + "验证码通知已处理 package="
                            + sbn.getPackageName()
            );
        }
    }

    private String collectText(
            Bundle extras
    ) {
        if (extras == null) {
            return "";
        }

        StringBuilder builder =
                new StringBuilder();

        append(
                builder,
                extras.getCharSequence(
                        Notification.EXTRA_TITLE
                )
        );
        append(
                builder,
                extras.getCharSequence(
                        Notification.EXTRA_TEXT
                )
        );
        append(
                builder,
                extras.getCharSequence(
                        Notification.EXTRA_BIG_TEXT
                )
        );
        append(
                builder,
                extras.getCharSequence(
                        Notification.EXTRA_SUB_TEXT
                )
        );
        append(
                builder,
                extras.getCharSequence(
                        Notification.EXTRA_SUMMARY_TEXT
                )
        );

        CharSequence[] lines =
                extras.getCharSequenceArray(
                        Notification.EXTRA_TEXT_LINES
                );

        if (lines != null) {
            for (CharSequence line : lines) {
                append(
                        builder,
                        line
                );
            }
        }

        return builder.toString();
    }

    private void append(
            StringBuilder builder,
            CharSequence value
    ) {
        if (TextUtils.isEmpty(value)) {
            return;
        }

        if (builder.length() > 0) {
            builder.append(
                    '\n'
            );
        }

        builder.append(
                value
        );
    }
}
