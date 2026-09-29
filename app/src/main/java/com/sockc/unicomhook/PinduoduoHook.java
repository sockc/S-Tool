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
    private static final String TARGET_PACKAGE =
            "com.xunmeng.pinduoduo";

    @Override
    public void handleLoadPackage(
            LoadPackageParam lpparam
    ) {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        HookConfig config = HookConfig.load();

        boolean universalFilePrivacy =
                config.isUniversalAppEnabled(
                        FeatureRegistry.UNIVERSAL_FILE_MEDIA,
                        TARGET_PACKAGE
                );

        boolean filePrivacy =
                config.isEnabled(
                        "pinduoduo",
                        "pinduoduo.file_privacy"
                )
                        && !universalFilePrivacy;
        boolean permissionPrivacy =
                config.isEnabled(
                        "pinduoduo",
                        "pinduoduo.permission_privacy"
                );
        boolean splashAd =
                config.isEnabled(
                        "pinduoduo",
                        "pinduoduo.splash_ad"
                );

        XposedBridge.log(
                TAG
                        + "子功能: file="
                        + filePrivacy
                        + ", permission="
                        + permissionPrivacy
                        + ", ad="
                        + splashAd
        );

        if (filePrivacy) {
            hookFilePrivacy();
        }

        if (permissionPrivacy) {
            hookPermissionPrivacy(
                    lpparam.classLoader
            );
        }

        if (splashAd) {
            hookSplashAd();
        }
    }

    private void hookFilePrivacy() {
        try {
            XposedHelpers.findAndHookMethod(
                    File.class,
                    "listFiles",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(
                                MethodHookParam param
                        ) {
                            File file =
                                    (File) param.thisObject;

                            String path =
                                    file.getAbsolutePath();

                            if (path.contains("DCIM")
                                    || path.contains(
                                    "Pictures"
                            )
                                    || path.contains(
                                    "Download"
                            )) {
                                XposedBridge.log(
                                        TAG
                                                + "拦截目录扫描: "
                                                + path
                                );
                                param.setResult(
                                        new File[0]
                                );
                            }
                        }
                    }
            );

            XposedHelpers.findAndHookMethod(
                    Environment.class,
                    "getExternalStorageDirectory",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(
                                MethodHookParam param
                        ) {
                            XposedBridge.log(
                                    TAG
                                            + "检测到外部存储根目录请求"
                            );
                        }
                    }
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    TAG
                            + "文件隐私 Hook 失败: "
                            + throwable
            );
        }
    }

    private void hookPermissionPrivacy(
            ClassLoader classLoader
    ) {
        try {
            XposedHelpers.findAndHookMethod(
                    "android.app.ContextImpl",
                    classLoader,
                    "checkPermission",
                    String.class,
                    int.class,
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(
                                MethodHookParam param
                        ) {
                            String permission =
                                    (String) param.args[0];

                            if (permission == null) {
                                return;
                            }

                            if (permission.contains(
                                    "STORAGE"
                            )
                                    || permission.contains(
                                    "READ_MEDIA"
                            )
                                    || permission.contains(
                                    "READ_PHONE_STATE"
                            )
                                    || permission.contains(
                                    "QUERY_ALL_PACKAGES"
                            )) {
                                XposedBridge.log(
                                        TAG
                                                + "拒绝敏感权限: "
                                                + permission
                                );
                                param.setResult(
                                        PackageManager
                                                .PERMISSION_DENIED
                                );
                            }
                        }
                    }
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    TAG
                            + "权限保护 Hook 失败: "
                            + throwable
            );
        }
    }

    private void hookSplashAd() {
        try {
            XposedHelpers.findAndHookMethod(
                    Activity.class,
                    "onCreate",
                    Bundle.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(
                                MethodHookParam param
                        ) {
                            Activity activity =
                                    (Activity) param.thisObject;

                            if (!TARGET_PACKAGE.equals(
                                    activity.getPackageName()
                            )) {
                                return;
                            }

                            View decorView =
                                    activity
                                            .getWindow()
                                            .getDecorView();

                            final ViewTreeObserver
                                    .OnGlobalLayoutListener[]
                                    holder =
                                    new ViewTreeObserver
                                            .OnGlobalLayoutListener[1];

                            holder[0] =
                                    new ViewTreeObserver
                                            .OnGlobalLayoutListener() {
                                        @Override
                                        public void onGlobalLayout() {
                                            if (scanAndKillAd(
                                                    decorView
                                            )) {
                                                XposedBridge.log(
                                                        TAG
                                                                + "已处理拼多多开屏广告"
                                                );

                                                removeListener(
                                                        decorView,
                                                        holder[0]
                                                );
                                            }
                                        }
                                    };

                            decorView
                                    .getViewTreeObserver()
                                    .addOnGlobalLayoutListener(
                                            holder[0]
                                    );

                            decorView.postDelayed(
                                    () ->
                                            removeListener(
                                                    decorView,
                                                    holder[0]
                                            ),
                                    5000L
                            );
                        }
                    }
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    TAG
                            + "开屏广告 Hook 失败: "
                            + throwable
            );
        }
    }

    private void removeListener(
            View root,
            ViewTreeObserver.OnGlobalLayoutListener listener
    ) {
        try {
            ViewTreeObserver observer =
                    root.getViewTreeObserver();

            if (observer.isAlive()
                    && listener != null) {
                observer.removeOnGlobalLayoutListener(
                        listener
                );
            }
        } catch (Throwable ignored) {
        }
    }

    private boolean scanAndKillAd(
            View view
    ) {
        if (view instanceof ViewGroup) {
            ViewGroup group =
                    (ViewGroup) view;

            for (int i = 0;
                    i < group.getChildCount();
                    i++) {
                View child =
                        group.getChildAt(i);

                if (child instanceof TextView) {
                    String text =
                            ((TextView) child)
                                    .getText()
                                    .toString();

                    if (text.contains("跳过")
                            || text.equals("Skip")) {
                        child.performClick();
                        return true;
                    }
                } else if (scanAndKillAd(
                        child
                )) {
                    return true;
                }
            }
        }

        return false;
    }
}
