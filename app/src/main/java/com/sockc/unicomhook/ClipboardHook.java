package com.sockc.unicomhook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ClipboardHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_Clipboard: ";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        // 这里不写死包名！让 arrays.xml 来控制狙击范围
        XposedBridge.log(TAG + "已潜入 " + lpparam.packageName + "，剪贴板粉碎机启动！");

        try {
            Class<?> clipboardManager = XposedHelpers.findClass("android.content.ClipboardManager", lpparam.classLoader);

            // 1. 拦截“是否有剪贴板内容”的询问 -> 永远回答：没有 (false)
            XposedHelpers.findAndHookMethod(clipboardManager, "hasPrimaryClip", new XC_MethodReplacement() {
                @Override protected Object replaceHookedMethod(MethodHookParam param) { return false; }
            });

            // 2. 拦截“是否有文本”的询问 -> 永远回答：没有 (false)
            XposedHelpers.findAndHookMethod(clipboardManager, "hasText", new XC_MethodReplacement() {
                @Override protected Object replaceHookedMethod(MethodHookParam param) { return false; }
            });

            // 3. 拦截“获取剪贴板数据”的核心动作 -> 强制返回空 (null)
            XposedHelpers.findAndHookMethod(clipboardManager, "getPrimaryClip", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log(TAG + "抓获 " + lpparam.packageName + " 试图偷窥剪贴板，已塞给它一团空气！");
                    return null;
                }
            });

            // 4. 拦截“获取剪贴板描述” (有些 App 会先拿描述看数据类型) -> 返回空
            XposedHelpers.findAndHookMethod(clipboardManager, "getPrimaryClipDescription", new XC_MethodReplacement() {
                @Override protected Object replaceHookedMethod(MethodHookParam param) { return null; }
            });

            // 5. 拦截老版本 API 的纯文本获取 -> 返回空字符串
            XposedHelpers.findAndHookMethod(clipboardManager, "getText", new XC_MethodReplacement() {
                @Override protected Object replaceHookedMethod(MethodHookParam param) { return ""; }
            });

        } catch (Throwable t) {
            XposedBridge.log(TAG + "剪贴板拦截异常: " + t.getMessage());
        }
    }
}
