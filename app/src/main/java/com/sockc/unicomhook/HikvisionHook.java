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

public class HikvisionHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_Hik: ";
    // 注意：海康有多个版本，常见的是 Hik-Connect
    private static final String TARGET_PACKAGE = "com.hikvision.hikconnect"; 

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "已进入海康威视，开始守卫监控纯净...");

        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                // 延迟一秒扫描，监控 App 的启动通常比普通 App 重一点
                activity.getWindow().getDecorView().postDelayed(() -> {
                    recursiveScan(activity.getWindow().getDecorView());
                }, 1000);
            }
        });
    }

    private void recursiveScan(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof TextView) {
                    String text = ((TextView) child).getText().toString();
                    if (text.contains("跳过") || text.contains("Skip")) {
                        child.performClick();
                        XposedBridge.log(TAG + "成功跳过海康开屏广告");
                        return;
                    }
                }
                recursiveScan(child);
            }
        }
    }
}
