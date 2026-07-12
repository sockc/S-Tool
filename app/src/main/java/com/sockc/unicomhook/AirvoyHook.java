package com.sockc.unicomhook;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class AirvoyHook {

    private static final String TARGET_PACKAGE = "com.airvoy.airvoy";
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    private static final String[] CLOSE_KEYWORDS = {
            "close",
            "dismiss",
            "skip",
            "done",
            "关闭",
            "跳过",
            "完成"
    };

    private AirvoyHook() {
    }

    public static void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        XposedHelpers.findAndHookMethod(
                Activity.class,
                "onResume",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Activity activity = (Activity) param.thisObject;
                        startCloseButtonScanner(activity);
                    }
                }
        );
    }

    private static void startCloseButtonScanner(Activity activity) {
        final int[] scanCount = {0};

        Runnable scanner = new Runnable() {
            @Override
            public void run() {
                if (activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }

                View decorView = activity.getWindow().getDecorView();
                View closeButton = findCloseButton(decorView);

                if (closeButton != null) {
                    closeButton.performClick();
                    return;
                }

                scanCount[0]++;

                // 最多扫描约 90 秒
                if (scanCount[0] < 150) {
                    HANDLER.postDelayed(this, 600);
                }
            }
        };

        HANDLER.postDelayed(scanner, 800);
    }

    private static View findCloseButton(View view) {
        if (view == null || !view.isShown()) {
            return null;
        }

        if (isSafeCloseButton(view)) {
            return view;
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;

            for (int i = 0; i < group.getChildCount(); i++) {
                View result = findCloseButton(group.getChildAt(i));

                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    private static boolean isSafeCloseButton(View view) {
        if (!view.isClickable() || !view.isEnabled()) {
            return false;
        }

        StringBuilder description = new StringBuilder();

        CharSequence contentDescription = view.getContentDescription();
        if (contentDescription != null) {
            description.append(contentDescription).append(' ');
        }

        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            if (text != null) {
                description.append(text).append(' ');
            }
        }

        try {
            int id = view.getId();

            if (id != View.NO_ID) {
                description.append(
                        view.getResources().getResourceEntryName(id)
                );
            }
        } catch (Throwable ignored) {
        }

        String value = description
                .toString()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (value.isEmpty()) {
            return false;
        }

        boolean keywordMatched = false;

        for (String keyword : CLOSE_KEYWORDS) {
            if (value.contains(keyword)) {
                keywordMatched = true;
                break;
            }
        }

        if (!keywordMatched) {
            return false;
        }

        // 防止误点页面中间的广告内容，只允许屏幕上方控件
        int[] location = new int[2];
        view.getLocationOnScreen(location);

        int screenHeight = view.getResources()
                .getDisplayMetrics()
                .heightPixels;

        return location[1] < screenHeight * 0.35f;
    }
}
