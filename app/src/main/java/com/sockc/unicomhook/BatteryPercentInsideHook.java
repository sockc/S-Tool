package com.sockc.unicomhook;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.lang.reflect.Field;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class BatteryPercentInsideHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_BatteryInside: ";

    private static final String[] SYSTEMUI_PACKAGES = {
            "com.android.systemui",
            "com.oplus.systemui"
    };

    private static final String[] BATTERY_VIEW_CLASSES = {
            "com.android.systemui.battery.BatteryMeterView",
            "com.android.systemui.statusbar.phone.BatteryMeterView",
            "com.android.systemui.statusbar.BatteryMeterView",
            "com.oplus.systemui.statusbar.widget.OplusBatteryMeterView",
            "com.oplus.systemui.statusbar.phone.OplusBatteryMeterView"
    };

    private static final String INSIDE_TAG = "sockc_inside_battery_percent";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!isSystemUiProcess(lpparam.packageName, lpparam.processName)) return;

        XposedBridge.log(TAG + "注入到 SystemUI: " + lpparam.packageName);

        Class<?> batteryCls = findFirstExistingClass(lpparam.classLoader, BATTERY_VIEW_CLASSES);
        if (batteryCls == null) {
            XposedBridge.log(TAG + "没找到 BatteryMeterView 候选类");
            return;
        }

        XposedBridge.log(TAG + "命中电池类: " + batteryCls.getName());

        XposedBridge.hookAllConstructors(batteryCls, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                tryInstall((View) param.thisObject);
            }
        });

        hookMethodIfExists(batteryCls, "onAttachedToWindow", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                tryInstall((View) param.thisObject);
            }
        });

        // 常见更新点，哪个存在就会生效
        String[] updateMethods = {
                "onBatteryLevelChanged",
                "updatePercentText",
                "updateBatteryState",
                "updateView",
                "updateAll"
        };

        for (String method : updateMethods) {
            hookAllMethodsIfExists(batteryCls, method, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        updateInsidePercent((View) param.thisObject);
                    } catch (Throwable t) {
                        XposedBridge.log(TAG + "更新文字失败: " + t);
                    }
                }
            });
        }
    }

    private boolean isSystemUiProcess(String pkg, String proc) {
        if (pkg == null || proc == null) return false;
        if (!pkg.equals(proc)) return false;

        for (String p : SYSTEMUI_PACKAGES) {
            if (p.equals(pkg)) return true;
        }
        return false;
    }

    private Class<?> findFirstExistingClass(ClassLoader cl, String[] names) {
        for (String name : names) {
            try {
                return XposedHelpers.findClass(name, cl);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private void hookMethodIfExists(Class<?> cls, String methodName, XC_MethodHook hook) {
        try {
            XposedHelpers.findAndHookMethod(cls, methodName, hook);
        } catch (Throwable ignored) {
        }
    }

    private void hookAllMethodsIfExists(Class<?> cls, String methodName, XC_MethodHook hook) {
        try {
            XposedBridge.hookAllMethods(cls, methodName, hook);
        } catch (Throwable ignored) {
        }
    }

    private void tryInstall(View root) {
        if (!(root instanceof ViewGroup)) return;

        ViewGroup host = (ViewGroup) root;
        host.post(() -> {
            try {
                hideOutsidePercent(host);

                TextView inside = findInsideText(host);
                if (inside == null) {
                    inside = createInsideText(host.getContext());

                    ViewGroup overlayHost = host;
                    if (host instanceof FrameLayout) {
                        overlayHost.addView(inside, new FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                Gravity.CENTER
                        ));
                    } else {
                        overlayHost.addView(inside, new ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                        ));
                    }

                    XposedBridge.log(TAG + "已添加图标内百分比层");
                }

                updateInsidePercent(host);
            } catch (Throwable t) {
                XposedBridge.log(TAG + "安装失败: " + t);
            }
        });
    }

    private TextView createInsideText(Context context) {
        TextView tv = new TextView(context);
        tv.setTag(INSIDE_TAG);
        tv.setGravity(Gravity.CENTER);
        tv.setIncludeFontPadding(false);
        tv.setSingleLine(true);
        tv.setTextColor(Color.WHITE);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setClickable(false);
        tv.setFocusable(false);
        tv.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return tv;
    }

    private TextView findInsideText(ViewGroup root) {
        View v = root.findViewWithTag(INSIDE_TAG);
        if (v instanceof TextView) return (TextView) v;
        return null;
    }

    private void updateInsidePercent(View root) {
        if (!(root instanceof ViewGroup)) return;

        ViewGroup host = (ViewGroup) root;
        TextView tv = findInsideText(host);
        if (tv == null) return;

        int level = readBatteryLevelFromObject(host);
        if (level < 0) {
            level = readBatteryLevelFromSystem(host.getContext());
        }
        if (level < 0) return;

        // 100 会比较挤，先直接显示 100；后续可按你喜好改成 "99" 或缩小字号
        tv.setText(String.valueOf(level));

        float textPx = Math.max(14f, host.getHeight() * 0.42f);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);

        // 有些 ROM 电池图标不在正中，必要时微调这个值
        tv.setTranslationX(0f);
        tv.setTranslationY(0f);
        tv.bringToFront();
    }

    private int readBatteryLevelFromObject(Object obj) {
        String[] fieldNames = {
                "mLevel",
                "mBatteryLevel",
                "mCurrentLevel",
                "mBatteryPercent"
        };

        for (String name : fieldNames) {
            try {
                Field f = XposedHelpers.findField(obj.getClass(), name);
                f.setAccessible(true);
                Object value = f.get(obj);
                if (value instanceof Integer) {
                    int level = (Integer) value;
                    if (level >= 0 && level <= 100) return level;
                }
            } catch (Throwable ignored) {
            }
        }
        return -1;
    }

    private int readBatteryLevelFromSystem(Context context) {
        try {
            Intent battery = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery == null) return -1;
            return battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private void hideOutsidePercent(ViewGroup root) {
        hidePercentTextRecursive(root);
    }

    private void hidePercentTextRecursive(View view) {
        if (view == null) return;

        if (view instanceof TextView) {
            TextView tv = (TextView) view;

            CharSequence text = tv.getText();
            String idName = getIdName(tv);

            boolean looksLikePercent =
                    (!TextUtils.isEmpty(text) && text.toString().matches("\\d{1,3}%?"))
                            || idName.contains("percent")
                            || idName.contains("battery_percentage")
                            || idName.contains("battery_percent");

            // 不要把我们自己加进去的 TextView 隐藏了
            if (looksLikePercent && tv.getTag() == null) {
                tv.setVisibility(View.GONE);
                XposedBridge.log(TAG + "已隐藏外部百分比: id=" + idName + " text=" + text);
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                hidePercentTextRecursive(vg.getChildAt(i));
            }
        }
    }

    private String getIdName(View view) {
        int id = view.getId();
        if (id == View.NO_ID) return "";
        try {
            return view.getResources().getResourceEntryName(id);
        } catch (Throwable ignored) {
            return "";
        }
    }
}
