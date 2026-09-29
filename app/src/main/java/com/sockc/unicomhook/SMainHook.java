package com.sockc.unicomhook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class SMainHook implements IXposedHookLoadPackage {

    private static final String TAG = "S-Tool/Main: ";

    private static final class HookEntry {
        final String featureId;
        final IXposedHookLoadPackage delegate;

        HookEntry(
                String featureId,
                IXposedHookLoadPackage delegate
        ) {
            this.featureId = featureId;
            this.delegate = delegate;
        }
    }

    private final HookEntry[] entries = new HookEntry[] {
            new HookEntry("unicom", new UnicomHook()),
            new HookEntry("gaode", new GaodeHook()),
            new HookEntry("taobao", new TaobaoHook()),
            new HookEntry("tiktok", new TiktokHook()),
            new HookEntry("qq", new QQHook()),
            new HookEntry("hikvision", new HikvisionHook()),
            new HookEntry("guazi", new GuaziHook()),
            new HookEntry("che300", new Che300Hook()),
            new HookEntry("firsty", new FirstyHook()),
            new HookEntry(
                    "screenshot_privacy",
                    new ScreenshotPrivacyHook()
            ),
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
            if (!config.isEnabled(entry.featureId)) {
                continue;
            }

            try {
                entry.delegate.handleLoadPackage(lpparam);
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
