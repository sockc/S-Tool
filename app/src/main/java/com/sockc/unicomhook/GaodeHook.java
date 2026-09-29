package com.sockc.unicomhook;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.sockc.unicomhook.compat.XC_MethodHook;
import com.sockc.unicomhook.compat.XposedBridge;
import com.sockc.unicomhook.compat.XposedHelpers;

public class GaodeHook implements HookModule {

    private static final String TAG = "Sockc_Gaode: ";
    private static final String TARGET_PACKAGE = "com.autonavi.minimap";
    private static final int TAG_LISTENER_INSTALLED = 0x7F0B7001;

    private static final Set<String> DIAGNOSTIC_SIGNATURES =
            java.util.Collections.synchronizedSet(
                    new HashSet<>()
            );

    private static final Set<String> HOME_CARD_TEXTS =
            new HashSet<>(Arrays.asList(
                    "探索本地",
                    "美食",
                    "酒店",
                    "景点",
                    "加油站",
                    "休闲娱乐",
                    "超市"
            ));

    private static final Set<String> BOTTOM_TAB_TEXTS =
            new HashSet<>(Arrays.asList(
                    "探索",
                    "AI对话",
                    "路线"
            ));

    private static final String[] FLOAT_BADGE_KEYWORDS =
            new String[]{
                    "扫街榜",
                    "订周末"
            };

    private static final String[] HOME_MARKERS =
            new String[]{
                    "设置家",
                    "设置单位",
                    "常去地点",
                    "探索本地"
            };

    private boolean adSkipEnabled;
    private boolean exploreLocalEnabled;
    private boolean bottomTabsEnabled;
    private boolean floatBadgesEnabled;

