package com.sockc.unicomhook;

import android.app.Activity;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class AdMobBypassHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_AdBypass: ";
    private static final String TARGET_PACKAGE = "com.airvoy.airvoy";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "已注入 Airvoy，準備對 Google AdMob 進行降維打擊...");

        try {
            // 目標：Google AdMob 激勵影片的核心類別
            String REWARDED_AD_CLASS = "com.google.android.gms.ads.rewarded.RewardedAd";
            String LISTENER_CLASS = "com.google.android.gms.ads.OnUserEarnedRewardListener";

            // 攔截 show() 方法：這個方法原本是用來彈出廣告全螢幕畫面的
            XposedHelpers.findAndHookMethod(REWARDED_AD_CLASS, lpparam.classLoader, "show",
                    Activity.class, LISTENER_CLASS, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log(TAG + "攔截到廣告播放請求！阻斷畫面彈出，直接發放獎勵！");

                            // param.args[1] 就是 Flutter 傳過來的獎勵監聽器 (OnUserEarnedRewardListener)
                            Object rewardListener = param.args[1];

                            if (rewardListener != null) {
                                // 騙 Flutter 說：用戶已經看完了，給你發獎勵回調！
                                // 這裡傳入 null 作為 RewardItem，多數情況下 App 只要收到回調就會加流量
                                XposedHelpers.callMethod(rewardListener, "onUserEarnedReward", new Object[]{null});
                                XposedBridge.log(TAG + "偽造完播回調發送成功！");
                            }

                            // 返回 null，強行終止廣告組件的渲染
                            return null;
                        }
                    });

        } catch (Throwable t) {
            XposedBridge.log(TAG + "AdMob Hook 失敗: " + t.getMessage());
        }
    }
}
