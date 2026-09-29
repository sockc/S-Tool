package com.sockc.unicomhook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public final class UniversalPrivacyHook
        implements IXposedHookLoadPackage {

    private static final String TAG =
            "S-Tool/UniversalPrivacy: ";

    private static final String MODULE_PACKAGE =
            "com.sockc.unicomhook";

    @Override
    public void handleLoadPackage(
            LoadPackageParam lpparam
    ) {
        if (shouldSkip(
                lpparam.packageName
        )) {
            return;
        }

        HookConfig config =
                HookConfig.load();

        boolean locationEnabled =
                config.isUniversalAppEnabled(
                        FeatureRegistry.UNIVERSAL_LOCATION,
                        lpparam.packageName
                );

        boolean screenshotEnabled =
                config.isUniversalAppEnabled(
                        FeatureRegistry.UNIVERSAL_SCREENSHOT,
                        lpparam.packageName
                );

        if (!locationEnabled
                && !screenshotEnabled) {
            return;
        }

        XposedBridge.log(
                TAG
                        + "注入 "
                        + lpparam.packageName
                        + " location="
                        + locationEnabled
                        + " screenshot="
                        + screenshotEnabled
        );

        if (locationEnabled) {
            LocationPrivacyEngine.install(
                    TAG,
                    lpparam.packageName
            );
        }

        if (screenshotEnabled) {
            ScreenshotPrivacyEngine.install(
                    TAG,
                    lpparam.packageName
            );
        }
    }

    private boolean shouldSkip(
            String packageName
    ) {
        if (packageName == null) {
            return true;
        }

        return MODULE_PACKAGE.equals(
                packageName
        )
                || "android".equals(
                packageName
        )
                || "com.android.systemui".equals(
                packageName
        )
                || "com.android.settings".equals(
                packageName
        );
    }
}
