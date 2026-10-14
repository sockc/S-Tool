package com.sockc.unicomhook;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.sockc.unicomhook.compat.XC_MethodHook;
import com.sockc.unicomhook.compat.XposedBridge;
import com.sockc.unicomhook.compat.XposedHelpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * UI-only sender name overlay for classic Android TextView/RecyclerView inbox rows.
 * Does not read the SMS database, modify contacts, or send messages anywhere.
 * Compose-only Google Messages builds require separate version-specific support.
 */
public final class SmsSenderHook implements HookModule {
    private static final String TARGET = "com.google.android.apps.messaging";
    private static final String TAG = "S-Tool/SmsSender: ";
    private static final String CACHE_FILE = "s_tool_sms_sender_cache";
    private static final long SCAN_INTERVAL_MS = 750;
    private static final int MAX_NODES = 600;
    private static final int MAX_ROW_NODES = 40;

    private final Map<Activity, ViewTreeObserver.OnGlobalLayoutListener> listeners =
            Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<Activity, Long> lastScans =
            Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<String, String> overrides = new HashMap<>();

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) {
        if (!TARGET.equals(lpparam.packageName)
                || !TARGET.equals(lpparam.processName)) return;

        XposedHelpers.findAndHookMethod(Activity.class, "onResume",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        final Activity activity = (Activity) param.thisObject;
                        if (!isInbox(activity)) return;
                        try {
                            loadOverrides(activity);
                            install(activity);
                            activity.getWindow().getDecorView().postDelayed(
                                    () -> safeScan(activity), 450);
                        } catch (Throwable ignored) { }
                    }
                });

        XposedHelpers.findAndHookMethod(Activity.class, "onDestroy",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        uninstall((Activity) param.thisObject);
                    }
                });
        XposedBridge.log(TAG + "native TextView inbox adapter enabled");
    }

    private boolean isInbox(Activity activity) {
        if (activity == null || activity.isFinishing()) return false;
        String name = activity.getClass().getName().toLowerCase(java.util.Locale.ROOT);
        return name.contains("conversationlist")
                || name.contains("conversationoverview")
                || name.contains("homeactivity")
                || name.endsWith(".mainactivity");
    }

    private void loadOverrides(Context context) {
        try {
            Bundle data = context.getContentResolver().call(
                    ConfigProvider.CONTENT_URI,
                    ConfigProvider.METHOD_GET_SMS_OVERRIDES, null, null);
            if (data == null) return;
            synchronized (overrides) {
                overrides.clear();
                for (String key : data.keySet()) {
                    String value = data.getString(key);
                    if (SmsSenderRecognizer.normalizeSender(key) != null
                            && SmsSenderRecognizer.validName(value)) {
                        overrides.put(key, value);
                    }
                }
            }
        } catch (Throwable ignored) { }
    }

    private void install(Activity activity) {
        if (listeners.containsKey(activity)) return;
        final View root = activity.getWindow().getDecorView();
        ViewTreeObserver.OnGlobalLayoutListener listener =
                () -> safeScan(activity);
        ViewTreeObserver observer = root.getViewTreeObserver();
        if (!observer.isAlive()) return;
        observer.addOnGlobalLayoutListener(listener);
        listeners.put(activity, listener);
    }

    private void uninstall(Activity activity) {
        ViewTreeObserver.OnGlobalLayoutListener listener = listeners.remove(activity);
        lastScans.remove(activity);
        if (listener == null) return;
        try {
            ViewTreeObserver observer =
                    activity.getWindow().getDecorView().getViewTreeObserver();
            if (observer.isAlive()) observer.removeOnGlobalLayoutListener(listener);
        } catch (Throwable ignored) { }
    }

    private void safeScan(Activity activity) {
        try {
            if (activity.isFinishing() || activity.isDestroyed()) return;
            long now = SystemClock.uptimeMillis();
            Long last = lastScans.get(activity);
            if (last != null && now - last < SCAN_INTERVAL_MS) return;
            lastScans.put(activity, now);
            View root = activity.getWindow().getDecorView();
            SharedPreferences cache =
                    activity.getSharedPreferences(CACHE_FILE, Context.MODE_PRIVATE);
            int[] budget = {MAX_NODES};
            scanTree(root, cache, budget);
        } catch (Throwable ignored) {
            // Never let a Google Messages rendering change crash the host process.
        }
    }

    private void scanTree(View view, SharedPreferences cache, int[] budget) {
        if (view == null || budget[0]-- <= 0 || view.getVisibility() != View.VISIBLE) return;
        if (looksLikeRecycler(view) && view instanceof ViewGroup) {
            ViewGroup list = (ViewGroup) view;
            for (int i = 0; i < list.getChildCount() && budget[0] > 0; i++) {
                View child = list.getChildAt(i);
                scanRow(child, cache);
                budget[0]--;
            }
            return;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount() && budget[0] > 0; i++) {
                scanTree(group.getChildAt(i), cache, budget);
            }
        }
    }

    private boolean looksLikeRecycler(View view) {
        // Google Messages can subclass RecyclerView under an obfuscated name.
        for (Class<?> type = view.getClass(); type != null; type = type.getSuperclass()) {
            String name = type.getName();
            if (name.contains("RecyclerView") || name.contains("ListView")) {
                return true;
            }
        }
        return false;
    }

    private void scanRow(View row, SharedPreferences cache) {
        List<TextView> texts = new ArrayList<>();
        collectTextViews(row, texts, new int[]{MAX_ROW_NODES});
        if (texts.size() < 2) return;

        TextView senderView = null;
        String sender = null;
        String signature = null;

        for (TextView textView : texts) {
            CharSequence text = textView.getText();
            if (text == null) continue;
            String raw = text.toString().trim();
            if (senderView == null) {
                String normalized = SmsSenderRecognizer.normalizeSender(raw);
                if (normalized != null) {
                    senderView = textView;
                    sender = normalized;
                    continue;
                }
            }
            if (signature == null && raw.length() < 300) {
                signature = SmsSenderRecognizer.signature(raw);
            }
        }
        if (senderView == null) return;

        String label;
        synchronized (overrides) {
            label = overrides.get(sender);
        }
        if (label == null) label = SmsSenderRecognizer.official(sender);
        if (label == null && signature != null) {
            label = signature;
            // Sender-specific local cache, not a trusted or network-verified identity.
            if (!label.equals(cache.getString(sender, null))) {
                cache.edit().putString(sender, label).apply();
            }
        }
        if (label == null) label = cache.getString(sender, null);
        if (SmsSenderRecognizer.validName(label)
                && !label.contentEquals(senderView.getText())) {
            senderView.setText(label);
        }
    }

    private void collectTextViews(View view, List<TextView> out, int[] budget) {
        if (view == null || budget[0]-- <= 0 || out.size() >= 16) return;
        if (view instanceof TextView) {
            out.add((TextView) view);
        } else if (view instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) view;
            for (int i = 0; i < parent.getChildCount() && budget[0] > 0; i++) {
                collectTextViews(parent.getChildAt(i), out, budget);
            }
        }
    }
}
