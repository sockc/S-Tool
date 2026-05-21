package com.sockc.unicomhook;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.telephony.TelephonyManager;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
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

        XposedBridge.log(TAG + "已注入 TikTok，开启全方位越狱与拦截...");

        try {
            // ==========================================
            // 1. 伪装海外环境 (免拔卡) - 你原来的代码
            // ==========================================
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getSimCountryIso", new XC_MethodReplacement() {
                @Override protected Object replaceHookedMethod(MethodHookParam param) { return "us"; }
            });
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getNetworkCountryIso", new XC_MethodReplacement() {
                @Override protected Object replaceHookedMethod(MethodHookParam param) { return "us"; }
            });
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getSimOperator", new XC_MethodReplacement() {
                @Override protected Object replaceHookedMethod(MethodHookParam param) { return "310410"; }
            });
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getNetworkOperator", new XC_MethodReplacement() {
                @Override protected Object replaceHookedMethod(MethodHookParam param) { return "310410"; }
            });
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getSimOperatorName", new XC_MethodReplacement() {
                @Override protected Object replaceHookedMethod(MethodHookParam param) { return "AT&T"; }
            });
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getNetworkOperatorName", new XC_MethodReplacement() {
                @Override protected Object replaceHookedMethod(MethodHookParam param) { return "AT&T"; }
            });

            // ==========================================
            // 2. 暴力拦截“强制登录”跳转 (拦截 Activity)
            // ==========================================
            XposedHelpers.findAndHookMethod(Activity.class, "startActivity", Intent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Intent intent = (Intent) param.args[0];
                    if (intent != null && intent.getComponent() != null) {
                        String targetClass = intent.getComponent().getClassName().toLowerCase();
                        // 模糊匹配：如果将要跳转的页面名字里包含 login(登录) 或 passport(通行证/账号系统)
                        if (targetClass.contains("login") || targetClass.contains("passport") || targetClass.contains("account")) {
                            XposedBridge.log(TAG + "抓获强制登录页面跳转: " + targetClass + "，已暴力截断！");
                            param.setResult(null); // 直接让这次跳转失效
                        }
                    }
                }
            });

            // ==========================================
            // 3. 暴力拦截“强制登录”弹窗 (拦截 Dialog)
            // ==========================================
            XposedHelpers.findAndHookMethod(Dialog.class, "show", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Dialog dialog = (Dialog) param.thisObject;
                    String dialogClass = dialog.getClass().getName().toLowerCase();
                    
                    // 很多时候弹出的只是一个底部的弹窗，不是完整的页面
                    if (dialogClass.contains("login") || dialogClass.contains("sign") || dialogClass.contains("auth")) {
                        XposedBridge.log(TAG + "抓获强制登录弹窗: " + dialogClass + "，已拒绝显示！");
                        param.setResult(null); // 让弹窗显示失败
                    }
                }
            });

        } catch (Throwable t) {
            XposedBridge.log(TAG + "TikTok Hook 异常: " + t.getMessage());
        }
    }
}
