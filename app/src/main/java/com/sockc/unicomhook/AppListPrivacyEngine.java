package com.sockc.unicomhook;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;

import java.util.ArrayList;
import java.util.List;

import com.sockc.unicomhook.compat.XC_MethodHook;
import com.sockc.unicomhook.compat.XposedBridge;
import com.sockc.unicomhook.compat.XposedHelpers;

final class AppListPrivacyEngine {

    private AppListPrivacyEngine() {
    }

    static void install(
            String logTag,
            String packageName,
            ClassLoader classLoader
    ) {
        try {
            Class<?> applicationPackageManager =
                    XposedHelpers.findClass(
                            "android.app.ApplicationPackageManager",
                            classLoader
                    );

            hookListMethod(
                    applicationPackageManager,
                    "getInstalledApplications",
                    packageName
            );

            hookListMethod(
                    applicationPackageManager,
                    "getInstalledPackages",
                    packageName
            );

            XposedBridge.log(
                    logTag
                            + "应用列表保护已启用: "
                            + packageName
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    logTag
                            + "应用列表 Hook 失败: "
                            + throwable
            );
        }
    }

    private static void hookListMethod(
            Class<?> targetClass,
            String methodName,
            String ownPackage
    ) {
        XposedBridge.hookAllMethods(
                targetClass,
                methodName,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(
                            MethodHookParam param
                    ) {
                        Object result =
                                param.getResult();

                        if (!(result instanceof List)) {
                            return;
                        }

                        List<?> original =
                                (List<?>) result;
                        List<Object> filtered =
                                new ArrayList<>();

                        for (Object item : original) {
                            String packageName =
                                    packageNameOf(
                                            item
                                    );

                            if (ownPackage.equals(
                                    packageName
                            )
                                    || "android".equals(
                                    packageName
                            )) {
                                filtered.add(
                                        item
                                );
                            }
                        }

                        param.setResult(
                                filtered
                        );
                    }
                }
        );
    }

    private static String packageNameOf(
            Object item
    ) {
        if (item instanceof ApplicationInfo) {
            return ((ApplicationInfo) item)
                    .packageName;
        }

        if (item instanceof PackageInfo) {
            return ((PackageInfo) item)
                    .packageName;
        }

        return null;
    }
}
