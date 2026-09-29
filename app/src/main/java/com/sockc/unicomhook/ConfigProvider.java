package com.sockc.unicomhook;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

public final class ConfigProvider extends ContentProvider {

    public static final String AUTHORITY =
            "com.sockc.unicomhook.config";
    public static final Uri CONTENT_URI =
            Uri.parse("content://" + AUTHORITY);

    public static final String METHOD_PING =
            "ping";
    public static final String METHOD_GET_ALL =
            "get_all";

    public static final String KEY_PROTOCOL_VERSION =
            "_protocol_version";
    public static final String KEY_PROVIDER_READY =
            "_provider_ready";

    public static final int PROTOCOL_VERSION = 2;

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Bundle call(
            String method,
            String arg,
            Bundle extras
    ) {
        if (METHOD_PING.equals(method)) {
            Bundle result = new Bundle();
            result.putBoolean(
                    KEY_PROVIDER_READY,
                    true
            );
            result.putInt(
                    KEY_PROTOCOL_VERSION,
                    PROTOCOL_VERSION
            );
            return result;
        }

        if (METHOD_GET_ALL.equals(method)) {
            return buildConfigBundle();
        }

        return super.call(
                method,
                arg,
                extras
        );
    }

    private Bundle buildConfigBundle() {
        Context context = getContext();

        if (context == null) {
            return null;
        }

        SharedPreferences preferences =
                context.getSharedPreferences(
                        FeaturePrefs.PREF_FILE,
                        Context.MODE_PRIVATE
                );

        Bundle result = new Bundle();

        result.putBoolean(
                KEY_PROVIDER_READY,
                true
        );
        result.putInt(
                KEY_PROTOCOL_VERSION,
                PROTOCOL_VERSION
        );

        for (String key
                : FeatureRegistry.allPreferenceKeys()) {
            boolean defaultValue =
                    FeatureRegistry.defaultEnabled(
                            key
                    );
            boolean value;

            if ("unicom.screenshot_privacy".equals(key)
                    && !preferences.contains(key)
                    && preferences.contains(
                    "screenshot_privacy"
            )) {
                value = preferences.getBoolean(
                        "screenshot_privacy",
                        defaultValue
                );
            } else {
                value = preferences.getBoolean(
                        key,
                        defaultValue
                );
            }

            result.putBoolean(
                    key,
                    value
            );
        }

        result.putLong(
                FeaturePrefs.KEY_LAST_CHANGED_AT,
                preferences.getLong(
                        FeaturePrefs.KEY_LAST_CHANGED_AT,
                        0L
                )
        );

        return result;
    }

    public static boolean isAvailable(
            Context context
    ) {
        if (context == null) {
            return false;
        }

        try {
            Bundle result =
                    context.getContentResolver()
                            .call(
                                    CONTENT_URI,
                                    METHOD_PING,
                                    null,
                                    null
                            );

            return result != null
                    && result.getBoolean(
                    KEY_PROVIDER_READY,
                    false
            );
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder
    ) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(
            Uri uri,
            ContentValues values
    ) {
        throw new UnsupportedOperationException(
                "S Tool config is read-only"
        );
    }

    @Override
    public int delete(
            Uri uri,
            String selection,
            String[] selectionArgs
    ) {
        throw new UnsupportedOperationException(
                "S Tool config is read-only"
        );
    }

    @Override
    public int update(
            Uri uri,
            ContentValues values,
            String selection,
            String[] selectionArgs
    ) {
        throw new UnsupportedOperationException(
                "S Tool config is read-only"
        );
    }
}
