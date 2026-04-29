package com.sockc.unicomhook;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class InstagramHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_IG: ";
    private static final String TARGET_PACKAGE = "com.instagram.android";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "已注入 Instagram，开始扫描广告特征...");

        // 拦截 Activity 的创建，IG 的主页通常在这些核心 Activity 里
        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                
                // IG 的 Feed 广告是动态滑动的，所以需要给根布局设置一个监听
                activity.getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                    // 每次界面变动（滑动）时，递归扫描广告
                    scanAndHideAds(activity.getWindow().getDecorView());
                });
            }
        });
    }

    private void scanAndHideAds(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);

                // 如果是文本，检查是否包含“赞助”关键字
                if (child instanceof TextView) {
                    String text = ((TextView) child).getText().toString();
                    if (text.equals("Sponsored") || text.equals("赞助内容") || text.equals("赞助")) {
                        // 发现广告特征！向上寻找包裹整个帖子的容器（通常是第 4-6 层父类）
                        hidePostContainer(child);
                    }
                } else {
                    scanAndHideAds(child);
                }
            }
        }
    }

    private void hidePostContainer(View skipText) {
        View current = skipText;
        // 向上递归寻找父容器，直到找到看起来像是一个完整帖子的 View
        // 在 IG 中，广告文案向上大约 5-8 层就是整个卡片容器
        for (int i = 0; i < 8; i++) {
            if (current.getParent() instanceof View) {
                current = (View) current.getParent();
                // 尝试通过宽高比来判断，或者直接根据层级强行隐藏
                if (i >= 5) { 
                    if (current.getVisibility() != View.GONE) {
                        XposedBridge.log(TAG + "成功通过关键字拦截并隐藏了一个 IG 广告帖");
                        current.setVisibility(View.GONE);
                        // 把宽高设为 0，防止留下一块空白
                        ViewGroup.LayoutParams params = current.getLayoutParams();
                        params.height = 0;
                        current.setLayoutParams(params);
                    }
                }
            }
        }
    }
}
