package com.sockc.unicomhook;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.sockc.unicomhook.compat.XposedBridge;

/**
 * Gaode minimal-home UI engine.
 *
 * This engine never changes Gaode's splash/launch state machine.
 * Its only job is to keep the map/search shell and remove home discovery UI.
 */
final class GaodeMinimalHomeEngine {

    private static final String TAG =
            "Sockc_Gaode_Minimal: ";

    private static final String[] SEARCH_MARKERS =
            new String[]{
                    "查找地点",
                    "公交、地铁",
                    "公交,地铁"
            };

    private static final String[] BOTTOM_TABS =
            new String[]{
                    "首页",
                    "对话",
                    "AI对话",
                    "Ai对话",
                    "打车",
                    "未登录",
                    "我的"
            };

    private static final String[] RIGHT_FLOATING =
            new String[]{
                    "扫街榜",
                    "更多"
            };

    private static final String[] HOME_TOOL_LABELS =
            new String[]{
                    "驾车",
                    "公交",
                    "打车",
                    "实时公交",
                    "订酒店",
                    "火车票机票",
                    "顺风车",
                    "超划算",
                    "离线地图",
                    "旅游度假",
                    "收藏夹",
                    "步行",
                    "骑行",
                    "更多工具"
            };

    private static final String[] HOME_CHANNEL_LABELS =
            new String[]{
                    "关注",
                    "推荐",
                    "附近",
                    "十一去哪",
                    "美食",
                    "周末出游"
            };

    private static final Map<Activity, Boolean> HOME_CONFIRMED =
            Collections.synchronizedMap(
                    new WeakHashMap<Activity, Boolean>()
            );

    private static final Set<View> HIDDEN =
            Collections.newSetFromMap(
                    new WeakHashMap<View, Boolean>()
            );

    private GaodeMinimalHomeEngine() {
    }

    static void apply(
            Activity activity,
            View root
    ) {
        if (activity == null
                || root == null
                || activity.isFinishing()) {
            return;
        }

        View searchAnchor =
                findSearchAnchor(
                        root,
                        0
                );

        boolean confirmed =
                Boolean.TRUE.equals(
                        HOME_CONFIRMED.get(
                                activity
                        )
                );

        if (!confirmed) {
            int tabs =
                    countLabels(
                            root,
                            BOTTOM_TABS,
                            0,
                            0,
                            3
                    );

            int tools =
                    countLabels(
                            root,
                            HOME_TOOL_LABELS,
                            0,
                            0,
                            5
                    );

            int channels =
                    countLabels(
                            root,
                            HOME_CHANNEL_LABELS,
                            0,
                            0,
                            3
                    );

            confirmed =
                    searchAnchor != null
                            && (tabs >= 2
                            || tools >= 4
                            || channels >= 2);

            if (confirmed) {
                HOME_CONFIRMED.put(
                        activity,
                        Boolean.TRUE
                );

                XposedBridge.log(
                        TAG
                                + "home confirmed activity="
                                + activity.getClass()
                                .getName()
                );
            }
        }

        if (!confirmed) {
            return;
        }

        hideBottomNavigation(
                activity,
                root
        );

        hideRightFloatingEntries(
                activity,
                root
        );

        if (searchAnchor != null) {
            trimExpandedHomePanel(
                    activity,
                    searchAnchor
            );
        }
    }

    private static void hideBottomNavigation(
            Activity activity,
            View root
    ) {
        ViewGroup row =
                findBottomTabRow(
                        activity,
                        root,
                        0
                );

        if (row == null) {
            return;
        }

        hide(
                row,
                "bottom_navigation"
        );
    }

