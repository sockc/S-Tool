package com.sockc.unicomhook;

import android.app.Application;
import android.content.Context;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class OplusGameHook implements IXposedHookLoadPackage {
    private static final String TAG = "OplusGameHook";
    private static final String TARGET = "com.oplus.games";

    private static final String CN_GAME = "com.tencent.tmgp.sgame";
    private static final String BLACK_KEY = "game_automation_black_list";

    private static boolean hooked = false;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET.equals(lpparam.packageName)) return;

        XposedBridge.log(TAG + ": loaded " + lpparam.packageName);

        try {
            XposedBridge.hookAllMethods(Application.class, "attach", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Context ctx = (Context) param.args[0];
                    ClassLoader cl = ctx.getClassLoader();

                    XposedBridge.log(TAG + ": attach ok, package=" + ctx.getPackageName());

                    hookMMKV(cl);
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": hook attach failed: " + t);
        }
    }

    private static void hookMMKV(ClassLoader cl) {
        if (hooked) return;

        try {
            final Class<?> mmkvClass = Class.forName("com.tencent.mmkv.MMKV", false, cl);

            for (final Method method : mmkvClass.getDeclaredMethods()) {
                String name = method.getName();

                if (
                        "decodeString".equals(name) ||
                        "getString".equals(name) ||
                        "decodeStringSet".equals(name) ||
                        "getStringSet".equals(name)
                ) {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                if (param.args == null || param.args.length < 1) return;
                                if (!(param.args[0] instanceof String)) return;

                                String key = (String) param.args[0];

                                if (!isAutomationBlackKey(key)) return;

                                Object result = param.getResult();

                                if (result instanceof String || method.getReturnType() == String.class) {
                                    param.setResult("[]");
                                    XposedBridge.log(TAG + ": force " + key + " => [] by " + method.getName());
                                    return;
                                }

                                if (result instanceof Set || Set.class.isAssignableFrom(method.getReturnType())) {
                                    param.setResult(new HashSet<String>());
                                    XposedBridge.log(TAG + ": force " + key + " => empty set by " + method.getName());
                                }
                            } catch (Throwable t) {
                                XposedBridge.log(TAG + ": afterHook " + method.getName() + " failed: " + t);
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

    private static boolean isAutomationBlackKey(String key) {
        if (key == null) return false;

        return BLACK_KEY.equals(key)
                || key.toLowerCase().contains("game_automation_black");
    }
}
