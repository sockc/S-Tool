package com.sockc.unicomhook;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.TextView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class UnicomHook implements IXposedHookLoadPackage {
    private static final String TAG = "SockcHook: ";
    private static final String TARGET_PACKAGE = "com.sinovatech.unicom.ui";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "已注入联通客户端，准备执行净化与欺骗...");

        // 1. 执行去广告逻辑 (冷启动)
        hookSplashAd(lpparam);

        // 1.5 执行热启动去广告 (本次融合新增：对付后台切回前台的广告)
        hookWarmSplash(lpparam);

        // 2. 执行隐私权限欺骗
        hookPrivacy(lpparam);

        // 3. 执行风险弹窗拦截 (保持注释状态，防止 Native 级闪退)
        //hookRiskDialog(lpparam);
    }

    /**
     * 去除开屏广告 (防闪退强化版 - 针对冷启动)
     */
    private void hookSplashAd(LoadPackageParam lpparam) {
        String SPLASH_ACTIVITY = "com.sinovatech.unicom.basic.ui.activity.WelcomeClient";
        String MAIN_ACTIVITY = "com.sinovatech.unicom.basic.ui.activity.MainActivity";

        Class<?> splashClass = XposedHelpers.findClassIfExists(SPLASH_ACTIVITY, lpparam.classLoader);
        if (splashClass == null) {
            XposedBridge.log(TAG + "当前为旧版联通，未找到新版开屏类，安全放行，不执行去广告。");
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(splashClass, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        Activity activity = (Activity) param.thisObject;
                        XposedBridge.log(TAG + "拦截到开屏页，正在跳转主界面...");

                        Intent intent = new Intent();
                        intent.setClassName(TARGET_PACKAGE, MAIN_ACTIVITY);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                        activity.startActivity(intent);
                        activity.finish();
                    } catch (ActivityNotFoundException e) {
                        XposedBridge.log(TAG + "警告：找不到主界面，旧版跳转失败，不强制跳转以免闪退！");
                    } catch (Throwable t) {
                        XposedBridge.log(TAG + "开屏跳过逻辑发生未知异常: " + t.getMessage());
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + "去广告 Hook 失败: " + t.getMessage());
        }
    }

    /**
     * 去除热启动广告 (新增逻辑：针对从桌面返回 App 时的广告)
     */
    private void hookWarmSplash(LoadPackageParam lpparam) {
        try {
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
                                XposedBridge.log(TAG + "抓获热启动广告【跳过】按钮，已底层光速点击！");
                                decorView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            }
                        }
                    });
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + "热启动 Hook 失败: " + t.getMessage());
        }
    }

    /**
     * 配合热启动的视图扫描器
     */
    private boolean scanAndClickSkip(View view) {
        if (view instanceof TextView) {
            String text = ((TextView) view).getText().toString();
            // 匹配各类常见的跳过文案
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

    /**
     * 权限欺骗与伪造数据 (安全强化版)
     */
    private void hookPrivacy(LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(ContextWrapper.class, "checkSelfPermission", String.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        String permission = (String) param.args[0];
                        if (permission != null && (
                            permission.contains("LOCATION") || 
                            permission.contains("CONTACTS") || 
                            permission.contains("READ_MEDIA_IMAGES") || 
                            permission.contains("READ_EXTERNAL_STORAGE"))) {
                            
                            XposedBridge.log(TAG + "拦截到权限检查: " + permission + "，强行返回已授权！");
                            return PackageManager.PERMISSION_GRANTED;
                        }
                        return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                    } catch (Throwable t) {
                        XposedBridge.log(TAG + "权限欺骗回调内部异常，放行: " + t.getMessage());
                        return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(LocationManager.class, "getLastKnownLocation", String.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        XposedBridge.log(TAG + "拦截到获取定位，返回伪造坐标！");
                        Location mockLocation = new Location(LocationManager.GPS_PROVIDER);
                        mockLocation.setLatitude(23.1291);  
                        mockLocation.setLongitude(113.2644); 
                        mockLocation.setAccuracy(100f);
                        mockLocation.setTime(System.currentTimeMillis());
                        return mockLocation;
                    } catch (Throwable t) {
                        XposedBridge.log(TAG + "伪造定位失败，返回空: " + t.getMessage());
                        return null; 
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + "隐私 Hook 绑定失败: " + t.getMessage());
        }
    }

    /**
     * 风险警告弹窗拦截 (防 Root 检测警告)
     */
    private void hookRiskDialog(LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(Dialog.class, "show", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    final Dialog dialog = (Dialog) param.thisObject;
                    Window window = dialog.getWindow();
                    if (window == null) return;
                    
                    final View decorView = window.getDecorView();
                    decorView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            if (scanAndKillRiskDialog(decorView)) {
                                XposedBridge.log(TAG + "抓获环境风险警告弹窗，已强行销毁！");
                                dialog.dismiss();
                                decorView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            }
                        }
                    });
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + "弹窗拦截 Hook 失败: " + t.getMessage());
        }
    }

    /**
     * 扫描视图树匹配敏感警告词条
     */
    private boolean scanAndKillRiskDialog(View view) {
        if (view instanceof TextView) {
            String text = ((TextView) view).getText().toString();
            if (text.contains("current device environment") || text.contains("环境存在风险")) {
                return true;
            }
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (scanAndKillRiskDialog(group.getChildAt(i))) return true;
            }
        }
        return false;
    }
}
