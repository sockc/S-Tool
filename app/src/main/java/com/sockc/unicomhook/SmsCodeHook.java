package com.sockc.unicomhook;

import android.app.Application;
import android.content.Context;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class SmsCodeHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_SmsCode: ";
    private static final String TARGET_PACKAGE = "com.google.android.apps.messaging";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        XposedBridge.log(TAG + "handleLoadPackage -> "
                + lpparam.packageName + " / " + lpparam.processName);

        if (!TARGET_PACKAGE.equals(lpparam.packageName)) return;

        XposedBridge.log(TAG + "命中谷歌信息");

        XposedHelpers.findAndHookMethod(
                Application.class,
                "onCreate",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Context context = (Context) param.thisObject;
                        XposedBridge.log(TAG + "Application.onCreate -> " + context.getPackageName());
                        SmsCodeAutoCopy.start(context);
                    }
                }
        );
    }
}
