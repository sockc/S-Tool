package com.sockc.unicomhook;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class UnicomHook implements IXposedHookLoadPackage {
    private static final String TAG = "SockcHook: ";
    private static final String TARGET_PACKAGE = "com.sinovatech.unicom.ui";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "已注入联通客户端，准备执行净化与欺骗...");

        // 1. 执行去广告逻辑
        hookSplashAd(lpparam);

        // 2. 执行隐私权限欺骗
        hookPrivacy(lpparam);
    }

    /**
     * 去除开屏广告 (防闪退强化版)
     */
    private void hookSplashAd(LoadPackageParam lpparam) {
        String SPLASH_ACTIVITY = "com.sinovatech.unicom.basic.ui.activity.WelcomeClient";
        String MAIN_ACTIVITY = "com.sinovatech.unicom.basic.ui.activity.MainActivity";

        // 第一道防线：先试探这个类在当前版本存不存在
        Class<?> splashClass = XposedHelpers.findClassIfExists(SPLASH_ACTIVITY, lpparam.classLoader);
        if (splashClass == null) {
            XposedBridge.log(TAG + "当前为旧版联通，未找到新版开屏类，安全放行，不执行去广告。");
            return; // 类都不存在，直接撤退，绝不恋战
        }

        try {
            XposedHelpers.findAndHookMethod(splashClass, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // 第二道防线：把运行时逻辑包起来，防止旧版没有 MainActivity 导致崩溃
                    try {
                        Activity activity = (Activity) param.thisObject;
                        XposedBridge.log(TAG + "拦截到开屏页，正在跳转主界面...");

                        Intent intent = new Intent();
                        intent.setClassName(TARGET_PACKAGE, MAIN_ACTIVITY);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                        activity.startActivity(intent);
                        activity.finish();
                    } catch (ActivityNotFoundException e) {
                        XposedBridge.log(TAG + "警告：找不到主界面，旧版跳转失败，不强制跳转以免闪退！");
                    } catch (Throwable t) {
                        XposedBridge.log(TAG + "开屏跳过逻辑发生未知异常: " + t.getMessage());
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + "去广告 Hook 失败: " + t.getMessage());
        }
    }

    /**
     * 权限欺骗与伪造数据 (安全强化版)
     */
    private void hookPrivacy(LoadPackageParam lpparam) {
        try {
            // ==========================================
            // 第一步：欺骗权限检查，永远返回“已授权”
            // ==========================================
            XposedHelpers.findAndHookMethod(ContextWrapper.class, "checkSelfPermission", String.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        String permission = (String) param.args[0];
                        if (permission != null && (
                            permission.contains("LOCATION") || 
                            permission.contains("CONTACTS") || 
                            permission.contains("READ_MEDIA_IMAGES") || 
                            permission.contains("READ_EXTERNAL_STORAGE"))) {
                            
                            XposedBridge.log(TAG + "拦截到权限检查: " + permission + "，强行返回已授权！");
                            return PackageManager.PERMISSION_GRANTED;
                        }
                        // 其他权限正常放行
                        return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                    } catch (Throwable t) {
                        XposedBridge.log(TAG + "权限欺骗回调内部异常，放行: " + t.getMessage());
                        return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                    }
                }
            });

            // ==========================================
            // 第二步：伪造定位数据
            // ==========================================
            XposedHelpers.findAndHookMethod(LocationManager.class, "getLastKnownLocation", String.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        XposedBridge.log(TAG + "拦截到获取定位，返回伪造坐标！");
                        Location mockLocation = new Location(LocationManager.GPS_PROVIDER);
                        mockLocation.setLatitude(23.1291);  // 纬度
                        mockLocation.setLongitude(113.2644); // 经度
                        mockLocation.setAccuracy(100f);
                        mockLocation.setTime(System.currentTimeMillis());
                        return mockLocation;
                    } catch (Throwable t) {
                        XposedBridge.log(TAG + "伪造定位失败，返回空: " + t.getMessage());
                        return null; // 如果伪造代码出错，宁可返回空也不能让 App 崩溃
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + "隐私 Hook 绑定失败: " + t.getMessage());
        }
    }
}
