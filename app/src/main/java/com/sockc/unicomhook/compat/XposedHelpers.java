package com.sockc.unicomhook.compat;

import java.lang.reflect.Method;

public final class XposedHelpers {

    private XposedHelpers() {
    }

    public static final class ClassNotFoundError
            extends Error {
        public ClassNotFoundError(
                String className,
                Throwable cause
        ) {
            super(
                    className,
                    cause
            );
        }
    }

    public static Class<?> findClass(
            String className,
            ClassLoader classLoader
    ) {
        try {
            return Class.forName(
                    className,
                    false,
                    classLoader
            );
        } catch (ClassNotFoundException exception) {
            throw new ClassNotFoundError(
                    className,
                    exception
            );
        }
    }

    public static Class<?> findClassIfExists(
            String className,
            ClassLoader classLoader
    ) {
        try {
            return Class.forName(
                    className,
                    false,
                    classLoader
            );
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    public static XC_MethodHook.Unhook findAndHookMethod(
            String className,
            ClassLoader classLoader,
            String methodName,
            Object... parameterTypesAndCallback
    ) {
        return findAndHookMethod(
                findClass(
                        className,
                        classLoader
                ),
                methodName,
                parameterTypesAndCallback
        );
    }

    public static XC_MethodHook.Unhook findAndHookMethod(
            Class<?> hookClass,
            String methodName,
            Object... parameterTypesAndCallback
    ) {
        ParsedHookArgs parsed =
                parseHookArgs(
                        hookClass.getClassLoader(),
                        parameterTypesAndCallback
                );

        Method method =
                findMethodExact(
                        hookClass,
                        methodName,
                        parsed.parameterTypes
                );

        return XposedBridge.hookMethod(
                method,
                parsed.callback
        );
    }

    private static Method findMethodExact(
            Class<?> hookClass,
            String methodName,
            Class<?>[] parameterTypes
    ) {
        Class<?> current =
                hookClass;

        while (current != null) {
            try {
                Method method =
                        current.getDeclaredMethod(
                                methodName,
                                parameterTypes
                        );
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                current =
                        current.getSuperclass();
            }
        }

        throw new NoSuchMethodError(
                hookClass.getName()
                        + "#"
                        + methodName
        );
    }

    private static ParsedHookArgs parseHookArgs(
            ClassLoader classLoader,
            Object... values
    ) {
        if (values == null
                || values.length == 0
                || !(values[values.length - 1]
                instanceof XC_MethodHook)) {
            throw new IllegalArgumentException(
                    "Last argument must be XC_MethodHook"
            );
        }

        XC_MethodHook callback =
                (XC_MethodHook)
                        values[values.length - 1];

        Class<?>[] parameterTypes =
                new Class<?>[
                        values.length - 1
                        ];

        for (int i = 0;
                i < parameterTypes.length;
                i++) {
            Object type =
                    values[i];

            if (type instanceof Class<?>) {
                parameterTypes[i] =
                        (Class<?>) type;
            } else if (type instanceof String) {
                parameterTypes[i] =
                        findClass(
                                (String) type,
                                classLoader
                        );
            } else {
                throw new IllegalArgumentException(
                        "Unsupported parameter type at "
                                + i
                );
            }
        }

        return new ParsedHookArgs(
                parameterTypes,
                callback
        );
    }

    private static final class ParsedHookArgs {
        final Class<?>[] parameterTypes;
        final XC_MethodHook callback;

        ParsedHookArgs(
                Class<?>[] parameterTypes,
                XC_MethodHook callback
        ) {
            this.parameterTypes =
                    parameterTypes;
            this.callback =
                    callback;
        }
    }
}
