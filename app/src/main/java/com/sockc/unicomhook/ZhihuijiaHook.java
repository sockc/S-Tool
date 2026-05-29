package com.sockc.unicomhook;

import android.app.Dialog;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ZhihuijiaHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_Zhj: ";
    private static final String TARGET_PACKAGE = "com.changhong.ssc.cookbook"; // 智汇家包名

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "已注入智汇家，准备猎杀升级弹窗...");

        try {
            XposedHelpers.findAndHookMethod(Dialog.class, "show", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    final Dialog dialog = (Dialog) param.thisObject;
                    Window window = dialog.getWindow();
                    if (window == null) return;
                    
                    final View decorView = window.getDecorView();
                    
                    // 祖传异步延时，防止干涉 UI 渲染引擎导致闪退
                    decorView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (dialog.isShowing() && scanAndKillUpdateDialog(decorView)) {
                                    XposedBridge.log(TAG + "发现版本升级弹窗，已在底层暗杀！");
                                    dialog.dismiss(); // 瞬间销毁
                                }
                            } catch (Throwable t) {
                                XposedBridge.log(TAG + "销毁弹窗异常: " + t.getMessage());
                            }
                        }
                    }, 50);
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + "智汇家 Hook 失败: " + t.getMessage());
        }
    }

    private boolean scanAndKillUpdateDialog(View view) {
        if (view instanceof TextView) {
            String text = ((TextView) view).getText().toString();
            // 锁定图中的核心关键词
            if (text.contains("新版本来袭") || text.contains("免流量安装") || text.contains("下次再说")) {
                return true;
            }
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (scanAndKillUpdateDialog(group.getChildAt(i))) return true;
            }
        }
        return false;
    }
                      }
