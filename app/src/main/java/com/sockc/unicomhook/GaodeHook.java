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

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class GaodeHook implements IXposedHookLoadPackage {

    private static final String TAG = "Sockc_Gaode: ";
    private static final String TARGET_PACKAGE = "com.autonavi.minimap";

    private static final int TAG_LISTENER_INSTALLED = 0x7F0B7001;

    // 首页“探索本地”卡片区里你要去掉的项
    private static final Set<String> HOME_CARD_TEXTS = new HashSet<>(Arrays.asList(
            "探索本地",
            "美食",
            "酒店",
            "景点",
            "加油站",
            "休闲娱乐",
            "超市"
    ));

    // 底部想去掉的 tab
    private static final Set<String> BOTTOM_TAB_TEXTS = new HashSet<>(Arrays.asList(
            "探索",
            "AI对话",
            "路线"
    ));

    // 右上角运营挂件
    private static final String[] FLOAT_BADGE_KEYWORDS = new String[]{
            "扫街榜",
            "订周末"
    };

    // 用来判断当前是不是首页
    private static final String[] HOME_MARKERS = new String[]{
            "设置家",
            "设置单位",
            "常去地点",
            "探索本地"
    };

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!TARGET_PACKAGE.equals(lpparam.packageName) || !TARGET_PACKAGE.equals(lpparam.processName)) {
            return;
        }

        XposedBridge.log(TAG + "已注入高德主进程");

        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Activity activity = (Activity) param.thisObject;
                if (!TARGET_PACKAGE.equals(activity.getPackageName())) return;
                installCleaner(activity);
            }
        });

        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Activity activity = (Activity) param.thisObject;
                if (!TARGET_PACKAGE.equals(activity.getPackageName())) return;
                installCleaner(activity);
                scheduleRepeatedClean(activity);
            }
        });
    }

    private void installCleaner(final Activity activity) {
        final View decorView = activity.getWindow().getDecorView();
        if (decorView == null) return;

        Object installed = decorView.getTag(TAG_LISTENER_INSTALLED);
        if (installed != null) return;

        decorView.setTag(TAG_LISTENER_INSTALLED, Boolean.TRUE);

        ViewTreeObserver.OnGlobalLayoutListener listener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                try {
                    scanAndClickSkip(decorView);
                    cleanHomePage(activity, decorView);
                } catch (Throwable t) {
                    XposedBridge.log(TAG + "onGlobalLayout clean error: " + t);
                }
            }
        };

        decorView.getViewTreeObserver().addOnGlobalLayoutListener(listener);
        scheduleRepeatedClean(activity);
    }

    private void scheduleRepeatedClean(final Activity activity) {
        final View decorView = activity.getWindow().getDecorView();
        if (decorView == null) return;

        Handler handler = new Handler(Looper.getMainLooper());
        long[] delays = new long[]{0, 250, 600, 1200, 2200, 4000};

        for (final long delay : delays) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        scanAndClickSkip(decorView);
                        cleanHomePage(activity, decorView);
                    } catch (Throwable t) {
                        XposedBridge.log(TAG + "delayed clean error(" + delay + "): " + t);
                    }
                }
            }, delay);
        }
    }

    private void cleanHomePage(Activity activity, View root) {
        if (!looksLikeHomePage(root)) return;

        // 1. 干掉“探索本地”整块
        hideExploreLocalSection(activity, root);

        // 2. 干掉底部 tab
        hideBottomTabs(activity, root);

        // 3. 干掉右上角扫街榜之类挂件
        hideFloatBadges(activity, root);
    }

    // 保留原来的跳广告逻辑
    private void scanAndClickSkip(View view) {
        if (view == null) return;

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);

                if (child instanceof TextView) {
                    CharSequence cs = ((TextView) child).getText();
                    if (cs != null) {
                        String text = cs.toString();
                        if (text.contains("跳过") || text.contains("关闭广告")) {
                            XposedBridge.log(TAG + "发现广告按钮: " + text);
                            child.performClick();
                            if (child.getParent() instanceof View) {
                                ((View) child.getParent()).performClick();
                            }
                            child.setVisibility(View.GONE);
                            return;
                        }
                    }
                }

                scanAndClickSkip(child);
            }
        }
    }

    private void hideExploreLocalSection(Activity activity, View root) {
        View titleView = findFirstViewByText(root, "探索本地");
        if (titleView == null) return;

        View target = findAncestorForSection(activity, titleView);
        if (target == null) return;

        if (target.getVisibility() != View.GONE) {
            XposedBridge.log(TAG + "隐藏整块: 探索本地");
            target.setVisibility(View.GONE);
            target.setEnabled(false);
            target.setClickable(false);
        }
    }

    private void hideBottomTabs(Activity activity, View root) {
        for (String text : BOTTOM_TAB_TEXTS) {
            View hit = findFirstViewByText(root, text);
            if (hit == null) continue;

            View target = findAncestorForBottomTab(activity, hit);
            if (target == null) continue;

            if (target.getVisibility() != View.GONE) {
                XposedBridge.log(TAG + "隐藏底部 Tab: " + text);
                target.setVisibility(View.GONE);
                target.setEnabled(false);
                target.setClickable(false);
            }
        }
    }

    private void hideFloatBadges(Activity activity, View root) {
        View hit = findFirstViewByKeywords(root, FLOAT_BADGE_KEYWORDS);
        if (hit == null) return;

        View target = findAncestorForFloatBadge(activity, hit);
        if (target == null) return;

        if (target.getVisibility() != View.GONE) {
            XposedBridge.log(TAG + "隐藏右侧挂件: " + viewText(hit));
            target.setVisibility(View.GONE);
            target.setEnabled(false);
            target.setClickable(false);
        }
    }

    private boolean looksLikeHomePage(View root) {
        for (String marker : HOME_MARKERS) {
            if (findFirstViewByText(root, marker) != null) {
                return true;
            }
        }
        return false;
    }

    private View findFirstViewByText(View view, String target) {
        if (view == null) return null;

        String current = viewText(view);
        if (target.equals(current)) return view;

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findFirstViewByText(group.getChildAt(i), target);
                if (found != null) return found;
            }
        }

        return null;
    }

    private View findFirstViewByKeywords(View view, String[] keywords) {
        if (view == null) return null;

        String current = viewText(view);
        if (current != null) {
            for (String keyword : keywords) {
                if (current.contains(keyword)) return view;
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findFirstViewByKeywords(group.getChildAt(i), keywords);
                if (found != null) return found;
            }
        }

        return null;
    }

    private String viewText(View view) {
        if (view == null) return null;

        if (view instanceof TextView) {
            CharSequence cs = ((TextView) view).getText();
            if (cs != null) {
                String t = cs.toString().trim();
                if (!t.isEmpty()) return t;
            }
        }

        CharSequence desc = view.getContentDescription();
        if (desc != null) {
            String t = desc.toString().trim();
            if (!t.isEmpty()) return t;
        }

        return null;
    }

    private View findAncestorForSection(Activity activity, View start) {
        View current = start;
        int minWidth = dp(activity, 220);
        int minHeight = dp(activity, 220);

        for (int i = 0; i < 8; i++) {
            ViewParent parent = current.getParent();
            if (!(parent instanceof View)) break;

            View pv = (View) parent;
            if (pv.getWidth() >= minWidth && pv.getHeight() >= minHeight) {
                return pv;
            }
            if (pv.getId() == android.R.id.content) break;
            current = pv;
        }

        return current;
    }

    private View findAncestorForBottomTab(Activity activity, View start) {
        View current = start;
        int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;

        for (int i = 0; i < 7; i++) {
            ViewParent parent = current.getParent();
            if (!(parent instanceof View)) break;

            View pv = (View) parent;
            int[] loc = new int[2];
            pv.getLocationOnScreen(loc);

            boolean nearBottom = loc[1] > (int) (screenHeight * 0.80f);
            boolean reasonableHeight = pv.getHeight() > dp(activity, 36) && pv.getHeight() < dp(activity, 140);

            if (nearBottom && reasonableHeight) {
                return pv;
            }

            if (pv.getId() == android.R.id.content) break;
            current = pv;
        }

        return current;
    }

    private View findAncestorForFloatBadge(Activity activity, View start) {
        View current = start;
        int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;

        for (int i = 0; i < 7; i++) {
            ViewParent parent = current.getParent();
            if (!(parent instanceof View)) break;

            View pv = (View) parent;
            int[] loc = new int[2];
            pv.getLocationOnScreen(loc);

            boolean atRightSide = loc[0] > (int) (screenWidth * 0.75f);
            if (atRightSide) {
                return pv;
            }

            if (pv.getId() == android.R.id.content) break;
            current = pv;
        }

        return current;
    }

    private int dp(Activity activity, int value) {
        float density = activity.getResources().getDisplayMetrics().density;
        return (int) (value * density + 0.5f);
    }
}
