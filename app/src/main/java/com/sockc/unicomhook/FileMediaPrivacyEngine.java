package com.sockc.unicomhook;

import java.io.File;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class FileMediaPrivacyEngine {

    private FileMediaPrivacyEngine() {
    }

    static void install(
            String logTag,
            String packageName
    ) {
        try {
            XposedBridge.hookAllMethods(
                    File.class,
                    "listFiles",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(
                                MethodHookParam param
                        ) {
                            File file =
                                    (File) param.thisObject;

                            if (isProtectedPath(
                                    file
                            )) {
                                param.setResult(
                                        new File[0]
                                );
                            }
                        }
                    }
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    logTag
                            + "File.listFiles Hook 失败: "
                            + throwable
            );
        }

        try {
            XposedBridge.hookAllMethods(
                    File.class,
                    "list",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(
                                MethodHookParam param
                        ) {
                            File file =
                                    (File) param.thisObject;

                            if (isProtectedPath(
                                    file
                            )) {
                                param.setResult(
                                        new String[0]
                                );
                            }
                        }
                    }
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    logTag
                            + "File.list Hook 失败: "
                            + throwable
            );
        }

        XposedBridge.log(
                logTag
                        + "文件/相册保护已启用: "
                        + packageName
        );
    }

    private static boolean isProtectedPath(
            File file
    ) {
        if (file == null) {
            return false;
        }

        String path =
                file.getAbsolutePath()
                        .replace(
                                '\\',
                                '/'
                        )
                        .toLowerCase(
                                Locale.US
                        );

        return path.contains(
                "/dcim"
        )
                || path.contains(
                "/pictures"
        )
                || path.contains(
                "/download"
        )
                || path.contains(
                "/movies"
        )
                || path.contains(
                "/screenshots"
        );
    }
}