    @Override
    public void handleLoadPackage(
            LoadPackageParam lpparam
    ) {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)
                || !TARGET_PACKAGE.equals(lpparam.processName)) {
            return;
        }

        HookConfig config = HookConfig.load();

        adSkipEnabled =
                config.isEnabled(
                        "gaode",
                        "gaode.ad_skip"
                );
        exploreLocalEnabled =
                config.isEnabled(
                        "gaode",
                        "gaode.explore_local"
                );
        bottomTabsEnabled =
                config.isEnabled(
                        "gaode",
                        "gaode.bottom_tabs"
                );
        floatBadgesEnabled =
                config.isEnabled(
                        "gaode",
                        "gaode.float_badges"
                );

        if (!adSkipEnabled
                && !exploreLocalEnabled
                && !bottomTabsEnabled
                && !floatBadgesEnabled) {
            XposedBridge.log(
                    TAG + "全部高德子功能已关闭"
            );
            return;
        }

        XposedBridge.log(
                TAG
                        + "子功能: ad="
                        + adSkipEnabled
                        + ", explore="
                        + exploreLocalEnabled
                        + ", tabs="
                        + bottomTabsEnabled
                        + ", badges="
                        + floatBadgesEnabled
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
                                (Activity) param.thisObject;

                        if (!TARGET_PACKAGE.equals(
                                activity.getPackageName()
                        )) {
                            return;
                        }

                        installCleaner(activity);
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
                                (Activity) param.thisObject;

                        if (!TARGET_PACKAGE.equals(
                                activity.getPackageName()
                        )) {
                            return;
                        }

                        installCleaner(activity);
                        scheduleRepeatedClean(activity);
                    }
                }
        );
    }

    private void installCleaner(
            final Activity activity
    ) {
        final View decorView =
                activity.getWindow().getDecorView();

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

        ViewTreeObserver.OnGlobalLayoutListener listener =
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        try {
                            runEnabledCleaners(
                                    activity,
                                    decorView
                            );
                        } catch (Throwable throwable) {
                            XposedBridge.log(
                                    TAG
                                            + "布局清理异常: "
                                            + throwable
                            );
                        }
                    }
                };

        decorView
                .getViewTreeObserver()
                .addOnGlobalLayoutListener(listener);

        scheduleRepeatedClean(activity);
    }

    private void scheduleRepeatedClean(
            final Activity activity
    ) {
        final View decorView =
                activity.getWindow().getDecorView();

        if (decorView == null) {
            return;
        }

        Handler handler =
                new Handler(Looper.getMainLooper());

        long[] delays =
                new long[]{
                        0,
                        250,
                        600,
                        1200,
                        2200,
                        4000
                };

        for (final long delay : delays) {
            handler.postDelayed(
                    () -> {
                        try {
                            runEnabledCleaners(
                                    activity,
                                    decorView
                            );
                        } catch (Throwable throwable) {
                            XposedBridge.log(
                                    TAG
                                            + "延时清理异常("
                                            + delay
                                            + "): "
                                            + throwable
                            );
                        }
                    },
                    delay
            );
        }
    }

    private void runEnabledCleaners(
            Activity activity,
            View root
    ) {
        diagnoseInterestingViews(
                activity,
                root
        );

        if (adSkipEnabled) {
            scanAndClickSkip(root);
        }

        if (exploreLocalEnabled
                || bottomTabsEnabled
                || floatBadgesEnabled) {
            cleanHomePage(activity, root);
        }
    }

    private void cleanHomePage(
            Activity activity,
            View root
    ) {
        if (!looksLikeHomePage(root)) {
            return;
        }

        if (exploreLocalEnabled) {
            hideExploreLocalSection(
                    activity,
                    root
            );
        }

        if (bottomTabsEnabled) {
            hideBottomTabs(
                    activity,
                    root
            );
        }

        if (floatBadgesEnabled) {
            hideFloatBadges(
                    activity,
                    root
            );
        }
    }

    private void scanAndClickSkip(View view) {
        if (view == null) {
            return;
        }

        if (view instanceof ViewGroup) {
            ViewGroup group =
                    (ViewGroup) view;

            for (int i = 0;
                    i < group.getChildCount();
                    i++) {
                View child =
                        group.getChildAt(i);

                if (child instanceof TextView) {
                    CharSequence cs =
                            ((TextView) child).getText();

                    if (cs != null) {
                        String text =
                                cs.toString();

                        if (text.contains("跳过")
                                || text.contains(
                                "关闭广告"
                        )) {
                            XposedBridge.log(
                                    TAG
                                            + "发现广告按钮: "
                                            + text
                            );

                            child.performClick();

                            if (child.getParent()
                                    instanceof View) {
                                ((View) child.getParent())
                                        .performClick();
                            }

                            child.setVisibility(
                                    View.GONE
                            );
                            return;
                        }
                    }
                }

                scanAndClickSkip(child);
            }
        }
    }

    private void hideExploreLocalSection(
            Activity activity,
            View root
    ) {
        View titleView =
                findFirstViewByText(
                        root,
                        "探索本地"
                );

        if (titleView == null) {
            titleView =
                    findFirstViewByIdentity(
                            root,
                            new String[]{
                                    "explore_local",
                                    "local_explore",
                                    "home_explore",
                                    "localexplore"
                            }
                    );
        }

        if (titleView == null) {
            return;
        }

        View target =
                findAncestorForSection(
                        activity,
                        titleView
                );

        if (target == null) {
            return;
        }

        if (target.getVisibility()
                != View.GONE) {
            XposedBridge.log(
                    TAG + "隐藏整块: 探索本地"
            );
            target.setVisibility(View.GONE);
            target.setEnabled(false);
            target.setClickable(false);
        }
    }

    private void hideBottomTabs(
            Activity activity,
            View root
    ) {
        for (String text
                : BOTTOM_TAB_TEXTS) {
            View hit =
                    findFirstViewByText(
                            root,
                            text
                    );

            if (hit == null) {
                String hint =
                        "AI对话".equals(text)
                                ? "ai"
                                : "路线".equals(text)
                                ? "route"
                                : "explore";

                hit =
                        findBottomCandidate(
                                activity,
                                root,
                                hint
                        );
            }

            if (hit == null) {
                continue;
            }

            View target =
                    findAncestorForBottomTab(
                            activity,
                            hit
                    );

            if (target == null) {
                continue;
            }

            if (target.getVisibility()
                    != View.GONE) {
                XposedBridge.log(
                        TAG
                                + "隐藏底部 Tab: "
                                + text
                );
                target.setVisibility(View.GONE);
                target.setEnabled(false);
                target.setClickable(false);
            }
        }
    }

    private void hideFloatBadges(
            Activity activity,
            View root
    ) {
        View hit =
                findFirstViewByKeywords(
                        root,
                        FLOAT_BADGE_KEYWORDS
                );

        if (hit == null) {
            hit =
                    findRightCandidate(
                            activity,
                            root,
                            new String[]{
                                    "weekend",
                                    "rank",
                                    "street",
                                    "badge"
                            }
                    );
        }

        if (hit == null) {
            return;
        }

        View target =
                findAncestorForFloatBadge(
                        activity,
                        hit
                );

        if (target == null) {
            return;
        }

        if (target.getVisibility()
                != View.GONE) {
            XposedBridge.log(
                    TAG
                            + "隐藏右侧挂件: "
                            + viewText(hit)
            );

            target.setVisibility(View.GONE);
            target.setEnabled(false);
            target.setClickable(false);
        }
    }

    private boolean looksLikeHomePage(
            View root
    ) {
        for (String marker
                : HOME_MARKERS) {
            if (findFirstViewByText(
                    root,
                    marker
            ) != null) {
                return true;
            }
        }

        return false;
    }

    private View findFirstViewByText(
            View view,
            String target
    ) {
        if (view == null) {
            return null;
        }

        String current =
                viewText(view);

        if (target.equals(current)) {
            return view;
        }

        if (view instanceof ViewGroup) {
            ViewGroup group =
                    (ViewGroup) view;

            for (int i = 0;
                    i < group.getChildCount();
                    i++) {
                View found =
                        findFirstViewByText(
                                group.getChildAt(i),
                                target
                        );

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private View findFirstViewByKeywords(
            View view,
            String[] keywords
    ) {
        if (view == null) {
            return null;
        }

        String current =
                viewText(view);

        if (current != null) {
            for (String keyword : keywords) {
                if (current.contains(keyword)) {
                    return view;
                }
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group =
                    (ViewGroup) view;

            for (int i = 0;
                    i < group.getChildCount();
                    i++) {
                View found =
                        findFirstViewByKeywords(
                                group.getChildAt(i),
                                keywords
                        );

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private void diagnoseInterestingViews(
            Activity activity,
            View root
    ) {
        if (root == null) {
            return;
        }

        diagnoseViewRecursive(
                activity,
                root,
                0
        );
    }

    private void diagnoseViewRecursive(
            Activity activity,
            View view,
            int depth
    ) {
        if (view == null
                || depth > 32) {
            return;
        }

        String identity =
                viewIdentity(
                        view
                );

        String lower =
                identity.toLowerCase(
                        java.util.Locale.US
                );

        boolean interesting =
                lower.contains(
                        "探索"
                )
                        || lower.contains(
                        "explore"
                )
                        || lower.contains(
                        "ai对话"
                )
                        || lower.contains(
                        "route"
                )
                        || lower.contains(
                        "路线"
                )
                        || lower.contains(
                        "扫街"
                )
                        || lower.contains(
                        "weekend"
                )
                        || lower.contains(
                        "订周末"
                )
                        || lower.contains(
                        "rank"
                )
                        || lower.contains(
                        "bottom"
                )
                        || lower.contains(
                        "tab"
                );

        if (interesting) {
            String signature =
                    activity.getClass()
                            .getName()
                            + "|"
                            + identity
                            + "|"
                            + parentChain(
                            view
                    );

            if (DIAGNOSTIC_SIGNATURES.add(
                    signature
            )) {
                XposedBridge.log(
                        TAG
                                + "R2 fingerprint activity="
                                + activity.getClass()
                                .getName()
                                + " view="
                                + identity
                                + " parent="
                                + parentChain(
                                view
                        )
                );
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group =
                    (ViewGroup) view;

            for (int i = 0;
                    i < group.getChildCount();
                    i++) {
                diagnoseViewRecursive(
                        activity,
                        group.getChildAt(i),
                        depth + 1
                );
            }
        }
    }

    private View findFirstViewByIdentity(
            View view,
            String[] hints
    ) {
        if (view == null) {
            return null;
        }

        String identity =
                viewIdentity(
                        view
                ).toLowerCase(
                        java.util.Locale.US
                );

        for (String hint : hints) {
            if (identity.contains(
                    hint.toLowerCase(
                            java.util.Locale.US
                    )
            )) {
                return view;
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group =
                    (ViewGroup) view;

            for (int i = 0;
                    i < group.getChildCount();
                    i++) {
                View found =
                        findFirstViewByIdentity(
                                group.getChildAt(i),
                                hints
                        );

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private View findBottomCandidate(
            Activity activity,
            View view,
            String hint
    ) {
        if (view == null) {
            return null;
        }

        int[] location =
                new int[2];

        view.getLocationOnScreen(
                location
        );

        int screenHeight =
                activity.getResources()
                        .getDisplayMetrics()
                        .heightPixels;

        String identity =
                viewIdentity(
                        view
                ).toLowerCase(
                        java.util.Locale.US
                );

        boolean nearBottom =
                location[1]
                        > (int) (
                        screenHeight * 0.72f
                );

        if (nearBottom
                && identity.contains(
                hint.toLowerCase(
                        java.util.Locale.US
                )
        )) {
            return view;
        }

        if (view instanceof ViewGroup) {
            ViewGroup group =
                    (ViewGroup) view;

            for (int i = 0;
                    i < group.getChildCount();
                    i++) {
                View found =
                        findBottomCandidate(
                                activity,
                                group.getChildAt(i),
                                hint
                        );

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private View findRightCandidate(
            Activity activity,
            View view,
            String[] hints
    ) {
        if (view == null) {
            return null;
        }

        int[] location =
                new int[2];

        view.getLocationOnScreen(
                location
        );

        int screenWidth =
                activity.getResources()
                        .getDisplayMetrics()
                        .widthPixels;

        String identity =
                viewIdentity(
                        view
                ).toLowerCase(
                        java.util.Locale.US
                );

        boolean atRight =
                location[0]
                        > (int) (
                        screenWidth * 0.60f
                );

        if (atRight) {
            for (String hint : hints) {
                if (identity.contains(
                        hint.toLowerCase(
                                java.util.Locale.US
                        )
                )) {
                    return view;
                }
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group =
                    (ViewGroup) view;

            for (int i = 0;
                    i < group.getChildCount();
                    i++) {
                View found =
                        findRightCandidate(
                                activity,
                                group.getChildAt(i),
                                hints
                        );

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private String viewIdentity(
            View view
    ) {
        StringBuilder builder =
                new StringBuilder();

        builder.append(
                view.getClass()
                        .getName()
        );

        String resourceName =
                resourceName(
                        view
                );

        if (resourceName != null) {
            builder.append(
                    "#"
            ).append(
                    resourceName
            );
        }

        String text =
                viewText(
                        view
                );

        if (text != null) {
            builder.append(
                    "["
            ).append(
                    text
            ).append(
                    "]"
            );
        }

        return builder.toString();
    }

    private String resourceName(
            View view
    ) {
        if (view == null
                || view.getId()
                == View.NO_ID) {
            return null;
        }

        try {
            return view.getResources()
                    .getResourceName(
                            view.getId()
                    );
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String parentChain(
            View start
    ) {
        StringBuilder builder =
                new StringBuilder();

        View current =
                start;

        for (int i = 0;
                i < 6
                        && current != null;
                i++) {
            if (builder.length() > 0) {
                builder.append(
                        " <- "
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

            if (resource != null) {
                builder.append(
                        "#"
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

    private String viewText(View view) {
        if (view == null) {
            return null;
        }

        if (view instanceof TextView) {
            CharSequence cs =
                    ((TextView) view).getText();

            if (cs != null) {
                String text =
                        cs.toString().trim();

                if (!text.isEmpty()) {
                    return text;
                }
            }
        }

        CharSequence description =
                view.getContentDescription();

        if (description != null) {
            String text =
                    description
                            .toString()
                            .trim();

            if (!text.isEmpty()) {
                return text;
            }
        }

        return null;
    }

    private View findAncestorForSection(
            Activity activity,
            View start
    ) {
        View current = start;
        int minWidth = dp(activity, 220);
        int minHeight = dp(activity, 220);

        for (int i = 0; i < 8; i++) {
            ViewParent parent =
                    current.getParent();

            if (!(parent instanceof View)) {
                break;
            }

            View parentView =
                    (View) parent;

            if (parentView.getWidth()
                    >= minWidth
                    && parentView.getHeight()
                    >= minHeight) {
                return parentView;
            }

            if (parentView.getId()
                    == android.R.id.content) {
                break;
            }

            current = parentView;
        }

        return current;
    }

    private View findAncestorForBottomTab(
            Activity activity,
            View start
    ) {
        View current = start;
        int screenHeight =
                activity
                        .getResources()
                        .getDisplayMetrics()
                        .heightPixels;

        for (int i = 0; i < 7; i++) {
            ViewParent parent =
                    current.getParent();

            if (!(parent instanceof View)) {
                break;
            }

            View parentView =
                    (View) parent;
            int[] location = new int[2];

            parentView.getLocationOnScreen(
                    location
            );

            boolean nearBottom =
                    location[1]
                            > (int) (
                            screenHeight * 0.80f
                    );

            boolean reasonableHeight =
                    parentView.getHeight()
                            > dp(activity, 36)
                            && parentView.getHeight()
                            < dp(activity, 140);

            if (nearBottom
                    && reasonableHeight) {
                return parentView;
            }

            if (parentView.getId()
                    == android.R.id.content) {
                break;
            }

            current = parentView;
        }

        return current;
    }

    private View findAncestorForFloatBadge(
            Activity activity,
            View start
    ) {
        View current = start;
        int screenWidth =
                activity
                        .getResources()
                        .getDisplayMetrics()
                        .widthPixels;

        for (int i = 0; i < 7; i++) {
            ViewParent parent =
                    current.getParent();

            if (!(parent instanceof View)) {
                break;
            }

            View parentView =
                    (View) parent;
            int[] location =
                    new int[2];

            parentView.getLocationOnScreen(
                    location
            );

            boolean atRightSide =
                    location[0]
                            > (int) (
                            screenWidth * 0.75f
                    );

            if (atRightSide) {
                return parentView;
            }

            if (parentView.getId()
                    == android.R.id.content) {
                break;
            }

            current = parentView;
        }

        return current;
    }

    private int dp(
            Activity activity,
            int value
    ) {
        float density =
                activity
                        .getResources()
                        .getDisplayMetrics()
                        .density;

        return (int) (
                value * density + 0.5f
        );
    }
}
