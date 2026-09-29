package com.sockc.unicomhook;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

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

    private static final Map<View, String> textAnchors =
            Collections.synchronizedMap(
                    new WeakHashMap<View, String>()
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
        // 17.x semantic gate: let Gaode's own launch state machine
        // take the NO_SPLASH path instead of swallowing launch callbacks.
        hookBooleanFalse(
                classLoader,
                "com.autonavi.bundle.amaphome.impl.BootBizDataPreloaderImpl",
                "canShowSplash",
                "splash_gate"
        );

        // Observe the real finish path. Do not alter it.
        hookObserveAll(
                classLoader,
                "com.autonavi.minimap.g",
                "e",
                "splash_finish"
        );

        // 16.x compatibility: execute the original gate method first,
        // then change only the returned finish-reason field to NO_SPLASH.
        installLegacySplashGate(
                classLoader,
                "u96"
        );
        installLegacySplashGate(
                classLoader,
                "za6"
        );

        // IMPORTANT:
        // Do NOT swallow SplashScreenServiceImpl.fetchRealTime(),
        // showSplashMaskView(), isSplashShowing() or related launch-state
        // methods here. Some Gaode builds wait on their callback/state chain;
        // short-circuiting them can leave SplashActivity alive forever.

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

    private static void installLegacySplashGate(
            ClassLoader classLoader,
            String className
    ) {
        Class<?> target =
                XposedHelpers.findClassIfExists(
                        className,
                        classLoader
                );

        if (target == null) {
            XposedBridge.log(
                    TAG
                            + "legacy gate class miss "
                            + className
            );
            return;
        }

        Method method;

        try {
            method =
                    target.getDeclaredMethod(
                            "g",
                            int.class,
                            String.class
                    );
            method.setAccessible(
                    true
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    TAG
                            + "legacy gate signature miss "
                            + className
                            + ".g(int,String)"
            );
            return;
        }

        String hookId =
                "legacy_gate:"
                        + className;

        if (!installedIds.add(
                hookId
        )) {
            return;
        }

        try {
            XposedBridge.hookMethod(
                    method,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(
                                MethodHookParam param
                        ) {
                            Object result =
                                    param.getResult();

                            if (result == null) {
                                return;
                            }

                            if (setIntField(
                                    result,
                                    "a",
                                    1
                            )) {
                                logHit(
                                        hookId
                                                + ":NO_SPLASH"
                                );
                            } else {
                                XposedBridge.log(
                                        TAG
                                                + "legacy gate returned "
                                                + result.getClass()
                                                .getName()
                                                + " but field a was not writable"
                                );
                            }
                        }
                    }
            );

            XposedBridge.log(
                    TAG
                            + "HOOKED "
                            + className
                            + ".g(int,String)"
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    TAG
                            + "legacy gate hook failed "
                            + className
                            + " / "
                            + throwable
            );
        }
    }

    private static boolean setIntField(
            Object target,
            String fieldName,
            int value
    ) {
        for (Class<?> current =
                target.getClass();
                current != null
                        && current != Object.class;
                current =
                        current.getSuperclass()) {
            try {
                Field field =
                        current.getDeclaredField(
                                fieldName
                        );

                if (field.getType()
                        != int.class
                        && field.getType()
                        != Integer.class) {
                    return false;
                }

                field.setAccessible(
                        true
                );

                if (field.getType()
                        == int.class) {
                    field.setInt(
                            target,
                            value
                    );
                } else {
                    field.set(
                            target,
                            Integer.valueOf(
                                    value
                            )
                    );
                }

                return true;
            } catch (NoSuchFieldException ignored) {
            } catch (Throwable throwable) {
                XposedBridge.log(
                        TAG
                                + "set field failed "
                                + fieldName
                                + " / "
                                + throwable
                );
                return false;
            }
        }

        return false;
    }

    private static void hookObserveAll(
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
                            + "observe class miss "
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

                String hookId =
                        "observe:"
                                + id
                                + ":"
                                + current.getName()
                                + "#"
                                + methodName
                                + "/"
                                + method.getParameterTypes()
                                .length;

                if (!installedIds.add(
                        hookId
                )) {
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
                                    logHit(
                                            id
                                    );
                                }
                            }
                    );
                    count++;
                } catch (Throwable throwable) {
                    XposedBridge.log(
                            TAG
                                    + "observe hook failed "
                                    + hookId
                                    + " / "
                                    + throwable
                    );
                }
            }
        }

        XposedBridge.log(
                TAG
                        + "OBSERVE "
                        + id
                        + " hooks="
                        + count
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
                                                        param
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
        textAnchors.put(
                view,
                text
        );

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

        String direct =
                textFor(
                        view
                );

        if (direct != null) {
            String text =
                    normalize(
                            direct
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
                installListAdapterHook(
                        parentView
                );

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
            XC_MethodHook.MethodHookParam param
    ) {
        if (param == null
                || param.args == null) {
            return null;
        }

        String methodName =
                param.method == null
                        ? ""
                        : param.method.getName();

        if ("setText".equals(
                methodName
        )) {
            for (Object arg : param.args) {
                if (arg instanceof String) {
                    String value =
                            ((String) arg)
                                    .trim();

                    if (!value.isEmpty()) {
                        return value;
                    }
                }
            }
        }

        if ("setAttribute".equals(
                methodName
        )
                && param.args.length >= 2
                && param.args[0]
                instanceof String) {
            String attribute =
                    ((String) param.args[0])
                            .trim()
                            .toLowerCase();

            if (("text".equals(attribute)
                    || "value".equals(attribute)
                    || "content".equals(attribute))
                    && param.args[1]
                    instanceof String) {
                return ((String) param.args[1])
                        .trim();
            }
        }

        for (Object arg : param.args) {
            if (!(arg instanceof String)) {
                continue;
            }

            String value =
                    ((String) arg)
                            .trim();

            if ("探索本地".equals(
                    normalize(
                            value
                    )
            )
                    || value.contains(
                    "扫街榜"
            )
                    || value.contains(
                    "订周末"
            )) {
                return value;
            }
        }

        return null;
    }

    private static void installListAdapterHook(
            View list
    ) {
        if (list == null) {
            return;
        }

        try {
            Method getAdapter =
                    findNoArgMethod(
                            list.getClass(),
                            "getAdapter"
                    );

            if (getAdapter == null) {
                return;
            }

            getAdapter.setAccessible(
                    true
            );

            Object adapter =
                    getAdapter.invoke(
                            list
                    );

            if (adapter == null) {
                return;
            }

            Class<?> adapterClass =
                    adapter.getClass();

            String id =
                    "ajx_bind:"
                            + adapterClass.getName();

            if (!installedIds.add(id)) {
                return;
            }

            Method bind =
                    findBindMethod(
                            adapterClass
                    );

            if (bind == null) {
                XposedBridge.log(
                        TAG
                                + "BIND MISS "
                                + adapterClass.getName()
                );
                return;
            }

            bind.setAccessible(
                    true
            );

            XposedBridge.hookMethod(
                    bind,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(
                                MethodHookParam param
                        ) {
                            View item =
                                    itemViewOf(
                                            param.args != null
                                                    && param.args.length > 0
                                                    ? param.args[0]
                                                    : null
                                    );

                            if (item == null) {
                                return;
                            }

                            processBoundItem(
                                    item
                            );
                        }
                    }
            );

            XposedBridge.log(
                    TAG
                            + "BIND HOOKED "
                            + adapterClass.getName()
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    TAG
                            + "BIND hook failed: "
                            + throwable
            );
        }
    }

    private static Method findNoArgMethod(
            Class<?> type,
            String name
    ) {
        for (Class<?> current = type;
                current != null
                        && current != Object.class;
                current =
                        current.getSuperclass()) {
            try {
                return current.getDeclaredMethod(
                        name
                );
            } catch (NoSuchMethodException ignored) {
            }
        }

        return null;
    }

    private static Method findBindMethod(
            Class<?> type
    ) {
        for (Class<?> current = type;
                current != null
                        && current != Object.class;
                current =
                        current.getSuperclass()) {
            for (Method method
                    : current.getDeclaredMethods()) {
                if ("onBindViewHolder".equals(
                        method.getName()
                )
                        && method.getParameterTypes()
                        .length == 2
                        && !Modifier.isAbstract(
                        method.getModifiers()
                )) {
                    return method;
                }
            }
        }

        return null;
    }

    private static View itemViewOf(
            Object holder
    ) {
        if (holder == null) {
            return null;
        }

        if (holder instanceof View) {
            return (View) holder;
        }

        for (Class<?> current =
                holder.getClass();
                current != null
                        && current != Object.class;
                current =
                        current.getSuperclass()) {
            try {
                Field field =
                        current.getDeclaredField(
                                "itemView"
                        );

                field.setAccessible(
                        true
                );

                Object value =
                        field.get(
                                holder
                        );

                if (value instanceof View) {
                    return (View) value;
                }
            } catch (NoSuchFieldException ignored) {
            } catch (Throwable throwable) {
                return null;
            }
        }

        return null;
    }

    private static void processBoundItem(
            View item
    ) {
        if (item == null) {
            return;
        }

        if (exploreLocalEnabled
                && subtreeContainsText(
                item,
                "探索本地",
                0
        )) {
            if (collapseSafely(
                    item,
                    "bind:explore_local",
                    0.62f
            )) {
                logHit(
                        "bind:explore_local"
                );
            }
            return;
        }

        if (floatBadgesEnabled) {
            View badge =
                    findAnchoredView(
                            item,
                            FLOAT_TEXTS,
                            0
                    );

            if (badge != null
                    && hideOperationalHost(
                    badge,
                    "bind:badge"
            )) {
                logHit(
                        "bind:badge"
                );
            }
        }
    }

    private static boolean subtreeContainsText(
            View view,
            String expected,
            int depth
    ) {
        if (view == null
                || depth > 12) {
            return false;
        }

        String text =
                textFor(
                        view
                );

        if (expected.equals(
                normalize(
                        text
                )
        )) {
            return true;
        }

        if (view instanceof ViewGroup) {
            ViewGroup group =
                    (ViewGroup) view;

            for (int i = 0;
                    i < group.getChildCount();
                    i++) {
                if (subtreeContainsText(
                        group.getChildAt(i),
                        expected,
                        depth + 1
                )) {
                    return true;
                }
            }
        }

        return false;
    }

    private static View findAnchoredView(
            View view,
            String[] tokens,
            int depth
    ) {
        if (view == null
                || depth > 12) {
            return null;
        }

        String text =
                textFor(
                        view
                );

        if (text != null) {
            for (String token : tokens) {
                if (text.contains(
                        token
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
                        findAnchoredView(
                                group.getChildAt(i),
                                tokens,
                                depth + 1
                        );

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private static String textFor(
            View view
    ) {
        String anchored =
                textAnchors.get(
                        view
                );

        if (anchored != null) {
            return anchored;
        }

        if (view instanceof TextView) {
            CharSequence value =
                    ((TextView) view)
                            .getText();

            if (value != null) {
                return value.toString();
            }
        }

        CharSequence description =
                view.getContentDescription();

        return description == null
                ? null
                : description.toString();
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
