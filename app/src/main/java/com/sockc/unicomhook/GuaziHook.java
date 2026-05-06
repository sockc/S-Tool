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

public class GuaziHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_Guazi: ";
    private static final String TARGET_PACKAGE = "com.ganji.android.haoche";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE) || !lpparam.processName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "已进入瓜子二手车主进程...");

        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                View decorView = activity.getWindow().getDecorView();

                // 使用实时布局监听，只要广告按钮渲染出来，立刻点掉
                decorView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (fastScan(decorView)) {
                            // 成功点击后移除监听，防止循环点击
                            decorView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    }
                });
            }
        });
    }

    private boolean fastScan(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof TextView) {
                    String text = ((TextView) child).getText().toString();
                    if (text.contains("跳过") || text.contains("关闭")) {
                        child.performClick();
                        XposedBridge.log(TAG + "秒点瓜子开屏广告！");
                        return true;
                    }
                } else if (fastScan(child)) return true;
            }
        }
        return false;
    }
}
