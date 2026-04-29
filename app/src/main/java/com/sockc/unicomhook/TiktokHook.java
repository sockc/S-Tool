package com.sockc.unicomhook;

import android.telephony.TelephonyManager;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class TiktokHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_TikTok: ";
    private static final String TARGET_PACKAGE = "com.zhiliaoapp.musically"; // TikTok 的包名

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "已注入 TikTok，开始伪装海外 SIM 卡...");

        try {
            // 1. 伪装国家代码 (ISO) 为美国 (us)
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getSimCountryIso", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) { return "us"; }
            });
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getNetworkCountryIso", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) { return "us"; }
            });

            // 2. 伪装运营商 MCC/MNC 为美国 AT&T (310410)
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getSimOperator", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) { return "310410"; }
            });
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getNetworkOperator", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) { return "310410"; }
            });

            // 3. 伪装运营商名称
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getSimOperatorName", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) { return "AT&T"; }
            });
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getNetworkOperatorName", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) { return "AT&T"; }
            });

        } catch (Throwable t) {
            XposedBridge.log(TAG + "SIM 卡伪装失败: " + t.getMessage());
        }
    }
}
