package com.sockc.unicomhook;

import android.content.Context;
import android.content.SharedPreferences;

final class InjectionStatus {

    private static final String PREF_FILE =
            "s_tool_injection_status";

    private static final String KEY_PREFIX =
            "last_injected.";

    private InjectionStatus() {
    }

    static void record(
            Context context,
            String packageName
    ) {
        if (context == null
                || packageName == null
                || packageName.trim().isEmpty()) {
            return;
        }

        context.getSharedPreferences(
                PREF_FILE,
                Context.MODE_PRIVATE
        ).edit()
                .putLong(
                        KEY_PREFIX + packageName,
                        System.currentTimeMillis()
                )
                .apply();
    }

    static long getLastInjectedAt(
            Context context,
            String packageName
    ) {
        if (context == null
                || packageName == null) {
            return 0L;
        }

        SharedPreferences preferences =
                context.getSharedPreferences(
                        PREF_FILE,
                        Context.MODE_PRIVATE
                );

        return preferences.getLong(
                KEY_PREFIX + packageName,
                0L
        );
    }
}
