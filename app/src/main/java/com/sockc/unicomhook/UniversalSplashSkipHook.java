package com.sockc.unicomhook;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import com.sockc.unicomhook.compat.XC_MethodHook;
import com.sockc.unicomhook.compat.XposedBridge;
import com.sockc.unicomhook.compat.XposedHelpers;

/**
 * Universal cold-start splash-ad skip.
 *
 * Safety model:
 * - UI only. Never hooks ad SDK loading/show callbacks or app launch state.
 * - Active only during the first 10 seconds of the target process.
 * - Strict score threshold.
 * - At most one native performClick() per process session.
 */
public final class UniversalSplashSkipHook
        implements HookModule {

    private static final String TAG =
            "S-Tool/SplashSkip: ";

    private static final String MODULE_PACKAGE =
            "com.sockc.unicomhook";

    private static final long WINDOW_MS =
            10_000L;

    private static final int SCORE_THRESHOLD =
            85;

    private static final int MAX_SCAN_NODES =
            500;

    private static final Pattern COUNTDOWN_AFTER =
            Pattern.compile(
                    ".*跳过\\d{1,2}(?:秒|s)?.*",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern COUNTDOWN_BEFORE =
            Pattern.compile(
                    ".*\\d{1,2}(?:秒|s)?(?:后)?跳过.*",
                    Pattern.CASE_INSENSITIVE
            );

    private final long processStartedAt =
            SystemClock.elapsedRealtime();

    private final AtomicBoolean clicked =
            new AtomicBoolean(
                    false
            );

    private final Map<Activity, Boolean> installedActivities =
            Collections.synchronizedMap(
                    new WeakHashMap<Activity, Boolean>()
            );

    private String packageName;

    @Override
    public void handleLoadPackage(
            LoadPackageParam lpparam
    ) {
        packageName =
                lpparam.packageName;

        if (shouldSkipProcess(
                lpparam
        )) {
            return;
        }

        HookConfig config =
                HookConfig.load();

        if (!config.isUniversalAppEnabled(
                FeatureRegistry.UNIVERSAL_SPLASH_SKIP,
                lpparam.packageName
        )) {
            return;
        }

        XposedBridge.log(
                TAG
                        + "enabled package="
                        + lpparam.packageName
                        + " window="
                        + WINDOW_MS
                        + "ms threshold="
                        + SCORE_THRESHOLD
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

                        if (isTargetActivity(
                                activity
                        )) {
                            installActivityScanner(
                                    activity
                            );
                        }
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

                        if (!isTargetActivity(
                                activity
                        )) {
                            return;
                        }

                        installActivityScanner(
                                activity
                        );
                        scan(
                                activity,
                                "resume"
                        );
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                Activity.class,
                "onWindowFocusChanged",
                boolean.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(
                            MethodHookParam param
                    ) {
                        if (param.args == null
                                || param.args.length == 0
                                || !Boolean.TRUE.equals(
                                param.args[0]
                        )) {
                            return;
                        }

                        Activity activity =
                                (Activity)
                                        param.thisObject;

                        if (isTargetActivity(
                                activity
                        )) {
                            scan(
                                    activity,
                                    "focus"
                            );
                        }
                    }
                }
        );
    }

    private boolean shouldSkipProcess(
            LoadPackageParam lpparam
    ) {
        if (lpparam == null
                || lpparam.packageName == null) {
            return true;
        }

        if (MODULE_PACKAGE.equals(
                lpparam.packageName
        )
                || "android".equals(
                lpparam.packageName
        )
                || "com.android.systemui".equals(
                lpparam.packageName
        )
                || "com.android.settings".equals(
                lpparam.packageName
        )) {
            return true;
        }

        // Splash UI normally lives in the package's main process.
        // Do not install UI lifecycle hooks into remote/service processes.
        return lpparam.processName != null
                && !lpparam.packageName.equals(
                lpparam.processName
        );
    }

    private boolean isTargetActivity(
            Activity activity
    ) {
        return activity != null
                && packageName != null
                && packageName.equals(
                activity.getPackageName()
        )
                && !activity.isFinishing()
                && !activity.isDestroyed()
                && isWithinWindow()
                && !clicked.get();
    }

    private boolean isWithinWindow() {
        return SystemClock.elapsedRealtime()
                - processStartedAt
                <= WINDOW_MS;
    }

    private void installActivityScanner(
            final Activity activity
    ) {
        if (!isTargetActivity(
                activity
        )) {
            return;
        }

        synchronized (installedActivities) {
            if (installedActivities.containsKey(
                    activity
            )) {
                return;
            }

            installedActivities.put(
                    activity,
                    Boolean.TRUE
            );
        }

        final View root =
                activity.getWindow()
                        .getDecorView();

        if (root == null) {
            return;
        }

        final ViewTreeObserver observer =
                root.getViewTreeObserver();

        final ViewTreeObserver.OnGlobalLayoutListener listener =
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        scan(
                                activity,
                                "layout"
                        );
                    }
                };

        if (observer != null
                && observer.isAlive()) {
            observer.addOnGlobalLayoutListener(
                    listener
            );
        }

        Handler handler =
                new Handler(
                        Looper.getMainLooper()
                );

        long[] delays =
                new long[]{
                        0L,
                        80L,
                        180L,
                        350L,
                        650L,
                        1000L,
                        1600L,
                        2500L,
                        4000L,
                        6500L,
                        9000L
                };

        for (long delay : delays) {
            handler.postDelayed(
                    () -> scan(
                            activity,
                            "delay:"
                                    + delay
                    ),
                    delay
            );
        }

        handler.postDelayed(
                () -> {
                    try {
                        ViewTreeObserver current =
                                root.getViewTreeObserver();

                        if (current != null
                                && current.isAlive()) {
                            current.removeOnGlobalLayoutListener(
                                    listener
                            );
                        }
                    } catch (Throwable ignored) {
                    }
                },
                WINDOW_MS + 750L
        );
    }

    private void scan(
            Activity activity,
            String source
    ) {
        if (!isTargetActivity(
                activity
        )) {
            return;
        }

        View root =
                activity.getWindow()
                        .getDecorView();

        if (root == null
                || !root.isShown()) {
            return;
        }

        Candidate best =
                findBestCandidate(
                        activity,
                        root
                );

        if (best == null
                || best.score
                < SCORE_THRESHOLD
                || !best.strongSignal) {
            return;
        }

        if (!clicked.compareAndSet(
                false,
                true
        )) {
            return;
        }

        boolean success =
                false;

        try {
            success =
                    best.clickTarget.performClick();
        } catch (Throwable throwable) {
            XposedBridge.log(
                    TAG
                            + "click failed package="
                            + packageName
                            + " score="
                            + best.score
                            + " / "
                            + throwable
            );
        }

        if (!success) {
            clicked.set(
                    false
            );

            XposedBridge.log(
                    TAG
                            + "candidate not clickable package="
                            + packageName
                            + " score="
                            + best.score
                            + " source="
                            + source
            );
            return;
        }

        boolean reported =
                ConfigProvider.reportSplashSkip(
                        activity,
                        packageName,
                        best.score
                );

        XposedBridge.log(
                TAG
                        + "clicked package="
                        + packageName
                        + " score="
                        + best.score
                        + " source="
                        + source
                        + " view="
                        + best.sourceView.getClass()
                        .getName()
                        + " report="
                        + reported
        );
    }

    private Candidate findBestCandidate(
            Activity activity,
            View root
    ) {
        ScanState state =
                new ScanState();

        collectCandidate(
                activity,
                root,
                0,
                state
        );

        return state.best;
    }

    private void collectCandidate(
            Activity activity,
            View view,
            int depth,
            ScanState state
    ) {
        if (view == null
                || depth > 32
                || state.nodes
                >= MAX_SCAN_NODES) {
            return;
        }

        state.nodes++;

        if (view.getVisibility()
                != View.VISIBLE
                || view.getAlpha()
                <= 0.05f) {
            return;
        }

        Candidate candidate =
                scoreCandidate(
                        activity,
                        view
                );

        if (candidate != null
                && (state.best == null
                || candidate.score
                > state.best.score)) {
            state.best =
                    candidate;
        }

        if (view instanceof ViewGroup) {
            ViewGroup group =
                    (ViewGroup) view;

            for (int i = 0;
                    i < group.getChildCount();
                    i++) {
                collectCandidate(
                        activity,
                        group.getChildAt(i),
                        depth + 1,
                        state
                );

                if (state.nodes
                        >= MAX_SCAN_NODES) {
                    break;
                }
            }
        }
    }

    private Candidate scoreCandidate(
            Activity activity,
            View source
    ) {
        String text =
                normalize(
                        visibleText(
                                source
                        )
                );

        String resource =
                resourceName(
                        source
                ).toLowerCase(
                        Locale.US
                );

        String hierarchy =
                hierarchySignature(
                        source
                ).toLowerCase(
                        Locale.US
                );

        String activityName =
                activity.getClass()
                        .getName()
                        .toLowerCase(
                                Locale.US
                        );

        boolean activityAdContext =
                containsAdContext(
                        activityName
                );

        boolean hierarchyAdContext =
                containsAdContext(
                        hierarchy
                );

        boolean resourceSkip =
                resource.contains(
                        "skip"
                )
                        || resource.contains(
                        "tiaoguo"
                )
                        || resource.contains(
                        "jump_ad"
                )
                        || resource.contains(
                        "ad_close"
                )
                        || resource.contains(
                        "close_ad"
                );

        boolean textStrong =
                isStrongSkipText(
                        text
                );

        boolean closeWithAdContext =
                ("关闭".equals(
                        text
                )
                        || "close".equals(
                        text
                ))
                        && (activityAdContext
                        || hierarchyAdContext
                        || resource.contains(
                        "ad"
                ));

        if (!resourceSkip
                && !textStrong
                && !closeWithAdContext) {
            return null;
        }

        View clickTarget =
                clickableHost(
                        source
                );

        if (clickTarget == null) {
            return null;
        }

        int score = 0;

        if ("跳过广告".equals(
                text
        )
                || "关闭广告".equals(
                text
        )
                || "skipad".equals(
                text
        )
                || "skipads".equals(
                text
        )
                || "closead".equals(
                text
        )) {
            score += 65;
        } else if ("跳过".equals(
                text
        )
                || "skip".equals(
                text
        )) {
            score += 52;
        } else if (COUNTDOWN_AFTER.matcher(
                text
        ).matches()
                || COUNTDOWN_BEFORE.matcher(
                text
        ).matches()) {
            score += 55;
        } else if (text.contains(
                "跳过"
        )
                || text.contains(
                "skip"
        )) {
            score += 42;
        } else if (closeWithAdContext) {
            score += 32;
        }

        if (resourceSkip) {
            score += 40;
        }

        if (activityAdContext) {
            score += 18;
        }

        if (hierarchyAdContext) {
            score += 16;
        }

        int[] location =
                new int[2];

        clickTarget.getLocationOnScreen(
                location
        );

        int screenWidth =
                activity.getResources()
                        .getDisplayMetrics()
                        .widthPixels;

        int screenHeight =
                activity.getResources()
                        .getDisplayMetrics()
                        .heightPixels;

        if (screenWidth > 0
                && screenHeight > 0) {
            float x =
                    (float) location[0]
                            / screenWidth;
            float y =
                    (float) location[1]
                            / screenHeight;

            if (x >= 0.62f
                    && y <= 0.35f) {
                score += 25;
            } else if (x <= 0.38f
                    && y <= 0.28f) {
                score += 15;
            } else if (x >= 0.68f
                    && y >= 0.65f) {
                score += 10;
            } else if (y <= 0.18f) {
                score += 8;
            }

            if (clickTarget.getWidth()
                    <= screenWidth * 0.42f
                    && clickTarget.getHeight()
                    <= screenHeight * 0.20f) {
                score += 6;
            }
        }

        if (source.isClickable()) {
            score += 15;
        } else if (clickTarget != source) {
            score += 12;
        }

        if (isNegativeText(
                text
        )) {
            score -= 120;
        }

        if (isGuideContext(
                activityName
        )
                && !activityAdContext
                && !hierarchyAdContext
                && !resourceSkip) {
            score -= 45;
        }

        boolean strongSignal =
                textStrong
                        || resourceSkip
                        || activityAdContext
                        || hierarchyAdContext;

        return new Candidate(
                source,
                clickTarget,
                score,
                strongSignal
        );
    }

    private String visibleText(
            View view
    ) {
        if (view instanceof TextView) {
            TextView textView =
                    (TextView) view;

            CharSequence text =
                    textView.getText();

            if (text != null
                    && text.length() > 0) {
                return text.toString();
            }

            CharSequence hint =
                    textView.getHint();

            if (hint != null
                    && hint.length() > 0) {
                return hint.toString();
            }
        }

        CharSequence description =
                view.getContentDescription();

        return description == null
                ? ""
                : description.toString();
    }

    private boolean isStrongSkipText(
            String text
    ) {
        if (text == null
                || text.isEmpty()) {
            return false;
        }

        return "跳过".equals(
                text
        )
                || "跳过广告".equals(
                text
        )
                || "关闭广告".equals(
                text
        )
                || "skip".equals(
                text
        )
                || "skipad".equals(
                text
        )
                || "skipads".equals(
                text
        )
                || "closead".equals(
                text
        )
                || COUNTDOWN_AFTER.matcher(
                text
        ).matches()
                || COUNTDOWN_BEFORE.matcher(
                text
        ).matches()
                || text.contains(
                "跳过广告"
        )
                || text.contains(
                "skipad"
        );
    }

    private boolean isNegativeText(
            String text
    ) {
        if (text == null) {
            return false;
        }

        return text.contains(
                "跳过此步骤"
        )
                || text.contains(
                "跳过引导"
        )
                || text.contains(
                "跳过教程"
        )
                || text.contains(
                "跳过登录"
        )
                || text.contains(
                "跳过绑定"
        )
                || text.contains(
                "跳过设置"
        )
                || text.contains(
                "暂不"
        )
                || text.contains(
                "下次再说"
        );
    }

    private boolean isGuideContext(
            String value
    ) {
        return value.contains(
                "guide"
        )
                || value.contains(
                "tutorial"
        )
                || value.contains(
                "onboarding"
        )
                || value.contains(
                "login"
        )
                || value.contains(
                "register"
        )
                || value.contains(
                "permission"
        )
                || value.contains(
                "consent"
        );
    }

    private boolean containsAdContext(
            String value
    ) {
        return value.contains(
                "splash"
        )
                || value.contains(
                "advert"
        )
                || value.contains(
                "startupad"
        )
                || value.contains(
                "launchad"
        )
                || value.contains(
                "adcontainer"
        )
                || value.contains(
                "adview"
        );
    }

    private View clickableHost(
            View start
    ) {
        View current =
                start;

        for (int i = 0;
                i < 5
                        && current != null;
                i++) {
            if (current.isClickable()
                    && current.isEnabled()
                    && current.getVisibility()
                    == View.VISIBLE) {
                return current;
            }

            ViewParent parent =
                    current.getParent();

            current =
                    parent instanceof View
                            ? (View) parent
                            : null;
        }

        return null;
    }

    private String resourceName(
            View view
    ) {
        if (view == null
                || view.getId()
                == View.NO_ID) {
            return "";
        }

        try {
            return view.getResources()
                    .getResourceName(
                            view.getId()
                    );
        } catch (Throwable ignored) {
            return "";
        }
    }

    private String hierarchySignature(
            View view
    ) {
        StringBuilder builder =
                new StringBuilder();

        View current =
                view;

        for (int i = 0;
                i < 5
                        && current != null;
                i++) {
            if (builder.length() > 0) {
                builder.append(
                        '|'
                );
            }

            builder.append(
                    current.getClass()
                            .getName()
            );

            String resource =
                    resourceName(
                            current
                    );

            if (!resource.isEmpty()) {
                builder.append(
                        '#'
                ).append(
                        resource
                );
            }

            ViewParent parent =
                    current.getParent();

            current =
                    parent instanceof View
                            ? (View) parent
                            : null;
        }

        return builder.toString();
    }

    private String normalize(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return value.toLowerCase(
                Locale.US
        ).replace(
                " ",
                ""
        ).replace(
                "\n",
                ""
        ).replace(
                "\r",
                ""
        ).trim();
    }

    private static final class ScanState {
        int nodes;
        Candidate best;
    }

    private static final class Candidate {
        final View sourceView;
        final View clickTarget;
        final int score;
        final boolean strongSignal;

        Candidate(
                View sourceView,
                View clickTarget,
                int score,
                boolean strongSignal
        ) {
            this.sourceView =
                    sourceView;
            this.clickTarget =
                    clickTarget;
            this.score =
                    score;
            this.strongSignal =
                    strongSignal;
        }
    }
}
