package com.sockc.unicomhook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Single Xposed entry point for S Tool.
 *
 * Individual feature classes remain isolated so a failure in one module
 * cannot prevent the remaining modules from loading.
 */
public final class SMainHook implements IXposedHookLoadPackage {

    private static final String TAG = "S-Tool/Main: ";

    private final IXposedHookLoadPackage[] delegates = new IXposedHookLoadPackage[] {
            new UnicomHook(),
            new GaodeHook(),
            new TaobaoHook(),
            new TiktokHook(),
            new QQHook(),
            new HikvisionHook(),
            new GuaziHook(),
            new Che300Hook(),
            new FirstyHook(),
            new ScreenshotPrivacyHook(),
            new MeituanHook(),
            new PinduoduoHook(),
            new ClipboardHook(),
            new ZhihuijiaHook(),
            new XianyuHook(),
            new SmsCodeHook(),
            new YingyongbaoHook(),
            new OplusGameHook(),
            new AirvoyHook()
    };

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        for (IXposedHookLoadPackage delegate : delegates) {
            try {
                delegate.handleLoadPackage(lpparam);
            } catch (Throwable throwable) {
                XposedBridge.log(TAG
                        + delegate.getClass().getSimpleName()
                        + " failed for "
                        + lpparam.packageName
                        + ": "
                        + throwable);
            }
        }
    }
}
