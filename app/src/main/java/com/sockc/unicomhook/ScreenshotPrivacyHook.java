package com.sockc.unicomhook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ScreenshotPrivacyHook
        implements IXposedHookLoadPackage {

    private static final String TAG =
            "Sockc_Screenshot: ";
    private static final String TARGET_PACKAGE =
            "com.sinovatech.unicom.ui";

    @Override
    public void handleLoadPackage(
            LoadPackageParam lpparam
    ) {
        if (!TARGET_PACKAGE.equals(
                lpparam.packageName
        )) {
            return;
        }

        HookConfig config =
                HookConfig.load();

        if (config.isEnabled(
                "universal_privacy",
                "universal_privacy.screenshot"
        )) {
            XposedBridge.log(
                    TAG
                            + "通用截图隐私已启用，跳过联通重复 Hook"
            );
            return;
        }

        XposedBridge.log(
                TAG
                        + "启用联通专属截图隐私"
        );

        ScreenshotPrivacyEngine.install(
                TAG,
                lpparam.packageName
        );
    }
}
