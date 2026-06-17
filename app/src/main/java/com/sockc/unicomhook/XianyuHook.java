package com.sockc.unicomhook;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XianyuHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_Xianyu: ";
    private static final String TARGET_PACKAGE = "com.taobao.idlefish";

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "跳过", "关闭", "跳過", "skip", "close", "略过"
    ));

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) return;
        if (!TARGET_PACKAGE.equals(lpparam.processName)) return;

        XposedBridge.log(TAG + "V3 已注入主进程: " + lpparam.processName);

        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                String cls = activity.getClass().getName();
                XposedBridge.log(TAG + "onResume -> " + cls);

                if (!"com.taobao.fleamarket.home.activity.InitActivity".equals(cls)) return;

                XposedBridge.log(TAG + "命中 InitActivity，准备扫描视图树");
                View root = activity.getWindow() == null ? null : activity.getWindow().getDecorView();
                if (root == null) return;

                startScanAndClick(activity, root);
            }
        });
    }

    private void startScanAndClick(final Activity activity, final View root) {
        Handler handler = new Handler(Looper.getMainLooper());

        Runnable task = new Runnable() {
            int count = 0;

            @Override
            public void run() {
                try {
                    if (activity.isFinishing()) return;

                    XposedBridge.log(TAG + "第 " + (count + 1) + " 次扫描开始");
                    dumpViewTree(root, 0);

                    View target = findSkipOrCloseView(root);
                    if (target != null) {
                        XposedBridge.log(TAG + "找到候选控件: " + viewInfo(target));
                        boolean ok = target.performClick();
                        XposedBridge.log(TAG + "performClick 结果: " + ok);

                        if (!ok) {
                            XposedBridge.log(TAG + "performClick 失败，尝试父级 clickable");
                            View parent = findClickableParent(target);
                            if (parent != null) {
                                XposedBridge.log(TAG + "父级控件: " + viewInfo(parent));
                                boolean ok2 = parent.performClick();
                                XposedBridge.log(TAG + "父级 performClick 结果: " + ok2);
                            }
                        }
                        return;
                    }

                    count++;
                    if (count < 10) {
                        handler.postDelayed(this, 300);
                    } else {
                        XposedBridge.log(TAG + "扫描结束，未找到跳过/关闭候选控件");
                    }
                } catch (Throwable t) {
                    XposedBridge.log(TAG + "扫描异常: " + t);
                }
            }
        };

        handler.postDelayed(task, 300);
    }

    private View findSkipOrCloseView(View view) {
        if (view == null || !view.isShown()) return null;

        CharSequence text = null;
        CharSequence desc = null;

        if (view instanceof TextView) {
            text = ((TextView) view).getText();
        }
        desc = view.getContentDescription();

        if (!TextUtils.isEmpty(text) && containsKeyword(text.toString())) {
            return view;
        }

        if (!TextUtils.isEmpty(desc) && containsKeyword(desc.toString())) {
            return view;
        }

        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View found = findSkipOrCloseView(vg.getChildAt(i));
                if (found != null) return found;
            }
        }

        return null;
    }

    private boolean containsKeyword(String s) {
        if (s == null) return false;
        String lower = s.trim().toLowerCase();
        for (String k : KEYWORDS) {
            if (lower.contains(k.toLowerCase())) return true;
        }
        return false;
    }

    private void dumpViewTree(View view, int depth) {
        if (view == null || !view.isShown()) return;

        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            prefix.append("  ");
        }

        String info = prefix + viewInfo(view);
        XposedBridge.log(TAG + info);

        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                dumpViewTree(vg.getChildAt(i), depth + 1);
            }
        }
    }

    private String viewInfo(View view) {
        String cls = view.getClass().getName();
        String text = "";
        String desc = "";
        boolean clickable = view.isClickable();

        if (view instanceof TextView) {
            CharSequence t = ((TextView) view).getText();
            if (!TextUtils.isEmpty(t)) {
                text = " text=" + safe(t.toString());
            }
        }

        CharSequence d = view.getContentDescription();
        if (!TextUtils.isEmpty(d)) {
            desc = " desc=" + safe(d.toString());
        }

        Rect rect = new Rect();
        view.getGlobalVisibleRect(rect);

        return cls
                + " clickable=" + clickable
                + " rect=[" + rect.left + "," + rect.top + "," + rect.right + "," + rect.bottom + "]"
                + text
                + desc;
    }

    private String safe(String s) {
        if (s == null) return "";
        s = s.replace("\n", "\\n").replace("\r", "");
        if (s.length() > 80) {
            return "\"" + s.substring(0, 80) + "...\"";
        }
        return "\"" + s + "\"";
    }

    private View findClickableParent(View view) {
        android.view.ViewParent p = view.getParent();
        while (p instanceof View) {
            View pv = (View) p;
            if (pv.isClickable() && pv.isShown()) return pv;
            p = pv.getParent();
        }
        return null;
    }
}
