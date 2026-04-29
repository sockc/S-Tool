package com.sockc.unicomhook;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class UnicomHook implements IXposedHookLoadPackage {
    private static final String TAG = "UnicomHook: ";
    private static final String TARGET_PACKAGE = "com.sinovatech.unicom.ui";
    private static final String SPLASH_ACTIVITY = "com.sinovatech.unicom.basic.ui.activity.WelcomeClient";
    private static final String MAIN_ACTIVITY = "com.sinovatech.unicom.basic.ui.activity.MainActivity";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "已注入联通客户端，正在清理开屏广告...");

        XposedHelpers.findAndHookMethod(SPLASH_ACTIVITY, lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                XposedBridge.log(TAG + "拦截到开屏页，正在跳转主界面...");
                
                // 构造跳转 Intent
                Intent intent = new Intent();
                intent.setClassName(TARGET_PACKAGE, MAIN_ACTIVITY);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                
                activity.startActivity(intent);
                activity.finish(); // 结束广告页
            }
        });
    }
}
