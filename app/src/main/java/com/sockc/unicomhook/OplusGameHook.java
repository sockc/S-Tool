package com.sockc.unicomhook;

import android.app.Application;
import android.os.Bundle;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import com.sockc.unicomhook.compat.XC_MethodHook;
import com.sockc.unicomhook.compat.XposedBridge;
import com.sockc.unicomhook.compat.XposedHelpers;

public class OplusGameHook implements HookModule {

    private static final String TAG =
            "OplusGameHook";

    private static final String TARGET_OPLUS =
            "com.oplus.games";
    private static final String TARGET_COLOROS_UI =
            "com.coloros.gamespaceui";
    private static final String TARGET_COLOROS_SERVICE =
            "com.coloros.gamespace";

    private static volatile boolean hooked;
    private static volatile boolean retryInstalled;

    private static final boolean FORCE_SUPPORT_ALL_GAMES =
            true;

    private static final String EMPTY_BLACKLIST =
            "[]";

    private static final String UNIVERSAL_AUTOMATION_CONFIG =
            "[{\"conditionSet\":[],\"result\":{\"functionEnabled\":1},\"ext\":{\"default_open_list\":[\"0\",\"1\",\"2\",\"3\",\"4\"]}}]";

    @Override
    public void handleLoadPackage(
            final LoadPackageParam lpparam
    ) {
        if (!isTarget(
                lpparam.packageName
        )) {
            return;
        }

        XposedBridge.log(
                TAG
                        + ": packageReady package="
                        + lpparam.packageName
                        + " process="
                        + lpparam.processName
        );

        // SMainHook 本身已经在 Application.attach 阶段分发到这里，
        // 不能再套一层 Application.attach Hook，否则当前 attach 很可能已经错过。
        if (hookMMKV(
                lpparam.classLoader
        )) {
            return;
        }

        installOnCreateRetry(
                lpparam
        );
    }

    private static boolean isTarget(
            String packageName
    ) {
        return TARGET_OPLUS.equals(
                packageName
        )
                || TARGET_COLOROS_UI.equals(
                packageName
        )
                || TARGET_COLOROS_SERVICE.equals(
                packageName
        );
    }

