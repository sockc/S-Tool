package com.sockc.unicomhook;

import android.app.Application;
import android.content.Context;

import com.sockc.unicomhook.compat.XC_MethodHook;
import com.sockc.unicomhook.compat.XposedBridge;
import com.sockc.unicomhook.compat.XposedHelpers;

public class SmsCodeHook implements HookModule {
    private static final String TAG = "Sockc_SmsCode: ";
    private static final String TARGET_PACKAGE = "com.google.android.apps.messaging";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        XposedBridge.log(TAG + "handleLoadPackage -> "
                + lpparam.packageName + " / " + lpparam.processName);

        if (!TARGET_PACKAGE.equals(lpparam.packageName)) return;
        if (!TARGET_PACKAGE.equals(lpparam.processName)) return;
        
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
