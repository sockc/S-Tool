package com.sockc.unicomhook;

import android.content.Context;
import android.content.SharedPreferences;

public final class FeaturePrefs {

    public static final String PREF_FILE = "s_tool_features";
    public static final String KEY_LAST_CHANGED_AT = "_last_changed_at";

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
            this.crossProcessAvailable = crossProcessAvailable;
        }
    }

    @SuppressWarnings("deprecation")
    public static OpenResult open(Context context) {
        try {
            SharedPreferences preferences = context.getSharedPreferences(
                    PREF_FILE,
                    Context.MODE_WORLD_READABLE
            );
            return new OpenResult(preferences, true);
        } catch (SecurityException ignored) {
            SharedPreferences fallback = context.getSharedPreferences(
                    PREF_FILE,
                    Context.MODE_PRIVATE
            );
            return new OpenResult(fallback, false);
        }
    }

    public static boolean isEnabled(
            SharedPreferences preferences,
            String featureId
    ) {
        return preferences.getBoolean(featureId, true);
    }

    public static boolean setEnabled(
            SharedPreferences preferences,
            String featureId,
            boolean enabled
    ) {
        return preferences.edit()
                .putBoolean(featureId, enabled)
                .putLong(KEY_LAST_CHANGED_AT, System.currentTimeMillis())
                .commit();
    }
}
