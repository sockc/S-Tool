package com.sockc.unicomhook;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;

final class SplashSkipStatus {

    private static final String PREF_FILE =
            "s_tool_splash_skip_status";

    static final class Entry {
        final long lastSuccessAt;
        final int todayCount;
        final int lastScore;

        Entry(
                long lastSuccessAt,
                int todayCount,
                int lastScore
        ) {
            this.lastSuccessAt =
                    lastSuccessAt;
            this.todayCount =
                    todayCount;
            this.lastScore =
                    lastScore;
        }
    }

    private SplashSkipStatus() {
    }

    static synchronized void recordSuccess(
            Context context,
            String packageName,
            int score
    ) {
        if (context == null
                || packageName == null
                || packageName.trim().isEmpty()) {
            return;
        }

        SharedPreferences preferences =
                context.getSharedPreferences(
                        PREF_FILE,
                        Context.MODE_PRIVATE
                );

        long now =
                System.currentTimeMillis();

        int today =
                dayToken(
                        now
                );

        String dayKey =
                "day."
                        + packageName;
        String countKey =
                "count."
                        + packageName;

        int storedDay =
                preferences.getInt(
                        dayKey,
                        0
                );

        int count =
                storedDay == today
                        ? preferences.getInt(
                        countKey,
                        0
                )
                        : 0;

        preferences.edit()
                .putLong(
                        "last."
                                + packageName,
                        now
                )
                .putInt(
                        dayKey,
                        today
                )
                .putInt(
                        countKey,
                        count + 1
                )
                .putInt(
                        "score."
                                + packageName,
                        score
                )
                .apply();
    }

    static Entry read(
            Context context,
            String packageName
    ) {
        if (context == null
                || packageName == null) {
            return new Entry(
                    0L,
                    0,
                    0
            );
        }

        SharedPreferences preferences =
                context.getSharedPreferences(
                        PREF_FILE,
                        Context.MODE_PRIVATE
                );

        long last =
                preferences.getLong(
                        "last."
                                + packageName,
                        0L
                );

        int storedDay =
                preferences.getInt(
                        "day."
                                + packageName,
                        0
                );

        int todayCount =
                storedDay == dayToken(
                        System.currentTimeMillis()
                )
                        ? preferences.getInt(
                        "count."
                                + packageName,
                        0
                )
                        : 0;

        int score =
                preferences.getInt(
                        "score."
                                + packageName,
                        0
                );

        return new Entry(
                last,
                todayCount,
                score
        );
    }

    private static int dayToken(
            long time
    ) {
        Calendar calendar =
                Calendar.getInstance();

        calendar.setTimeInMillis(
                time
        );

        return calendar.get(
                Calendar.YEAR
        ) * 1000
                + calendar.get(
                Calendar.DAY_OF_YEAR
        );
    }
}
