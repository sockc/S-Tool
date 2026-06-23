package com.sockc.unicomhook;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.telephony.TelephonyManager;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class TiktokHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_TikTok: ";
    private static final String TARGET_PACKAGE = "com.zhiliaoapp.musically";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "已注入 TikTok，开启【无闪退版】免登录与区域越狱...");

        try {
            // ==========================================
            // 1. 极其稳定的底层 SIM 卡环境伪装 (免拔卡)
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
            // 2. 强制登录页“安乐死” (Hook onCreate)
            // ==========================================
            XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = (Activity) param.thisObject;
                    String activityName = activity.getClass().getName().toLowerCase();
                    
                    // 仅对 TikTok 内部类生效，避免误伤系统组件
                    if (activityName.startsWith(TARGET_PACKAGE) || activityName.contains("aweme")) {
                        if (activityName.contains("login") || activityName.contains("passport") || activityName.contains("sign")) {
                            XposedBridge.log(TAG + "捕获强制登录页: " + activityName + "，执行合法销毁！");
                            // 核心修复：用官方 API 关闭它，绝不触发系统空指针崩溃
                            activity.finish(); 
                        }
                    }
                }
            });

            // ==========================================
            // 3. 强制登录弹窗“安乐死” (Hook show)
            // ==========================================
            XposedHelpers.findAndHookMethod(Dialog.class, "show", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Dialog dialog = (Dialog) param.thisObject;
                    String dialogClass = dialog.getClass().getName().toLowerCase();
                    
                    if (dialogClass.startsWith(TARGET_PACKAGE) || dialogClass.contains("aweme")) {
                        if (dialogClass.contains("login") || dialogClass.contains("auth")) {
                            XposedBridge.log(TAG + "捕获强制登录弹窗: " + dialogClass + "，执行合规遣散！");
                            // 核心修复：等它画完，立刻让它合法消失
                            dialog.dismiss(); 
                        }
                    }
                }
            });

        } catch (Throwable t) {
            XposedBridge.log(TAG + "TikTok Hook 异常: " + t.getMessage());
        }
    }
}
