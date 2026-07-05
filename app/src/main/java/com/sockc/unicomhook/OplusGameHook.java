package com.sockc.oplusgamehook;

import android.app.Application;
import android.content.Context;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Enumeration;

import dalvik.system.DexFile;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class OplusGameHook implements IXposedHookLoadPackage {
    private static final String TAG = "OplusGameHook";
    private static final String CN = "com.tencent.tmgp.sgame";
    private static final String GLOBAL = "com.levelinfinite.sgameGlobal";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!"com.oplus.games".equals(lpparam.packageName)) return;

        XposedBridge.log(TAG + ": loaded com.oplus.games");

        XposedBridge.hookAllMethods(Application.class, "attach", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Context ctx = (Context) param.args[0];
                scanAndHook(ctx, ctx.getClassLoader());
            }
        });
    }

    private static void scanAndHook(Context ctx, ClassLoader cl) {
        try {
            String apk = ctx.getApplicationInfo().sourceDir;
            XposedBridge.log(TAG + ": apk=" + apk);

            hookDex(apk, cl);

            String[] splits = ctx.getApplicationInfo().splitSourceDirs;
            if (splits != null) {
                for (String s : splits) {
                    hookDex(s, cl);
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": scan failed " + t);
        }
    }

    private static void hookDex(String apk, ClassLoader cl) {
        try {
            DexFile dex = new DexFile(apk);
            Enumeration<String> entries = dex.entries();

            while (entries.hasMoreElements()) {
                String name = entries.nextElement();
                String low = name.toLowerCase();

                if (
                        low.contains("gameautomation") ||
                        low.contains("gamecombo") ||
                        low.contains("combokey") ||
                        low.contains("gameassistant") ||
                        low.contains("trackservice") ||
                        low.contains("nativeinterface") ||
                        low.contains("toolpanel") ||
                        low.contains("edg")
                ) {
                    hookClass(name, cl);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void hookClass(String name, ClassLoader cl) {
        try {
            Class<?> c = Class.forName(name, false, cl);

            for (Method m : c.getDeclaredMethods()) {
                try {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            boolean changed = false;

                            for (int i = 0; i < param.args.length; i++) {
                                if (CN.equals(param.args[i])) {
                                    param.args[i] = GLOBAL;
                                    changed = true;
                                }
                            }

                            if (changed) {
                                XposedBridge.log(TAG + ": arg CN->GLOBAL in " + name + "." + m.getName()
                                        + " args=" + Arrays.toString(param.args));
                            }
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Object r = param.getResult();

                            if (CN.equals(r)) {
                                param.setResult(GLOBAL);
                                XposedBridge.log(TAG + ": result CN->GLOBAL in " + name + "." + m.getName());
                            }

                            String n = name.toLowerCase();
                            String mn = m.getName().toLowerCase();

                            if (
                                    r instanceof Boolean &&
                                    Boolean.FALSE.equals(r) &&
                                    (n.contains("gameassistant") || n.contains("gameautomation")) &&
                                    (mn.contains("switch") || mn.contains("enable") || mn.contains("support"))
                            ) {
                                param.setResult(true);
                                XposedBridge.log(TAG + ": force boolean true in " + name + "." + m.getName());
                            }
                        }
                    });
                } catch (Throwable ignored) {}
            }

            XposedBridge.log(TAG + ": hooked " + name);
        } catch (Throwable ignored) {}
    }
}