    private static void installOnCreateRetry(
            final LoadPackageParam lpparam
    ) {
        if (retryInstalled) {
            return;
        }

        retryInstalled = true;

        try {
            XposedHelpers.findAndHookMethod(
                    Application.class,
                    "onCreate",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(
                                MethodHookParam param
                        ) {
                            if (hooked) {
                                return;
                            }

                            ClassLoader appClassLoader =
                                    param.thisObject
                                            .getClass()
                                            .getClassLoader();

                            XposedBridge.log(
                                    TAG
                                            + ": Application.onCreate retry package="
                                            + lpparam.packageName
                            );

                            hookMMKV(
                                    appClassLoader != null
                                            ? appClassLoader
                                            : lpparam.classLoader
                            );
                        }
                    }
            );

            XposedBridge.log(
                    TAG
                            + ": MMKV 尚未就绪，已安装 onCreate 重试"
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    TAG
                            + ": install onCreate retry failed: "
                            + throwable
            );
        }
    }

    private static boolean hookMMKV(
            ClassLoader classLoader
    ) {
        if (hooked) {
            return true;
        }

        if (classLoader == null) {
            XposedBridge.log(
                    TAG + ": classLoader is null"
            );
            return false;
        }

        try {
            final Class<?> mmkvClass =
                    Class.forName(
                            "com.tencent.mmkv.MMKV",
                            false,
                            classLoader
                    );

            int hookCount = 0;

            for (final Method method
                    : mmkvClass.getDeclaredMethods()) {
                String name =
                        method.getName();

                if (!isStringMethod(name)
                        && !isStringSetMethod(name)
                        && !isBooleanMethod(name)) {
                    continue;
                }

                XposedBridge.hookMethod(
                        method,
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(
                                    MethodHookParam param
                            ) {
                                try {
                                    if (param.args == null
                                            || param.args.length < 1
                                            || !(param.args[0]
                                            instanceof String)) {
                                        return;
                                    }

                                    String key =
                                            (String) param.args[0];

                                    String lowerKey =
                                            key.toLowerCase();

                                    if (looksLikeGameKey(
                                            lowerKey
                                    )) {
                                        XposedBridge.log(
                                                TAG
                                                        + ": observed key="
                                                        + key
                                                        + " method="
                                                        + method.getName()
                                        );
                                    }

                                    if (isAutomationBlacklistKey(
                                            lowerKey
                                    )) {
                                        forceEmptyBlacklist(
                                                method,
                                                param,
                                                key
                                        );
                                        return;
                                    }

                                    if (FORCE_SUPPORT_ALL_GAMES
                                            && isAutomationSupportKey(
                                            lowerKey
                                    )) {
                                        forceAutomationSupport(
                                                method,
                                                param,
                                                key
                                        );
                                        return;
                                    }

                                    if (FORCE_SUPPORT_ALL_GAMES
                                            && isBooleanMethod(
                                            method.getName()
                                    )
                                            && isAutomationBooleanKey(
                                            lowerKey
                                    )) {
                                        Object result =
                                                param.getResult();

                                        if (Boolean.FALSE.equals(
                                                result
                                        )) {
                                            param.setResult(
                                                    true
                                            );

                                            XposedBridge.log(
                                                    TAG
                                                            + ": force boolean true, key="
                                                            + key
                                                            + ", method="
                                                            + method.getName()
                                            );
                                        }
                                    }
                                } catch (Throwable throwable) {
                                    XposedBridge.log(
                                            TAG
                                                    + ": afterHook failed "
                                                    + method.getName()
                                                    + ": "
                                                    + throwable
                                    );
                                }
                            }
                        }
                );

                hookCount++;
            }

            hooked =
                    hookCount > 0;

            XposedBridge.log(
                    TAG
                            + ": MMKV class found, hooked methods="
                            + hookCount
            );

            return hooked;
        } catch (ClassNotFoundException exception) {
            XposedBridge.log(
                    TAG
                            + ": MMKV class not ready"
            );
            return false;
        } catch (Throwable throwable) {
            XposedBridge.log(
                    TAG
                            + ": hook MMKV failed: "
                            + throwable
            );
            return false;
        }
    }

    private static boolean looksLikeGameKey(
            String lowerKey
    ) {
        return lowerKey != null
                && (lowerKey.contains(
                "game"
        )
                || lowerKey.contains(
                "automation"
        )
                || lowerKey.contains(
                "assistant"
        )
                || lowerKey.contains(
                "black"
        ));
    }

    private static boolean isStringMethod(
            String name
    ) {
        return "decodeString".equals(
                name
        )
                || "getString".equals(
                name
        );
    }

    private static boolean isStringSetMethod(
            String name
    ) {
        return "decodeStringSet".equals(
                name
        )
                || "getStringSet".equals(
                name
        );
    }

    private static boolean isBooleanMethod(
            String name
    ) {
        return "decodeBool".equals(
                name
        )
                || "getBoolean".equals(
                name
        );
    }

    private static boolean isAutomationBlacklistKey(
            String lowerKey
    ) {
        return lowerKey.equals(
                "game_automation_black_list"
        )
                || lowerKey.contains(
                "game_automation_black"
        );
    }

    private static boolean isAutomationSupportKey(
            String lowerKey
    ) {
        return lowerKey.contains(
                "game_automation"
        )
                && (lowerKey.contains(
                "valid_check"
        )
                || lowerKey.contains(
                "support"
        )
                || lowerKey.contains(
                "switch"
        )
                || lowerKey.contains(
                "config"
        ));
    }

    private static boolean isAutomationBooleanKey(
            String lowerKey
    ) {
        return lowerKey.contains(
                "game_automation"
        )
                && (lowerKey.contains(
                "switch"
        )
                || lowerKey.contains(
                "enable"
        )
                || lowerKey.contains(
                "support"
        ));
    }

    private static void forceEmptyBlacklist(
            Method method,
            XC_MethodHook.MethodHookParam param,
            String key
    ) {
        Class<?> returnType =
                method.getReturnType();

        if (returnType == String.class
                || param.getResult()
                instanceof String) {
            param.setResult(
                    EMPTY_BLACKLIST
            );

            XposedBridge.log(
                    TAG
                            + ": force blacklist empty, key="
                            + key
            );
            return;
        }

        if (Set.class.isAssignableFrom(
                returnType
        )
                || param.getResult()
                instanceof Set) {
            param.setResult(
                    new HashSet<String>()
            );

            XposedBridge.log(
                    TAG
                            + ": force blacklist empty set, key="
                            + key
            );
        }
    }

    private static void forceAutomationSupport(
            Method method,
            XC_MethodHook.MethodHookParam param,
            String key
    ) {
        Class<?> returnType =
                method.getReturnType();

        if (returnType == String.class
                || param.getResult()
                instanceof String) {
            Object old =
                    param.getResult();

            if (old == null
                    || old.toString()
                    .contains(
                            "functionEnabled"
                    )
                    || old.toString()
                    .contains(
                            "conditionSet"
                    )) {
                param.setResult(
                        UNIVERSAL_AUTOMATION_CONFIG
                );

                XposedBridge.log(
                        TAG
                                + ": force automation config, key="
                                + key
                );
            }
        }
    }
}
