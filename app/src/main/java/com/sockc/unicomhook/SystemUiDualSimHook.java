package com.sockc.unicomhook;

import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.WeakHashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class SystemUiDualSimHook implements IXposedHookLoadPackage {
    private static final String TAG = "SystemUiDualSimHook";
    private static final String TARGET = "com.android.systemui";

    private static boolean installed = false;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final WeakHashMap<ViewGroup, Boolean> BADGED = new WeakHashMap<>();

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET.equals(lpparam.packageName)) return;

        XposedBridge.log(TAG + ": loaded " + lpparam.packageName);

        try {
            XposedBridge.hookAllMethods(Application.class, "attach", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Context ctx = (Context) param.args[0];
                    XposedBridge.log(TAG + ": attach ok, package=" + ctx.getPackageName());
                    installViewHook();
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": hook Application.attach failed: " + t);
        }
    }

    private static void installViewHook() {
        if (installed) return;
        installed = true;

        try {
            XposedBridge.hookAllMethods(ViewGroup.class, "addView", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        if (!(param.thisObject instanceof ViewGroup)) return;
                        ViewGroup parent = (ViewGroup) param.thisObject;

                        MAIN.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    processTree(parent);
                                } catch (Throwable t) {
                                    XposedBridge.log(TAG + ": processTree failed: " + t);
                                }
                            }
                        }, 80);
                    } catch (Throwable t) {
                        XposedBridge.log(TAG + ": addView hook failed: " + t);
                    }
                }
            });

            XposedBridge.log(TAG + ": ViewGroup.addView hook installed");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": installViewHook failed: " + t);
        }
    }

    private static void processTree(View root) {
        if (root == null) return;

        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            processOneParent(vg);

            int count = vg.getChildCount();
            for (int i = 0; i < count; i++) {
                processTree(vg.getChildAt(i));
            }
        }
    }

    private static void processOneParent(ViewGroup parent) {
        if (parent == null) return;

        List<View> mobileGroups = new ArrayList<>();

        int count = parent.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = parent.getChildAt(i);

            if (isMobileSignalGroup(child)) {
                mobileGroups.add(child);
            }
        }

        if (mobileGroups.size() < 2) return;

        View first = mobileGroups.get(0);

        for (int i = 1; i < mobileGroups.size(); i++) {
            View v = mobileGroups.get(i);
            if (v.getVisibility() != View.GONE) {
                v.setVisibility(View.GONE);
                XposedBridge.log(TAG + ": hide second mobile signal view: " + viewName(v));
            }
        }

        addDualSimBadge(first);
    }

    private static boolean isMobileSignalGroup(View v) {
        if (v == null) return false;

        String name = viewName(v).toLowerCase(Locale.ROOT);
        String cls = v.getClass().getName().toLowerCase(Locale.ROOT);

        boolean nameHit =
                name.contains("mobile")
                        && (
                        name.contains("signal")
                                || name.contains("combo")
                                || name.contains("group")
                                || name.contains("container")
                                || name.contains("phone")
                );

        boolean classHit =
                cls.contains("mobile")
                        && (
                        cls.contains("signal")
                                || cls.contains("statusbar")
                                || cls.contains("icon")
                );

        if (!(nameHit || classHit)) return false;

        // 尽量避免影响控制中心 / QS 面板里的网络卡片
        if (isInsideQuickSettings(v)) return false;

        return true;
    }

    private static boolean isInsideQuickSettings(View v) {
        View cur = v;

        for (int i = 0; i < 8 && cur != null; i++) {
            String name = viewName(cur).toLowerCase(Locale.ROOT);
            String cls = cur.getClass().getName().toLowerCase(Locale.ROOT);

            if (name.contains("qs")
                    || name.contains("quick")
                    || name.contains("shade")
                    || cls.contains("qs")
                    || cls.contains("quicksettings")) {
                return true;
            }

            if (!(cur.getParent() instanceof View)) break;
            cur = (View) cur.getParent();
        }

        return false;
    }

    private static void addDualSimBadge(View target) {
        if (!(target instanceof ViewGroup)) return;

        ViewGroup group = (ViewGroup) target;

        if (BADGED.containsKey(group)) return;
        BADGED.put(group, true);

        Context ctx = group.getContext();

        TextView badge = new TextView(ctx);
        badge.setText("2");
        badge.setTextColor(Color.WHITE);
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 7);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setIncludeFontPadding(false);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.BLACK);
        badge.setBackground(bg);

        int size = dp(ctx, 9);
        int margin = dp(ctx, 0);

        try {
            if (group instanceof FrameLayout) {
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size);
                lp.gravity = Gravity.RIGHT | Gravity.BOTTOM;
                lp.rightMargin = margin;
                lp.bottomMargin = margin;
                group.addView(badge, lp);
            } else if (group instanceof LinearLayout) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
                lp.leftMargin = dp(ctx, 1);
                lp.gravity = Gravity.BOTTOM;
                group.addView(badge, lp);
            } else {
                ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(size, size);
                group.addView(badge, lp);
            }

            XposedBridge.log(TAG + ": add dual sim badge on " + viewName(group));
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": addDualSimBadge failed: " + t);
        }
    }

    private static int dp(Context ctx, float value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                ctx.getResources().getDisplayMetrics()
        );
    }

    private static String viewName(View v) {
        if (v == null) return "null";

        try {
            int id = v.getId();
            if (id != View.NO_ID) {
                return v.getResources().getResourceEntryName(id);
            }
        } catch (Throwable ignored) {
        }

        return v.getClass().getSimpleName();
    }
}
