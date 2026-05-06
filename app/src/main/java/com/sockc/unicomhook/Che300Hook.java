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

public class Che300Hook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_Che300: ";
    
    // 把个人版和专业版的包名都加进去，双管齐下
    private static final String[] TARGET_PACKAGES = {"com.car300.activity", "com.che300.price"};

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        boolean isTarget = false;
        for (String pkg : TARGET_PACKAGES) {
            if (lpparam.packageName.equals(pkg)) {
                isTarget = true;
                break;
            }
        }
        if (!isTarget) return;

        XposedBridge.log(TAG + "已进入车300二手车，准备拦截开屏广告...");

        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                final View decorView = activity.getWindow().getDecorView();

                // 监听布局变化，只要“跳过”按钮一露头就秒杀
                decorView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    boolean clicked = false;

                    @Override
                    public void onGlobalLayout() {
                        if (!clicked && scanAndSkip(decorView)) {
                            clicked = true;
                            // 刺杀成功，立刻销毁监听器，不占用多余性能
                            decorView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    }
                });
            }
        });
    }

    // 递归扫描屏幕上的控件
    private boolean scanAndSkip(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof TextView) {
                    String text = ((TextView) child).getText().toString();
                    if (text.contains("跳过") || text.contains("关闭")) {
                        // 模拟人类点击文字
                        child.performClick();
                        XposedBridge.log(TAG + "秒点车300开屏广告！");
                        
                        // 保险机制：有些毒瘤广告的点击事件绑在它的父容器上
                        if (child.getParent() instanceof View) {
                            ((View) child.getParent()).performClick();
                        }
                        return true;
                    }
                } else if (scanAndSkip(child)) {
                    return true;
                }
            }
        }
        return false;
    }
}
