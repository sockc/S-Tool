package com.sockc.unicomhook;

import android.app.Activity;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XianyuHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_Xianyu: ";
    private static final String TARGET_PACKAGE = "com.taobao.idlefish";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "已注入新版闲鱼，启动坐标盲狙模式...");

        try {
            XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = (Activity) param.thisObject;
                    String activityName = activity.getClass().getName().toLowerCase();
                    
                    // 只在包含启动页特征的 Activity 中开火
                    if (activityName.contains("splash") || activityName.contains("boot")) {
                        Window window = activity.getWindow();
                        if (window == null) return;
                        
                        final View decorView = window.getDecorView();
                        
                        // 延时 800 毫秒（等广告刚渲染出来），直接对右上角开枪
                        decorView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    // 模拟点击右上角坐标 (通常是屏幕宽度减去 100px，高度 150px 的位置)
                                    int screenWidth = decorView.getWidth();
                                    float targetX = screenWidth - 120f; // 靠右
                                    float targetY = 160f; // 靠上（避开状态栏）
                                    
                                    XposedBridge.log(TAG + "正在对坐标 (" + targetX + ", " + targetY + ") 执行盲狙跳过！");
                                    
                                    long downTime = SystemClock.uptimeMillis();
                                    long eventTime = SystemClock.uptimeMillis() + 100;
                                    
                                    // 构造按下和抬起事件
                                    MotionEvent downEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, targetX, targetY, 0);
                                    MotionEvent upEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, targetX, targetY, 0);
                                    
                                    decorView.dispatchTouchEvent(downEvent);
                                    decorView.dispatchTouchEvent(upEvent);
                                    
                                    downEvent.recycle();
                                    upEvent.recycle();
                                } catch (Throwable t) {
                                    XposedBridge.log(TAG + "盲狙失败: " + t.getMessage());
                                }
                            }
                        }, 800); // 延时可根据你手机加载速度微调
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + "闲鱼 Hook 异常: " + t.getMessage());
        }
    }
}
