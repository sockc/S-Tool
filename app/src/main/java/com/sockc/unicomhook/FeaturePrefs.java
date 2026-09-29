package com.sockc.unicomhook;

import android.content.Context;
import android.content.SharedPreferences;

public final class FeaturePrefs {

    public static final String PREF_FILE =
            "s_tool_features";
    public static final String KEY_LAST_CHANGED_AT =
            "_last_changed_at";

    private static final String LEGACY_SCREENSHOT_PRIVACY =
            "screenshot_privacy";
    private static final String NEW_SCREENSHOT_PRIVACY =
            "unicom.screenshot_privacy";
    private static final String KEY_UNIVERSAL_APP_SCOPE_MIGRATED =
            "_universal_app_scope_migrated_v1";

    private FeaturePrefs() {
    }

    public static final class OpenResult {
        public final SharedPreferences preferences;
        public final boolean crossProcessAvailable;

        private OpenResult(
                SharedPreferences preferences,
                boolean crossProcessAvailable
        ) {
            this.preferences = preferences;
            this.crossProcessAvailable =
                    crossProcessAvailable;
        }
    }

    public static OpenResult open(
            Context context
    ) {
        SharedPreferences preferences =
                context.getSharedPreferences(
                        PREF_FILE,
                        Context.MODE_PRIVATE
                );

        migrateLegacyPreferences(
                preferences
        );
        migrateUniversalAppSelections(
                preferences
        );

        return new OpenResult(
                preferences,
                ConfigProvider.isAvailable(
                        context
                )
        );
    }

    private static void migrateLegacyPreferences(
            SharedPreferences preferences
    ) {
        if (preferences.contains(
                LEGACY_SCREENSHOT_PRIVACY
        )
                && !preferences.contains(
                NEW_SCREENSHOT_PRIVACY
        )) {
            boolean oldValue =
                    preferences.getBoolean(
                            LEGACY_SCREENSHOT_PRIVACY,
                            true
                    );

            preferences.edit()
                    .putBoolean(
                            NEW_SCREENSHOT_PRIVACY,
                            oldValue
                    )
                    .remove(
                            LEGACY_SCREENSHOT_PRIVACY
                    )
                    .commit();
        }
    }

    static void migrateUniversalAppSelections(
            SharedPreferences preferences
    ) {
        if (preferences.getBoolean(
                KEY_UNIVERSAL_APP_SCOPE_MIGRATED,
                false
        )) {
            return;
        }

        boolean universalWasEnabled =
                preferences.getBoolean(
                        FeatureRegistry.UNIVERSAL_PRIVACY,
                        FeatureRegistry.defaultEnabled(
                                FeatureRegistry.UNIVERSAL_PRIVACY
                        )
                );

        SharedPreferences.Editor editor =
                preferences.edit();

        for (FeatureRegistry.TargetApp app
                : FeatureRegistry.targetApps()) {
            editor.putBoolean(
                    FeatureRegistry.universalAppKey(
                            FeatureRegistry.UNIVERSAL_LOCATION,
                            app.packageName
                    ),
                    universalWasEnabled
            );
            editor.putBoolean(
                    FeatureRegistry.universalAppKey(
                            FeatureRegistry.UNIVERSAL_SCREENSHOT,
                            app.packageName
                    ),
                    universalWasEnabled
            );
        }

        editor.putBoolean(
                KEY_UNIVERSAL_APP_SCOPE_MIGRATED,
                true
        );
        editor.commit();
    }

    public static boolean isEnabled(
            SharedPreferences preferences,
            String featureId
    ) {
        return preferences.getBoolean(
                featureId,
                FeatureRegistry.defaultEnabled(
                        featureId
                )
        );
    }

    public static boolean setEnabled(
            SharedPreferences preferences,
            String featureId,
            boolean enabled
    ) {
        return preferences.edit()
                .putBoolean(
                        featureId,
                        enabled
                )
                .putLong(
                        KEY_LAST_CHANGED_AT,
                        System.currentTimeMillis()
                )
                .commit();
    }
}
