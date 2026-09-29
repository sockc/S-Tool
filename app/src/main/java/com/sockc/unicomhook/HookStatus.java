package com.sockc.unicomhook;

import android.content.Context;
import android.content.SharedPreferences;

final class HookStatus {

    private static final String PREF_FILE =
            "s_tool_hook_status";

    private static final String TIME_PREFIX =
            "failure_time.";
    private static final String SUMMARY_PREFIX =
            "failure_summary.";

    private HookStatus() {
    }

    static void recordFailure(
            Context context,
            String packageName,
            String featureId,
            String summary
    ) {
        if (context == null
                || packageName == null
                || featureId == null) {
            return;
        }

        String key =
                packageName
                        + "."
                        + featureId;

        context.getSharedPreferences(
                PREF_FILE,
                Context.MODE_PRIVATE
        ).edit()
                .putLong(
                        TIME_PREFIX + key,
                        System.currentTimeMillis()
                )
                .putString(
                        SUMMARY_PREFIX + key,
                        sanitize(
                                summary
                        )
                )
                .apply();
    }

    static void clearFailure(
            Context context,
            String packageName,
            String featureId
    ) {
        if (context == null
                || packageName == null
                || featureId == null) {
            return;
        }

        String key =
                packageName
                        + "."
                        + featureId;

        context.getSharedPreferences(
                PREF_FILE,
                Context.MODE_PRIVATE
        ).edit()
                .remove(
                        TIME_PREFIX + key
                )
                .remove(
                        SUMMARY_PREFIX + key
                )
                .apply();
    }

    static long getFailureAt(
            Context context,
            String packageName,
            String featureId
    ) {
        if (context == null
                || packageName == null
                || featureId == null) {
            return 0L;
        }

        String key =
                packageName
                        + "."
                        + featureId;

        return context.getSharedPreferences(
                PREF_FILE,
                Context.MODE_PRIVATE
        ).getLong(
                TIME_PREFIX + key,
                0L
        );
    }

    static String getFailureSummary(
            Context context,
            String packageName,
            String featureId
    ) {
        if (context == null
                || packageName == null
                || featureId == null) {
            return null;
        }

        String key =
                packageName
                        + "."
                        + featureId;

        return context.getSharedPreferences(
                PREF_FILE,
                Context.MODE_PRIVATE
        ).getString(
                SUMMARY_PREFIX + key,
                null
        );
    }

    static void clearAll(
            Context context
    ) {
        if (context == null) {
            return;
        }

        context.getSharedPreferences(
                PREF_FILE,
                Context.MODE_PRIVATE
        ).edit()
                .clear()
                .apply();
    }

    private static String sanitize(
            String value
    ) {
        if (value == null) {
            return "未知异常";
        }

        String clean =
                value.replace(
                        '\n',
                        ' '
                )
                        .replace(
                                '\r',
                                ' '
                        )
                        .trim();

        if (clean.length() > 160) {
            return clean.substring(
                    0,
                    160
            );
        }

        return clean;
    }
}
