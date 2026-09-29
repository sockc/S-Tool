package com.sockc.unicomhook;

import android.content.ClipboardManager;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;

final class ClipboardPrivacyEngine {

    private ClipboardPrivacyEngine() {
    }

    static void install(
            String logTag,
            String packageName
    ) {
        hookReturn(
                logTag,
                packageName,
                "hasPrimaryClip",
                false
        );
        hookReturn(
                logTag,
                packageName,
                "hasText",
                false
        );
        hookReturn(
                logTag,
                packageName,
                "getPrimaryClip",
                null
        );
        hookReturn(
                logTag,
                packageName,
                "getPrimaryClipDescription",
                null
        );
        hookReturn(
                logTag,
                packageName,
                "getText",
                ""
        );
    }

    private static void hookReturn(
            String logTag,
            String packageName,
            String methodName,
            Object value
    ) {
        try {
            XposedBridge.hookAllMethods(
                    ClipboardManager.class,
                    methodName,
                    XC_MethodReplacement.returnConstant(
                            value
                    )
            );

            XposedBridge.log(
                    logTag
                            + "剪贴板保护 "
                            + methodName
                            + ": "
                            + packageName
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    logTag
                            + "剪贴板 "
                            + methodName
                            + " Hook 失败: "
                            + throwable
            );
        }
    }
}
