package com.sockc.unicomhook;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.XposedBridge;

public class SmsCodeAutoCopy {
    private static final String TAG = "Sockc_SmsCode: ";
    private static final Uri SMS_URI = Uri.parse("content://sms");
    private static final Uri SMS_INBOX_URI = Uri.parse("content://sms/inbox");

    private static boolean sStarted = false;
    private static Context sAppContext;

    // 已处理短信ID，防重复
    private static final Set<Long> sHandledSmsIds = new HashSet<>();

    // 防止短时间内重复触发
    private static long sLastHandleTime = 0L;

    // 防止同一个验证码反复复制
    private static String sLastCopiedCode = null;
    private static long sLastCopyTime = 0L;

    public static synchronized void start(Context context) {
        if (context == null) return;

        if (sStarted) {
            XposedBridge.log(TAG + "已经启动过，跳过重复注册");
            return;
        }

        sAppContext = context.getApplicationContext();

        registerSmsReceiver();
        registerSmsObserver();

        sStarted = true;
        XposedBridge.log(TAG + "短信自动复制已启动");

        // 启动后短轮询几次，解决“短信已收到但只有点已读才触发”的问题
        scheduleStartupScan();
    }

    private static void registerSmsReceiver() {
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
            filter.addAction(Telephony.Sms.Intents.SMS_DELIVER_ACTION);

            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        if (intent == null) return;

                        String action = intent.getAction();
                        XposedBridge.log(TAG + "收到短信广播: " + action);

                        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                        if (messages == null || messages.length == 0) {
                            XposedBridge.log(TAG + "广播里没有 SmsMessage");
                            return;
                        }

                        StringBuilder bodyBuilder = new StringBuilder();
                        String address = null;

                        for (SmsMessage msg : messages) {
                            if (msg == null) continue;

                            if (address == null) {
                                address = msg.getOriginatingAddress();
                            }
                            if (msg.getMessageBody() != null) {
                                bodyBuilder.append(msg.getMessageBody());
                            }
                        }

                        handleSmsBody("broadcast", address, bodyBuilder.toString());
                    } catch (Throwable t) {
                        XposedBridge.log(TAG + "处理短信广播失败: " + t);
                    }
                }
            };

            if (Build.VERSION.SDK_INT >= 33) {
                sAppContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                sAppContext.registerReceiver(receiver, filter);
            }

            XposedBridge.log(TAG + "已注册短信 BroadcastReceiver");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "注册短信 BroadcastReceiver 失败: " + t);
        }
    }

    private static void registerSmsObserver() {
        try {
            ContentObserver observer = new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    XposedBridge.log(TAG + "ContentObserver onChange(boolean)");
                    handleRecentUnreadSms("observer");
                    handleLatestSms("observer_fallback");
                }

                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    super.onChange(selfChange, uri);
                    XposedBridge.log(TAG + "ContentObserver onChange(uri) -> " + uri);
                    handleRecentUnreadSms("observer_uri");
                    handleLatestSms("observer_uri_fallback");
                }
            };

            sAppContext.getContentResolver().registerContentObserver(SMS_URI, true, observer);
            sAppContext.getContentResolver().registerContentObserver(SMS_INBOX_URI, true, observer);

            XposedBridge.log(TAG + "已注册短信 ContentObserver");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "注册短信监听失败: " + t);
        }
    }

    private static void scheduleStartupScan() {
        Handler handler = new Handler(Looper.getMainLooper());

        for (int i = 0; i < 6; i++) {
            final int index = i + 1;
            int delay = 500 + i * 1000;

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        XposedBridge.log(TAG + "启动轮询扫描第 " + index + " 次");
                        handleRecentUnreadSms("startup_poll");
                        handleLatestSms("startup_poll_fallback");
                    } catch (Throwable t) {
                        XposedBridge.log(TAG + "启动轮询失败: " + t);
                    }
                }
            }, delay);
        }
    }

    private static void handleRecentUnreadSms(String source) {
        if (sAppContext == null) return;

        Cursor cursor = null;
        try {
            long now = System.currentTimeMillis();
            long minTime = now - 5 * 60 * 1000L; // 只看近5分钟未读短信

            cursor = sAppContext.getContentResolver().query(
                    SMS_INBOX_URI,
                    new String[]{"_id", "address", "body", "date", "read"},
                    "read=? AND date>?",
                    new String[]{"0", String.valueOf(minTime)},
                    "date DESC"
            );

            if (cursor == null || !cursor.moveToFirst()) {
                XposedBridge.log(TAG + "未找到最近未读短信 source=" + source);
                return;
            }

            do {
                long smsId = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
                String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));

                if (sHandledSmsIds.contains(smsId)) {
                    continue;
                }
                sHandledSmsIds.add(smsId);

                trimHandledIdSet(smsId);

                handleSmsBody(source, address, body);
                return;
            } while (cursor.moveToNext());

            XposedBridge.log(TAG + "最近未读短信都已处理过 source=" + source);
        } catch (Throwable t) {
            XposedBridge.log(TAG + "扫描最近未读短信失败 source=" + source + ": " + t);
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static void handleLatestSms(String source) {
        if (sAppContext == null) return;

        long now = System.currentTimeMillis();
        if (now - sLastHandleTime < 800) {
            XposedBridge.log(TAG + "触发过快，跳过一次 source=" + source);
            return;
        }
        sLastHandleTime = now;

        Cursor cursor = null;
        try {
            cursor = sAppContext.getContentResolver().query(
                    SMS_INBOX_URI,
                    new String[]{"_id", "address", "body", "date"},
                    null,
                    null,
                    "date DESC"
            );

            if (cursor == null || !cursor.moveToFirst()) {
                XposedBridge.log(TAG + "短信查询为空 source=" + source);
                return;
            }

            long smsId = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
            String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
            String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));

            if (sHandledSmsIds.contains(smsId)) {
                XposedBridge.log(TAG + "短信已处理过, id=" + smsId + ", source=" + source);
                return;
            }
            sHandledSmsIds.add(smsId);

            trimHandledIdSet(smsId);

            handleSmsBody(source, address, body);
        } catch (Throwable t) {
            XposedBridge.log(TAG + "处理短信失败 source=" + source + ": " + t);
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static void handleSmsBody(String source, String address, String body) {
        if (TextUtils.isEmpty(body)) {
            XposedBridge.log(TAG + "短信正文为空 source=" + source);
            return;
        }

        String code = extractCode(body);
        if (TextUtils.isEmpty(code)) {
            XposedBridge.log(TAG + "未识别到验证码, source=" + source
                    + ", from=" + address + ", body=" + safe(body));
            return;
        }

        long now = System.currentTimeMillis();
        if (code.equals(sLastCopiedCode) && (now - sLastCopyTime) < 120000) {
            XposedBridge.log(TAG + "验证码与上次相同，跳过复制: " + code + ", source=" + source);
            return;
        }

        copyToClipboard(code);
        sLastCopiedCode = code;
        sLastCopyTime = now;

        XposedBridge.log(TAG + "已复制验证码: " + code + ", source=" + source + ", from=" + address);
        Toast.makeText(sAppContext, "验证码已复制: " + code, Toast.LENGTH_SHORT).show();
    }

    private static String extractCode(String body) {
        if (TextUtils.isEmpty(body)) return null;

        Pattern p1 = Pattern.compile(
                "(?:验证码|校验码|动态码|驗證碼|verification code|code|otp)\\D{0,12}([0-9]{4,8})",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m1 = p1.matcher(body);
        if (m1.find()) return m1.group(1);

        Pattern p2 = Pattern.compile(
                "\\b([0-9]{4,8})\\b\\D{0,12}(?:验证码|校验码|动态码|驗證碼|code|otp)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m2 = p2.matcher(body);
        if (m2.find()) return m2.group(1);

        Pattern p3 = Pattern.compile("\\b([0-9]{4,8})\\b");
        Matcher m3 = p3.matcher(body);
        if (m3.find()) return m3.group(1);

        return null;
    }

    private static void copyToClipboard(String code) {
        ClipboardManager cm = (ClipboardManager) sAppContext.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null) {
            XposedBridge.log(TAG + "ClipboardManager 为空");
            return;
        }

        ClipData clip = ClipData.newPlainText("sms_code", code);
        cm.setPrimaryClip(clip);
    }

    private static void trimHandledIdSet(long keepId) {
        if (sHandledSmsIds.size() > 100) {
            sHandledSmsIds.clear();
            sHandledSmsIds.add(keepId);
        }
    }

    private static String safe(String s) {
        if (s == null) return "";
        s = s.replace("\n", "\\n").replace("\r", "");
        if (s.length() > 80) {
            return s.substring(0, 80) + "...";
        }
        return s;
    }
}
