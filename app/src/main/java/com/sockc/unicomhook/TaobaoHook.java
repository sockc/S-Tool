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

public class TaobaoHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_Taobao: ";
    private static final String TARGET_PACKAGE = "com.taobao.taobao";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        // 1. 核心修复：双重校验，只在“主进程”中执行，放过那些沙盒和推送子进程
        if (!lpparam.packageName.equals(TARGET_PACKAGE) || !lpparam.processName.equals(TARGET_PACKAGE)) {
            return;
        }

        XposedBridge.log(TAG + "已精准注入淘宝主进程...");

        try {
            XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = (Activity) param.thisObject;
                    String className = activity.getClass().getName();

                    if (className.toLowerCase().contains("welcome") || className.toLowerCase().contains("bootimage")) {
                        XposedBridge.log(TAG + "成功拦截到开屏页面: " + className);
                        
                        // 延迟 800 毫秒，等它把牛皮癣广告全画出来
                        activity.getWindow().getDecorView().postDelayed(() -> {
                            // 2. 核心修复：穿上防弹衣，即使找不到或者出错，淘宝也不会死
                            try {
                                scanAndClickSkip(activity.getWindow().getDecorView());
                            } catch (Exception e) {
                                XposedBridge.log(TAG + "扫描 UI 时发生意外，已拦截崩溃: " + e.getMessage());
                            }
                        }, 800); 
                    }
                }
            });
        } catch (Exception e) {
            XposedBridge.log(TAG + "Hook 注入失败: " + e.getMessage());
        }
    }

    private void scanAndClickSkip(View view) {
        if (view == null) return;

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                
                if (child instanceof TextView) {
                    CharSequence cs = ((TextView) child).getText();
                    if (cs != null) {
                        String text = cs.toString();
                        if (text.contains("跳过") || text.contains("跳转")) {
                            XposedBridge.log(TAG + "锁定目标！发现 [" + text + "]，执行无感秒点！");
                            child.performClick();
                            if (child.getParent() instanceof View) {
                                ((View) child.getParent()).performClick();
                            }
                            return; 
                        }
                    }
                }
                scanAndClickSkip(child);
            }
        }
    }
}
