package com.sockc.unicomhook;

final class LoadPackageParam {
    final String packageName;
    final String processName;
    final ClassLoader classLoader;

    LoadPackageParam(
            String packageName,
            String processName,
            ClassLoader classLoader
    ) {
        this.packageName = packageName;
        this.processName = processName;
        this.classLoader = classLoader;
    }
}
