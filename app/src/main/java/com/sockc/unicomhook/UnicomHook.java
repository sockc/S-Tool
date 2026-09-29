package com.sockc.unicomhook;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.TextView;

import com.sockc.unicomhook.compat.XC_MethodHook;
import com.sockc.unicomhook.compat.XC_MethodReplacement;
import com.sockc.unicomhook.compat.XposedBridge;
import com.sockc.unicomhook.compat.XposedHelpers;

public class UnicomHook implements HookModule {

    private static final String TAG = "SockcHook: ";
    private static final String TARGET_PACKAGE =
            "com.sinovatech.unicom.ui";

    private static final int TAG_WARM_SPLASH_LISTENER =
            0x7F0B7101;
    private static final long WARM_SPLASH_SCAN_TIMEOUT_MS =
            5000L;

    @Override
    public void handleLoadPackage(
            LoadPackageParam lpparam
    ) {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        HookConfig config = HookConfig.load();

        boolean coldSplash =
                config.isEnabled(
                        "unicom",
                        "unicom.cold_splash"
                );
        boolean warmSplash =
                config.isEnabled(
                        "unicom",
                        "unicom.warm_splash"
                );
        boolean permissionPrivacy =
                config.isEnabled(
                        "unicom",
                        "unicom.permission_privacy"
                );
        boolean locationPrivacy =
                config.isEnabled(
                        "unicom",
                        "unicom.location_privacy"
                );

        boolean universalLocationPrivacy =
                config.isUniversalAppEnabled(
                        FeatureRegistry.UNIVERSAL_LOCATION,
                        TARGET_PACKAGE
                );

        XposedBridge.log(
                TAG
                        + "联通子功能: cold="
                        + coldSplash
                        + ", warm="
                        + warmSplash
                        + ", permission="
                        + permissionPrivacy
                        + ", location="
                        + locationPrivacy
                        + ", universalLocation="
                        + universalLocationPrivacy
        );

        if (coldSplash) {
            hookSplashAd(lpparam);
        }

        if (warmSplash) {
            hookWarmSplash();
        }

        if (permissionPrivacy) {
            hookPermissionPrivacy();
        }

        if (locationPrivacy
                && !universalLocationPrivacy) {
            hookLocationPrivacy();
        }
    }

