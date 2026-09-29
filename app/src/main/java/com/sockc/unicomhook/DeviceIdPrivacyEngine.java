package com.sockc.unicomhook;

import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;

final class DeviceIdPrivacyEngine {

    private static final String PROTECTED_IMEI =
            "000000000000000";
    private static final String PROTECTED_MEID =
            "00000000000000";
    private static final String PROTECTED_ANDROID_ID =
            "0000000000000000";

    private DeviceIdPrivacyEngine() {
    }

    static void install(
            String logTag,
            String packageName
    ) {
        hookTelephony(
                logTag,
                packageName,
                "getDeviceId",
                PROTECTED_IMEI
        );
        hookTelephony(
                logTag,
                packageName,
                "getImei",
                PROTECTED_IMEI
        );
        hookTelephony(
                logTag,
                packageName,
                "getMeid",
                PROTECTED_MEID
        );
        hookTelephony(
                logTag,
                packageName,
                "getSubscriberId",
                null
        );
        hookTelephony(
                logTag,
                packageName,
                "getSimSerialNumber",
                null
        );

        try {
            XposedBridge.hookAllMethods(
                    Build.class,
                    "getSerial",
                    XC_MethodReplacement.returnConstant(
                            Build.UNKNOWN
                    )
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    logTag
                            + "Build.getSerial Hook 失败: "
                            + throwable
            );
        }

        try {
            XposedBridge.hookAllMethods(
                    Settings.Secure.class,
                    "getString",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(
                                MethodHookParam param
                        ) {
                            if (param.args == null
                                    || param.args.length < 2) {
                                return;
                            }

                            Object name =
                                    param.args[1];

                            if (Settings.Secure.ANDROID_ID.equals(
                                    name
                            )) {
                                param.setResult(
                                        PROTECTED_ANDROID_ID
                                );
                            }
                        }
                    }
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    logTag
                            + "Android ID Hook 失败: "
                            + throwable
            );
        }

        XposedBridge.log(
                logTag
                        + "设备标识保护已启用: "
                        + packageName
        );
    }

    private static void hookTelephony(
            String logTag,
            String packageName,
            String methodName,
            Object value
    ) {
        try {
            XposedBridge.hookAllMethods(
                    TelephonyManager.class,
                    methodName,
                    XC_MethodReplacement.returnConstant(
                            value
                    )
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    logTag
                            + "Telephony "
                            + methodName
                            + " Hook 失败: "
                            + throwable
            );
        }
    }
}
