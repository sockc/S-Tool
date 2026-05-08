package com.sockc.unicomhook;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class GaodeHook implements IXposedHookLoadPackage {

    private static final String TAG = "Sockc_Gaode: ";
    private static final String TARGET_PACKAGE = "com.autonavi.minimap";

    // 防止同一个 Activity 被重复挂监听
    private static final WeakHashMap<Activity, Boolean> hookedActivities = new WeakHashMap<>();

    // 必须保留的核心功能关键词，命中这些就不隐藏
    private static final Set<String> KEEP_KEYWORDS = new HashSet<>(Arrays.asList(
            "搜索", "导航", "路线", "路线规划", "公交", "步行", "骑行",
            "收藏", "家", "公司", "回家", "去公司", "定位",
            "地图", "我的位置", "附近", "停车场"
    ));

    // 要精简隐藏的外围功能关键词
    private static final Set<String> HIDE_KEYWORDS = new HashSet<>(Arrays.asList(
            "酒店", "门票", "景点", "旅游", "火车票", "机票",
            "打车", "顺风车", "代驾", "租车",
            "加油", "充电", "洗车", "车主服务",
            "商城", "会员", "积分", "签到", "福利", "红包", "优惠券",
            "发现", "精选", "推荐", "活动", "任务", "直播",
            "消息", "通知", "高德指南", "周边游",
            "美食", "电影", "外卖", "团购"
    ));

    // 开屏/弹窗按钮关键词
    private static final Set<String> SKIP_KEYWORDS = new HashSet<>(Arrays.asList(
            "跳过", "关闭广告", "关闭", "以后再说", "暂不开启"
    ));

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        // 只注入高德主进程，避开 locationservice / push / webview 等子进程
        if (!TARGET_PACKAGE.equals(lpparam.packageName) || !TARGET_PACKAGE.equals(lpparam.processName)) {
            return;
        }

        XposedBridge.log(TAG + "已注入高德主进程，启用精简模式");

        try {
            XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = (Activity) param.thisObject;
                    attachCleaner(activity);
                }
            });
        } catch (Throwable e) {
            XposedBridge.log(TAG + "Hook 注入失败: " + e.getMessage());
        }
    }

    private void attachCleaner(final Activity activity) {
        if (activity == null) return;

        synchronized (hookedActivities) {
            if (hookedActivities.containsKey(activity)) return;
            hookedActivities.put(activity, true);
        }

        final View root = activity.getWindow() == null ? null : activity.getWindow().getDecorView();
        if (root == null) return;

        ViewTreeObserver.OnGlobalLayoutListener listener = new ViewTreeObserver.OnGlobalLayoutListener() {
            private long lastRun = 0L;

            @Override
            public void onGlobalLayout() {
                long now = System.currentTimeMillis();

                // 限流，避免每帧都递归扫描导致卡顿
                if (now - lastRun < 350) return;
                lastRun = now;

                try {
                    scanAndClean(root);
                } catch (Throwable ignored) {
                    // 不让高德因为模块闪退
                }
            }
        };

        root.getViewTreeObserver().addOnGlobalLayoutListener(listener);

        // 启动后延迟扫几次，覆盖动态加载的首页卡片和弹窗
        root.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    scanAndClean(root);
                } catch (Throwable ignored) {
                }
            }
        }, 800);

        root.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    scanAndClean(root);
                } catch (Throwable ignored) {
                }
            }
        }, 1800);
    }

    private void scanAndClean(View view) {
        if (view == null) return;

        if (view instanceof TextView) {
            handleTextView((TextView) view);
            return;
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            int count = group.getChildCount();

            for (int i = 0; i < count; i++) {
                View child = group.getChildAt(i);
                scanAndClean(child);
            }
        }
    }

    private void handleTextView(TextView tv) {
        CharSequence cs = tv.getText();
        if (cs == null) return;

        String text = cs.toString().trim();
        if (text.length() == 0) return;

        // 1. 开屏/弹窗跳过
        if (containsAny(text, SKIP_KEYWORDS)) {
            XposedBridge.log(TAG + "发现弹窗/广告按钮: " + text + "，尝试点击并隐藏");
            safeClick(tv);
            hideSafely(tv);
            return;
        }

        // 2. 核心功能不碰
        if (containsAny(text, KEEP_KEYWORDS)) {
            return;
        }

        // 3. 外围入口隐藏
        if (containsAny(text, HIDE_KEYWORDS)) {
            XposedBridge.log(TAG + "隐藏外围入口: " + text);
            hideSmallContainer(tv);
        }
    }

    private boolean containsAny(String text, Set<String> keywords) {
        if (text == null) return false;

        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private void safeClick(View view) {
        try {
            view.performClick();

            if (view.getParent() instanceof View) {
                ((View) view.getParent()).performClick();
            }
        } catch (Throwable ignored) {
        }
    }

    private void hideSafely(View view) {
        try {
            view.setVisibility(View.GONE);
        } catch (Throwable ignored) {
        }
    }

    /**
     * 尽量只隐藏小入口，不隐藏整页。
     * 规则：
     * - 先隐藏 TextView 本身
     * - 如果父容器子项很少，说明大概率是一个按钮/卡片入口，也一起隐藏
     * - 最多向上处理 2 层，避免误隐藏整个首页
     */
    private void hideSmallContainer(TextView tv) {
        hideSafely(tv);

        try {
            View current = tv;
            for (int depth = 0; depth < 2; depth++) {
                if (!(current.getParent() instanceof ViewGroup)) return;

                ViewGroup parent = (ViewGroup) current.getParent();

                // 子项太多通常是大容器，不碰
                if (parent.getChildCount() > 5) return;

                // 避免隐藏全屏级容器
                if (parent.getWidth() > 0 && parent.getHeight() > 0) {
                    int area = parent.getWidth() * parent.getHeight();

                    View root = tv.getRootView();
                    int rootArea = root.getWidth() * root.getHeight();

                    if (rootArea > 0 && area > rootArea / 3) {
                        return;
                    }
                }

                parent.setVisibility(View.GONE);
                current = parent;
            }
        } catch (Throwable ignored) {
        }
    }
}
