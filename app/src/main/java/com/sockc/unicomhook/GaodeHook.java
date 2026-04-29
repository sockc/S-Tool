package com.sockc.unicomhook;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class GaodeHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_Gaode: ";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.autonavi.minimap")) return;

        // 1. 强制干掉开屏 (直接 Hook 所有的 Activity，只要包含 Splash 字符就 finish)
        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                String className = activity.getClass().getName();
                if (className.contains("Splash") || className.contains("Welcome")) {
                    XposedBridge.log(TAG + "发现开屏 Activity: " + className + "，正在终结...");
                    activity.finish();
                }
                
                // 2. 首页净化：不再依赖 ID 名，改为“特征识别”
                if (className.contains("NewMainActivity")) {
                    XposedBridge.log(TAG + "已进入主界面，开始扫描并删除精彩笔记...");
                    // 延迟 3 秒执行，等它加载完
                    activity.getWindow().getDecorView().postDelayed(() -> {
                        scanAndHideView(activity.getWindow().getDecorView());
                    }, 3000);
                }
            }
        });
    }

    private void scanAndHideView(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                
                // 识别特征：如果这是一个 TextView 且内容包含“精彩笔记”
                if (child instanceof TextView) {
                    String text = ((TextView) child).getText().toString();
                    if (text.contains("精彩笔记") || text.contains("探索") || text.contains("对话")) {
                        XposedBridge.log(TAG + "找到目标文字: " + text + "，正在隐藏其父容器...");
                        // 隐藏它的爷爷辈（通常卡片是在这个文字的父布局上）
                        if (child.getParent() instanceof View) {
                            ((View)child.getParent()).setVisibility(View.GONE);
                        }
                    }
                }
                scanAndHideView(child); // 递归遍历
            }
        }
    }
}
