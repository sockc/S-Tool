package com.sockc.unicomhook;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.TextView;

import com.sockc.unicomhook.compat.XC_MethodHook;
import com.sockc.unicomhook.compat.XposedBridge;
import com.sockc.unicomhook.compat.XposedHelpers;

public class TaobaoHook implements HookModule {

    private static final String TAG = "Sockc_Taobao: ";
    private static final String TARGET_PACKAGE =
            "com.taobao.taobao";

    @Override
    public void handleLoadPackage(
            LoadPackageParam lpparam
    ) {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)
                || !TARGET_PACKAGE.equals(
                lpparam.processName
        )) {
            return;
        }

        HookConfig config = HookConfig.load();

        boolean coldSplash =
                config.isEnabled(
                        "taobao",
                        "taobao.cold_splash"
                );
        boolean warmSplash =
                config.isEnabled(
                        "taobao",
                        "taobao.warm_splash"
                );

        XposedBridge.log(
                TAG
                        + "子功能: cold="
                        + coldSplash
                        + ", warm="
                        + warmSplash
        );

        if (coldSplash) {
            hookColdSplash();
        }

        if (warmSplash) {
            hookWarmSplash();
        }
    }

    private void hookColdSplash() {
        try {
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

                            String className =
                                    activity
                                            .getClass()
                                            .getName()
                                            .toLowerCase();

                            if (!className.contains(
                                    "welcome"
                            )
                                    && !className.contains(
                                    "bootimage"
                            )) {
                                return;
                            }

                            XposedBridge.log(
                                    TAG
                                            + "检测到淘宝冷启动页: "
                                            + className
                            );

                            activity
                                    .getWindow()
                                    .getDecorView()
                                    .postDelayed(
                                            () -> {
                                                try {
                                                    scanAndClickSkip(
                                                            activity
                                                                    .getWindow()
                                                                    .getDecorView()
                                                    );
                                                } catch (
                                                        Throwable throwable
                                                ) {
                                                    XposedBridge.log(
                                                            TAG
                                                                    + "冷启动扫描异常: "
                                                                    + throwable
                                                    );
                                                }
                                            },
                                            800L
                                    );
                        }
                    }
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    TAG
                            + "冷启动 Hook 失败: "
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

                            Window window =
                                    activity.getWindow();

                            if (window == null) {
                                return;
                            }

                            final View decorView =
                                    window.getDecorView();

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
                                            try {
                                                if (scanAndClickSkip(
                                                        decorView
                                                )) {
                                                    XposedBridge.log(
                                                            TAG
                                                                    + "已处理淘宝热启动广告"
                                                    );

                                                    removeListener(
                                                            decorView,
                                                            holder[0]
                                                    );
                                                }
                                            } catch (
                                                    Throwable throwable
                                            ) {
                                                XposedBridge.log(
                                                        TAG
                                                                + "热启动扫描异常: "
                                                                + throwable
                                                );
                                            }
                                        }
                                    };

                            decorView
                                    .getViewTreeObserver()
                                    .addOnGlobalLayoutListener(
                                            holder[0]
                                    );

                            decorView.postDelayed(
                                    () ->
                                            removeListener(
                                                    decorView,
                                                    holder[0]
                                            ),
                                    5000L
                            );
                        }
                    }
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    TAG
                            + "热启动 Hook 失败: "
                            + throwable
            );
        }
    }

    private void removeListener(
            View root,
            ViewTreeObserver.OnGlobalLayoutListener listener
    ) {
        try {
            ViewTreeObserver observer =
                    root.getViewTreeObserver();

            if (observer.isAlive()
                    && listener != null) {
                observer.removeOnGlobalLayoutListener(
                        listener
                );
            }
        } catch (Throwable ignored) {
        }
    }

    private boolean scanAndClickSkip(
            View view
    ) {
        if (view == null) {
            return false;
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
                                "跳转"
                        )) {
                            child.performClick();

                            if (child.getParent()
                                    instanceof View) {
                                ((View) child.getParent())
                                        .performClick();
                            }

                            return true;
                        }
                    }
                }

                if (scanAndClickSkip(child)) {
                    return true;
                }
            }
        }

        return false;
    }
}
