package com.sockc.unicomhook;

import android.app.Activity;
import android.os.Bundle;
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

public class TaobaoHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_Taobao: ";
    private static final String TARGET_PACKAGE = "com.taobao.taobao";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        // 双重校验，只在“主进程”中执行，放过沙盒和推送子进程
        if (!lpparam.packageName.equals(TARGET_PACKAGE) || !lpparam.processName.equals(TARGET_PACKAGE)) {
            return;
        }

        XposedBridge.log(TAG + "已精准注入淘宝主进程，开启冷热双杀模式...");

        // 1. 狙击冷启动 (你原有的逻辑优化)
        hookColdSplash();

        // 2. 狙击热启动 (本次新增：解决后台切回有广告的问题)
        hookWarmSplash();
    }

    /**
     * 防线一：冷启动狙击
     */
    private void hookColdSplash() {
        try {
            XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = (Activity) param.thisObject;
                    String className = activity.getClass().getName();

                    if (className.toLowerCase().contains("welcome") || className.toLowerCase().contains("bootimage")) {
                        XposedBridge.log(TAG + "拦截到冷启动开屏页面: " + className);
                        
                        activity.getWindow().getDecorView().postDelayed(() -> {
                            try {
                                scanAndClickSkip(activity.getWindow().getDecorView());
                            } catch (Exception e) {
                                XposedBridge.log(TAG + "扫描 UI 时发生意外: " + e.getMessage());
                            }
                        }, 800); 
                    }
                }
            });
        } catch (Exception e) {
            XposedBridge.log(TAG + "冷启动 Hook 失败: " + e.getMessage());
        }
    }

    /**
     * 防线二：热启动狙击 (后台切前台)
     */
    private void hookWarmSplash() {
        try {
            XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = (Activity) param.thisObject;
                    Window window = activity.getWindow();
                    if (window == null) return;

                    final View decorView = window.getDecorView();

                    // 部署高敏视图监听：不管它什么时候把广告画出来，一露头就秒
                    decorView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            try {
                                if (scanAndClickSkip(decorView)) {
                                    XposedBridge.log(TAG + "抓获热启动广告，已物理超度并解除监听！");
                                    // 击杀完毕，立刻注销监听器，防止内存泄漏和卡顿
                                    decorView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                }
                            } catch (Exception e) {
                                XposedBridge.log(TAG + "热启动扫描异常: " + e.getMessage());
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            XposedBridge.log(TAG + "热启动 Hook 失败: " + e.getMessage());
        }
    }

    /**
     * 核心雷达：递归扫描并返回击杀结果
     * (已将 void 改为 boolean，配合监听器实现击杀后自动卸载)
     */
    private boolean scanAndClickSkip(View view) {
        if (view == null) return false;

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                
                if (child instanceof TextView) {
                    CharSequence cs = ((TextView) child).getText();
                    if (cs != null) {
                        String text = cs.toString();
                        // 精准锁定阿里系常用的跳过文案
                        if (text.contains("跳过") || text.contains("跳转")) {
                            XposedBridge.log(TAG + "锁定目标！发现 [" + text + "]，执行无感秒点！");
                            child.performClick();
                            
                            // 穿透点击：有时候文字不能点，它爹（父布局）才能点
                            if (child.getParent() instanceof View) {
                                ((View) child.getParent()).performClick();
                            }
                            return true; // 成功击杀，返回 true
                        }
                    }
                }
                // 深度递归
                if (scanAndClickSkip(child)) return true;
            }
        }
        return false;
    }
}
