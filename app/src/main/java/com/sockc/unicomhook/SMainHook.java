package com.sockc.unicomhook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class SMainHook implements IXposedHookLoadPackage {

    private static final String TAG = "S-Tool/Main: ";

    private static final class HookEntry {
        final String featureId;
        final String requiredSubFeatureId;
        final IXposedHookLoadPackage delegate;

        HookEntry(
                String featureId,
                IXposedHookLoadPackage delegate
        ) {
            this(featureId, null, delegate);
        }

        HookEntry(
                String featureId,
                String requiredSubFeatureId,
                IXposedHookLoadPackage delegate
        ) {
            this.featureId = featureId;
            this.requiredSubFeatureId = requiredSubFeatureId;
            this.delegate = delegate;
        }
    }

    private final HookEntry[] entries = new HookEntry[] {
            new HookEntry(
                    "universal_privacy",
                    new UniversalPrivacyHook()
            ),
            new HookEntry("unicom", new UnicomHook()),
            new HookEntry(
                    "unicom",
                    "unicom.screenshot_privacy",
                    new ScreenshotPrivacyHook()
            ),
            new HookEntry("gaode", new GaodeHook()),
            new HookEntry("taobao", new TaobaoHook()),
            new HookEntry("tiktok", new TiktokHook()),
            new HookEntry("qq", new QQHook()),
            new HookEntry("hikvision", new HikvisionHook()),
            new HookEntry("guazi", new GuaziHook()),
            new HookEntry("che300", new Che300Hook()),
            new HookEntry("firsty", new FirstyHook()),
            new HookEntry("meituan", new MeituanHook()),
            new HookEntry("pinduoduo", new PinduoduoHook()),
            new HookEntry("clipboard", new ClipboardHook()),
            new HookEntry("zhihuijia", new ZhihuijiaHook()),
            new HookEntry("xianyu", new XianyuHook()),
            new HookEntry("sms_code", new SmsCodeHook()),
            new HookEntry("yingyongbao", new YingyongbaoHook()),
            new HookEntry("oplus_game", new OplusGameHook()),
            new HookEntry("airvoy", new AirvoyHook())
    };

    @Override
    public void handleLoadPackage(
            XC_LoadPackage.LoadPackageParam lpparam
    ) {
        HookConfig config = HookConfig.load();

        for (HookEntry entry : entries) {
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
                entry.delegate.handleLoadPackage(
                        lpparam
                );
            } catch (Throwable throwable) {
                XposedBridge.log(
                        TAG
                                + entry.delegate
                                .getClass()
                                .getSimpleName()
                                + " failed for "
                                + lpparam.packageName
                                + ": "
                                + throwable
                );
            }
        }
    }
}
