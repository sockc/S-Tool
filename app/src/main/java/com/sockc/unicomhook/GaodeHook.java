package com.sockc.unicomhook; // 沿用之前的包名，方便你管理

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout; // 假设底部是LinearLayout

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class GaodeHook implements IXposedHookLoadPackage {
    private static final String TAG = "SockcHook_Gaode: ";
    private static final String TARGET_PACKAGE = "com.autonavi.minimap"; // 高德地图包名

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

    /**
     * 1. 去除开屏广告
     */
    private void hookSplashAd(LoadPackageParam lpparam) {
        // !!! 注意：这里必须是你在 ADB 里抓取到的真正开屏 Activity 类名 !!!
        String SPLASH_ACTIVITY = "com.autonavi.map.activity.SplashActivity"; 

        try {
            XposedHelpers.findAndHookMethod(SPLASH_ACTIVITY, lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = (Activity) param.thisObject;
                    XposedBridge.log(TAG + "拦截到开屏 Activity，准备秒进主界面...");

                    // 1. 阻止原有的 onCreate 继续执行
                    param.setResult(null);

                    // 2. 构造跳转到主界面的 Intent (主界面类名通常是 MainActivity，但要确认)
                    String MAIN_ACTIVITY = "com.autonavi.map.activity.NewMainActivity";
                    Intent intent = new Intent();
                    intent.setClassName(TARGET_PACKAGE, MAIN_ACTIVITY);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                    activity.startActivity(intent);
                    activity.finish(); // 结束开屏页
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + "去开屏广告失败: " + t.getMessage());
        }
    }

    /**
     * 2. 去除首页“精彩笔记”推荐卡片
     * 方法论：Hook 布局加载，拦截对应卡片的添加。
     */
    private void hookHomePageCards(LoadPackageParam lpparam) {
        try {
            // Hook 首页主界面的生命周期，在 onCreate 之后去寻找布局并修改
            String MAIN_ACTIVITY = "com.autonavi.map.activity.NewMainActivity"; // 需要确认
            XposedHelpers.findAndHookMethod(MAIN_ACTIVITY, lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = (Activity) param.thisObject;
                    
                    // 利用 XposedHelpers 查找资源 ID
                    int cardContainerId = XposedHelpers.findIntField(
                        lpparam.classLoader.loadClass("com.autonavi.minimap.R$id"), "home_card_container"); // !!! 这里的ID名称必须在 MT 里找到 !!!
                    
                    if (cardContainerId != 0) {
                        // 在运行时去寻找这个 View
                        View cardContainer = activity.findViewById(cardContainerId);
                        if (cardContainer != null && cardContainer instanceof ViewGroup) {
                            XposedBridge.log(TAG + "成功拦截卡片容器，准备移出精彩笔记卡片...");
                            // 遍历容器里的子控件，找到那个讨厌的卡片，移出它
                            removeNoteCard(activity, (ViewGroup) cardContainer);
                        }
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + "去首页卡片失败: " + t.getMessage());
        }
    }
    
    // 移出精彩笔记卡片的具体辅助方法
    private void removeNoteCard(Activity activity, ViewGroup container) {
        try {
            // 这个方法需要你在 MT 里抓到“精彩笔记”这个 View 的具体ID或特征
            int noteCardId = XposedHelpers.findIntField(
                activity.getClassLoader().loadClass("com.autonavi.minimap.R$id"), "home_page_note_card"); // !!! 必须确认ID !!!

            if (noteCardId != 0) {
                View noteCard = activity.findViewById(noteCardId);
                if (noteCard != null) {
                    container.removeView(noteCard); // 从布局中物理移出
                    XposedBridge.log(TAG + "成功移出精彩笔记卡片！");
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + "遍历卡片寻找 ID 失败: " + t.getMessage());
        }
    }

    /**
     * 3. 去除底部 Tab (探索、AI对话)
     * 方法论：遍历底部导航栏的 LinearLayout，隐藏不需要的 Tab。
     */
    private void hookBottomTabs(LoadPackageParam lpparam) {
        try {
            // Hook 底部导航栏布局加载完毕的时机 (假设它是一个 LinearLayout)
            XposedHelpers.findAndHookMethod(LinearLayout.class, "onFinishInflate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    LinearLayout bottomBar = (LinearLayout) param.thisObject;
                    Context context = bottomBar.getContext();
                    
                    // 确认包名，避免 Hook 到其他应用的 LinearLayout
                    if (context.getPackageName().equals(TARGET_PACKAGE)) {
                        
                        // 遍历子 View，寻找对应的 Tab
                        for (int i = 0; i < bottomBar.getChildCount(); i++) {
                            View child = bottomBar.getChildAt(i);
                            
                            // 获取 View 的 ID 名称的通用方法
                            String viewIdName = getResourceName(context, child.getId()); // 获取 View 的 ID 名称

                            // 如果匹配到“探索”或“AI对话” Tab (这里需要替换为你在抓ID步骤中找到的真正混淆后的ID名称)
                            if (viewIdName.equals("explore_tab_a5g") || viewIdName.equals("ai_dialog_tab_xxx")) { 
                                XposedBridge.log(TAG + "拦截到底部 Tab: " + viewIdName + "，准备隐藏...");
                                child.setVisibility(View.GONE); // 彻底隐藏
                            }
                        }
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + "去底部 Tab 失败: " + t.getMessage());
        }
    }
    
    // 获取 View 的 ID 名称的通用方法
    private String getResourceName(Context context, int id) {
        try {
            if (id == 0) return "null_id";
            return context.getResources().getResourceEntryName(id);
        } catch (Exception e) {
            return "unknown_id";
        }
    }
}
