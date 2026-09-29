package com.sockc.unicomhook;

import android.app.Activity;
import android.content.ContentResolver;
import android.net.Uri;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class ScreenshotPrivacyEngine {

    private ScreenshotPrivacyEngine() {
    }

    static void install(
            String logTag,
            String packageName
    ) {
        hookAndroid14ScreenCapture(
                logTag,
                packageName
        );
        hookMediaObservers(
                logTag,
                packageName
        );
    }

    private static void hookAndroid14ScreenCapture(
            String logTag,
            String packageName
    ) {
        try {
            XposedBridge.hookAllMethods(
                    Activity.class,
                    "registerScreenCaptureCallback",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(
                                MethodHookParam param
                        ) {
                            param.setResult(null);
                        }
                    }
            );

            XposedBridge.log(
                    logTag
                            + "已保护 Android 14+ 截图回调: "
                            + packageName
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    logTag
                            + "截图回调 Hook 失败: "
                            + throwable
            );
        }
    }

    private static void hookMediaObservers(
            String logTag,
            String packageName
    ) {
        try {
            XposedBridge.hookAllMethods(
                    ContentResolver.class,
                    "registerContentObserver",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(
                                MethodHookParam param
                        ) {
                            Uri uri =
                                    findUri(
                                            param.args
                                    );

                            if (!looksLikeScreenshotObserver(
                                    uri
                            )) {
                                return;
                            }

                            XposedBridge.log(
                                    logTag
                                            + "阻止媒体库截图监听: "
                                            + packageName
                            );

                            param.setResult(null);
                        }
                    }
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    logTag
                            + "媒体库监听 Hook 失败: "
                            + throwable
            );
        }
    }

    private static Uri findUri(
            Object[] args
    ) {
        if (args == null) {
            return null;
        }

        for (Object arg : args) {
            if (arg instanceof Uri) {
                return (Uri) arg;
            }
        }

        return null;
    }

    private static boolean looksLikeScreenshotObserver(
            Uri uri
    ) {
        if (uri == null) {
            return false;
        }

        String value =
                uri.toString()
                        .toLowerCase();

        return value.contains(
                "screenshots"
        )
                || value.contains(
                "content://media/external/images/media"
        )
                || value.contains(
                "content://media/external_primary/images/media"
        );
    }
}
