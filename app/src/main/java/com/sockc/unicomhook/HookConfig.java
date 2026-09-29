package com.sockc.unicomhook;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

final class HookConfig {

    private static final String TAG = "S-Tool/Config: ";
    private static final String MODULE_PACKAGE = "com.sockc.unicomhook";

    private final XSharedPreferences preferences;
    private final boolean readable;

    private HookConfig(
            XSharedPreferences preferences,
            boolean readable
    ) {
        this.preferences = preferences;
        this.readable = readable;
    }

    static HookConfig load() {
        try {
            XSharedPreferences preferences =
                    new XSharedPreferences(
                            MODULE_PACKAGE,
                            FeaturePrefs.PREF_FILE
                    );

            preferences.reload();

            boolean readable =
                    preferences.getFile() != null
                            && preferences.getFile().canRead();

            if (!readable) {
                XposedBridge.log(
                        TAG + "配置文件不可读，所有功能按默认开启处理"
                );
            }

            return new HookConfig(preferences, readable);
        } catch (Throwable throwable) {
            XposedBridge.log(
                    TAG + "读取跨进程配置失败，使用默认开启: "
                            + throwable
            );
            return new HookConfig(null, false);
        }
    }

    boolean isEnabled(String featureId) {
        if (!readable || preferences == null) {
            return true;
        }

        try {
            return preferences.getBoolean(featureId, true);
        } catch (Throwable throwable) {
            XposedBridge.log(
                    TAG + "读取 " + featureId + " 失败，按开启处理: "
                            + throwable
            );
            return true;
        }
    }
}
