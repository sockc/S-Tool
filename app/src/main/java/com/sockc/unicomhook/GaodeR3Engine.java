package com.sockc.unicomhook;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.sockc.unicomhook.compat.XC_MethodHook;
import com.sockc.unicomhook.compat.XposedBridge;
import com.sockc.unicomhook.compat.XposedHelpers;

/**
 * Gaode R3.
 *
 * High-value rules are installed at Gaode's own business / AJX layer.
 * Generic view scanning remains only as a fallback in GaodeHook.
 */
final class GaodeR3Engine {

    private static final String TAG =
            "Sockc_Gaode_R3: ";

    private static final String[] AJX_TEXT_CLASSES =
            new String[]{
                    "com.autonavi.minimap.ajx3.widget.view.Label",
                    "com.autonavi.minimap.ajx3.widget.view.Html",
                    "com.autonavi.minimap.ajx3.widget.view.RichText",
                    "com.autonavi.minimap.ajx3.widget.view.Text"
            };

    private static final Set<String> BOTTOM_HIDE =
            Collections.unmodifiableSet(
                    new HashSet<>(
                            Arrays.asList(
                                    "探索",
                                    "AI对话",
                                    "路线"
                            )
                    )
            );

    private static final String[] FLOAT_TEXTS =
            new String[]{
                    "扫街榜",
                    "订周末"
            };

    private static final Set<String> installedIds =
            Collections.synchronizedSet(
                    new HashSet<String>()
            );

    private static final Set<String> hitIds =
            Collections.synchronizedSet(
                    new HashSet<String>()
            );

    private static volatile boolean exploreLocalEnabled;
    private static volatile boolean bottomTabsEnabled;
    private static volatile boolean floatBadgesEnabled;

    private GaodeR3Engine() {
    }

    static void install(
            ClassLoader classLoader,
            boolean adSkip,
            boolean exploreLocal,
            boolean bottomTabs,
            boolean floatBadges
    ) {
        exploreLocalEnabled =
                exploreLocal;
        bottomTabsEnabled =
                bottomTabs;
        floatBadgesEnabled =
                floatBadges;

        if (adSkip) {
            installSemanticAdHooks(
                    classLoader
            );
        }

        if (exploreLocal
                || floatBadges) {
            installAjxTextHooks(
                    classLoader
            );
        }

        XposedBridge.log(
                TAG
                        + "installed ad="
                        + adSkip
                        + " explore="
                        + exploreLocal
                        + " tabs="
                        + bottomTabs
                        + " badges="
                        + floatBadges
        );
    }

    private static void installSemanticAdHooks(
            ClassLoader classLoader
    ) {
        hookBooleanFalse(
                classLoader,
                "com.autonavi.bundle.amaphome.impl.BootBizDataPreloaderImpl",
                "canShowSplash",
                "splash_gate"
        );

        hookSafeSuppress(
                classLoader,
                "com.autonavi.minimap.impl.SplashScreenServiceImpl",
                "fetchRealTime",
                "splash_realtime"
        );

        hookBooleanFalse(
                classLoader,
                "com.autonavi.minimap.impl.SplashScreenServiceImpl",
                "isSplashShowing",
                "splash_showing"
        );

        hookBooleanFalse(
                classLoader,
                "com.autonavi.minimap.impl.SplashScreenServiceImpl",
                "isContinueLaunchMaskViewShowing",
                "splash_mask_state"
        );

        hookSafeSuppress(
                classLoader,
                "com.autonavi.minimap.impl.SplashScreenServiceImpl",
                "showSplashMaskView",
                "splash_mask_show"
        );

        hookSafeSuppress(
                classLoader,
                "com.autonavi.bundle.banner.manager.BannerManager",
                "a",
                "banner_manager_a"
        );

        hookSafeSuppress(
                classLoader,
                "com.autonavi.bundle.banner.manager.BannerManager",
                "b",
                "banner_manager_b"
        );

        hookSafeSuppress(
                classLoader,
                "com.autonavi.bundle.banner.net.BannerParser",
                "a",
                "banner_parse"
        );

        hookSafeSuppress(
                classLoader,
                "com.autonavi.minimap.bundle.msgbox.push.BackgroundMsgManager",
                "a",
                "background_message"
        );

        hookSafeSuppress(
                classLoader,
                "com.autonavi.minimap.splashscreen.ajx.NativesModuleSplashScreen",
                "getLinkageMsg",
                "splash_linkage"
        );

        hookSafeSuppress(
                classLoader,
                "com.autonavi.minimap.splashscreen.ajx.NativesModuleSplashScreen",
                "getCurrentLinkageMsg",
                "splash_current_linkage"
        );

        hookSafeSuppress(
                classLoader,
                "com.autonavi.minimap.search.inter.splash.SplashModel",
                "getData",
                "search_splash_data"
        );

        hookSafeSuppress(
                classLoader,
                "com.autonavi.minimap.search.inter.splash.SplashModel",
                "getTemplate",
                "search_splash_template"
        );

        hookSafeSuppress(
                classLoader,
                "com.autonavi.minimap.search.inter.splash.SplashModel",
                "getXmlUrl",
                "search_splash_xml"
        );

        hookSafeSuppress(
                classLoader,
                "com.autonavi.minimap.search.inter.splash.SplashModel",
                "getCssUrl",
                "search_splash_css"
        );

        hookAdViewAttach(
                classLoader,
                "com.autonavi.bundle.banner.view.DBanner",
                "dbanner"
        );
    }

