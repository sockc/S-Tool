package com.sockc.unicomhook;

import android.location.LocationManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class LocationPrivacyEngine {

    private LocationPrivacyEngine() {
    }

    static void install(
            String logTag,
            String packageName
    ) {
        try {
            XposedBridge.hookAllMethods(
                    LocationManager.class,
                    "getLastKnownLocation",
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
                            + "已保护 getLastKnownLocation: "
                            + packageName
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    logTag
                            + "getLastKnownLocation Hook 失败: "
                            + throwable
            );
        }

        try {
            XposedBridge.hookAllMethods(
                    LocationManager.class,
                    "getCurrentLocation",
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
                            + "已保护 getCurrentLocation: "
                            + packageName
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    logTag
                            + "getCurrentLocation Hook 失败: "
                            + throwable
            );
        }

        hookRequestMethod(
                logTag,
                packageName,
                "requestLocationUpdates"
        );

        hookRequestMethod(
                logTag,
                packageName,
                "requestSingleUpdate"
        );
    }

    private static void hookRequestMethod(
            String logTag,
            String packageName,
            String methodName
    ) {
        try {
            XposedBridge.hookAllMethods(
                    LocationManager.class,
                    methodName,
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
                            + "已保护 "
                            + methodName
                            + ": "
                            + packageName
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    logTag
                            + methodName
                            + " Hook 失败: "
                            + throwable
            );
        }
    }
}
