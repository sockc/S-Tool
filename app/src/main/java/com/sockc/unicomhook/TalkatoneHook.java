package com.sockc.unicomhook;

import android.content.Context;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class TalkatoneHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_Talk: ";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.talkatone.android")) return;

        XposedBridge.log(TAG + "开始注入...");

        try {
            // 改为 Hook ContextImpl 的 getPackageManager，这是所有 App 获取来源的必经之路
            XposedHelpers.findAndHookMethod("android.app.ContextImpl", lpparam.classLoader, "getPackageManager", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object pm = param.getResult(); // 获取真实的 PackageManager
                    
                    // 动态代理或再次 Hook 这个 pm 对象的方法
                    XposedHelpers.findAndHookMethod(pm.getClass(), "getInstallerPackageName", String.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            String pkgName = (String) param.args[0];
                            if ("com.talkatone.android".equals(pkgName)) {
                                XposedBridge.log(TAG + "成功拦截来源请求，强行返回 Play Store");
                                param.setResult("com.android.vending");
                            }
                        }
                    });
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + "Hook 失败，这可能是闪退原因: " + t.getMessage());
        }
    }
}