    private static ViewGroup findBottomTabRow(
            Activity activity,
            View view,
            int depth
    ) {
        if (view == null
                || depth > 18) {
            return null;
        }

        if (view instanceof ViewGroup) {
            ViewGroup group =
                    (ViewGroup) view;

            if (isBottomTabRow(
                    activity,
                    group
            )) {
                return group;
            }

            // Prefer the deepest matching row.
            for (int i = 0;
                    i < group.getChildCount();
                    i++) {
                ViewGroup found =
                        findBottomTabRow(
                                activity,
                                group.getChildAt(i),
                                depth + 1
                        );

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private static boolean isBottomTabRow(
            Activity activity,
            ViewGroup group
    ) {
        if (group.getChildCount() < 2
                || group.getChildCount() > 7) {
            return false;
        }

        int screenWidth =
                activity.getResources()
                        .getDisplayMetrics()
                        .widthPixels;

        int screenHeight =
                activity.getResources()
                        .getDisplayMetrics()
                        .heightPixels;

        if (group.getWidth()
                < screenWidth * 0.65f) {
            return false;
        }

        int[] location =
                new int[2];

        group.getLocationOnScreen(
                location
        );

        if (location[1]
                < screenHeight * 0.72f) {
            return false;
        }

        int known =
                countLabels(
                        group,
                        BOTTOM_TABS,
                        0,
                        0,
                        4
                );

        return known >= 2;
    }

    private static void hideRightFloatingEntries(
            Activity activity,
            View root
    ) {
        hideRightFloatingRecursive(
                activity,
                root,
                0
        );
    }

    private static void hideRightFloatingRecursive(
            Activity activity,
            View view,
            int depth
    ) {
        if (view == null
                || depth > 28) {
            return;
        }

        String text =
                normalizedText(
                        view
                );

        if (matchesRightFloating(
                text
        )) {
            int[] location =
                    new int[2];

            view.getLocationOnScreen(
                    location
            );

            int screenWidth =
                    activity.getResources()
                            .getDisplayMetrics()
                            .widthPixels;

            if (location[0]
                    > screenWidth * 0.55f) {
                View host =
                        findSmallRightHost(
                                activity,
                                view
                        );

                if (host != null) {
                    hide(
                            host,
                            "right:"
                                    + text
                    );
                }
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group =
                    (ViewGroup) view;

            for (int i = 0;
                    i < group.getChildCount();
                    i++) {
                hideRightFloatingRecursive(
                        activity,
                        group.getChildAt(i),
                        depth + 1
                );
            }
        }
    }

    private static boolean matchesRightFloating(
            String text
    ) {
        if (text == null
                || text.isEmpty()) {
            return false;
        }

        if ("更多".equals(
                text
        )) {
            return true;
        }

        return text.contains(
                "扫街榜"
        );
    }

    private static View findSmallRightHost(
            Activity activity,
            View start
    ) {
        int screenWidth =
                activity.getResources()
                        .getDisplayMetrics()
                        .widthPixels;

        int screenHeight =
                activity.getResources()
                        .getDisplayMetrics()
                        .heightPixels;

        View current =
                start;
        View best =
                start;

        for (int i = 0;
                i < 7
                        && current != null;
                i++) {
            ViewParent parent =
                    current.getParent();

            if (!(parent instanceof View)) {
                break;
            }

            View candidate =
                    (View) parent;

            int width =
                    candidate.getWidth();
            int height =
                    candidate.getHeight();

            if (width <= 0
                    || height <= 0) {
                current =
                        candidate;
                continue;
            }

            if (width
                    <= screenWidth * 0.48f
                    && height
                    <= screenHeight * 0.30f) {
                best =
                        candidate;
                current =
                        candidate;
                continue;
            }

            break;
        }

        return best;
    }

    private static void trimExpandedHomePanel(
            Activity activity,
            View searchAnchor
    ) {
        View searchHost =
                findSearchHost(
                        activity,
                        searchAnchor
                );

        if (searchHost == null) {
            return;
        }

        View panel =
                findExpandedPanel(
                        activity,
                        searchHost
                );

        if (panel == null) {
            return;
        }

        View current =
                searchHost;

        while (current != null
                && current != panel) {
            ViewParent parent =
                    current.getParent();

            if (!(parent instanceof ViewGroup)) {
                return;
            }

            ViewGroup group =
                    (ViewGroup) parent;

            hideSiblingsExcept(
                    group,
                    current
            );

            current =
                    group;
        }

        if (current == panel
                && panel instanceof ViewGroup) {
            // Search branch has already been preserved while walking upward.
            XposedBridge.log(
                    TAG
                            + "expanded home panel trimmed, search preserved"
            );
        }
    }

    private static View findSearchHost(
            Activity activity,
            View searchAnchor
    ) {
        int screenWidth =
                activity.getResources()
                        .getDisplayMetrics()
                        .widthPixels;

        int minHeight =
                dp(
                        activity,
                        42
                );

        int maxHeight =
                dp(
                        activity,
                        115
                );

        View current =
                searchAnchor;
        View best =
                null;

        for (int i = 0;
                i < 8
                        && current != null;
                i++) {
            int width =
                    current.getWidth();
            int height =
                    current.getHeight();

            if (width
                    >= screenWidth * 0.65f
                    && height >= minHeight
                    && height <= maxHeight) {
                best =
                        current;
            }

            ViewParent parent =
                    current.getParent();

            current =
                    parent instanceof View
                            ? (View) parent
                            : null;
        }

        return best;
    }

    private static View findExpandedPanel(
            Activity activity,
            View searchHost
    ) {
        int screenWidth =
                activity.getResources()
                        .getDisplayMetrics()
                        .widthPixels;

        int minExtraHeight =
                dp(
                        activity,
                        130
                );

        View current =
                searchHost;

        for (int i = 0;
                i < 10
                        && current != null;
                i++) {
            ViewParent parent =
                    current.getParent();

            if (!(parent instanceof View)) {
                break;
            }

            View candidate =
                    (View) parent;

            if (candidate.getWidth()
                    >= screenWidth * 0.80f
                    && candidate.getHeight()
                    >= searchHost.getHeight()
                    + minExtraHeight) {
                int tools =
                        countLabels(
                                candidate,
                                HOME_TOOL_LABELS,
                                0,
                                0,
                                5
                        );

                int channels =
                        countLabels(
                                candidate,
                                HOME_CHANNEL_LABELS,
                                0,
                                0,
                                3
                        );

                if (tools >= 4
                        || channels >= 2) {
                    return candidate;
                }
            }

            current =
                    candidate;
        }

        return null;
    }

    private static void hideSiblingsExcept(
            ViewGroup parent,
            View keep
    ) {
        for (int i = 0;
                i < parent.getChildCount();
                i++) {
            View child =
                    parent.getChildAt(i);

            if (child == keep) {
                continue;
            }

            // Never hide a sibling that itself contains the preserved search host.
            if (containsView(
                    child,
                    keep,
                    0
            )) {
                continue;
            }

            hide(
                    child,
                    "home_panel_content"
            );
        }
    }

    private static boolean containsView(
            View root,
            View target,
            int depth
    ) {
        if (root == target) {
            return true;
        }

        if (!(root instanceof ViewGroup)
                || depth > 12) {
            return false;
        }

        ViewGroup group =
                (ViewGroup) root;

        for (int i = 0;
                i < group.getChildCount();
                i++) {
            if (containsView(
                    group.getChildAt(i),
                    target,
                    depth + 1
            )) {
                return true;
            }
        }

        return false;
    }

    private static View findSearchAnchor(
            View view,
            int depth
    ) {
        if (view == null
                || depth > 28) {
            return null;
        }

        String text =
                rawText(
                        view
                );

        if (text != null) {
            for (String marker
                    : SEARCH_MARKERS) {
                if (text.contains(
                        marker
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
                        findSearchAnchor(
                                group.getChildAt(i),
                                depth + 1
                        );

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private static int countLabels(
            View view,
            String[] labels,
            int depth,
            int count,
            int stopAt
    ) {
        if (view == null
                || depth > 22
                || count >= stopAt) {
            return count;
        }

        String text =
                normalizedText(
                        view
                );

        if (text != null) {
            for (String label : labels) {
                if (label.equals(
                        text
                )) {
                    count++;
                    break;
                }
            }
        }

        if (count >= stopAt) {
            return count;
        }

        if (view instanceof ViewGroup) {
            ViewGroup group =
                    (ViewGroup) view;

            for (int i = 0;
                    i < group.getChildCount();
                    i++) {
                count =
                        countLabels(
                                group.getChildAt(i),
                                labels,
                                depth + 1,
                                count,
                                stopAt
                        );

                if (count >= stopAt) {
                    break;
                }
            }
        }

        return count;
    }

    private static String rawText(
            View view
    ) {
        String anchored =
                GaodeR3Engine.textFor(
                        view
                );

        if (anchored != null
                && !anchored.trim().isEmpty()) {
            return anchored.trim();
        }

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
                ? null
                : description.toString();
    }

    private static String normalizedText(
            View view
    ) {
        String value =
                rawText(
                        view
                );

        if (value == null) {
            return null;
        }

        return value.replace(
                "\n",
                ""
        ).replace(
                "\r",
                ""
        ).replace(
                " ",
                ""
        ).replace(
                "　",
                ""
        ).trim();
    }

    private static void hide(
            View view,
            String reason
    ) {
        if (view == null) {
            return;
        }

        if (view.getVisibility()
                == View.GONE
                && HIDDEN.contains(
                view
        )) {
            return;
        }

        try {
            HIDDEN.add(
                    view
            );

            view.setVisibility(
                    View.GONE
            );
            view.setEnabled(
                    false
            );
            view.setClickable(
                    false
            );

            XposedBridge.log(
                    TAG
                            + "HIDE "
                            + reason
                            + " cls="
                            + view.getClass()
                            .getName()
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    TAG
                            + "HIDE failed "
                            + reason
                            + " / "
                            + throwable
            );
        }
    }

    private static int dp(
            Activity activity,
            int value
    ) {
        return Math.round(
                value
                        * activity.getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
}
