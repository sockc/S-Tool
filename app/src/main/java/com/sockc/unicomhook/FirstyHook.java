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

public class FirstyHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_Firsty: ";
    private static final String TARGET_PACKAGE = "app.firsty.firsty";

    // 激励广告关闭按钮常见的特征
    private static final String[] CLOSE_KEYWORDS = {"Close", "关闭", "X", "x"};

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "已成功潜入 Firsty，准备自动领取上网奖励...");

        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                final View decorView = activity.getWindow().getDecorView();

                // 激励广告时间长，我们需要全程监听布局变化
                decorView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        // 如果成功点到了关闭按钮
                        if (scanAndReward(decorView)) {
                            XposedBridge.log(TAG + "检测到关闭按钮，已自动点击，30分钟奖励到手！");
                            // 这种按钮可能会反复出现，所以这里不立即移除监听，或者根据需要手动移除
                        }
                    }
                });
            }
        });
    }

    private boolean scanAndReward(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                
                // 1. 检查是否有文字标识 (如 "Close")
                if (child instanceof TextView) {
                    String text = ((TextView) child).getText().toString();
                    for (String key : CLOSE_KEYWORDS) {
                        if (text.equalsIgnoreCase(key) && child.isShown()) {
                            child.performClick();
                            return true;
                        }
                    }
                }

                // 2. 检查内容描述 (ContentDescription)
                // 很多广告 SDK 的 "X" 按钮没有 Text，但会有 "Close ad" 的描述
                CharSequence desc = child.getContentDescription();
                if (desc != null) {
                    String d = desc.toString().toLowerCase();
                    if (d.contains("close") || d.contains("dismiss") || d.equals("x")) {
                        if (child.isShown() && child.isClickable()) {
                            child.performClick();
                            return true;
                        }
                    }
                }

                // 递归深挖
                if (scanAndReward(child)) return true;
            }
        }
        return false;
    }
}
