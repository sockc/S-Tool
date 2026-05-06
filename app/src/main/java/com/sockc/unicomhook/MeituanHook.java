package com.sockc.unicomhook;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import java.io.File;

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

        XposedBridge.log(TAG + "已注入美团，执行去广告 + 权限剥夺 + 本地文件硬隔离！");

        // ==========================================
        // 1. 本地文件/相册硬隔离 (和 PDD 同款套餐)
        // ==========================================
        try {
            XposedHelpers.findAndHookMethod(File.class, "listFiles", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    File file = (File) param.thisObject;
                    String path = file.getAbsolutePath();
                    
                    // 只要敢扫相册、截图、下载目录，统统返回空集
                    if (path.contains("DCIM") || path.contains("Pictures") || path.contains("Download")) {
                        XposedBridge.log(TAG + "拦截美团扫描本地目录: " + path + "，已强制返回空！");
                        param.setResult(new File[0]);
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + "本地文件隔离异常: " + t.getMessage());
        }

        // ==========================================
        // 2. 秒杀开屏广告
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
                            decorView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    }
                });
            }
        });

        // ==========================================
        // 3. 权限请求屏蔽 (位置、联系人、相机等全部斩断)
        // ==========================================
        XposedHelpers.findAndHookMethod("android.app.ContextImpl", lpparam.classLoader, "checkPermission", 
                String.class, int.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String permission = (String) param.args[0];
                if (permission.contains("LOCATION") || permission.contains("READ_PHONE_STATE") || 
                    permission.contains("CONTACTS") || permission.contains("CAMERA") || 
                    permission.contains("READ_MEDIA") || permission.contains("STORAGE")) {
                    XposedBridge.log(TAG + "硬拦截美团权限请求: " + permission);
                    param.setResult(PackageManager.PERMISSION_DENIED);
                }
            }
        });

        // ==========================================
        // 4. 设备 ID 屏蔽 (防止大数据杀熟定位)
        // ==========================================
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
