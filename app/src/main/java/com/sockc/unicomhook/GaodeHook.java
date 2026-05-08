package com.sockc.unicomhook;

import android.app.Activity;
import android.os.Bundle;
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

    // 你圈出来想去掉的功能
    private static final Set<String> HIDE_EXACT = new HashSet<>(Arrays.asList(
            "探索",
            "AI对话",
            "路线",
            "美食",
            "酒店",
            "景点",
            "加油站",
            "休闲娱乐",
            "超市",
            "探索本地"
    ));

    // 运营挂件、活动角标这类用关键字更稳
    private static final String[] HIDE_KEYWORDS = new String[] {
            "扫街榜",
            "订周末"
    };

    // 用来判断是不是首页，避免别的页面误伤
    private static final String[] HOME_MARKERS = new String[] {
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

        XposedBridge.log(TAG + "已精准注入高德主进程...");

        try {
            XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    final Activity activity = (Activity) param.thisObject;
                    final View decorView = activity.getWindow().getDecorView();

                    ViewTreeObserver.OnGlobalLayoutListener listener = new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            try {
                                // 你原来的广告跳过
                                scanAndClickSkip(decorView);

                                // 只在首页净化，别的页面不碰
                                if (looksLikeHomePage(decorView)) {
                                    scanAndHideJunk(decorView);
                                }
                            } catch (Throwable ignored) {
                            }
                        }
                    };

                    decorView.getViewTreeObserver().addOnGlobalLayoutListener(listener);
                }
            });
        } catch (Throwable e) {
            XposedBridge.log(TAG + "Hook 注入失败: " + e);
        }
    }

    // 你原来的广告跳过逻辑，保留
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
                            XposedBridge.log(TAG + "锁定高德开屏目标！发现 [" + text + "]，执行无感秒点！");
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

    // 首页净化：递归扫描并隐藏你圈出来的区域
    private void scanAndHideJunk(View view) {
        if (view == null) return;

        // 先看 contentDescription，有些控件文字不一定走 TextView
        CharSequence desc = view.getContentDescription();
        if (desc != null) {
            String d = desc.toString().trim();
            if (shouldHideText(d)) {
                hideMatchedContainer(view, d);
                return;
            }
        }

        if (view instanceof TextView) {
            CharSequence cs = ((TextView) view).getText();
            if (cs != null) {
                String text = cs.toString().trim();
                if (shouldHideText(text)) {
                    hideMatchedContainer(view, text);
                    return;
                }
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                scanAndHideJunk(group.getChildAt(i));
            }
        }
    }

    private boolean shouldHideText(String text) {
        if (text == null || text.isEmpty()) return false;

        if (HIDE_EXACT.contains(text)) return true;

        for (String keyword : HIDE_KEYWORDS) {
            if (text.contains(keyword)) return true;
        }

        return false;
    }

    // 判断是不是首页，避免误伤其它业务页
    private boolean looksLikeHomePage(View root) {
        for (String marker : HOME_MARKERS) {
            if (containsTextDeep(root, marker)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsTextDeep(View view, String target) {
        if (view == null) return false;

        if (view instanceof TextView) {
            CharSequence cs = ((TextView) view).getText();
            if (cs != null && target.equals(cs.toString().trim())) {
                return true;
            }
        }

        CharSequence desc = view.getContentDescription();
        if (desc != null && target.equals(desc.toString().trim())) {
            return true;
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (containsTextDeep(group.getChildAt(i), target)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void hideMatchedContainer(View hitView, String text) {
        View target = findBestContainer(hitView, text);
        if (target == null) return;

        if (target.getVisibility() != View.GONE) {
            XposedBridge.log(TAG + "隐藏首页冗余项: " + text);
            target.setVisibility(View.GONE);
            target.setEnabled(false);
            target.setClickable(false);
        }
    }

    private View findBestContainer(View start, String text) {
        View current = start;

        int maxUp;
        if (isBottomTab(text)) {
            maxUp = 3;   // 底部 tab 往上抬 3 层基本够
        } else if (isFloatBadge(text)) {
            maxUp = 4;   // 运营挂件通常套得更深
        } else {
            maxUp = 4;   // 首页卡片区域
        }

        for (int i = 0; i < maxUp; i++) {
            ViewParent parent = current.getParent();
            if (!(parent instanceof View)) break;

            View pv = (View) parent;
            if (pv.getId() == android.R.id.content) break;

            current = pv;
        }

        return current;
    }

    private boolean isBottomTab(String text) {
        return "探索".equals(text) || "AI对话".equals(text) || "路线".equals(text);
    }

    private boolean isFloatBadge(String text) {
        return text.contains("扫街榜") || text.contains("订周末");
    }
}
