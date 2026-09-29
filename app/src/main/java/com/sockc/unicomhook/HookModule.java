package com.sockc.unicomhook;

interface HookModule {
    void handleLoadPackage(
            LoadPackageParam lpparam
    ) throws Throwable;
}
