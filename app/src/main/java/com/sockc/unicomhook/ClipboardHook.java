package com.sockc.unicomhook;

import com.sockc.unicomhook.compat.XC_MethodReplacement;
import com.sockc.unicomhook.compat.XposedBridge;
import com.sockc.unicomhook.compat.XposedHelpers;

public class ClipboardHook implements HookModule {
    private static final String TAG = "Sockc_Clipboard: ";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        // 【核心修复：避开底层地雷】
        // 绝对不能注入到 WebView 引擎里，否则会导致 C++ 层面的 SIGSEGV 崩溃！
        if (lpparam.packageName.equals("com.google.android.webview") || 
            lpparam.packageName.equals("com.android.webview") || 
            lpparam.packageName.equals("android")) {
            return; 
        }

        XposedBridge.log(TAG + "已潜入 " + lpparam.packageName + "，剪贴板粉碎机启动！");

        try {
            Class<?> clipboardManager = XposedHelpers.findClass("android.content.ClipboardManager", lpparam.classLoader);

            XposedHelpers.findAndHookMethod(clipboardManager, "hasPrimaryClip", new XC_MethodReplacement() {
                @Override protected Object replaceHookedMethod(MethodHookParam param) { return false; }
            });

            XposedHelpers.findAndHookMethod(clipboardManager, "hasText", new XC_MethodReplacement() {
                @Override protected Object replaceHookedMethod(MethodHookParam param) { return false; }
            });

            XposedHelpers.findAndHookMethod(clipboardManager, "getPrimaryClip", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log(TAG + "抓获 " + lpparam.packageName + " 试图偷窥剪贴板，已塞给它一团空气！");
                    return null;
                }
            });

            XposedHelpers.findAndHookMethod(clipboardManager, "getPrimaryClipDescription", new XC_MethodReplacement() {
                @Override protected Object replaceHookedMethod(MethodHookParam param) { return null; }
            });

            XposedHelpers.findAndHookMethod(clipboardManager, "getText", new XC_MethodReplacement() {
                @Override protected Object replaceHookedMethod(MethodHookParam param) { return ""; }
            });

        } catch (Throwable t) {
            XposedBridge.log(TAG + "剪贴板拦截异常: " + t.getMessage());
        }
    }
}
