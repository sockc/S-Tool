package com.sockc.unicomhook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class TalkatoneBypass implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_Talk_Bypass: ";
    private static final String TARGET_PACKAGE = "com.talkatone.android";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "正在为 Talkatone 伪装『无谷歌』环境...");

        // 策略：拦截谷歌官方的“检查谷歌服务可用性”方法
        // 强制让它返回“SERVICE_MISSING”（服务缺失）或“SERVICE_DISABLED”（服务已禁用）
        try {
            // 这是 Google Play Services SDK 里的核心检查类
            Class<?> googleApiAvailability = XposedHelpers.findClass(
                "com.google.android.gms.common.GoogleApiAvailability", lpparam.classLoader);

            XposedHelpers.findAndHookMethod(googleApiAvailability, "isGooglePlayServicesAvailable", 
                android.content.Context.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) {
                    XposedBridge.log(TAG + "成功拦截校验：骗它说谷歌服务不存在！");
                    // 返回 1 代表 SUCCESS，这里我们返回 18 (SERVICE_UPDATING) 
                    // 或者 2 (SERVICE_VERSION_UPDATE_REQUIRED) 甚至 3 (SERVICE_DISABLED)
                    // 试一下 3 (代表服务被禁用了)
                    return 2; 
                }
            });

            // 同时拦截另一种常见的检查方式
            XposedHelpers.findAndHookMethod("com.google.android.gms.common.GooglePlayServicesUtil", 
                lpparam.classLoader, "isGooglePlayServicesAvailable", 
                android.content.Context.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) {
                    return 2; 
                }
            });

        } catch (Throwable t) {
            XposedBridge.log(TAG + "伪装失败（可能混淆了）：" + t.getMessage());
        }
    }
}
