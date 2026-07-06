$code = @'
package com.sockc.unicomhook;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class SystemUiDualSimHook implements IXposedHookLoadPackage {
    private static final String TAG = "SystemUiDualSimHook";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (!"com.android.systemui".equals(lpparam.packageName)) return;

        log("loaded " + lpparam.packageName);

        XposedBridge.hookAllMethods(Application.class, "attach", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Context ctx = (Context) param.args[0];
                log("attach ok, package=" + ctx.getPackageName());
            }
        });
    }

    private static void log(String msg) {
        XposedBridge.log(TAG + ": " + msg);
        Log.i(TAG, msg);
    }
}
'@

New-Item -ItemType Directory -Force .\app\src\main\java\com\sockc\unicomhook | Out-Null
Set-Content -Path .\app\src\main\java\com\sockc\unicomhook\SystemUiDualSimHook.java -Value $code -Encoding UTF8