    private void hookSplashAd(
            LoadPackageParam lpparam
    ) {
        String splashActivity =
                "com.sinovatech.unicom.basic.ui.activity.WelcomeClient";
        String mainActivity =
                "com.sinovatech.unicom.basic.ui.activity.MainActivity";

        Class<?> splashClass =
                XposedHelpers.findClassIfExists(
                        splashActivity,
                        lpparam.classLoader
                );

        if (splashClass == null) {
            XposedBridge.log(
                    TAG + "未找到新版开屏类，放行"
            );
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(
                    splashClass,
                    "onCreate",
                    Bundle.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(
                                MethodHookParam param
                        ) {
                            try {
                                Activity activity =
                                        (Activity) param.thisObject;

                                Intent intent = new Intent();
                                intent.setClassName(
                                        TARGET_PACKAGE,
                                        mainActivity
                                );
                                intent.addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK
                                                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                                );

                                activity.startActivity(intent);
                                activity.finish();

                                XposedBridge.log(
                                        TAG + "已处理联通冷启动开屏"
                                );
                            } catch (
                                    ActivityNotFoundException e
                            ) {
                                XposedBridge.log(
                                        TAG + "找不到主界面，放弃跳转"
                                );
                            } catch (Throwable throwable) {
                                XposedBridge.log(
                                        TAG
                                                + "冷启动广告处理异常: "
                                                + throwable
                                );
                            }
                        }
                    }
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    TAG
                            + "冷启动广告 Hook 失败: "
                            + throwable
            );
        }
    }

    private void hookWarmSplash() {
        try {
            XposedHelpers.findAndHookMethod(
                    Activity.class,
                    "onResume",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(
                                MethodHookParam param
                        ) {
                            Activity activity =
                                    (Activity) param.thisObject;

                            if (!TARGET_PACKAGE.equals(
                                    activity.getPackageName()
                            )) {
                                return;
                            }

                            Window window =
                                    activity.getWindow();
                            if (window == null) {
                                return;
                            }

                            final View decorView =
                                    window.getDecorView();

                            if (Boolean.TRUE.equals(
                                    decorView.getTag(
                                            TAG_WARM_SPLASH_LISTENER
                                    )
                            )) {
                                return;
                            }

                            final ViewTreeObserver
                                    .OnGlobalLayoutListener[]
                                    holder =
                                    new ViewTreeObserver
                                            .OnGlobalLayoutListener[1];

                            holder[0] =
                                    new ViewTreeObserver
                                            .OnGlobalLayoutListener() {
                                        @Override
                                        public void onGlobalLayout() {
                                            if (scanAndClickSkip(
                                                    decorView
                                            )) {
                                                XposedBridge.log(
                                                        TAG
                                                                + "已处理联通热启动广告"
                                                );
                                                removeWarmSplashListener(
                                                        decorView,
                                                        holder[0]
                                                );
                                            }
                                        }
                                    };

                            decorView.setTag(
                                    TAG_WARM_SPLASH_LISTENER,
                                    Boolean.TRUE
                            );

                            decorView
                                    .getViewTreeObserver()
                                    .addOnGlobalLayoutListener(
                                            holder[0]
                                    );

                            decorView.postDelayed(
                                    () ->
                                            removeWarmSplashListener(
                                                    decorView,
                                                    holder[0]
                                            ),
                                    WARM_SPLASH_SCAN_TIMEOUT_MS
                            );
                        }
                    }
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    TAG
                            + "热启动广告 Hook 失败: "
                            + throwable
            );
        }
    }

    private void removeWarmSplashListener(
            View decorView,
            ViewTreeObserver.OnGlobalLayoutListener listener
    ) {
        try {
            ViewTreeObserver observer =
                    decorView.getViewTreeObserver();

            if (observer.isAlive() && listener != null) {
                observer.removeOnGlobalLayoutListener(
                        listener
                );
            }
        } catch (Throwable ignored) {
        } finally {
            decorView.setTag(
                    TAG_WARM_SPLASH_LISTENER,
                    null
            );
        }
    }

    private boolean scanAndClickSkip(View view) {
        if (view instanceof TextView) {
            String text =
                    ((TextView) view)
                            .getText()
                            .toString();

            if (text.contains("跳过")
                    || text.equals("关闭")
                    || text.equals("点击跳过")) {
                view.performClick();
                return true;
            }
        } else if (view instanceof ViewGroup) {
            ViewGroup group =
                    (ViewGroup) view;

            for (int i = 0;
                    i < group.getChildCount();
                    i++) {
                if (scanAndClickSkip(
                        group.getChildAt(i)
                )) {
                    return true;
                }
            }
        }

        return false;
    }

    private void hookPermissionPrivacy() {
        try {
            XposedHelpers.findAndHookMethod(
                    ContextWrapper.class,
                    "checkSelfPermission",
                    String.class,
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(
                                MethodHookParam param
                        ) throws Throwable {
                            String permission =
                                    (String) param.args[0];

                            if (permission != null
                                    && (
                                    permission.contains(
                                            "LOCATION"
                                    )
                                            || permission.contains(
                                            "CONTACTS"
                                    )
                                            || permission.contains(
                                            "READ_MEDIA_IMAGES"
                                    )
                                            || permission.contains(
                                            "READ_EXTERNAL_STORAGE"
                                    )
                            )) {
                                XposedBridge.log(
                                        TAG
                                                + "保护权限检查: "
                                                + permission
                                );
                                return PackageManager
                                        .PERMISSION_GRANTED;
                            }

                            return XposedBridge
                                    .invokeOriginalMethod(
                                            param.method,
                                            param.thisObject,
                                            param.args
                                    );
                        }
                    }
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    TAG
                            + "权限保护 Hook 失败: "
                            + throwable
            );
        }
    }

    private void hookLocationPrivacy() {
        try {
            XposedHelpers.findAndHookMethod(
                    LocationManager.class,
                    "getLastKnownLocation",
                    String.class,
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(
                                MethodHookParam param
                        ) {
                            Location mockLocation =
                                    new Location(
                                            LocationManager
                                                    .GPS_PROVIDER
                                    );

                            mockLocation.setLatitude(
                                    23.1291
                            );
                            mockLocation.setLongitude(
                                    113.2644
                            );
                            mockLocation.setAccuracy(
                                    100f
                            );
                            mockLocation.setTime(
                                    System.currentTimeMillis()
                            );

                            XposedBridge.log(
                                    TAG + "已返回保护定位"
                            );
                            return mockLocation;
                        }
                    }
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    TAG
                            + "定位保护 Hook 失败: "
                            + throwable
            );
        }
    }
}
