package com.sockc.unicomhook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class TalkatoneHook implements IXposedHookLoadPackage {
    private static final String TAG = "SockcHook_Talkatone: ";
    private static final String TARGET_PACKAGE = "com.talkatone.android"; // Talkatone 包名

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "已注入 Talkatone，开始伪造 Google Play 安装来源...");

        // ==========================================
        // 1. 拦截老版本 API (Android 10 及以下) 的来源获取
        // ==========================================
        try {
            Class<?> packageManagerClass = XposedHelpers.findClass("android.app.ApplicationPackageManager", lpparam.classLoader);
            
            XposedHelpers.findAndHookMethod(packageManagerClass, "getInstallerPackageName", String.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    String pkgName = (String) param.args[0];
                    if (TARGET_PACKAGE.equals(pkgName)) {
                        XposedBridge.log(TAG + "拦截到老版本来源检测，强制返回 Play 商店！");
                        return "com.android.vending"; // 骗它是由 Google Play 安装的
                    }
                    return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + "老版本来源 Hook 失败: " + t.getMessage());
        }

        // ==========================================
        // 2. 拦截新版本 API (Android 11+) 的来源获取
        // ==========================================
        try {
            // Android 11 引入了 InstallSourceInfo 类来管理安装来源，我们直接拦截它内部的 get 方法
            Class<?> installSourceInfoClass = XposedHelpers.findClass("android.content.pm.InstallSourceInfo", lpparam.classLoader);
            
            // 修改返回的“安装应用包名”
            XposedHelpers.findAndHookMethod(installSourceInfoClass, "getInstallingPackageName", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log(TAG + "拦截到新版安装者检测，强行伪装！");
                    return "com.android.vending";
                }
            });

            // 修改返回的“发起安装应用包名”
            XposedHelpers.findAndHookMethod(installSourceInfoClass, "getInitiatingPackageName", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    return "com.android.vending";
                }
            });
        } catch (Throwable t) {
            // 如果是在 Android 11 以下的手机运行，这个类找不到会报错，忽略即可
            XposedBridge.log(TAG + "新版本来源 Hook 失败(可能是系统版本低): " + t.getMessage());
        }
    }
}
