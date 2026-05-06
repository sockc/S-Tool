package com.sockc.unicomhook;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import android.content.pm.PackageManager;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class MeituanHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_Meituan: ";
    private static final String TARGET_PACKAGE = "com.sankuai.meituan";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "已成功注入美团，准备执行净化与隐私隔离...");

        // ==========================================
        // 1. 秒杀开屏广告（自动点击跳过）
        // ==========================================
        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                final Activity activity = (Activity) param.thisObject;
                final View decorView = activity.getWindow().getDecorView();

                decorView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    boolean clicked = false;
                    @Override
                    public void onGlobalLayout() {
                        if (!clicked && scanAndClickSkip(decorView)) {
                            clicked = true;
                            XposedBridge.log(TAG + "已自动跳过美团开屏广告");
                            // 成功后移除监听
                            decorView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    }
                });
            }
        });

        // ==========================================
        // 2. 权限/隐私屏蔽（让它变成瞎子）
        // ==========================================
        
        // 屏蔽权限检查：无论美团问系统要什么权限（位置、联系人、电话），统统返回“已拒绝”
        XposedHelpers.findAndHookMethod("android.app.ContextImpl", lpparam.classLoader, "checkPermission", 
                String.class, int.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String permission = (String) param.args[0];
                // 排除一些基础权限防止崩溃，其他的（位置、相机、存储、电话）全部拦截
                if (permission.contains("LOCATION") || permission.contains("READ_PHONE_STATE") || 
                    permission.contains("CONTACTS") || permission.contains("CAMERA")) {
                    XposedBridge.log(TAG + "拦截权限请求: " + permission + "，强制返回 PERMISSION_DENIED");
                    param.setResult(PackageManager.PERMISSION_DENIED);
                }
            }
        });

        // 屏蔽设备 ID（IMEI/序列号等）：返回空字符串或伪造值
        try {
            XposedHelpers.findAndHookMethod("android.telephony.TelephonyManager", lpparam.classLoader, "getDeviceId", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) { param.setResult("00000000000000"); }
            });
            XposedHelpers.findAndHookMethod("android.telephony.TelephonyManager", lpparam.classLoader, "getSubscriberId", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) { param.setResult(null); }
            });
        } catch (Throwable ignored) {}
    }

    private boolean scanAndClickSkip(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof TextView) {
                    String text = ((TextView) child).getText().toString();
                    if (text.contains("跳过") || text.equals("Skip")) {
                        child.performClick();
                        return true;
                    }
                } else if (scanAndClickSkip(child)) {
                    return true;
                }
            }
        }
        return false;
    }
}
