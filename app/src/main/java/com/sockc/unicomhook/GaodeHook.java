package com.sockc.unicomhook;

import android.app.Activity;
import android.content.Context;
import android.content.Intent; // 修复：补充了 Intent 导入
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class GaodeHook implements IXposedHookLoadPackage {
    private static final String TAG = "SockcHook_Gaode: ";
    private static final String TARGET_PACKAGE = "com.autonavi.minimap"; 

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "已注入高德地图，开始净化 UI...");

        // 1. 去除开屏广告
        hookSplashAd(lpparam);

        // 2. 去除首页“精彩笔记”推荐块
        hookHomePageCards(lpparam);

        // 3. 去除底部 Tab (探索、AI对话)
        hookBottomTabs(lpparam);
    }

    private void hookSplashAd(LoadPackageParam lpparam) {
        String SPLASH_ACTIVITY = "com.autonavi.map.activity.SplashActivity"; 

        try {
            XposedHelpers.findAndHookMethod(SPLASH_ACTIVITY, lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = (Activity) param.thisObject;
                    XposedBridge.log(TAG + "拦截到开屏 Activity，准备秒进主界面...");

                    param.setResult(null);

                    String MAIN_ACTIVITY = "com.autonavi.map.activity.NewMainActivity";
                    Intent intent = new Intent();
                    intent.setClassName(TARGET_PACKAGE, MAIN_ACTIVITY);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                    activity.startActivity(intent);
                    activity.finish(); 
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + "去开屏广告失败: " + t.getMessage());
        }
    }

    private void hookHomePageCards(LoadPackageParam lpparam) {
        try {
            String MAIN_ACTIVITY = "com.autonavi.map.activity.NewMainActivity"; 
            XposedHelpers.findAndHookMethod(MAIN_ACTIVITY, lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = (Activity) param.thisObject;
                    
                    try {
                        // 修复：使用了正确的 getStaticIntField 方法来读取静态变量
                        int cardContainerId = XposedHelpers.getStaticIntField(
                            lpparam.classLoader.loadClass("com.autonavi.minimap.R$id"), "home_card_container"); 
                        
                        if (cardContainerId != 0) {
                            View cardContainer = activity.findViewById(cardContainerId);
                            if (cardContainer != null && cardContainer instanceof ViewGroup) {
                                XposedBridge.log(TAG + "成功拦截卡片容器，准备移出精彩笔记卡片...");
                                removeNoteCard(activity, (ViewGroup) cardContainer);
                            }
                        }
                    } catch (Throwable t) {
                        XposedBridge.log(TAG + "未找到卡片容器 ID: " + t.getMessage());
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + "去首页卡片失败: " + t.getMessage());
        }
    }
    
    private void removeNoteCard(Activity activity, ViewGroup container) {
        try {
            // 修复：使用了正确的 getStaticIntField 方法
            int noteCardId = XposedHelpers.getStaticIntField(
                activity.getClassLoader().loadClass("com.autonavi.minimap.R$id"), "home_page_note_card"); 

            if (noteCardId != 0) {
                View noteCard = activity.findViewById(noteCardId);
                if (noteCard != null) {
                    container.removeView(noteCard); 
                    XposedBridge.log(TAG + "成功移出精彩笔记卡片！");
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + "遍历卡片寻找 ID 失败: " + t.getMessage());
        }
    }

    private void hookBottomTabs(LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(LinearLayout.class, "onFinishInflate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    LinearLayout bottomBar = (LinearLayout) param.thisObject;
                    Context context = bottomBar.getContext();
                    
                    if (context.getPackageName().equals(TARGET_PACKAGE)) {
                        for (int i = 0; i < bottomBar.getChildCount(); i++) {
                            View child = bottomBar.getChildAt(i);
                            String viewIdName = getResourceName(context, child.getId()); 

                            if (viewIdName.equals("explore_tab_a5g") || viewIdName.equals("ai_dialog_tab_xxx")) { 
                                XposedBridge.log(TAG + "拦截到底部 Tab: " + viewIdName + "，准备隐藏...");
                                child.setVisibility(View.GONE); 
                            }
                        }
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + "去底部 Tab 失败: " + t.getMessage());
        }
    }
    
    private String getResourceName(Context context, int id) {
        try {
            if (id == 0) return "null_id";
            return context.getResources().getResourceEntryName(id);
        } catch (Exception e) {
            return "unknown_id";
        }
    }
}
