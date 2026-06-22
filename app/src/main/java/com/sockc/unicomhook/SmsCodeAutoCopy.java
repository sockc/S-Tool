package com.sockc.unicomhook;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.XposedBridge;

public class SmsCodeAutoCopy {
    private static final String TAG = "Sockc_SmsCode: ";
    private static final Uri SMS_INBOX_URI = Uri.parse("content://sms/inbox");

    private static boolean sStarted = false;
    private static Context sAppContext;

    // 防重复
    private static final Set<Long> sHandledSmsIds = new HashSet<>();
    private static long sLastHandleTime = 0L;
    private static String sLastCopiedCode = null;

    public static void start(Context context) {
        if (context == null) return;
        if (sStarted) {
            XposedBridge.log(TAG + "已经启动过，跳过重复注册");
            return;
        }

        sAppContext = context.getApplicationContext();

        try {
            sAppContext.getContentResolver().registerContentObserver(
                    SMS_INBOX_URI,
                    true,
                    new ContentObserver(new Handler(Looper.getMainLooper())) {
                        @Override
                        public void onChange(boolean selfChange) {
                            super.onChange(selfChange);
                            handleLatestSms();
                        }
                    }
            );

            sStarted = true;
            XposedBridge.log(TAG + "已注册短信 ContentObserver");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "注册短信监听失败: " + t);
        }
    }

    private static void handleLatestSms() {
        if (sAppContext == null) return;

        long now = System.currentTimeMillis();
        if (now - sLastHandleTime < 1200) {
            XposedBridge.log(TAG + "触发过快，跳过一次");
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
                XposedBridge.log(TAG + "短信查询为空");
                return;
            }

            long smsId = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
            String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
            String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));

            if (sHandledSmsIds.contains(smsId)) {
                XposedBridge.log(TAG + "短信已处理过, id=" + smsId);
                return;
            }
            sHandledSmsIds.add(smsId);

            // 避免 Set 无限变大
            if (sHandledSmsIds.size() > 100) {
                sHandledSmsIds.clear();
                sHandledSmsIds.add(smsId);
            }

            if (TextUtils.isEmpty(body)) {
                XposedBridge.log(TAG + "短信正文为空");
                return;
            }

            String code = extractCode(body);
            if (TextUtils.isEmpty(code)) {
                XposedBridge.log(TAG + "未识别到验证码, from=" + address + ", body=" + safe(body));
                return;
            }

            if (code.equals(sLastCopiedCode)) {
                XposedBridge.log(TAG + "验证码与上次相同，跳过复制: " + code);
                return;
            }

            copyToClipboard(code);
            sLastCopiedCode = code;

            XposedBridge.log(TAG + "已复制验证码: " + code + ", from=" + address);
            Toast.makeText(sAppContext, "验证码已复制: " + code, Toast.LENGTH_SHORT).show();

        } catch (Throwable t) {
            XposedBridge.log(TAG + "处理短信失败: " + t);
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Throwable ignored) {}
            }
        }
    }

    private static String extractCode(String body) {
        if (TextUtils.isEmpty(body)) return null;

        // 先匹配验证码关键词附近的 4-8 位数字
        Pattern p1 = Pattern.compile(
                "(?:验证码|校验码|动态码|驗證碼|verification code|code|otp)\\D{0,12}([0-9]{4,8})",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m1 = p1.matcher(body);
        if (m1.find()) return m1.group(1);

        // 再匹配 “123456 为您的验证码” 这种
        Pattern p2 = Pattern.compile(
                "\\b([0-9]{4,8})\\b\\D{0,12}(?:验证码|校验码|动态码|驗證碼|code|otp)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m2 = p2.matcher(body);
        if (m2.find()) return m2.group(1);

        // 最后兜底：独立 4-8 位数字
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

    private static String safe(String s) {
        if (s == null) return "";
        s = s.replace("\n", "\\n").replace("\r", "");
        if (s.length() > 80) {
            return s.substring(0, 80) + "...";
        }
        return s;
    }
}
