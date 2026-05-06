package com.sockc.unicomhook;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import java.io.File;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class PinduoduoHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_PDD: ";
    private static final String TARGET_PACKAGE = "com.xunmeng.pinduoduo";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "已成功注入拼多多，开启全方位隐私隔离...");

        // ==========================================
        // 1. 彻底切断文件系统访问 (文件隔离)
        // ==========================================
        try {
            // 拦截 File 类的 listFiles，当 PDD 尝试扫描目录（如相册、下载）时，返回空数组
            XposedHelpers.findAndHookMethod(File.class, "listFiles", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    File file = (File) param.thisObject;
                    String path = file.getAbsolutePath();
                    
                    // 如果路径包含 DCIM、Pictures、Download 等敏感目录，直接让它看个寂寞
                    if (path.contains("DCIM") || path.contains("Pictures") || path.contains("Download")) {
                        XposedBridge.log(TAG + "拦截到目录扫描: " + path + "，返回空列表");
                        param.setResult(new File[0]);
                    }
                }
            });

            // 拦截外部存储路径获取，让它拿不到 SD 卡的真实根目录
            XposedHelpers.findAndHookMethod(Environment.class, "getExternalStorageDirectory", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log(TAG + "拦截外部存储根目录请求");
                    // 也可以强行返回一个 App 内部的私有路径，不让它乱跑
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + "文件系统拦截异常: " + t.getMessage());
        }

        // ==========================================
        // 2. 权限硬拦截 (强制拒绝存储/隐私权限)
        // ==========================================
        XposedHelpers.findAndHookMethod("android.app.ContextImpl", lpparam.classLoader, "checkPermission", 
                String.class, int.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String permission = (String) param.args[0];
                if (permission.contains("STORAGE") || permission.contains("READ_MEDIA") || 
                    permission.contains("READ_PHONE_STATE") || permission.contains("QUERY_ALL_PACKAGES")) {
                    XposedBridge.log(TAG + "硬拦截权限请求: " + permission);
                    param.setResult(PackageManager.PERMISSION_DENIED);
                }
            }
        });

        // ==========================================
        // 3. 秒杀开屏广告
        // ==========================================
        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                final Activity activity = (Activity) param.thisObject;
                final View decorView = activity.getWindow().getDecorView();

                decorView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    boolean clicked = false;
                    @Override
                    public void onGlobalLayout() {
                        if (!clicked && scanAndKillAd(decorView)) {
                            clicked = true;
                            XposedBridge.log(TAG + "已自动跳过拼多多广告");
                            decorView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    }
                });
            }
        });
    }

    private boolean scanAndKillAd(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof TextView) {
                    String text = ((TextView) child).getText().toString();
                    if (text.contains("跳过") || text.equals("Skip")) {
                        child.performClick();
                        return true;
                    }
                } else if (scanAndKillAd(child)) {
                    return true;
                }
            }
        }
        return false;
    }
}
