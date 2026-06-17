package com.sockc.unicomhook;

import android.webkit.WebView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XianyuHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_Xianyu: ";
    private static final String TARGET_PACKAGE = "com.taobao.idlefish";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) return;
        if (!TARGET_PACKAGE.equals(lpparam.processName)) return;

        XposedBridge.log(TAG + "V6 已注入主进程: " + lpparam.processName);

        hookSplashDialog(lpparam.classLoader);
        hookAndroidWebView();
        hookUcWebViewIfExists(lpparam.classLoader);
    }

    private void hookSplashDialog(ClassLoader cl) {
        try {
            Class<?> splashDialogCls = XposedHelpers.findClass(
                    "com.taobao.fleamarket.splashad.SplashAdDialog", cl
            );

            XposedBridge.hookAllMethods(splashDialogCls, "show", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log(TAG + "已拦截 SplashAdDialog.show()");
                    param.setResult(null);
                }
            });

            XposedBridge.log(TAG + "已挂上 SplashAdDialog.show");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "Hook SplashAdDialog 失败: " + t);
        }
    }

    private void hookAndroidWebView() {
        try {
            XposedBridge.hookAllMethods(WebView.class, "loadUrl", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args == null || param.args.length == 0) return;
                    Object arg0 = param.args[0];
                    if (!(arg0 instanceof String)) return;

                    String url = (String) arg0;
                    String lower = url.toLowerCase();

                    if (lower.contains("splash")
                            || lower.contains("startup")
                            || lower.contains("openad")
                            || lower.contains("advert")
                            || lower.contains("adservice")) {
                        XposedBridge.log(TAG + "拦截 Android WebView URL: " + url);
                        param.setResult(null);
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + "Hook Android WebView 失败: " + t);
        }
    }

    private void hookUcWebViewIfExists(ClassLoader cl) {
        try {
            Class<?> ucWebView = XposedHelpers.findClass("com.uc.webview.export.WebView", cl);

            XposedBridge.hookAllMethods(ucWebView, "loadUrl", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args == null || param.args.length == 0) return;
                    Object arg0 = param.args[0];
                    if (!(arg0 instanceof String)) return;

                    String url = (String) arg0;
                    String lower = url.toLowerCase();

                    if (lower.contains("splash")
                            || lower.contains("startup")
                            || lower.contains("openad")
                            || lower.contains("advert")
                            || lower.contains("adservice")) {
                        XposedBridge.log(TAG + "拦截 UC WebView URL: " + url);
                        param.setResult(null);
                    }
                }
            });

            XposedBridge.log(TAG + "已挂上 UC WebView");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "未发现 UC WebView，跳过");
        }
    }
}
