package com.sockc.oplusgamehook;

import java.lang.reflect.Method;
import java.util.Arrays;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class OplusGameHook implements IXposedHookLoadPackage {
    private static final String TAG = "OplusGameHook";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!"com.oplus.games".equals(lpparam.packageName)) return;

        XposedBridge.log(TAG + ": loaded com.oplus.games");

        hookClass(lpparam, "business.module.gameautomation.GameComboPlayEventController");
        hookClass(lpparam, "business.module.gameautomation.ComboKeyDataManager");
        hookClass(lpparam, "business.module.gameautomation.GameAutomationUtil");
        hookClass(lpparam, "business.module.gameautomation.GameAssistantGetSwitchCommandExecutor");

        hookByNameContains(lpparam, "GameCombo");
        hookByNameContains(lpparam, "ComboKey");
        hookByNameContains(lpparam, "GameAutomation");
    }

    private void hookClass(XC_LoadPackage.LoadPackageParam lpparam, String name) {
        try {
            Class<?> c = Class.forName(name, false, lpparam.classLoader);
            hookAllMethods(c);
            XposedBridge.log(TAG + ": hooked class " + name);
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": class not found " + name + " / " + t);
        }
    }

    private void hookByNameContains(XC_LoadPackage.LoadPackageParam lpparam, String keyword) {
        // 这个函数先留空。第一版先用固定类名试。
    }

    private void hookAllMethods(Class<?> c) {
        for (Method m : c.getDeclaredMethods()) {
            try {
                XposedBridge.hookMethod(m, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log(TAG + ": before " + m);
                        XposedBridge.log(TAG + ": args=" + Arrays.toString(param.args));
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        XposedBridge.log(TAG + ": after " + m + " result=" + param.getResult());
                    }
                });
            } catch (Throwable ignored) {}
        }
    }
}
