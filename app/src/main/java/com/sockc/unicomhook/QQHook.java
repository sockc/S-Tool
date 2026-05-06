package com.sockc.unicomhook;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class QQHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_QQ: ";
    private static final String TARGET_PACKAGE = "com.tencent.mobileqq";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "已注入 QQ，准备猎杀升级弹窗...");

        // 1. 拦截升级 Activity 的启动 (最暴力有效的方法)
        XposedHelpers.findAndHookMethod("android.app.Instrumentation", lpparam.classLoader, "execStartActivity",
                android.content.Context.class, android.os.IBinder.class, android.os.IBinder.class,
                android.app.Activity.class, android.content.Intent.class, int.class, android.os.Bundle.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Intent intent = (Intent) param.args[4];
                        if (intent != null && intent.getComponent() != null) {
                            String className = intent.getComponent().getClassName();
                            // QQ 的升级 Activity 通常包含 UpgradeActivity
                            if (className.contains("UpgradeActivity")) {
                                XposedBridge.log(TAG + "成功拦截 QQ 升级 Activity 启动！");
                                param.setResult(null); // 终止启动
                            }
                        }
                    }
                });

        // 2. 拦截所有对话框显示，扫描“更新”关键字
        XposedHelpers.findAndHookMethod(Dialog.class, "show", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Dialog dialog = (Dialog) param.thisObject;
                View decorView = dialog.getWindow().getDecorView();
                if (decorView instanceof ViewGroup) {
                    if (scanForUpdateKeywords((ViewGroup) decorView)) {
                        XposedBridge.log(TAG + "成功拦截到包含‘更新/升级’字样的弹窗！");
                        param.setResult(null); // 终止显示
                    }
                }
            }
        });
    }

    private boolean scanForUpdateKeywords(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof TextView) {
                String text = ((TextView) child).getText().toString();
                if (text.contains("确认更新") || text.contains("发现新版本") || text.contains("版本更新") || text.contains("立即升级")) {
                    return true;
                }
            } else if (child instanceof ViewGroup) {
                if (scanForUpdateKeywords((ViewGroup) child)) return true;
            }
        }
        return false;
    }
}
