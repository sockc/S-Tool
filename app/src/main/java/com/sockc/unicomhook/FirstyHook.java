package com.sockc.unicomhook;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class FirstyHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_Firsty: ";
    // 修正后的官方包名
    private static final String TARGET_PACKAGE = "com.firsty.app";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        // 这里的过滤要放宽，因为广告可能在子进程里
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "已注入进程: " + lpparam.processName);

        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                final Activity activity = (Activity) param.thisObject;
                final View decorView = activity.getWindow().getDecorView();

                decorView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        // 尝试执行深度扫描并点击
                        if (deepScanAndClick(decorView)) {
                            XposedBridge.log(TAG + "成功捕获并点击关闭按钮！");
                        }
                    }
                });
            }
        });
    }

    private boolean deepScanAndClick(View view) {
        if (view == null || !view.isShown()) return false;

        // 1. 检查 TextView (文字识别)
        if (view instanceof TextView) {
            String text = ((TextView) view).getText().toString();
            if (text.equalsIgnoreCase("X") || text.contains("关闭") || text.equalsIgnoreCase("Close")) {
                return performValidClick(view);
            }
        }

        // 2. 检查 ContentDescription (很多图片按钮只有描述没有文字)
        CharSequence desc = view.getContentDescription();
        if (desc != null) {
            String d = desc.toString().toLowerCase();
            if (d.equals("x") || d.contains("close") || d.contains("dismiss") || d.contains("关闭")) {
                return performValidClick(view);
            }
        }

        // 3. 检查 Resource ID (针对常见的广告 SDK 按钮 ID)
        if (view.getId() != View.NO_ID) {
            try {
                String idName = view.getResources().getResourceEntryName(view.getId()).toLowerCase();
                if (idName.contains("close_button") || idName.contains("tt_skip") || idName.equals("btn_close")) {
                    return performValidClick(view);
                }
            } catch (Exception ignored) {}
        }

        // 4. 递归扫描子 View
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (deepScanAndClick(group.getChildAt(i))) return true;
            }
        }
        return false;
    }

    private boolean performValidClick(View view) {
        if (view.isClickable()) {
            view.performClick();
            return true;
        } else if (view.getParent() instanceof View && ((View)view.getParent()).isClickable()) {
            // 如果按钮本身不可点，点它的爹（常见于封装好的 SDK）
            ((View)view.getParent()).performClick();
            return true;
        }
        return false;
    }
}
