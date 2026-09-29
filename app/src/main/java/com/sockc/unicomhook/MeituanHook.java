package com.sockc.unicomhook;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
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

public class MeituanHook implements IXposedHookLoadPackage {

    private static final String TAG = "Sockc_Meituan: ";
    private static final String TARGET_PACKAGE =
            "com.sankuai.meituan";

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
                        "meituan",
                        "meituan.file_privacy"
                )
                        && !universalFilePrivacy;
        boolean splashAd =
                config.isEnabled(
                        "meituan",
                        "meituan.splash_ad"
                );
        boolean permissionPrivacy =
                config.isEnabled(
                        "meituan",
                        "meituan.permission_privacy"
                );
        boolean universalDeviceIdPrivacy =
                config.isUniversalAppEnabled(
                        FeatureRegistry.UNIVERSAL_DEVICE_ID,
                        TARGET_PACKAGE
                );

        boolean deviceIdPrivacy =
                config.isEnabled(
                        "meituan",
                        "meituan.device_id_privacy"
                )
                        && !universalDeviceIdPrivacy;

        XposedBridge.log(
                TAG
                        + "子功能: file="
                        + filePrivacy
                        + ", ad="
                        + splashAd
                        + ", permission="
                        + permissionPrivacy
                        + ", deviceId="
                        + deviceIdPrivacy
        );

        if (filePrivacy) {
            hookFilePrivacy();
        }

        if (splashAd) {
            hookSplashAd();
        }

        if (permissionPrivacy) {
            hookPermissionPrivacy(
                    lpparam.classLoader
            );
        }

        if (deviceIdPrivacy) {
            hookDeviceIdPrivacy(
                    lpparam.classLoader
            );
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
        } catch (Throwable throwable) {
            XposedBridge.log(
                    TAG
                            + "文件隐私 Hook 失败: "
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
                                            if (scanAndClickSkip(
                                                    decorView
                                            )) {
                                                XposedBridge.log(
                                                        TAG
                                                                + "已处理美团开屏广告"
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
                                    "LOCATION"
                            )
                                    || permission.contains(
                                    "READ_PHONE_STATE"
                            )
                                    || permission.contains(
                                    "CONTACTS"
                            )
                                    || permission.contains(
                                    "CAMERA"
                            )
                                    || permission.contains(
                                    "READ_MEDIA"
                            )
                                    || permission.contains(
                                    "STORAGE"
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

    private void hookDeviceIdPrivacy(
            ClassLoader classLoader
    ) {
        try {
            XposedHelpers.findAndHookMethod(
                    "android.telephony.TelephonyManager",
                    classLoader,
                    "getDeviceId",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(
                                MethodHookParam param
                        ) {
                            param.setResult(
                                    "00000000000000"
                            );
                        }
                    }
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    TAG
                            + "Device ID Hook 失败: "
                            + throwable
            );
        }

        try {
            XposedHelpers.findAndHookMethod(
                    "android.telephony.TelephonyManager",
                    classLoader,
                    "getSubscriberId",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(
                                MethodHookParam param
                        ) {
                            param.setResult(null);
                        }
                    }
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    TAG
                            + "Subscriber ID Hook 失败: "
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

    private boolean scanAndClickSkip(
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
                } else if (scanAndClickSkip(
                        child
                )) {
                    return true;
                }
            }
        }

        return false;
    }
}
