package com.sockc.unicomhook;

import android.app.Activity;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XianyuHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_Xianyu: ";
    private static final String TARGET_PACKAGE = "com.taobao.idlefish";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) return;
        if (!TARGET_PACKAGE.equals(lpparam.processName)) return;

        XposedBridge.log(TAG + "V2 已注入主进程: " + lpparam.processName);

        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                XposedBridge.log(TAG + "Activity -> " + activity.getClass().getName());
            }
        });
    }
}
