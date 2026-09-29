package com.sockc.unicomhook;

import android.content.Context;
import android.os.Bundle;

import com.sockc.unicomhook.compat.XposedBridge;

final class HookConfig {

    private static final String TAG =
            "S-Tool/Config: ";

    private static volatile HookConfig cached;

    private final Bundle providerValues;

    private HookConfig(
            Bundle providerValues
    ) {
        this.providerValues =
                providerValues;
    }

    static HookConfig load(
            Context context
    ) {
        HookConfig existing =
                cached;

        if (existing != null) {
            return existing;
        }

        synchronized (HookConfig.class) {
            existing = cached;

            if (existing != null) {
                return existing;
            }

            Bundle providerValues =
                    loadFromProvider(
                            context
                    );

            if (providerValues != null) {
                XposedBridge.log(
                        TAG
                                + "配置来源=ConfigProvider"
                );
            } else {
                XposedBridge.log(
                        TAG
                                + "ConfigProvider 不可用，使用功能默认值"
                );
            }

            existing =
                    new HookConfig(
                            providerValues
                    );
            cached =
                    existing;
            return existing;
        }
    }

    static HookConfig load() {
        HookConfig existing =
                cached;

        if (existing != null) {
            return existing;
        }

        synchronized (HookConfig.class) {
            if (cached == null) {
                cached =
                        new HookConfig(
                                null
                        );

                XposedBridge.log(
                        TAG
                                + "尚无目标 Context，使用功能默认值"
                );
            }

            return cached;
        }
    }

    private static Bundle loadFromProvider(
            Context context
    ) {
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
                    TAG
                            + "ConfigProvider 读取失败: "
                            + throwable
            );
            return null;
        }
    }

    boolean isEnabled(
            String featureId
    ) {
        return readBoolean(
                featureId,
                FeatureRegistry.defaultEnabled(
                        featureId
                )
        );
    }

    boolean isEnabled(
            String featureId,
            String subFeatureId
    ) {
        if (!isEnabled(
                featureId
        )) {
            return false;
        }

        return readBoolean(
                subFeatureId,
                FeatureRegistry.defaultEnabled(
                        subFeatureId
                )
        );
    }

    boolean isUniversalAppEnabled(
            String capabilityId,
            String packageName
    ) {
        if (!FeatureRegistry.isUniversalCapability(
                capabilityId
        )
                || !isEnabled(
                capabilityId
        )) {
            return false;
        }

        String key =
                FeatureRegistry.universalAppKey(
                        capabilityId,
                        packageName
                );

        return readBoolean(
                key,
                FeatureRegistry.defaultEnabled(
                        key
                )
        );
    }

    private boolean readBoolean(
            String key,
            boolean defaultValue
    ) {
        if (providerValues != null
                && providerValues.containsKey(
                key
        )) {
            return providerValues.getBoolean(
                    key,
                    defaultValue
            );
        }

        return defaultValue;
    }
}
