package com.sockc.unicomhook;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnLayoutChangeListener;
import android.widget.TextView;

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

    private static final String[] ICON_FIELD_NAMES = {
            "mBatteryIconView",
            "mBatteryIcon",
            "mIconView",
            "mBatteryView",
            "mBattery"
    };

    private static final String FIELD_INSIDE_TEXT = "sockc_inside_battery_text";
    private static final String FIELD_LAYOUT_LISTENER = "sockc_battery_layout_listener";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!isSystemUiProcess(lpparam.packageName, lpparam.processName)) return;

        XposedBridge.log(TAG + "注入到 SystemUI: " + lpparam.packageName);

        final Class<?> batteryCls = findFirstExistingClass(lpparam.classLoader, BATTERY_VIEW_CLASSES);
        if (batteryCls == null) {
            XposedBridge.log(TAG + "没找到 BatteryMeterView 候选类");
            return;
        }

        XposedBridge.log(TAG + "命中电池类: " + batteryCls.getName());

        // 备份：构造时也尝试一次
        XposedBridge.hookAllConstructors(batteryCls, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    final View host = (View) param.thisObject;
                    XposedBridge.log(TAG + "构造触发 BatteryMeterView");
                    attachLayoutListenerIfNeeded(host);

                    host.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                installOrUpdate(host);
                            } catch (Throwable t) {
                                XposedBridge.log(TAG + "构造后 post installOrUpdate 失败: " + t);
                            }
                        }
                    });

                    host.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                installOrUpdate(host);
                            } catch (Throwable t) {
                                XposedBridge.log(TAG + "构造后延迟 installOrUpdate 失败: " + t);
                            }
                        }
                    }, 1200);
                } catch (Throwable t) {
                    XposedBridge.log(TAG + "构造后处理失败: " + t);
                }
            }
        });

        // 主力：谁把 BatteryMeterView 加进状态栏，就在那里拦
        XposedBridge.hookAllMethods(ViewGroup.class, "addView", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    if (param.args == null || param.args.length == 0) return;
                    Object arg0 = param.args[0];
                    if (!(arg0 instanceof View)) return;

                    final View child = (View) arg0;
                    if (!batteryCls.isInstance(child)
                            && !child.getClass().getName().equals(batteryCls.getName())) {
                        return;
                    }

                    XposedBridge.log(TAG + "addView 命中 BatteryMeterView: " + child.getClass().getName());

                    attachLayoutListenerIfNeeded(child);

                    child.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                installOrUpdate(child);
                            } catch (Throwable t) {
                                XposedBridge.log(TAG + "addView post installOrUpdate 失败: " + t);
                            }
                        }
                    });

                    child.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                installOrUpdate(child);
                            } catch (Throwable t) {
                                XposedBridge.log(TAG + "addView 延迟 installOrUpdate 失败: " + t);
                            }
                        }
                    }, 800);

                } catch (Throwable t) {
                    XposedBridge.log(TAG + "addView Hook 失败: " + t);
                }
            }
        });
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

    private void attachLayoutListenerIfNeeded(View host) {
        try {
            Object old = XposedHelpers.getAdditionalInstanceField(host, FIELD_LAYOUT_LISTENER);
            if (old != null) return;

            OnLayoutChangeListener listener = new OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                           int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    try {
                        XposedBridge.log(TAG + "onLayoutChange 触发: " + v.getWidth() + "x" + v.getHeight());
                        installOrUpdate(v);
                    } catch (Throwable t) {
                        XposedBridge.log(TAG + "布局监听 installOrUpdate 失败: " + t);
                    }
                }
            };

            host.addOnLayoutChangeListener(listener);
            XposedHelpers.setAdditionalInstanceField(host, FIELD_LAYOUT_LISTENER, listener);
            XposedBridge.log(TAG + "已附加 OnLayoutChangeListener");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "附加布局监听失败: " + t);
        }
    }

    private void installOrUpdate(View host) {
        if (!(host instanceof ViewGroup)) return;

        ViewGroup batteryHost = (ViewGroup) host;

        if (batteryHost.getWidth() <= 0 || batteryHost.getHeight() <= 0) {
            XposedBridge.log(TAG + "宿主尺寸未就绪: " + batteryHost.getWidth() + "x" + batteryHost.getHeight());
            return;
        }

        View iconView = findBatteryIconView(batteryHost);
        if (iconView == null) {
            XposedBridge.log(TAG + "没找到内部电池图标 View，退回用宿主本身");
            iconView = batteryHost;
        }

        if (iconView.getWidth() <= 0 || iconView.getHeight() <= 0) {
            XposedBridge.log(TAG + "图标尺寸未就绪: " + iconView.getWidth() + "x" + iconView.getHeight());
            return;
        }

        TextView tv = getInsideText(batteryHost);
        if (tv == null) {
            tv = createInsideText(batteryHost.getContext());
            batteryHost.getOverlay().add(tv);
            XposedHelpers.setAdditionalInstanceField(batteryHost, FIELD_INSIDE_TEXT, tv);
            XposedBridge.log(TAG + "已添加图标内百分比 overlay");
        }

        updateInsideText(batteryHost, iconView, tv);
    }

    private TextView createInsideText(Context context) {
        TextView tv = new TextView(context);
        tv.setTextColor(Color.WHITE);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setIncludeFontPadding(false);
        tv.setSingleLine(true);
        tv.setClickable(false);
        tv.setFocusable(false);
        tv.setAlpha(0.96f);
        tv.setShadowLayer(1.2f, 0f, 0f, Color.BLACK);
        return tv;
    }

    private TextView getInsideText(View host) {
        try {
            Object obj = XposedHelpers.getAdditionalInstanceField(host, FIELD_INSIDE_TEXT);
            if (obj instanceof TextView) return (TextView) obj;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private View findBatteryIconView(View host) {
        for (String fieldName : ICON_FIELD_NAMES) {
            try {
                Object obj = XposedHelpers.getObjectField(host, fieldName);
                if (obj instanceof View) {
                    XposedBridge.log(TAG + "命中图标字段: " + fieldName);
                    return (View) obj;
                }
            } catch (Throwable ignored) {
            }
        }

        if (host instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) host;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View child = vg.getChildAt(i);
                String cls = child.getClass().getName().toLowerCase();
                String idName = getIdName(child).toLowerCase();

                boolean looksLikeIcon =
                        cls.contains("image")
                                || cls.contains("icon")
                                || idName.contains("battery")
                                || idName.contains("icon");

                if (looksLikeIcon && child.getWidth() > 0 && child.getHeight() > 0) {
                    XposedBridge.log(TAG + "命中兜底图标子 View: " + child.getClass().getName() + " id=" + idName);
                    return child;
                }
            }
        }

        return null;
    }

    private void updateInsideText(ViewGroup host, View iconView, TextView tv) {
        int level = readBatteryLevelFromObject(host);
        if (level < 0) {
            level = readBatteryLevelFromSystem(host.getContext());
        }
        if (level < 0) return;

        tv.setText(String.valueOf(level));

        int iconW = Math.max(iconView.getWidth(), 1);
        int iconH = Math.max(iconView.getHeight(), 1);

        float textPx = Math.max(8f, iconH * 0.48f);
        if (level >= 100) {
            textPx = Math.max(7f, iconH * 0.40f);
        }
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);

        int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        tv.measure(spec, spec);
        int tw = tv.getMeasuredWidth();
        int th = tv.getMeasuredHeight();
        tv.layout(0, 0, tw, th);

        float x = iconView.getLeft() + (iconW - tw) / 2f;
        float y = iconView.getTop() + (iconH - th) / 2f;

        tv.setX(x);
        tv.setY(y);
        tv.setVisibility(View.VISIBLE);

        XposedBridge.log(TAG + "更新电量文字: " + level
                + " host=" + host.getWidth() + "x" + host.getHeight()
                + " icon=" + iconW + "x" + iconH
                + " pos=(" + x + "," + y + ")");
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
                int level = XposedHelpers.getIntField(obj, name);
                if (level >= 0 && level <= 100) return level;
            } catch (Throwable ignored) {
            }

            try {
                Object value = XposedHelpers.getObjectField(obj, name);
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
