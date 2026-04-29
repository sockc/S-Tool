package com.sockc.unicomhook;

import android.app.Activity;
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
     * 去除开屏广告
     */
    private void hookSplashAd(LoadPackageParam lpparam) {
        String SPLASH_ACTIVITY = "com.sinovatech.unicom.basic.ui.activity.WelcomeClient";
        String MAIN_ACTIVITY = "com.sinovatech.unicom.basic.ui.activity.MainActivity";

        try {
            XposedHelpers.findAndHookMethod(SPLASH_ACTIVITY, lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = (Activity) param.thisObject;
                    XposedBridge.log(TAG + "拦截到开屏页，正在跳转主界面...");

                    Intent intent = new Intent();
                    intent.setClassName(TARGET_PACKAGE, MAIN_ACTIVITY);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                    activity.startActivity(intent);
                    activity.finish();
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + "去广告 Hook 失败: " + t.getMessage());
        }
    }

    /**
     * 权限欺骗与伪造数据
     */
    private void hookPrivacy(LoadPackageParam lpparam) {
        try {
            // ==========================================
            // 第一步：欺骗权限检查，永远返回“已授权”
            // ==========================================
            XposedHelpers.findAndHookMethod(ContextWrapper.class, "checkSelfPermission", String.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    String permission = (String) param.args[0];
                    // 如果 App 检查定位、联系人、相册存储的权限
                    if (permission.contains("LOCATION") || 
                        permission.contains("CONTACTS") || 
                        permission.contains("READ_MEDIA_IMAGES") || 
                        permission.contains("READ_EXTERNAL_STORAGE")) {
                        
                        XposedBridge.log(TAG + "拦截到权限检查: " + permission + "，强行返回已授权！");
                        return PackageManager.PERMISSION_GRANTED; // 常量值为 0
                    }
                    // 其他权限正常放行
                    return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                }
            });

            // ==========================================
            // 第二步：伪造定位数据
            // ==========================================
            XposedHelpers.findAndHookMethod(LocationManager.class, "getLastKnownLocation", String.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log(TAG + "拦截到获取定位，返回伪造坐标！");
                    // 构造一个虚拟的 GPS 定位信息
                    Location mockLocation = new Location(LocationManager.GPS_PROVIDER);
                    mockLocation.setLatitude(23.1291);  // 纬度
                    mockLocation.setLongitude(113.2644); // 经度
                    mockLocation.setAccuracy(100f);
                    mockLocation.setTime(System.currentTimeMillis());
                    return mockLocation;
                }
            });

            // 注：拦截通讯录和相册实际读取的 ContentResolver.query 方法相对复杂，
            // 通常 App 在 checkSelfPermission 得到“已授权”后，如果没有真的拿到数据（因为系统其实没授权），
            // 它们内部的代码逻辑往往会按“相册为空”或“通讯录为空”来处理，不会闪退。

        } catch (Throwable t) {
            XposedBridge.log(TAG + "隐私 Hook 失败: " + t.getMessage());
        }
    }
}
