package com.sockc.unicomhook;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.sockc.unicomhook.compat.XC_MethodHook;
import com.sockc.unicomhook.compat.XposedBridge;
import com.sockc.unicomhook.compat.XposedHelpers;

/**
 * Gaode entry.
 *
 * V1.4.3.2 deliberately keeps all launch/splash state untouched.
 * Minimal-home cleanup happens only after Activity UI exists.
 */
public class GaodeHook implements HookModule {

    private static final String TAG =
            "Sockc_Gaode: ";

    private static final String TARGET_PACKAGE =
            "com.autonavi.minimap";

    private static final int TAG_LISTENER_INSTALLED =
            0x7F0B7001;

    private boolean adSkipEnabled;
    private boolean minimalHomeEnabled;

    @Override
    public void handleLoadPackage(
            LoadPackageParam lpparam
    ) {
        if (!TARGET_PACKAGE.equals(
                lpparam.packageName
        )
                || !TARGET_PACKAGE.equals(
                lpparam.processName
        )) {
            return;
        }

        HookConfig config =
                HookConfig.load();

        minimalHomeEnabled =
                config.isEnabled(
                        "gaode",
                        "gaode.minimal_home"
                );

        adSkipEnabled =
                config.isEnabled(
                        "gaode",
                        "gaode.ad_skip"
                );

        if (!minimalHomeEnabled
                && !adSkipEnabled) {
            XposedBridge.log(
                    TAG
                            + "高德子功能全部关闭"
            );
            return;
        }

        XposedBridge.log(
                TAG
                        + "V1.4.3.2 minimal="
                        + minimalHomeEnabled
                        + ", visualAdSkip="
                        + adSkipEnabled
                        + ", splashHooks=false"
        );

        // Install only AJX text anchors / item helpers.
        // The first parameter is intentionally false:
        // never install R3 semantic splash hooks in this release.
        GaodeR3Engine.install(
                lpparam.classLoader,
                false,
                minimalHomeEnabled,
                false,
                minimalHomeEnabled
        );

        XposedHelpers.findAndHookMethod(
                Activity.class,
                "onCreate",
                Bundle.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(
                            MethodHookParam param
                    ) {
                        Activity activity =
                                (Activity)
                                        param.thisObject;

                        if (!TARGET_PACKAGE.equals(
                                activity.getPackageName()
                        )) {
                            return;
                        }

                        installCleaner(
                                activity
                        );
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                Activity.class,
                "onResume",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(
                            MethodHookParam param
                    ) {
                        Activity activity =
                                (Activity)
                                        param.thisObject;

                        if (!TARGET_PACKAGE.equals(
                                activity.getPackageName()
                        )) {
                            return;
                        }

                        installCleaner(
                                activity
                        );

                        scheduleRepeatedClean(
                                activity
                        );
                    }
                }
        );
    }

    private void installCleaner(
            final Activity activity
    ) {
        View decorView =
                activity.getWindow()
                        .getDecorView();

        if (decorView == null) {
            return;
        }

        if (decorView.getTag(
                TAG_LISTENER_INSTALLED
        ) != null) {
            return;
        }

        decorView.setTag(
                TAG_LISTENER_INSTALLED,
                Boolean.TRUE
        );

        ViewTreeObserver observer =
                decorView.getViewTreeObserver();

        if (observer != null
                && observer.isAlive()) {
            observer.addOnGlobalLayoutListener(
                    () -> runSafely(
                            activity,
                            decorView,
                            "layout"
                    )
            );
        }

        scheduleRepeatedClean(
                activity
        );
    }

    private void scheduleRepeatedClean(
            final Activity activity
    ) {
        View decorView =
                activity.getWindow()
                        .getDecorView();

        if (decorView == null) {
            return;
        }

        Handler handler =
                new Handler(
                        Looper.getMainLooper()
                );

        long[] delays =
                new long[]{
                        0L,
                        120L,
                        300L,
                        650L,
                        1200L,
                        2200L,
                        4000L
                };

        for (long delay : delays) {
            handler.postDelayed(
                    () -> runSafely(
                            activity,
                            decorView,
                            "delay:"
                                    + delay
                    ),
                    delay
            );
        }
    }

    private void runSafely(
            Activity activity,
            View root,
            String source
    ) {
        try {
            if (minimalHomeEnabled) {
                GaodeMinimalHomeEngine.apply(
                        activity,
                        root
                );
            }

            if (adSkipEnabled) {
                scanAndClickVisibleSkip(
                        root
                );
            }
        } catch (Throwable throwable) {
            XposedBridge.log(
                    TAG
                            + "UI cleanup failed source="
                            + source
                            + " / "
                            + throwable
            );
        }
    }

    /**
     * Experimental visual-only ad handling.
     *
     * This never touches canShowSplash(), SplashScreenServiceImpl,
     * u96/za6 or any launch callback/state method.
     */
    private boolean scanAndClickVisibleSkip(
            View view
    ) {
        if (view == null
                || view.getVisibility()
                != View.VISIBLE) {
            return false;
        }

        if (view instanceof TextView) {
            CharSequence value =
                    ((TextView) view)
                            .getText();

            if (value != null) {
                String text =
                        value.toString()
                                .trim();

                if (looksLikeSkipButton(
                        text
                )) {
                    XposedBridge.log(
                            TAG
                                    + "visual ad button="
                                    + text
                    );

                    boolean clicked =
                            view.performClick();

                    if (!clicked
                            && view.getParent()
                            instanceof View) {
                        clicked =
                                ((View) view.getParent())
                                        .performClick();
                    }

                    if (clicked) {
                        return true;
                    }
                }
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group =
                    (ViewGroup) view;

            for (int i = 0;
                    i < group.getChildCount();
                    i++) {
                if (scanAndClickVisibleSkip(
                        group.getChildAt(i)
                )) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean looksLikeSkipButton(
            String text
    ) {
        if (text == null
                || text.isEmpty()
                || text.length() > 16) {
            return false;
        }

        return text.contains(
                "跳过"
        )
                || "关闭广告".equals(
                text
        );
    }
}
