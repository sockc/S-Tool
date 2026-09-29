package com.sockc.unicomhook.compat;

import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.api.XposedInterface;

public final class XposedBridge {

    private static final String TAG =
            "S-Tool/Xposed";

    private static volatile XposedInterface xposedInterface;

    private static final Map<Member, List<XC_MethodHook>> HOOKS =
            new ConcurrentHashMap<>();

    private XposedBridge() {
    }

    public static void init(
            XposedInterface xposed
    ) {
        xposedInterface = xposed;
    }

    public static void log(
            String text
    ) {
        Log.i(
                TAG,
                text == null ? "" : text
        );
    }

    public static XC_MethodHook.Unhook hookMethod(
            Member member,
            XC_MethodHook callback
    ) {
        XposedInterface xposed =
                requireInterface();

        if (!(member instanceof Method)
                && !(member instanceof Constructor<?>)) {
            throw new IllegalArgumentException(
                    "Only Method/Constructor can be hooked"
            );
        }

        HOOKS.computeIfAbsent(
                member,
                key -> new ArrayList<>()
        ).add(
                callback
        );

        XposedInterface.HookHandle handle =
                xposed.hook(
                        (java.lang.reflect.Executable) member
                ).intercept(
                        chain -> {
                            XC_MethodHook.MethodHookParam param =
                                    new XC_MethodHook.MethodHookParam();

                            param.method =
                                    (Member) chain.getExecutable();
                            param.thisObject =
                                    chain.getThisObject();
                            param.args =
                                    chain.getArgs()
                                            .toArray(
                                                    new Object[0]
                                            );

                            List<XC_MethodHook> callbacks =
                                    HOOKS.get(
                                            member
                                    );

                            if (callbacks == null
                                    || callbacks.isEmpty()) {
                                return chain.proceed(
                                        param.args
                                );
                            }

                            int beforeCount = 0;

                            for (XC_MethodHook hook
                                    : new ArrayList<>(
                                    callbacks
                            )) {
                                hook.beforeHookedMethod(
                                        param
                                );
                                beforeCount++;

                                if (param.isReturnEarly()) {
                                    break;
                                }
                            }

                            if (!param.isReturnEarly()) {
                                try {
                                    param.setResult(
                                            chain.proceed(
                                                    param.args
                                            )
                                    );
                                } catch (Throwable throwable) {
                                    param.setThrowable(
                                            throwable
                                    );
                                }

                                param.resetReturnEarly();
                            }

                            List<XC_MethodHook> afterCallbacks =
                                    new ArrayList<>(
                                            callbacks
                                    );

                            int last =
                                    Math.min(
                                            beforeCount,
                                            afterCallbacks.size()
                                    ) - 1;

                            for (int i = last;
                                    i >= 0;
                                    i--) {
                                afterCallbacks.get(i)
                                        .afterHookedMethod(
                                                param
                                        );
                            }

                            if (param.hasThrowable()) {
                                throw param.getThrowable();
                            }

                            return param.getResult();
                        }
                );

        return callback.new Unhook(
                new HookHandleWrapper(
                        member,
                        callback,
                        handle
                )
        );
    }

    public static Set<XC_MethodHook.Unhook> hookAllMethods(
            Class<?> hookClass,
            String methodName,
            XC_MethodHook callback
    ) {
        Set<XC_MethodHook.Unhook> unhooks =
                new HashSet<>();

        for (Method method
                : hookClass.getDeclaredMethods()) {
            if (!methodName.equals(
                    method.getName()
            )) {
                continue;
            }

            try {
                method.setAccessible(true);
            } catch (Throwable ignored) {
            }

            unhooks.add(
                    hookMethod(
                            method,
                            callback
                    )
            );
        }

        if (unhooks.isEmpty()) {
            throw new NoSuchMethodError(
                    hookClass.getName()
                            + "#"
                            + methodName
            );
        }

        return unhooks;
    }

    public static Object invokeOriginalMethod(
            Member method,
            Object thisObject,
            Object[] args
    ) throws Throwable {
        XposedInterface xposed =
                requireInterface();

        if (method instanceof Method) {
            Method target =
                    (Method) method;

            XposedInterface.Invoker<?, Method> invoker =
                    xposed.getInvoker(
                            target
                    );

            invoker.setType(
                    XposedInterface.Invoker.Type.ORIGIN
            );

            try {
                return invoker.invoke(
                        thisObject,
                        args
                );
            } catch (InvocationTargetException exception) {
                throw exception.getCause();
            }
        }

        if (method instanceof Constructor<?>) {
            Constructor<?> target =
                    (Constructor<?>) method;

            XposedInterface.CtorInvoker<?> invoker =
                    xposed.getInvoker(
                            target
                    );

            try {
                return invoker.newInstance(
                        args
                );
            } catch (InvocationTargetException exception) {
                throw exception.getCause();
            }
        }

        throw new IllegalArgumentException(
                "Unsupported member type: "
                        + method
        );
    }

    private static XposedInterface requireInterface() {
        XposedInterface value =
                xposedInterface;

        if (value == null) {
            throw new IllegalStateException(
                    "libxposed interface not initialized"
            );
        }

        return value;
    }

    static final class HookHandleWrapper {
        private final Member member;
        private final XC_MethodHook callback;
        private final XposedInterface.HookHandle handle;

        HookHandleWrapper(
                Member member,
                XC_MethodHook callback,
                XposedInterface.HookHandle handle
        ) {
            this.member = member;
            this.callback = callback;
            this.handle = handle;
        }

        void unhook() {
            List<XC_MethodHook> list =
                    HOOKS.get(
                            member
                    );

            if (list != null) {
                list.remove(
                        callback
                );
            }

            handle.unhook();
        }
    }
}
