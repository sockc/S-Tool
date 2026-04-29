package com.sockc.unicomhook;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class InstagramHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_IG: ";
    private static final String TARGET_PACKAGE = "com.instagram.android";
    
    // 增加更多可能的广告关键字
    private static final String[] AD_KEYWORDS = {"Sponsored", "sponsored", "赞助内容", "赞助", "Suggested for you"};

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "全面升级版注入成功...");

        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                
                ViewTreeObserver.OnGlobalLayoutListener listener = new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        scanAndHideAds(activity.getWindow().getDecorView());
                    }
                };
                
                activity.getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(listener);
            }
        });
    }

    private void scanAndHideAds(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                
                if (child instanceof TextView) {
                    String text = ((TextView) child).getText().toString();
                    for (String keyword : AD_KEYWORDS) {
                        if (text.equals(keyword)) {
                            // 发现目标！执行精准切除
                            hideTheWholePost(child);
                            return;
                        }
                    }
                } else {
                    scanAndHideAds(child);
                }
            }
        }
    }

    private void hideTheWholePost(View adLabel) {
        View current = adLabel;
        // 策略升级：不再写死 8 层，而是不断向上找，直到找到一个占据屏幕大部分宽度的 View
        // 这通常就是整个帖子卡片的根布局
        while (current.getParent() instanceof View) {
            View parent = (View) current.getParent();
            
            // 如果这个父布局的宽度已经接近屏幕宽度，说明它就是帖子容器
            if (parent.getWidth() > 0 && parent.getHeight() > 0) {
                // 找到疑似卡片容器（高度通常大于 300 像素，防止误删小部件）
                if (parent.getHeight() > 300) {
                    if (parent.getVisibility() != View.GONE) {
                        XposedBridge.log(TAG + "锁定广告卡片，已将其物理抹除");
                        parent.setVisibility(View.GONE);
                        // 强制把高度设为 0，防止留白
                        ViewGroup.LayoutParams params = parent.getLayoutParams();
                        params.height = 0;
                        parent.setLayoutParams(params);
                    }
                    break; 
                }
            }
            current = parent;
            // 防止死循环，设定一个安全上限
            if (current.getId() == android.R.id.content) break;
        }
    }
}
