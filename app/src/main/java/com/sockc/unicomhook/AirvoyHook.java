package com.sockc.unicomhook;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class AirvoyHook {

    private static final String TARGET_PACKAGE = "com.airvoy.airvoy";
    private static final String TAG = "Sockc AirvoyAutoClose: ";

    private static final Handler MAIN_HANDLER =
            new Handler(Looper.getMainLooper());

    private static volatile WeakReference<Activity> currentActivity =
            new WeakReference<>(null);

    private static final AtomicLong LAST_REWARD_TIME =
            new AtomicLong(0L);

    private static boolean lifecycleRegistered = false;

    private AirvoyHook() {
    }

    public static void handleLoadPackage(
            XC_LoadPackage.LoadPackageParam lpparam
    ) {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        log("开始加载，process=" + lpparam.processName);

        hookApplicationLifecycle();
        hookRewardCallbacks(lpparam.classLoader);
    }

    /**
     * 保存当前前台 Activity。
     */
    private static void hookApplicationLifecycle() {
        XposedHelpers.findAndHookMethod(
                Application.class,
                "attach",
                Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Application application =
                                (Application) param.thisObject;

                        synchronized (AirvoyHook.class) {
                            if (lifecycleRegistered) {
                                return;
                            }

                            lifecycleRegistered = true;
                        }

                        application.registerActivityLifecycleCallbacks(
                                new Application.ActivityLifecycleCallbacks() {
                                    @Override
                                    public void onActivityCreated(
                                            Activity activity,
                                            Bundle savedInstanceState
                                    ) {
                                        log("Activity created: "
                                                + activity.getClass().getName());
                                    }

                                    @Override
                                    public void onActivityStarted(
                                            Activity activity
                                    ) {
                                    }

                                    @Override
                                    public void onActivityResumed(
                                            Activity activity
                                    ) {
                                        currentActivity =
                                                new WeakReference<>(activity);

                                        log("Activity resumed: "
                                                + activity.getClass().getName());
                                    }

                                    @Override
                                    public void onActivityPaused(
                                            Activity activity
                                    ) {
                                    }

                                    @Override
                                    public void onActivityStopped(
                                            Activity activity
                                    ) {
                                    }

                                    @Override
                                    public void onActivitySaveInstanceState(
                                            Activity activity,
                                            Bundle outState
                                    ) {
                                    }

                                    @Override
                                    public void onActivityDestroyed(
                                            Activity activity
                                    ) {
                                        Activity current =
                                                currentActivity.get();

                                        if (current == activity) {
                                            currentActivity =
                                                    new WeakReference<>(null);
                                        }
                                    }
                                }
                        );

                        log("Activity 生命周期监听已注册");
                    }
                }
        );
    }

    /**
     * Hook Flutter Google Mobile Ads 奖励回调。
     */
    private static void hookRewardCallbacks(ClassLoader classLoader) {
        hookRewardClass(
                classLoader,
                "io.flutter.plugins.googlemobileads.FlutterRewardedAd",
                "onUserEarnedReward"
        );

        hookRewardClass(
                classLoader,
                "io.flutter.plugins.googlemobileads.FlutterRewardedInterstitialAd",
                "onUserEarnedReward"
        );

        hookRewardClass(
                classLoader,
                "io.flutter.plugins.googlemobileads.AdInstanceManager",
                "onRewardedAdUserEarnedReward"
        );
    }

    private static void hookRewardClass(
            ClassLoader classLoader,
            String className,
            String methodName
    ) {
        try {
            Class<?> targetClass =
                    XposedHelpers.findClassIfExists(
                            className,
                            classLoader
                    );

            if (targetClass == null) {
                log("未找到类: " + className);
                return;
            }

            XposedBridge.hookAllMethods(
                    targetClass,
                    methodName,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(
                                MethodHookParam param
                        ) {
                            onRewardEarned(
                                    className + "." + methodName
                            );
                        }
                    }
            );

            log("Hook 成功: "
                    + className
                    + "."
                    + methodName);

        } catch (Throwable throwable) {
            log("Hook 失败: "
                    + className
                    + "."
                    + methodName
                    + " / "
                    + throwable);
        }
    }

    /**
     * 一个广告可能连续触发 FlutterRewardedAd 和
     * AdInstanceManager 两层回调，因此做防重复处理。
     */
    private static void onRewardEarned(String source) {
        long now = System.currentTimeMillis();
        long previous = LAST_REWARD_TIME.get();

        if (now - previous < 1500L) {
            return;
        }

        LAST_REWARD_TIME.set(now);

        Activity activity = currentActivity.get();

        log("检测到真实奖励回调: " + source);

        if (activity == null) {
            log("没有获取到当前 Activity");
            return;
        }

        log("奖励时 Activity: "
                + activity.getClass().getName());

        scheduleCloseAttempts(activity);
    }

    /**
     * 奖励回调出现时，关闭按钮可能刚刚生成。
     * 分几个时间点重复检测。
     */
    private static void scheduleCloseAttempts(Activity activity) {
        long[] delays = {
                200L,
                700L,
                1300L,
                2200L,
                3500L,
                5000L
        };

        for (int i = 0; i < delays.length; i++) {
            final int attempt = i;

            MAIN_HANDLER.postDelayed(
                    () -> attemptClose(activity, attempt),
                    delays[i]
            );
        }
    }

    private static void attemptClose(
            Activity rewardedActivity,
            int attempt
    ) {
        try {
            Activity current = currentActivity.get();

            if (current == null) {
                log("第 " + attempt + " 次：当前 Activity 为空");
                return;
            }

            if (current != rewardedActivity) {
                log("广告 Activity 已切换，停止点击");
                return;
            }

            if (current.isFinishing()
                    || current.isDestroyed()) {
                return;
            }

            View decorView =
                    current.getWindow().getDecorView();

            View candidate =
                    findBestCornerCloseView(decorView);

            if (candidate != null) {
                log("找到角落关闭控件: "
                        + candidate.getClass().getName()
                        + ", attempt="
                        + attempt);

                if (candidate.performClick()) {
                    log("performClick 成功");
                    return;
                }

                tapViewCenter(decorView, candidate);
                return;
            }

            String activityName =
                    current.getClass()
                            .getName()
                            .toLowerCase(Locale.ROOT);

            boolean adActivity =
                    activityName.contains("adactivity")
                            || activityName.contains("admob")
                            || containsWebView(decorView);

            if (!adActivity) {
                log("当前界面不像广告界面，不执行坐标点击: "
                        + current.getClass().getName());
                return;
            }

            /*
             * 第一次只扫描，不盲点。
             * 从第二次开始点右上角。
             * 最后一次补点左上角，兼容少量左侧关闭按钮广告。
             */
            if (attempt >= 1 && attempt <= 4) {
                tapTopRight(decorView);
                log("已尝试点击广告右上角，attempt=" + attempt);
            } else if (attempt == 5) {
                tapTopLeft(decorView);
                log("已尝试点击广告左上角");
            }

        } catch (Throwable throwable) {
            log("自动关闭异常: " + throwable);
        }
    }

    /**
     * 查找位于屏幕左上角或右上角的小型可点击控件。
     */
    private static View findBestCornerCloseView(View root) {
        if (root == null
                || root.getWidth() <= 0
                || root.getHeight() <= 0) {
            return null;
        }

        View[] best = {null};
        long[] bestArea = {Long.MAX_VALUE};

        scanViews(
                root,
                root.getWidth(),
                root.getHeight(),
                best,
                bestArea
        );

        return best[0];
    }

    private static void scanViews(
            View view,
            int screenWidth,
            int screenHeight,
            View[] best,
            long[] bestArea
    ) {
        if (view == null
                || view.getVisibility() != View.VISIBLE
                || !view.isShown()
                || view.getAlpha() <= 0.05f) {
            return;
        }

        if (!(view instanceof ViewGroup)
                || view.getWidth() < screenWidth * 0.9f
                || view.getHeight() < screenHeight * 0.9f) {

            if (isCornerCloseCandidate(
                    view,
                    screenWidth,
                    screenHeight
            )) {
                long area =
                        (long) view.getWidth()
                                * (long) view.getHeight();

                if (area > 0 && area < bestArea[0]) {
                    best[0] = view;
                    bestArea[0] = area;
                }
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;

            for (int i = 0; i < group.getChildCount(); i++) {
                scanViews(
                        group.getChildAt(i),
                        screenWidth,
                        screenHeight,
                        best,
                        bestArea
                );
            }
        }
    }

    private static boolean isCornerCloseCandidate(
            View view,
            int screenWidth,
            int screenHeight
    ) {
        if (!view.isEnabled()) {
            return false;
        }

        if (!(view.isClickable()
                || view.hasOnClickListeners())) {
            return false;
        }

        if (view instanceof WebView) {
            return false;
        }

        int width = view.getWidth();
        int height = view.getHeight();

        int minSize = dp(view, 10);
        int maxSize = dp(view, 120);

        if (width < minSize
                || height < minSize
                || width > maxSize
                || height > maxSize) {
            return false;
        }

        int[] location = new int[2];
        view.getLocationOnScreen(location);

        int left = location[0];
        int top = location[1];
        int right = left + width;

        boolean nearTop =
                top < screenHeight * 0.27f;

        boolean nearLeft =
                left < screenWidth * 0.22f;

        boolean nearRight =
                right > screenWidth * 0.78f;

        if (!nearTop || (!nearLeft && !nearRight)) {
            return false;
        }

        String description =
                getViewDescription(view);

        if (containsCloseKeyword(description)) {
            return true;
        }

        /*
         * 有些广告关闭按钮完全没有文字和描述，
         * 只要它足够小、可点击且位于顶部角落，也作为候选。
         */
        int geometricMax = dp(view, 80);

        return width <= geometricMax
                && height <= geometricMax;
    }

    private static String getViewDescription(View view) {
        StringBuilder builder = new StringBuilder();

        CharSequence contentDescription =
                view.getContentDescription();

        if (contentDescription != null) {
            builder.append(contentDescription)
                    .append(' ');
        }

        if (view instanceof TextView) {
            CharSequence text =
                    ((TextView) view).getText();

            if (text != null) {
                builder.append(text)
                        .append(' ');
            }
        }

        try {
            int id = view.getId();

            if (id != View.NO_ID) {
                builder.append(
                        view.getResources()
                                .getResourceEntryName(id)
                );
            }
        } catch (Throwable ignored) {
        }

        return builder.toString()
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private static boolean containsCloseKeyword(
            String value
    ) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        String normalized =
                value.trim()
                        .toLowerCase(Locale.ROOT);

        return normalized.equals("x")
                || normalized.equals("×")
                || normalized.contains("close")
                || normalized.contains("dismiss")
                || normalized.contains("skip")
                || normalized.contains("done")
                || normalized.contains("关闭")
                || normalized.contains("跳过")
                || normalized.contains("完成");
    }

    private static boolean containsWebView(View view) {
        if (view == null) {
            return false;
        }

        if (view instanceof WebView) {
            return true;
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;

            for (int i = 0; i < group.getChildCount(); i++) {
                if (containsWebView(group.getChildAt(i))) {
                    return true;
                }
            }
        }

        return false;
    }

    private static void tapTopRight(View decorView) {
        int width = decorView.getWidth();

        Rect visibleFrame = new Rect();
        decorView.getWindowVisibleDisplayFrame(visibleFrame);

        float x = width - dp(decorView, 28);
        float y = Math.max(
                visibleFrame.top + dp(decorView, 26),
                dp(decorView, 34)
        );

        dispatchTap(decorView, x, y);
    }

    private static void tapTopLeft(View decorView) {
        Rect visibleFrame = new Rect();
        decorView.getWindowVisibleDisplayFrame(visibleFrame);

        float x = dp(decorView, 28);
        float y = Math.max(
                visibleFrame.top + dp(decorView, 26),
                dp(decorView, 34)
        );

        dispatchTap(decorView, x, y);
    }

    private static void tapViewCenter(
            View decorView,
            View target
    ) {
        int[] decorLocation = new int[2];
        int[] targetLocation = new int[2];

        decorView.getLocationOnScreen(decorLocation);
        target.getLocationOnScreen(targetLocation);

        float x =
                targetLocation[0]
                        - decorLocation[0]
                        + target.getWidth() / 2f;

        float y =
                targetLocation[1]
                        - decorLocation[1]
                        + target.getHeight() / 2f;

        dispatchTap(decorView, x, y);
    }

    private static void dispatchTap(
            View decorView,
            float x,
            float y
    ) {
        long time = android.os.SystemClock.uptimeMillis();

        MotionEvent down = MotionEvent.obtain(
                time,
                time,
                MotionEvent.ACTION_DOWN,
                x,
                y,
                0
        );

        MotionEvent up = MotionEvent.obtain(
                time,
                time + 60L,
                MotionEvent.ACTION_UP,
                x,
                y,
                0
        );

        try {
            decorView.dispatchTouchEvent(down);
            decorView.dispatchTouchEvent(up);
        } finally {
            down.recycle();
            up.recycle();
        }
    }

    private static int dp(View view, int value) {
        float density =
                view.getResources()
                        .getDisplayMetrics()
                        .density;

        return Math.round(value * density);
    }

    private static void log(String message) {
        XposedBridge.log(TAG + message);
    }
}
