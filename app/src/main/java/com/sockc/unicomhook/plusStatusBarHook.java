package com.sockc.unicomhook;

import android.app.Application;
import android.content.Context;
import android.graphics.Typeface;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.WeakHashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class OplusStatusBarHook implements IXposedHookLoadPackage {
    private static final String TAG = "OplusStatusBarHook";
    private static final String TARGET = "com.android.systemui";

    private static boolean installed = false;
    private static final WeakHashMap<ViewGroup, Boolean> patchedRoots = new WeakHashMap<>();
    private static final WeakHashMap<TextView, Boolean> patchedClocks = new WeakHashMap<>();
    private static final WeakHashMap<ViewGroup, TextView> trafficViews = new WeakHashMap<>();

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET.equals(lpparam.packageName)) return;

        XposedBridge.log(TAG + ": loaded " + lpparam.packageName);

        try {
            XposedBridge.hookAllMethods(Application.class, "attach", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (installed) return;
                    installed = true;

                    XposedBridge.log(TAG + ": Application.attach ok");
                    hookViewGroupAttach();
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": hook attach failed: " + t);
        }
    }

    private static void hookViewGroupAttach() {
        try {
            XposedBridge.hookAllMethods(ViewGroup.class, "onAttachedToWindow", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    ViewGroup vg = (ViewGroup) param.thisObject;
                    String cls = vg.getClass().getName();

                    if (cls.contains("PhoneStatusBarView")
                            || cls.contains("CollapsedStatusBar")
                            || "status_bar".equals(getIdName(vg))) {
                        vg.postDelayed(() -> patchStatusBar(vg), 500);
                        vg.postDelayed(() -> patchStatusBar(vg), 1500);
                    }
                }
            });

            XposedBridge.log(TAG + ": ViewGroup attach hook installed");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": hook ViewGroup failed: " + t);
        }
    }

    private static void patchStatusBar(ViewGroup root) {
        try {
            if (patchedRoots.containsKey(root)) return;

            TextView clock = findClock(root);
            if (clock != null) {
                patchClock(clock);
            }

            ViewGroup right = findRightContainer(root);
            if (right != null) {
                addTrafficView(right);
            }

            patchedRoots.put(root, true);
            XposedBridge.log(TAG + ": patchStatusBar done, clock=" + (clock != null) + ", right=" + (right != null));
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": patchStatusBar failed: " + t);
        }
    }

    private static TextView findClock(View root) {
        if (root == null) return null;

        if (root instanceof TextView) {
            String id = getIdName(root);
            String cls = root.getClass().getName();

            if ("clock".equals(id)
                    || id.contains("clock")
                    || cls.toLowerCase(Locale.ROOT).contains("clock")) {
                return (TextView) root;
            }
        }

        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                TextView r = findClock(vg.getChildAt(i));
                if (r != null) return r;
            }
        }

        return null;
    }

    private static ViewGroup findRightContainer(View root) {
        if (root == null) return null;

        if (root instanceof ViewGroup) {
            String id = getIdName(root);

            if ("system_icon_area".equals(id)
                    || "system_icons".equals(id)
                    || "statusIcons".equals(id)
                    || "status_icons".equals(id)
                    || "right_status_bar".equals(id)
                    || id.contains("system_icon")
                    || id.contains("status_icon")
                    || id.contains("right")) {
                return (ViewGroup) root;
            }

            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                ViewGroup r = findRightContainer(vg.getChildAt(i));
                if (r != null) return r;
            }
        }

        return null;
    }

    private static void patchClock(final TextView clock) {
        if (patchedClocks.containsKey(clock)) return;
        patchedClocks.put(clock, true);

        try {
            clock.setSingleLine(false);
            clock.setLines(2);
            clock.setMaxLines(2);
            clock.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            clock.setIncludeFontPadding(false);
            clock.setLineSpacing(0f, 0.92f);
            clock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            clock.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

            Runnable updater = new Runnable() {
                @Override
                public void run() {
                    try {
                        clock.setText(makeClockText());
                    } catch (Throwable ignored) {
                    }
                    clock.postDelayed(this, 1000);
                }
            };

            clock.post(updater);
            XposedBridge.log(TAG + ": clock patched");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": patchClock failed: " + t);
        }
    }

    private static void addTrafficView(final ViewGroup right) {
        if (trafficViews.containsKey(right)) return;

        try {
            Context ctx = right.getContext();

            final TextView tv = new TextView(ctx);
            tv.setSingleLine(false);
            tv.setLines(2);
            tv.setMaxLines(2);
            tv.setGravity(Gravity.CENTER);
            tv.setIncludeFontPadding(false);
            tv.setLineSpacing(0f, 0.92f);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
            tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            tv.setText("0.0B\n0.0B");

            int mr = dp(ctx, 3);
            int ml = dp(ctx, 2);

            ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            lp.leftMargin = ml;
            lp.rightMargin = mr;

            right.addView(tv, 0, lp);
            trafficViews.put(right, tv);

            startTrafficUpdater(tv);

            XposedBridge.log(TAG + ": traffic view added to " + getIdName(right));
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": addTrafficView failed: " + t);
        }
    }

    private static void startTrafficUpdater(final TextView tv) {
        final long[] lastRx = {TrafficStats.getTotalRxBytes()};
        final long[] lastTx = {TrafficStats.getTotalTxBytes()};
        final long[] lastTime = {System.currentTimeMillis()};

        Runnable updater = new Runnable() {
            @Override
            public void run() {
                try {
                    long nowRx = TrafficStats.getTotalRxBytes();
                    long nowTx = TrafficStats.getTotalTxBytes();
                    long nowTime = System.currentTimeMillis();

                    long dt = Math.max(1, nowTime - lastTime[0]);

                    long rxSpeed = Math.max(0, (nowRx - lastRx[0]) * 1000 / dt);
                    long txSpeed = Math.max(0, (nowTx - lastTx[0]) * 1000 / dt);

                    lastRx[0] = nowRx;
                    lastTx[0] = nowTx;
                    lastTime[0] = nowTime;

                    // 上面一行上传，下面一行下载
                    tv.setText(formatSpeed(txSpeed) + "\n" + formatSpeed(rxSpeed));
                } catch (Throwable ignored) {
                }

                tv.postDelayed(this, 1000);
            }
        };

        tv.postDelayed(updater, 1000);
    }

    private static String makeClockText() {
        Calendar c = Calendar.getInstance();

        String[] weeks = {
                "周日", "周一", "周二", "周三", "周四", "周五", "周六"
        };

        int month = c.get(Calendar.MONTH) + 1;
        int day = c.get(Calendar.DAY_OF_MONTH);
        String week = weeks[c.get(Calendar.DAY_OF_WEEK) - 1];

        int h = c.get(Calendar.HOUR_OF_DAY);
        int m = c.get(Calendar.MINUTE);
        int s = c.get(Calendar.SECOND);

        return month + "月" + day + "日" + week + "\n"
                + String.format(Locale.CHINA, "%02d:%02d:%02d", h, m, s);
    }

    private static String formatSpeed(long bps) {
        DecimalFormat df = new DecimalFormat("0.0");

        if (bps < 1024) {
            return df.format(bps) + "B";
        }

        double kb = bps / 1024.0;
        if (kb < 1024) {
            return df.format(kb) + "K";
        }

        double mb = kb / 1024.0;
        return df.format(mb) + "M";
    }

    private static int dp(Context ctx, int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                v,
                ctx.getResources().getDisplayMetrics()
        );
    }

    private static String getIdName(View v) {
        try {
            int id = v.getId();
            if (id == View.NO_ID) return "";
            return v.getResources().getResourceEntryName(id);
        } catch (Throwable t) {
            return "";
        }
    }
}