    private static void installAjxTextHooks(
            ClassLoader classLoader
    ) {
        for (String className
                : AJX_TEXT_CLASSES) {
            Class<?> target =
                    XposedHelpers.findClassIfExists(
                            className,
                            classLoader
                    );

            if (target == null) {
                continue;
            }

            for (Class<?> current = target;
                    current != null
                            && current != Object.class;
                    current =
                            current.getSuperclass()) {
                for (Method method
                        : current.getDeclaredMethods()) {
                    String methodName =
                            method.getName();

                    if (!"setText".equals(
                            methodName
                    )
                            && !"setAttribute".equals(
                            methodName
                    )) {
                        continue;
                    }

                    if (Modifier.isAbstract(
                            method.getModifiers()
                    )) {
                        continue;
                    }

                    String id =
                            "ajx_text:"
                                    + current.getName()
                                    + "#"
                                    + methodName
                                    + "#"
                                    + method.getParameterTypes().length;

                    if (!installedIds.add(id)) {
                        continue;
                    }

                    try {
                        method.setAccessible(
                                true
                        );

                        XposedBridge.hookMethod(
                                method,
                                new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(
                                            MethodHookParam param
                                    ) {
                                        if (!(param.thisObject
                                                instanceof View)) {
                                            return;
                                        }

                                        String text =
                                                extractLikelyText(
                                                        param.args
                                                );

                                        if (text == null
                                                || text.isEmpty()) {
                                            return;
                                        }

                                        View view =
                                                (View)
                                                        param.thisObject;

                                        handleAjxText(
                                                view,
                                                normalize(
                                                        text
                                                )
                                        );
                                    }
                                }
                        );

                        XposedBridge.log(
                                TAG
                                        + "HOOKED "
                                        + id
                        );
                    } catch (Throwable throwable) {
                        XposedBridge.log(
                                TAG
                                        + "MISS "
                                        + id
                                        + " / "
                                        + throwable
                        );
                    }
                }
            }
        }
    }

    private static void handleAjxText(
            View view,
            String text
    ) {
        if (exploreLocalEnabled
                && "探索本地".equals(
                text
        )) {
            if (hideAjxListItem(
                    view,
                    "explore_local"
            )) {
                logHit(
                        "explore_local"
                );
            }
            return;
        }

        if (floatBadgesEnabled) {
            for (String keyword
                    : FLOAT_TEXTS) {
                if (text.contains(
                        keyword
                )) {
                    if (hideOperationalHost(
                            view,
                            "badge:"
                                    + keyword
                    )) {
                        logHit(
                                "badge:"
                                        + keyword
                        );
                    }
                    return;
                }
            }
        }
    }

    static void applyBottomTabs(
            Activity activity,
            View root
    ) {
        if (!bottomTabsEnabled
                || activity == null
                || root == null) {
            return;
        }

        ViewGroup row =
                findBottomTabRow(
                        activity,
                        root,
                        0
                );

        if (row == null) {
            return;
        }

        int visible = 0;
        boolean changed = false;

        for (int i = 0;
                i < row.getChildCount();
                i++) {
            View child =
                    row.getChildAt(i);

            String name =
                    findKnownTabName(
                            child
                    );

            if (name != null
                    && BOTTOM_HIDE.contains(
                    name
            )) {
                if (child.getVisibility()
                        != View.GONE) {
                    child.setVisibility(
                            View.GONE
                    );
                    changed = true;
                    logHit(
                            "bottom_tab:"
                                    + name
                    );
                }
            } else if (child.getVisibility()
                    != View.GONE) {
                visible++;
            }
        }

        if (!changed
                || visible <= 0) {
            return;
        }

        if (row instanceof LinearLayout) {
            for (int i = 0;
                    i < row.getChildCount();
                    i++) {
                View child =
                        row.getChildAt(i);

                if (child.getVisibility()
                        == View.GONE) {
                    continue;
                }

                ViewGroup.LayoutParams raw =
                        child.getLayoutParams();

                if (!(raw
                        instanceof LinearLayout.LayoutParams)) {
                    continue;
                }

                LinearLayout.LayoutParams params =
                        (LinearLayout.LayoutParams)
                                raw;

                params.width =
                        0;
                params.weight =
                        1f;
                child.setLayoutParams(
                        params
                );
            }
        }
    }

    private static ViewGroup findBottomTabRow(
            Activity activity,
            View view,
            int depth
    ) {
        if (view == null
                || depth > 15) {
            return null;
        }

        if (view instanceof ViewGroup) {
            ViewGroup group =
                    (ViewGroup) view;

            if (looksLikeBottomTabRow(
                    activity,
                    group
            )) {
                return group;
            }

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

    private static boolean looksLikeBottomTabRow(
            Activity activity,
            ViewGroup group
    ) {
        int count =
                group.getChildCount();

        if (count < 2
                || count > 7
                || group.getWidth()
                < activity.getResources()
                .getDisplayMetrics()
                .widthPixels * 0.65f) {
            return false;
        }

        int[] location =
                new int[2];

        group.getLocationOnScreen(
                location
        );

        int screenHeight =
                activity.getResources()
                        .getDisplayMetrics()
                        .heightPixels;

        if (location[1]
                < screenHeight * 0.70f) {
            return false;
        }

        int known = 0;

        for (int i = 0;
                i < count;
                i++) {
            if (findKnownTabName(
                    group.getChildAt(i)
            ) != null) {
                known++;
            }
        }

        return known >= 2;
    }

    private static String findKnownTabName(
            View view
    ) {
        if (view == null) {
            return null;
        }

        if (view instanceof TextView) {
            CharSequence value =
                    ((TextView) view)
                            .getText();

            if (value != null) {
                String text =
                        normalize(
                                value.toString()
                        );

                if ("首页".equals(text)
                        || "探索".equals(text)
                        || "AI对话".equals(text)
                        || "路线".equals(text)
                        || "长按说话".equals(text)
                        || "打车".equals(text)
                        || "我的".equals(text)) {
                    return text;
                }
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group =
                    (ViewGroup) view;

            for (int i = 0;
                    i < group.getChildCount();
                    i++) {
                String found =
                        findKnownTabName(
                                group.getChildAt(i)
                        );

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private static boolean hideAjxListItem(
            View anchor,
            String reason
    ) {
        View current =
                anchor;

        for (int i = 0;
                i < 30
                        && current != null;
                i++) {
            ViewParent parent =
                    current.getParent();

            if (!(parent instanceof View)) {
                break;
            }

            View parentView =
                    (View) parent;

            if (isAjxList(
                    parentView
            )) {
                return collapseSafely(
                        current,
                        reason,
                        0.62f
                );
            }

            current =
                    parentView;
        }

        return false;
    }

    private static boolean hideOperationalHost(
            View anchor,
            String reason
    ) {
        if (anchor == null) {
            return false;
        }

        View current =
                anchor;
        View best =
                null;

        int screenWidth =
                anchor.getResources()
                        .getDisplayMetrics()
                        .widthPixels;

        int screenHeight =
                anchor.getResources()
                        .getDisplayMetrics()
                        .heightPixels;

        for (int i = 0;
                i < 8
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

            if (width > 0
                    && height > 0
                    && width
                    <= screenWidth * 0.55f
                    && height
                    <= screenHeight * 0.35f) {
                best =
                        candidate;
            } else if (best != null) {
                break;
            }

            if (isAjxList(
                    candidate
            )) {
                break;
            }

            current =
                    candidate;
        }

        if (best == null) {
            best =
                    anchor;
        }

        return collapseSafely(
                best,
                reason,
                0.40f
        );
    }

    private static boolean collapseSafely(
            View view,
            String reason,
            float maxScreenHeightRatio
    ) {
        if (view == null) {
            return false;
        }

        int screenHeight =
                view.getResources()
                        .getDisplayMetrics()
                        .heightPixels;

        int height =
                view.getHeight();

        if (height > 0
                && height
                > screenHeight
                * maxScreenHeightRatio) {
            XposedBridge.log(
                    TAG
                            + "SKIP oversized "
                            + reason
                            + " cls="
                            + view.getClass()
                            .getName()
                            + " h="
                            + height
            );
            return false;
        }

        try {
            view.setAlpha(
                    0f
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

            ViewGroup.LayoutParams params =
                    view.getLayoutParams();

            if (params != null
                    && height > 0) {
                params.height =
                        0;
                view.setLayoutParams(
                        params
                );
            }

            XposedBridge.log(
                    TAG
                            + "HIDE "
                            + reason
                            + " cls="
                            + view.getClass()
                            .getName()
            );
            return true;
        } catch (Throwable throwable) {
            XposedBridge.log(
                    TAG
                            + "HIDE failed "
                            + reason
                            + " / "
                            + throwable
            );
            return false;
        }
    }

    private static boolean isAjxList(
            View view
    ) {
        if (view == null) {
            return false;
        }

        String name =
                view.getClass()
                        .getName()
                        .toLowerCase();

        return name.contains(
                "ajx3.widget.view.list"
        )
                || name.contains(
                "ajxlist"
        )
                || name.contains(
                "recyclerview"
        );
    }

    private static String extractLikelyText(
            Object[] args
    ) {
        if (args == null) {
            return null;
        }

        String candidate =
                null;

        for (Object arg : args) {
            if (!(arg instanceof String)) {
                continue;
            }

            String value =
                    ((String) arg)
                            .trim();

            if (value.isEmpty()
                    || "text".equalsIgnoreCase(
                    value
            )) {
                continue;
            }

            if (value.length() <= 40) {
                candidate =
                        value;
            }
        }

        return candidate;
    }

    private static String normalize(
            String value
    ) {
        if (value == null) {
            return "";
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

    private static void hookBooleanFalse(
            ClassLoader classLoader,
            String className,
            String methodName,
            String id
    ) {
        Class<?> target =
                XposedHelpers.findClassIfExists(
                        className,
                        classLoader
                );

        if (target == null) {
            XposedBridge.log(
                    TAG
                            + "MISS class "
                            + className
            );
            return;
        }

        int count = 0;

        for (Class<?> current = target;
                current != null
                        && current != Object.class;
                current =
                        current.getSuperclass()) {
            for (Method method
                    : current.getDeclaredMethods()) {
                if (!methodName.equals(
                        method.getName()
                )
                        || Modifier.isAbstract(
                        method.getModifiers()
                )
                        || (method.getReturnType()
                        != boolean.class
                        && method.getReturnType()
                        != Boolean.class)) {
                    continue;
                }

                if (installReturnHook(
                        method,
                        Boolean.FALSE,
                        id
                )) {
                    count++;
                }
            }
        }

        XposedBridge.log(
                TAG
                        + "INSTALL "
                        + id
                        + " hooks="
                        + count
        );
    }

    private static void hookSafeSuppress(
            ClassLoader classLoader,
            String className,
            String methodName,
            String id
    ) {
        Class<?> target =
                XposedHelpers.findClassIfExists(
                        className,
                        classLoader
                );

        if (target == null) {
            XposedBridge.log(
                    TAG
                            + "MISS class "
                            + className
            );
            return;
        }

        int count = 0;

        for (Class<?> current = target;
                current != null
                        && current != Object.class;
                current =
                        current.getSuperclass()) {
            for (Method method
                    : current.getDeclaredMethods()) {
                if (!methodName.equals(
                        method.getName()
                )
                        || Modifier.isAbstract(
                        method.getModifiers()
                )) {
                    continue;
                }

                Class<?> returnType =
                        method.getReturnType();

                Object replacement;

                if (returnType == void.class
                        || !returnType.isPrimitive()) {
                    replacement =
                            null;
                } else if (returnType
                        == boolean.class) {
                    replacement =
                            Boolean.FALSE;
                } else {
                    XposedBridge.log(
                            TAG
                                    + "SKIP unsafe primitive "
                                    + className
                                    + "#"
                                    + methodName
                                    + " return="
                                    + returnType.getName()
                    );
                    continue;
                }

                if (installReturnHook(
                        method,
                        replacement,
                        id
                )) {
                    count++;
                }
            }
        }

        XposedBridge.log(
                TAG
                        + "INSTALL "
                        + id
                        + " hooks="
                        + count
        );
    }

    private static boolean installReturnHook(
            Method method,
            Object replacement,
            String id
    ) {
        String hookId =
                id
                        + ":"
                        + method.getDeclaringClass()
                        .getName()
                        + "#"
                        + method.getName()
                        + "/"
                        + method.getParameterTypes()
                        .length;

        if (!installedIds.add(
                hookId
        )) {
            return false;
        }

        try {
            method.setAccessible(
                    true
            );

            XposedBridge.hookMethod(
                    method,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(
                                MethodHookParam param
                        ) {
                            logHit(
                                    id
                            );
                            param.setResult(
                                    replacement
                            );
                        }
                    }
            );

            return true;
        } catch (Throwable throwable) {
            XposedBridge.log(
                    TAG
                            + "HOOK failed "
                            + hookId
                            + " / "
                            + throwable
            );
            return false;
        }
    }

    private static void hookAdViewAttach(
            ClassLoader classLoader,
            String className,
            String id
    ) {
        Class<?> target =
                XposedHelpers.findClassIfExists(
                        className,
                        classLoader
                );

        if (target == null) {
            return;
        }

        Method method =
                null;

        for (Class<?> current = target;
                current != null
                        && current != View.class
                        && current != Object.class;
                current =
                        current.getSuperclass()) {
            try {
                method =
                        current.getDeclaredMethod(
                                "onAttachedToWindow"
                        );
                break;
            } catch (NoSuchMethodException ignored) {
            }
        }

        if (method == null) {
            return;
        }

        final Method targetMethod =
                method;

        String hookId =
                id
                        + ":attach";

        if (!installedIds.add(
                hookId
        )) {
            return;
        }

        try {
            targetMethod.setAccessible(
                    true
            );

            XposedBridge.hookMethod(
                    targetMethod,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(
                                MethodHookParam param
                        ) {
                            if (param.thisObject
                                    instanceof View) {
                                View view =
                                        (View)
                                                param.thisObject;
                                view.setVisibility(
                                        View.GONE
                                );
                                logHit(
                                        id
                                );
                            }
                        }
                    }
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    TAG
                            + "DBanner hook failed: "
                            + throwable
            );
        }
    }

    private static void logHit(
            String id
    ) {
        if (hitIds.add(
                id
        )) {
            XposedBridge.log(
                    TAG
                            + "HIT "
                            + id
            );
        }
    }
}
