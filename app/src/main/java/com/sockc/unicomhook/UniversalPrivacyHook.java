package com.sockc.unicomhook;

import com.sockc.unicomhook.compat.XposedBridge;

public final class UniversalPrivacyHook
        implements HookModule {

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

        boolean clipboardEnabled =
                config.isUniversalAppEnabled(
                        FeatureRegistry.UNIVERSAL_CLIPBOARD,
                        lpparam.packageName
                );

        boolean deviceIdEnabled =
                config.isUniversalAppEnabled(
                        FeatureRegistry.UNIVERSAL_DEVICE_ID,
                        lpparam.packageName
                );

        boolean fileMediaEnabled =
                config.isUniversalAppEnabled(
                        FeatureRegistry.UNIVERSAL_FILE_MEDIA,
                        lpparam.packageName
                );

        boolean appListEnabled =
                config.isUniversalAppEnabled(
                        FeatureRegistry.UNIVERSAL_APP_LIST,
                        lpparam.packageName
                );

        if (!locationEnabled
                && !screenshotEnabled
                && !clipboardEnabled
                && !deviceIdEnabled
                && !fileMediaEnabled
                && !appListEnabled) {
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
                        + " clipboard="
                        + clipboardEnabled
                        + " deviceId="
                        + deviceIdEnabled
                        + " fileMedia="
                        + fileMediaEnabled
                        + " appList="
                        + appListEnabled
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

        if (clipboardEnabled) {
            ClipboardPrivacyEngine.install(
                    TAG,
                    lpparam.packageName
            );
        }

        if (deviceIdEnabled) {
            DeviceIdPrivacyEngine.install(
                    TAG,
                    lpparam.packageName
            );
        }

        if (fileMediaEnabled) {
            FileMediaPrivacyEngine.install(
                    TAG,
                    lpparam.packageName
            );
        }

        if (appListEnabled) {
            AppListPrivacyEngine.install(
                    TAG,
                    lpparam.packageName,
                    lpparam.classLoader
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
        )
                || "com.google.android.webview".equals(
                packageName
        )
                || "com.android.webview".equals(
                packageName
        );
    }
}
