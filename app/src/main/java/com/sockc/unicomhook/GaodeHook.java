package com.sockc.unicomhook;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class GaodeHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_Gaode: ";
    private static final String TARGET_PACKAGE = "com.autonavi.minimap";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        // 1. 核心防御：只注入高德的“主进程”，坚决放过 locationservice 等后台服务
        if (!lpparam.packageName.equals(TARGET_PACKAGE) || !lpparam.processName.equals(TARGET_PACKAGE)) {
            return;
        }

        XposedBridge.log(TAG + "已精准注入高德主进程...");

        try {
            XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = (Activity) param.thisObject;
                    
                    // 2. 核心战术：新版高德广告是动态蹦出来的，我们用全局监听器“守株待兔”
                    ViewTreeObserver.OnGlobalLayoutListener listener = new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            try {
                                scanAndClickSkip(activity.getWindow().getDecorView());
                            } catch (Exception e) {
                                // 穿上防弹衣，扫不到也绝对不让高德闪退
                            }
                        }
                    };
                    activity.getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(listener);
                }
            });
        } catch (Exception e) {
            XposedBridge.log(TAG + "Hook 注入失败: " + e.getMessage());
        }
    }

    // 递归扫描 UI 树
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
                        // 覆盖高德新版可能出现的多种广告文案
                        if (text.contains("跳过") || text.contains("关闭广告")) {
                            XposedBridge.log(TAG + "锁定高德开屏目标！发现 [" + text + "]，执行无感秒点！");
                            
                            // 模拟手指点击
                            child.performClick();
                            if (child.getParent() instanceof View) {
                                ((View) child.getParent()).performClick();
                            }
                            
                            // 暴力手段：点完顺手把这个按钮从屏幕上抹除，防止闪烁残留
                            child.setVisibility(View.GONE);
                            return; 
                        }
                    }
                }
                scanAndClickSkip(child); // 继续深挖
            }
        }
    }
}
