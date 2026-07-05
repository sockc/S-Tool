package com.sockc.unicomhook;

import android.app.Application;
import android.content.Context;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class OplusGameHook implements IXposedHookLoadPackage {
    private static final String TAG = "OplusGameHook";
    private static final String TARGET = "com.oplus.games";

    private static boolean hooked = false;

    // 开关：true = 不仅清空黑名单，还强制所有“已被游戏助手识别为游戏”的应用支持游戏指令
    private static final boolean FORCE_SUPPORT_ALL_GAMES = true;

    private static final String EMPTY_BLACKLIST = "[]";

    private static final String UNIVERSAL_AUTOMATION_CONFIG =
            "[{\"conditionSet\":[],\"result\":{\"functionEnabled\":1},\"ext\":{\"default_open_list\":[\"0\",\"1\",\"2\",\"3\",\"4\"]}}]";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET.equals(lpparam.packageName)) return;

        XposedBridge.log(TAG + ": loaded " + lpparam.packageName);

        try {
            XposedBridge.hookAllMethods(Application.class, "attach", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Context ctx = (Context) param.args[0];
                    XposedBridge.log(TAG + ": attach ok, package=" + ctx.getPackageName());
                    hookMMKV(ctx.getClassLoader());
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": hook Application.attach failed: " + t);
        }
    }

    private static void hookMMKV(ClassLoader cl) {
        if (hooked) return;

        try {
            final Class<?> mmkvClass = Class.forName("com.tencent.mmkv.MMKV", false, cl);

            for (final Method method : mmkvClass.getDeclaredMethods()) {
                String name = method.getName();

                if (isStringMethod(name) || isStringSetMethod(name) || isBooleanMethod(name)) {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                if (param.args == null || param.args.length < 1) return;
                                if (!(param.args[0] instanceof String)) return;

                                String key = (String) param.args[0];
                                String lowerKey = key.toLowerCase();

                                if (isAutomationBlacklistKey(lowerKey)) {
                                    forceEmptyBlacklist(method, param, key);
                                    return;
                                }

                                if (FORCE_SUPPORT_ALL_GAMES && isAutomationSupportKey(lowerKey)) {
                                    forceAutomationSupport(method, param, key);
                                    return;
                                }

                                if (FORCE_SUPPORT_ALL_GAMES && isBooleanMethod(method.getName()) && isAutomationBooleanKey(lowerKey)) {
                                    Object result = param.getResult();
                                    if (result instanceof Boolean && Boolean.FALSE.equals(result)) {
                                        param.setResult(true);
                                        XposedBridge.log(TAG + ": force boolean true, key=" + key + ", method=" + method.getName());
                                    }
                                }
                            } catch (Throwable t) {
                                XposedBridge.log(TAG + ": afterHook failed: " + method.getName() + ", " + t);
                            }
                        }
                    });

                    XposedBridge.log(TAG + ": hooked MMKV." + name);
                }
            }

            hooked = true;
            XposedBridge.log(TAG + ": MMKV hook installed");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": hook MMKV failed: " + t);
        }
    }

    private static boolean isStringMethod(String name) {
        return "decodeString".equals(name)
                || "getString".equals(name);
    }

    private static boolean isStringSetMethod(String name) {
        return "decodeStringSet".equals(name)
                || "getStringSet".equals(name);
    }

    private static boolean isBooleanMethod(String name) {
        return "decodeBool".equals(name)
                || "getBoolean".equals(name);
    }

    private static boolean isAutomationBlacklistKey(String lowerKey) {
        return lowerKey.equals("game_automation_black_list")
                || lowerKey.contains("game_automation_black");
    }

    private static boolean isAutomationSupportKey(String lowerKey) {
        return lowerKey.contains("game_automation")
                && (
                lowerKey.contains("valid_check")
                        || lowerKey.contains("support")
                        || lowerKey.contains("switch")
                        || lowerKey.contains("config")
        );
    }

    private static boolean isAutomationBooleanKey(String lowerKey) {
        return lowerKey.contains("game_automation")
                && (
                lowerKey.contains("switch")
                        || lowerKey.contains("enable")
                        || lowerKey.contains("support")
        );
    }

    private static void forceEmptyBlacklist(Method method, XC_MethodHook.MethodHookParam param, String key) {
        Class<?> returnType = method.getReturnType();

        if (returnType == String.class || param.getResult() instanceof String) {
            param.setResult(EMPTY_BLACKLIST);
            XposedBridge.log(TAG + ": force blacklist empty, key=" + key + ", method=" + method.getName());
            return;
        }

        if (Set.class.isAssignableFrom(returnType) || param.getResult() instanceof Set) {
            param.setResult(new HashSet<String>());
            XposedBridge.log(TAG + ": force blacklist empty set, key=" + key + ", method=" + method.getName());
        }
    }

    private static void forceAutomationSupport(Method method, XC_MethodHook.MethodHookParam param, String key) {
        Class<?> returnType = method.getReturnType();

        if (returnType == String.class || param.getResult() instanceof String) {
            Object old = param.getResult();

            // 只覆盖云控 JSON，不覆盖普通文本
            if (old == null || old.toString().contains("functionEnabled") || old.toString().contains("conditionSet")) {
                param.setResult(UNIVERSAL_AUTOMATION_CONFIG);
                XposedBridge.log(TAG + ": force automation support all games, key=" + key + ", method=" + method.getName());
            }
        }
    }
}
