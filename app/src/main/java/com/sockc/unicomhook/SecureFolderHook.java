package com.sockc.securefolderprobe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class SecureFolderProbe implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_SF_Probe";

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        // 只看系统侧，避免把所有 APP 都刷爆日志
        if (!"android".equals(lpparam.packageName)) {
            return;
        }

        log("==================================================");
        log("Injected into package=" + lpparam.packageName
                + " process=" + lpparam.processName
                + " classLoader=" + lpparam.classLoader);
        log("==================================================");

        tryHookAttestationUtils(lpparam);
        tryHookSamsungAttestParameterSpec(lpparam);
        tryHookDarManagerService(lpparam);
        tryHookWeirdGenerateKeyPairClasses(lpparam);
    }

    private void tryHookAttestationUtils(final LoadPackageParam lpparam) {
        String[] candidates = new String[] {
                "com.samsung.android.security.keystore.AttestationUtils",
                "android.security.keystore.AttestationUtils"
        };

        for (String className : candidates) {
            try {
                Class<?> cls = XposedHelpers.findClassIfExists(className, lpparam.classLoader);
                if (cls == null) {
                    log("Class not found: " + className);
                    continue;
                }

                log("Found class: " + className);
                dumpDeclaredMethods(cls);

                for (Method m : cls.getDeclaredMethods()) {
                    if ("generateKeyPair".equals(m.getName())) {
                        log("Hooking method: " + methodToString(m));
                        XposedBridge.hookMethod(m, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                log("[BEFORE] AttestationUtils.generateKeyPair");
                                dumpMethodCall(param);
                                dumpStack("Stack before AttestationUtils.generateKeyPair");
                            }

                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                log("[AFTER] AttestationUtils.generateKeyPair");
                                if (param.hasThrowable()) {
                                    log("Throwable: " + android.util.Log.getStackTraceString(param.getThrowable()));
                                } else {
                                    Object result = param.getResult();
                                    log("Result type: " + safeClassName(result));
                                    log("Result value: " + String.valueOf(result));
                                }
                            }
                        });
                    }
                }
            } catch (Throwable t) {
                log("tryHookAttestationUtils error for " + className + ": "
                        + android.util.Log.getStackTraceString(t));
            }
        }
    }

    private void tryHookSamsungAttestParameterSpec(final LoadPackageParam lpparam) {
        String[] candidates = new String[] {
                "com.samsung.android.security.keystore.AttestParameterSpec",
                "android.security.keystore.KeyGenParameterSpec"
        };

        for (String className : candidates) {
            try {
                Class<?> cls = XposedHelpers.findClassIfExists(className, lpparam.classLoader);
                if (cls == null) {
                    log("Class not found: " + className);
                    continue;
                }

                log("Found class: " + className);
                dumpDeclaredFields(cls);
                dumpDeclaredMethods(cls);

                for (Constructor<?> c : cls.getDeclaredConstructors()) {
                    log("Hooking constructor: " + c.toString());
                    XposedBridge.hookMethod(c, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            log("[BEFORE] Constructor " + cls.getName());
                            dumpConstructorCall(param, cls);
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            log("[AFTER] Constructor " + cls.getName());
                            if (param.hasThrowable()) {
                                log("Constructor throwable: " + android.util.Log.getStackTraceString(param.getThrowable()));
                            } else {
                                log("Constructed object type: " + safeClassName(param.thisObject));
                                log("Constructed object: " + String.valueOf(param.thisObject));
                            }
                        }
                    });
                }
            } catch (Throwable t) {
                log("tryHookSamsungAttestParameterSpec error for " + className + ": "
                        + android.util.Log.getStackTraceString(t));
            }
        }
    }

    private void tryHookDarManagerService(final LoadPackageParam lpparam) {
        String className = "com.android.server.knox.dar.DarManagerService";
        try {
            Class<?> cls = XposedHelpers.findClassIfExists(className, lpparam.classLoader);
            if (cls == null) {
                log("Class not found: " + className);
                return;
            }

            log("Found class: " + className);
            dumpDeclaredMethods(cls);

            for (Method m : cls.getDeclaredMethods()) {
                String name = m.getName();
                if ("isKnoxKeyInstallable".equals(name)
                        || name.toLowerCase().contains("knox")
                        || name.toLowerCase().contains("key")
                        || name.toLowerCase().contains("attest")) {

                    log("Hooking DAR method: " + methodToString(m));
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            log("[BEFORE] DAR method: " + ((Method) param.method).getName());
                            dumpMethodCall(param);
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            log("[AFTER] DAR method: " + ((Method) param.method).getName());
                            if (param.hasThrowable()) {
                                log("Throwable: " + android.util.Log.getStackTraceString(param.getThrowable()));
                            } else {
                                log("Result type: " + safeClassName(param.getResult()));
                                log("Result value: " + String.valueOf(param.getResult()));
                            }
                        }
                    });
                }
            }
        } catch (Throwable t) {
            log("tryHookDarManagerService error: " + android.util.Log.getStackTraceString(t));
        }
    }

    private void tryHookWeirdGenerateKeyPairClasses(final LoadPackageParam lpparam) {
        String[] weirdCandidates = new String[] {
                "android.os.CraersshCient",
                "android.os.CraersshClient",
                "android.os.CraershCient",
                "android.os.CraershClient"
        };

        for (String className : weirdCandidates) {
            try {
                Class<?> cls = XposedHelpers.findClassIfExists(className, lpparam.classLoader);
                if (cls == null) {
                    log("Weird class not found: " + className);
                    continue;
                }

                log("Found weird class: " + className);
                dumpDeclaredMethods(cls);

                for (Method m : cls.getDeclaredMethods()) {
                    if ("generateKeyPair".equals(m.getName())) {
                        log("Hooking weird generateKeyPair: " + methodToString(m));
                        XposedBridge.hookMethod(m, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                log("[BEFORE] Weird generateKeyPair");
                                dumpMethodCall(param);
                                dumpStack("Stack before weird generateKeyPair");
                            }

                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                log("[AFTER] Weird generateKeyPair");
                                if (param.hasThrowable()) {
                                    log("Throwable: " + android.util.Log.getStackTraceString(param.getThrowable()));
                                } else {
                                    log("Result type: " + safeClassName(param.getResult()));
                                    log("Result value: " + String.valueOf(param.getResult()));
                                }
                            }
                        });
                    }
                }
            } catch (Throwable t) {
                log("tryHookWeirdGenerateKeyPairClasses error for " + className + ": "
                        + android.util.Log.getStackTraceString(t));
            }
        }
    }

    private void dumpMethodCall(XC_MethodHook.MethodHookParam param) {
        try {
            Method method = (Method) param.method;
            log("Method: " + methodToString(method));
            log("thisObject class: " + safeClassName(param.thisObject));
            log("args count: " + (param.args == null ? 0 : param.args.length));

            if (param.args != null) {
                for (int i = 0; i < param.args.length; i++) {
                    Object arg = param.args[i];
                    log("arg[" + i + "] class = " + safeClassName(arg));
                    log("arg[" + i + "] value = " + String.valueOf(arg));
                }
            }
        } catch (Throwable t) {
            log("dumpMethodCall error: " + android.util.Log.getStackTraceString(t));
        }
    }

    private void dumpConstructorCall(XC_MethodHook.MethodHookParam param, Class<?> cls) {
        try {
            log("Constructor class: " + cls.getName());
            log("args count: " + (param.args == null ? 0 : param.args.length));

            if (param.args != null) {
                for (int i = 0; i < param.args.length; i++) {
                    Object arg = param.args[i];
                    log("ctor arg[" + i + "] class = " + safeClassName(arg));
                    log("ctor arg[" + i + "] value = " + String.valueOf(arg));
                }
            }
        } catch (Throwable t) {
            log("dumpConstructorCall error: " + android.util.Log.getStackTraceString(t));
        }
    }

    private void dumpDeclaredMethods(Class<?> cls) {
        try {
            for (Method m : cls.getDeclaredMethods()) {
                log("Declared method => " + methodToString(m));
            }
        } catch (Throwable t) {
            log("dumpDeclaredMethods error on " + cls.getName() + ": "
                    + android.util.Log.getStackTraceString(t));
        }
    }

    private void dumpDeclaredFields(Class<?> cls) {
        try {
            Arrays.stream(cls.getDeclaredFields()).forEach(f ->
                    log("Declared field => " + f.getType().getName() + " " + f.getName()));
        } catch (Throwable t) {
            log("dumpDeclaredFields error on " + cls.getName() + ": "
                    + android.util.Log.getStackTraceString(t));
        }
    }

    private void dumpStack(String title) {
        try {
            Throwable t = new Throwable(title);
            log(android.util.Log.getStackTraceString(t));
        } catch (Throwable ignored) {
        }
    }

    private String methodToString(Method m) {
        StringBuilder sb = new StringBuilder();
        sb.append(m.getReturnType().getName())
          .append(" ")
          .append(m.getDeclaringClass().getName())
          .append(".")
          .append(m.getName())
          .append("(");

        Class<?>[] params = m.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(params[i].getName());
        }
        sb.append(")");
        return sb.toString();
    }

    private String safeClassName(Object obj) {
        return obj == null ? "null" : obj.getClass().getName();
    }

    private void log(String msg) {
        XposedBridge.log(TAG + " | " + msg);
    }
}
