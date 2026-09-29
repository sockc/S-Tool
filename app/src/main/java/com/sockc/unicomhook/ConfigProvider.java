package com.sockc.unicomhook;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
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
    public static final String METHOD_REPORT_INJECTION =
            "report_injection";
    public static final String METHOD_REPORT_HOOK_RESULT =
            "report_hook_result";

    public static final String KEY_PROTOCOL_VERSION =
            "_protocol_version";
    public static final String KEY_PROVIDER_READY =
            "_provider_ready";
    public static final String KEY_PACKAGE_NAME =
            "_package_name";
    public static final String KEY_PROCESS_NAME =
            "_process_name";
    public static final String KEY_FEATURE_ID =
            "_feature_id";
    public static final String KEY_SUCCESS =
            "_success";
    public static final String KEY_SUMMARY =
            "_summary";
    public static final String KEY_RECORDED =
            "_recorded";

    public static final int PROTOCOL_VERSION = 5;

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

        if (METHOD_REPORT_INJECTION.equals(method)) {
            return recordInjection(
                    extras
            );
        }

        if (METHOD_REPORT_HOOK_RESULT.equals(method)) {
            return recordHookResult(
                    extras
            );
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

        FeaturePrefs.migrateUniversalAppSelections(
                preferences
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

        for (java.util.Map.Entry<String, ?> entry
                : preferences.getAll().entrySet()) {
            String key =
                    entry.getKey();
            Object value =
                    entry.getValue();

            if (FeatureRegistry.isUniversalAppKey(
                    key
            )
                    && value instanceof Boolean) {
                result.putBoolean(
                        key,
                        (Boolean) value
                );
            }
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

    private Bundle recordInjection(
            Bundle extras
    ) {
        Bundle result = new Bundle();
        result.putBoolean(
                KEY_RECORDED,
                false
        );

        Context context = getContext();

        if (context == null
                || extras == null) {
            return result;
        }

        String packageName =
                extras.getString(
                        KEY_PACKAGE_NAME
                );

        if (!callerOwnsPackage(
                context,
                packageName
        )) {
            return result;
        }

        InjectionStatus.record(
                context,
                packageName
        );

        result.putBoolean(
                KEY_RECORDED,
                true
        );

        return result;
    }

    private Bundle recordHookResult(
            Bundle extras
    ) {
        Bundle result = new Bundle();
        result.putBoolean(
                KEY_RECORDED,
                false
        );

        Context context = getContext();

        if (context == null
                || extras == null) {
            return result;
        }

        String packageName =
                extras.getString(
                        KEY_PACKAGE_NAME
                );
        String featureId =
                extras.getString(
                        KEY_FEATURE_ID
                );

        if (featureId == null
                || !callerOwnsPackage(
                context,
                packageName
        )) {
            return result;
        }

        boolean success =
                extras.getBoolean(
                        KEY_SUCCESS,
                        false
                );

        if (success) {
            HookStatus.clearFailure(
                    context,
                    packageName,
                    featureId
            );
        } else {
            HookStatus.recordFailure(
                    context,
                    packageName,
                    featureId,
                    extras.getString(
                            KEY_SUMMARY
                    )
            );
        }

        result.putBoolean(
                KEY_RECORDED,
                true
        );

        return result;
    }

    private boolean callerOwnsPackage(
            Context context,
            String packageName
    ) {
        if (packageName == null
                || packageName.trim().isEmpty()) {
            return false;
        }

        PackageManager packageManager =
                context.getPackageManager();

        String[] packages =
                packageManager.getPackagesForUid(
                        Binder.getCallingUid()
                );

        if (packages == null) {
            return false;
        }

        for (String candidate : packages) {
            if (packageName.equals(candidate)) {
                return true;
            }
        }

        return false;
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

    public static boolean reportInjection(
            Context context,
            String packageName,
            String processName
    ) {
        if (context == null
                || packageName == null) {
            return false;
        }

        try {
            Bundle extras = new Bundle();
            extras.putString(
                    KEY_PACKAGE_NAME,
                    packageName
            );
            extras.putString(
                    KEY_PROCESS_NAME,
                    processName
            );

            Bundle result =
                    context.getContentResolver()
                            .call(
                                    CONTENT_URI,
                                    METHOD_REPORT_INJECTION,
                                    null,
                                    extras
                            );

            return result != null
                    && result.getBoolean(
                    KEY_RECORDED,
                    false
            );
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean reportHookResult(
            Context context,
            String packageName,
            String featureId,
            boolean success,
            String summary
    ) {
        if (context == null
                || packageName == null
                || featureId == null) {
            return false;
        }

        try {
            Bundle extras = new Bundle();
            extras.putString(
                    KEY_PACKAGE_NAME,
                    packageName
            );
            extras.putString(
                    KEY_FEATURE_ID,
                    featureId
            );
            extras.putBoolean(
                    KEY_SUCCESS,
                    success
            );
            extras.putString(
                    KEY_SUMMARY,
                    summary
            );

            Bundle result =
                    context.getContentResolver()
                            .call(
                                    CONTENT_URI,
                                    METHOD_REPORT_HOOK_RESULT,
                                    null,
                                    extras
                            );

            return result != null
                    && result.getBoolean(
                    KEY_RECORDED,
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
