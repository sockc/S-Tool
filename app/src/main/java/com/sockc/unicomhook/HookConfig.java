package com.sockc.unicomhook;

import android.content.Context;
import android.os.Bundle;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

final class HookConfig {

    private static final String TAG =
            "S-Tool/Config: ";
    private static final String MODULE_PACKAGE =
            "com.sockc.unicomhook";

    private final Bundle providerValues;
    private final XSharedPreferences legacyPreferences;
    private final boolean legacyReadable;

    private HookConfig(
            Bundle providerValues,
            XSharedPreferences legacyPreferences,
            boolean legacyReadable
    ) {
        this.providerValues =
                providerValues;
        this.legacyPreferences =
                legacyPreferences;
        this.legacyReadable =
                legacyReadable;
    }

    static HookConfig load() {
        Bundle providerValues =
                loadFromProvider();

        if (providerValues != null) {
            XposedBridge.log(
                    TAG + "配置来源=ConfigProvider"
            );

            return new HookConfig(
                    providerValues,
                    null,
                    false
            );
        }

        try {
            XSharedPreferences preferences =
                    new XSharedPreferences(
                            MODULE_PACKAGE,
                            FeaturePrefs.PREF_FILE
                    );

            preferences.reload();

            boolean readable =
                    preferences.getFile() != null
                            && preferences
                            .getFile()
                            .canRead();

            if (readable) {
                XposedBridge.log(
                        TAG + "配置来源=legacy XSharedPreferences"
                );
            } else {
                XposedBridge.log(
                        TAG + "配置桥与 legacy XSharedPreferences 均不可用，使用默认开启"
                );
            }

            return new HookConfig(
                    null,
                    preferences,
                    readable
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    TAG + "配置读取失败，使用默认开启: "
                            + throwable
            );

            return new HookConfig(
                    null,
                    null,
                    false
            );
        }
    }

    private static Bundle loadFromProvider() {
        Context context =
                findEarlyContext();

        if (context == null) {
            return null;
        }

        try {
            Bundle result =
                    context.getContentResolver()
                            .call(
                                    ConfigProvider.CONTENT_URI,
                                    ConfigProvider.METHOD_GET_ALL,
                                    null,
                                    null
                            );

            if (result == null
                    || !result.getBoolean(
                    ConfigProvider.KEY_PROVIDER_READY,
                    false
            )) {
                return null;
            }

            return result;
        } catch (Throwable throwable) {
            XposedBridge.log(
                    TAG + "ConfigProvider 读取失败: "
                            + throwable
            );
            return null;
        }
    }

    private static Context findEarlyContext() {
        try {
            Class<?> activityThreadClass =
                    XposedHelpers.findClass(
                            "android.app.ActivityThread",
                            null
                    );

            Object activityThread =
                    XposedHelpers.callStaticMethod(
                            activityThreadClass,
                            "currentActivityThread"
                    );

            if (activityThread == null) {
                return null;
            }

            Object systemContext =
                    XposedHelpers.callMethod(
                            activityThread,
                            "getSystemContext"
                    );

            return systemContext
                    instanceof Context
                    ? (Context) systemContext
                    : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    boolean isEnabled(String featureId) {
        return readBoolean(
                featureId,
                true
        );
    }

    boolean isEnabled(
            String featureId,
            String subFeatureId
    ) {
        if (!isEnabled(featureId)) {
            return false;
        }

        if ("unicom.screenshot_privacy".equals(
                subFeatureId
        )
                && providerValues == null
                && legacyReadable
                && legacyPreferences != null
                && !legacyPreferences.contains(
                subFeatureId
        )
                && legacyPreferences.contains(
                "screenshot_privacy"
        )) {
            return readBoolean(
                    "screenshot_privacy",
                    true
            );
        }

        return readBoolean(
                subFeatureId,
                true
        );
    }

    private boolean readBoolean(
            String key,
            boolean defaultValue
    ) {
        if (providerValues != null
                && providerValues.containsKey(key)) {
            return providerValues.getBoolean(
                    key,
                    defaultValue
            );
        }

        if (!legacyReadable
                || legacyPreferences == null) {
            return defaultValue;
        }

        try {
            return legacyPreferences.getBoolean(
                    key,
                    defaultValue
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    TAG + "读取 " + key + " 失败，使用默认值: "
                            + throwable
            );
            return defaultValue;
        }
    }
}
