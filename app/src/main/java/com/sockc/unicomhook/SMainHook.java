package com.sockc.unicomhook;

import android.app.Application;
import android.content.Context;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class SMainHook implements IXposedHookLoadPackage {

    private static final String TAG =
            "S-Tool/Main: ";

    private final Set<String> registeredProcesses =
            Collections.synchronizedSet(
                    new HashSet<>()
            );

    private static final class HookEntry {
        final String featureId;
        final String requiredSubFeatureId;
        final String delegateClassName;

        HookEntry(
                String featureId,
                String delegateClassName
        ) {
            this(
                    featureId,
                    null,
                    delegateClassName
            );
        }

        HookEntry(
                String featureId,
                String requiredSubFeatureId,
                String delegateClassName
        ) {
            this.featureId = featureId;
            this.requiredSubFeatureId =
                    requiredSubFeatureId;
            this.delegateClassName =
                    delegateClassName;
        }
    }

    private final HookEntry[] entries =
            new HookEntry[] {
                    new HookEntry(
                            FeatureRegistry.UNIVERSAL_PRIVACY,
                            "com.sockc.unicomhook.UniversalPrivacyHook"
                    ),
                    new HookEntry(
                            "unicom",
                            "com.sockc.unicomhook.UnicomHook"
                    ),
                    new HookEntry(
                            "unicom",
                            "unicom.screenshot_privacy",
                            "com.sockc.unicomhook.ScreenshotPrivacyHook"
                    ),
                    new HookEntry(
                            "gaode",
                            "com.sockc.unicomhook.GaodeHook"
                    ),
                    new HookEntry(
                            "taobao",
                            "com.sockc.unicomhook.TaobaoHook"
                    ),
                    new HookEntry(
                            "tiktok",
                            "com.sockc.unicomhook.TiktokHook"
                    ),
                    new HookEntry(
                            "qq",
                            "com.sockc.unicomhook.QQHook"
                    ),
                    new HookEntry(
                            "hikvision",
                            "com.sockc.unicomhook.HikvisionHook"
                    ),
                    new HookEntry(
                            "guazi",
                            "com.sockc.unicomhook.GuaziHook"
                    ),
                    new HookEntry(
                            "che300",
                            "com.sockc.unicomhook.Che300Hook"
                    ),
                    new HookEntry(
                            "firsty",
                            "com.sockc.unicomhook.FirstyHook"
                    ),
                    new HookEntry(
                            "meituan",
                            "com.sockc.unicomhook.MeituanHook"
                    ),
                    new HookEntry(
                            "pinduoduo",
                            "com.sockc.unicomhook.PinduoduoHook"
                    ),
                    new HookEntry(
                            "clipboard",
                            "com.sockc.unicomhook.ClipboardHook"
                    ),
                    new HookEntry(
                            "zhihuijia",
                            "com.sockc.unicomhook.ZhihuijiaHook"
                    ),
                    new HookEntry(
                            "xianyu",
                            "com.sockc.unicomhook.XianyuHook"
                    ),
                    new HookEntry(
                            "sms_code",
                            "com.sockc.unicomhook.SmsCodeHook"
                    ),
                    new HookEntry(
                            "yingyongbao",
                            "com.sockc.unicomhook.YingyongbaoHook"
                    ),
                    new HookEntry(
                            "oplus_game",
                            "com.sockc.unicomhook.OplusGameHook"
                    ),
                    new HookEntry(
                            "airvoy",
                            "com.sockc.unicomhook.AirvoyHook"
                    )
            };

    @Override
    public void handleLoadPackage(
            XC_LoadPackage.LoadPackageParam lpparam
    ) {
        XposedBridge.log(
                TAG
                        + "handleLoadPackage package="
                        + lpparam.packageName
                        + " process="
                        + lpparam.processName
        );

        String processKey =
                lpparam.processName != null
                        ? lpparam.processName
                        : lpparam.packageName;

        if (!registeredProcesses.add(
                processKey
        )) {
            return;
        }

        final boolean[] dispatched =
                {false};

        try {
            XposedHelpers.findAndHookMethod(
                    Application.class,
                    "attach",
                    Context.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(
                                MethodHookParam param
                        ) {
                            if (dispatched[0]) {
                                return;
                            }

                            dispatched[0] = true;

                            Context context =
                                    (Context) param.args[0];

                            XposedBridge.log(
                                    TAG
                                            + "Application.attach package="
                                            + lpparam.packageName
                                            + " process="
                                            + lpparam.processName
                            );

                            boolean reported =
                                    ConfigProvider
                                            .reportInjection(
                                                    context,
                                                    lpparam.packageName,
                                                    lpparam.processName
                                            );

                            XposedBridge.log(
                                    TAG
                                            + "注入状态回报="
                                            + reported
                                            + " package="
                                            + lpparam.packageName
                            );

                            HookConfig config =
                                    HookConfig.load(
                                            context
                                    );

                            dispatchHooks(
                                    lpparam,
                                    config,
                                    context
                            );
                        }
                    }
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    TAG
                            + "Application.attach Hook 失败，立即使用默认配置分发: "
                            + throwable
            );

            dispatchHooks(
                    lpparam,
                    HookConfig.load(),
                    null
            );
        }
    }

    private void dispatchHooks(
            XC_LoadPackage.LoadPackageParam lpparam,
            HookConfig config,
            Context context
    ) {
        for (HookEntry entry : entries) {
            if (!FeatureRegistry.appliesToPackage(
                    entry.featureId,
                    lpparam.packageName
            )) {
                continue;
            }

            boolean enabled =
                    entry.requiredSubFeatureId == null
                            ? config.isEnabled(
                            entry.featureId
                    )
                            : config.isEnabled(
                            entry.featureId,
                            entry.requiredSubFeatureId
                    );

            if (!enabled) {
                continue;
            }

            try {
                Class<?> hookClass =
                        XposedHelpers.findClass(
                                entry.delegateClassName,
                                SMainHook.class
                                        .getClassLoader()
                        );

                Object hook =
                        hookClass.newInstance();

                if (!(hook
                        instanceof IXposedHookLoadPackage)) {
                    throw new IllegalStateException(
                            entry.delegateClassName
                                    + " 未实现 IXposedHookLoadPackage"
                    );
                }

                ((IXposedHookLoadPackage) hook)
                        .handleLoadPackage(
                                lpparam
                        );

                ConfigProvider.reportHookResult(
                        context,
                        lpparam.packageName,
                        entry.featureId,
                        true,
                        null
                );
            } catch (Throwable throwable) {
                String summary =
                        buildErrorSummary(
                                throwable
                        );

                XposedBridge.log(
                        TAG
                                + "Hook 初始化/执行失败 "
                                + entry.delegateClassName
                                + " for "
                                + lpparam.packageName
                                + ": "
                                + summary
                );

                ConfigProvider.reportHookResult(
                        context,
                        lpparam.packageName,
                        entry.featureId,
                        false,
                        summary
                );
            }
        }
    }

    private String buildErrorSummary(
            Throwable throwable
    ) {
        if (throwable == null) {
            return "未知异常";
        }

        String name =
                throwable.getClass()
                        .getSimpleName();
        String message =
                throwable.getMessage();

        if (message == null
                || message.trim().isEmpty()) {
            return name;
        }

        return name
                + ": "
                + message;
    }
}
