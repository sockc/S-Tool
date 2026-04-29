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
        // 只拦截淘宝
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "已注入淘宝，猎杀开屏广告中...");

        // 拦截所有 Activity 的创建
        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                String className = activity.getClass().getName();

                // 识别淘宝的开屏/欢迎页 (淘宝通常使用 welcome 或 bootimage)
                if (className.toLowerCase().contains("welcome") || className.toLowerCase().contains("bootimage")) {
                    XposedBridge.log(TAG + "成功拦截到开屏页面: " + className);
                    
                    // 延迟 500 毫秒执行扫描，等广告和“跳过”按钮被渲染出来
                    activity.getWindow().getDecorView().postDelayed(() -> {
                        scanAndClickSkip(activity.getWindow().getDecorView());
                    }, 500); 
                }
            }
        });
    }

    /**
     * 递归扫描当前界面的所有控件，找到“跳过”并触发点击
     */
    private void scanAndClickSkip(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                
                // 如果这是一个文字控件
                if (child instanceof TextView) {
                    String text = ((TextView) child).getText().toString();
                    // 模糊匹配跳过按钮的常见文案
                    if (text.contains("跳过") || text.contains("跳转") || text.contains("跳过广告")) {
                        XposedBridge.log(TAG + "锁定目标！发现 [" + text + "] 按钮，正在执行无感秒点！");
                        
                        // 触发系统的点击事件，相当于人的手指按了下去
                        child.performClick(); 
                        
                        // 如果它的父布局才是真正的点击响应区，连父布局一起点
                        if (child.getParent() instanceof View) {
                            ((View) child.getParent()).performClick();
                        }
                        return; // 搞定收工，停止扫描
                    }
                }
                // 没找到就继续往深处挖
                scanAndClickSkip(child);
            }
        }
    }
}
