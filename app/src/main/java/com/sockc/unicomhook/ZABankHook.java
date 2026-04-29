package com.sockc.unicomhook;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

import java.util.ArrayList;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ZABankHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_ZABank: ";
    private static final String TARGET_PACKAGE = "com.zabank.android";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "开始为众安银行伪装环境...");

        // 1. 隐藏“开发者选项”和“ADB调试”
        XposedHelpers.findAndHookMethod(Settings.Global.class, "getInt", 
            android.content.ContentResolver.class, String.class, int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                String name = (String) param.args[1];
                if ("development_settings_enabled".equals(name) || "adb_enabled".equals(name)) {
                    return 0; // 骗它说没开
                }
                try {
                    return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                } catch (Exception e) {
                    return 0; 
                }
            }
        });

        // 2. 隐藏“无障碍服务” (针对 GKD)
        XposedHelpers.findAndHookMethod(AccessibilityManager.class, "getEnabledAccessibilityServiceList", int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                XposedBridge.log(TAG + "已拦截无障碍列表查询，返回空列表！");
                return new ArrayList<AccessibilityServiceInfo>(); 
            }
        });

        // 3. 拦截系统设置里的无障碍开启状态
        XposedHelpers.findAndHookMethod(Settings.Secure.class, "getInt", 
            android.content.ContentResolver.class, String.class, int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                String name = (String) param.args[1];
                if ("accessibility_enabled".equals(name)) {
                    return 0; // 骗它说无障碍总开关是关着的
                }
                try {
                    return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                } catch (Exception e) {
                    return 0;
                }
            }
        });
    }
}
