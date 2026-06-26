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

public class YingyongbaoHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_MyApp: ";
    private static final String TARGET_PACKAGE = "com.tencent.android.qqdownloader"; // 应用宝包名

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "已注入腾讯应用宝，启动开屏猎杀...");

        try {
            // 拦截所有 Activity，专蹲开屏和弹窗
            XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = (Activity) param.thisObject;
                    Window window = activity.getWindow();
                    if (window == null) return;
                    
                    final View decorView = window.getDecorView();
                    
                    decorView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            if (scanAndClickSkip(decorView)) {
                                XposedBridge.log(TAG + "抓获广点通开屏/弹窗，已光速点击跳过！");
                                decorView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            }
                        }
                    });
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + "应用宝 Hook 异常: " + t.getMessage());
        }
    }

    /**
     * 递归扫描视图，狙击腾讯系广告特有的文案
     */
    private boolean scanAndClickSkip(View view) {
        if (view instanceof TextView) {
            String text = ((TextView) view).getText().toString();
            // 涵盖应用宝开屏、内部弹窗最常用的几个关闭文案
            if (text.contains("跳过") || text.equals("关闭") || text.equals("点击跳过")) {
                view.performClick(); 
                return true;
            }
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (scanAndClickSkip(group.getChildAt(i))) return true;
            }
        }
        return false;
    }
}
