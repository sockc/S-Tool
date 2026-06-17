package com.sockc.unicomhook;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.TextView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XianyuHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_Xianyu: ";
    private static final String TARGET_PACKAGE = "com.taobao.idlefish"; // 闲鱼包名

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "已注入新版闲鱼，启动视图降维打击...");

        try {
            // 拦截所有 Activity 的 onResume 阶段
            XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = (Activity) param.thisObject;
                    String activityName = activity.getClass().getName().toLowerCase();
                    
                    // 为了性能，只在带有 Splash 或 Main 关键字的初始页面扫描
                    if (activityName.contains("splash") || activityName.contains("main") || activityName.contains("home")) {
                        Window window = activity.getWindow();
                        if (window == null) return;
                        
                        final View decorView = window.getDecorView();
                        
                        // 监听视图的全局布局事件，只要 UI 一画完，立刻开扫
                        decorView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                if (scanAndClickSkip(decorView)) {
                                    XposedBridge.log(TAG + "抓获开屏【跳过】按钮，已执行光速物理点击！");
                                    // 击杀成功后，立刻撤销监听器，不影响后续刷闲鱼的流畅度
                                    decorView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                }
                            }
                        });
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + "闲鱼 Hook 异常: " + t.getMessage());
        }
    }

    /**
     * 递归扫描视图树，寻找“跳过”按钮并强制触发 Click 事件
     */
    private boolean scanAndClickSkip(View view) {
        if (view instanceof TextView) {
            String text = ((TextView) view).getText().toString();
            // 匹配闲鱼的“跳过”、“跳过广告”、“跳过 3”等各种变体
            if (text.contains("跳过")) {
                view.performClick(); // 不废话，直接在底层按下去
                return true;
            }
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            // 深度优先遍历所有的子视图
            for (int i = 0; i < group.getChildCount(); i++) {
                if (scanAndClickSkip(group.getChildAt(i))) return true;
            }
        }
        return false;
    }
}
