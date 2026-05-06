package com.sockc.unicomhook;

import android.app.Activity;
import android.net.Uri;
import android.database.ContentObserver;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ScreenshotPrivacyHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_Screenshot: ";
    private static final String TARGET_PACKAGE = "com.sinovatech.unicom.ui";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "已注入联通，准备屏蔽截图检测...");

        // ==========================================
        // 1. 屏蔽 Android 14+ 原生截图监听 API
        // ==========================================
        try {
            // 拦截系统发给 App 的“已截屏”通知
            XposedHelpers.findAndHookMethod(Activity.class, "onScreenshot", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log(TAG + "成功拦截系统级 onScreenshot 回调，联通对此一无所知！");
                    return null; // 直接吃掉回调，App 的代码永远不会被触发
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + "当前系统版本不支持 onScreenshot API 或 Hook 失败");
        }

        // ==========================================
        // 2. 屏蔽 ContentObserver 监听 (防止 App 扫描相册)
        // ==========================================
        try {
            // 拦截 App 注册监听媒体库的行为
            XposedHelpers.findAndHookMethod("android.content.ContentResolver", lpparam.classLoader,
                    "registerContentObserver", Uri.class, boolean.class, ContentObserver.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Uri uri = (Uri) param.args[0];
                    if (uri != null) {
                        String uriString = uri.toString();
                        // 如果 App 试图盯着相册看（特别是截屏文件夹）
                        if (uriString.contains("content://media/external/images/media") || 
                            uriString.contains("screenshots")) {
                            XposedBridge.log(TAG + "检测到 App 试图监听相册变化，已强制阻断注册！");
                            param.setResult(null); // 拒绝它的注册请求
                        }
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + "媒体监听拦截失败: " + t.getMessage());
        }
    }
}
