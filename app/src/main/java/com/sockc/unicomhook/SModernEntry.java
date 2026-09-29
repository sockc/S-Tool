package com.sockc.unicomhook;

import com.sockc.unicomhook.compat.XposedBridge;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public final class SModernEntry
        extends XposedModule {

    private final SMainHook dispatcher =
            new SMainHook();

    private volatile String processName;

    @Override
    public void onModuleLoaded(
            XposedModuleInterface.ModuleLoadedParam param
    ) {
        XposedBridge.init(this);
        processName =
                param.getProcessName();

        XposedBridge.log(
                "S-Tool/Modern: module loaded process="
                        + processName
        );
    }

    @Override
    public void onPackageReady(
            XposedModuleInterface.PackageReadyParam param
    ) {
        if (!param.isFirstPackage()) {
            return;
        }

        XposedBridge.init(this);

        String packageName =
                param.getPackageName();

        String currentProcess =
                processName != null
                        ? processName
                        : packageName;

        try {
            dispatcher.handleLoadPackage(
                    new LoadPackageParam(
                            packageName,
                            currentProcess,
                            param.getClassLoader()
                    )
            );
        } catch (Throwable throwable) {
            XposedBridge.log(
                    "S-Tool/Modern: dispatch failed "
                            + packageName
                            + ": "
                            + throwable
            );
        }
    }
}
