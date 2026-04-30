package com.sockc.unicomhook;

import java.lang.reflect.Method;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class SecureFolderHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_SecureFolder: ";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        // 核心：安全文件夹的创建和校验逻辑深埋在 Android 系统框架里
        if (!lpparam.packageName.equals("android")) return;

        XposedBridge.log(TAG + "成功注入 Android 系统框架，开始猎杀 Knox 限制...");

        try {
            // 1. 拦截日志中暴露的乱码类 CraersshCient
            Class<?> craersshClientClass = XposedHelpers.findClassIfExists("android.os.CraersshCient", lpparam.classLoader);
            
            if (craersshClientClass != null) {
                // 动态遍历方法，无视参数类型 (z2 混淆)，直接 Hook 名字
                for (Method method : craersshClientClass.getDeclaredMethods()) {
                    if (method.getName().equals("generateKeyPair")) {
                        XposedBridge.log(TAG + "精准锁定目标方法: " + method.toString());
                        
                        XposedBridge.hookMethod(method, new XC_MethodReplacement() {
                            @Override
                            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                                XposedBridge.log(TAG + "已拦截底层密钥生成！正在签发伪造的 RSA 密钥...");
                                // 伪造一个不受物理 Knox 芯片保护的普通 RSA 密钥对，骗过上层验证
                                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                                kpg.initialize(2048);
                                return kpg.generateKeyPair();
                            }
                        });
                    }
                }
            }

            // 2. 双重保险：拦截日志里的 DarManagerService
            Class<?> darManagerClass = XposedHelpers.findClassIfExists("com.android.server.knox.dar.DarManagerService", lpparam.classLoader);
            if (darManagerClass != null) {
                XposedHelpers.findAndHookMethod(darManagerClass, "isKnoxKeyInstallable", int.class, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) {
                        XposedBridge.log(TAG + "已拦截 isKnoxKeyInstallable，强制放行！");
                        // 强制返回 true 或 false 来阻断异常抛出 (根据三星逻辑，这里通常返回 true 代表支持)
                        return true; 
                    }
                });
            }

        } catch (Throwable t) {
            XposedBridge.log(TAG + "Hook 注入发生异常: " + t.getMessage());
        }
    }
}
