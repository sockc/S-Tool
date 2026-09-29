package com.sockc.unicomhook;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

final class ScopeServiceBridge {

    enum RequestState {
        NONE,
        REQUESTING,
        APPROVED,
        DENIED,
        TIMEOUT,
        FAILED
    }

    interface ServiceListener {
        void onServiceStateChanged();
    }

    interface RequestListener {
        void onStateChanged(
                String packageName,
                RequestState state,
                String message
        );
    }

    private static final AtomicBoolean initialized =
            new AtomicBoolean(false);

    private static final Set<ServiceListener> listeners =
            new CopyOnWriteArraySet<>();

    private static final ConcurrentHashMap<String, RequestState>
            requestStates =
            new ConcurrentHashMap<>();

    private static volatile XposedService service;

    private static volatile Set<String> scopeSnapshot =
            Collections.emptySet();

    private ScopeServiceBridge() {
    }

    static void initialize() {
        if (!initialized.compareAndSet(
                false,
                true
        )) {
            return;
        }

        try {
            XposedServiceHelper.registerListener(
                    new XposedServiceHelper.OnServiceListener() {
                        @Override
                        public void onServiceBind(
                                XposedService boundService
                        ) {
                            service =
                                    boundService;
                            refreshScope();
                            notifyServiceListeners();
                        }

                        @Override
                        public void onServiceDied(
                                XposedService deadService
                        ) {
                            if (service
                                    == deadService) {
                                service =
                                        null;
                                scopeSnapshot =
                                        Collections.emptySet();
                                notifyServiceListeners();
                            }
                        }
                    }
            );
        } catch (Throwable ignored) {
        }
    }

    static boolean isConnected() {
        return service != null;
    }

    static void addListener(
            ServiceListener listener
    ) {
        if (listener == null) {
            return;
        }

        listeners.add(
                listener
        );
    }

    static void removeListener(
            ServiceListener listener
    ) {
        if (listener == null) {
            return;
        }

        listeners.remove(
                listener
        );
    }

    static Set<String> getScopeSnapshot() {
        return new HashSet<>(
                scopeSnapshot
        );
    }

    static boolean isInScope(
            String packageName
    ) {
        return packageName != null
                && scopeSnapshot.contains(
                packageName
        );
    }

    static RequestState getRequestState(
            String packageName
    ) {
        if (isInScope(
                packageName
        )) {
            return RequestState.APPROVED;
        }

        RequestState state =
                requestStates.get(
                        packageName
                );

        return state == null
                ? RequestState.NONE
                : state;
    }

    static void refreshScope() {
        XposedService current =
                service;

        if (current == null) {
            scopeSnapshot =
                    Collections.emptySet();
            return;
        }

        try {
            List<String> scope =
                    current.getScope();

            HashSet<String> values =
                    new HashSet<>(
                            scope
                    );

            scopeSnapshot =
                    Collections.unmodifiableSet(
                            values
                    );

            for (String packageName
                    : values) {
                requestStates.put(
                        packageName,
                        RequestState.APPROVED
                );
            }
        } catch (Throwable ignored) {
        }
    }

    static void requestScope(
            String packageName,
            RequestListener listener
    ) {
        if (packageName == null
                || packageName.trim().isEmpty()) {
            notifyRequest(
                    listener,
                    packageName,
                    RequestState.FAILED,
                    "包名为空"
            );
            return;
        }

        if (isInScope(
                packageName
        )) {
            notifyRequest(
                    listener,
                    packageName,
                    RequestState.APPROVED,
                    null
            );
            return;
        }

        RequestState currentState =
                requestStates.get(
                        packageName
                );

        if (currentState
                == RequestState.REQUESTING) {
            notifyRequest(
                    listener,
                    packageName,
                    RequestState.REQUESTING,
                    null
            );
            return;
        }

        XposedService current =
                service;

        if (current == null) {
            requestStates.put(
                    packageName,
                    RequestState.FAILED
            );

            notifyRequest(
                    listener,
                    packageName,
                    RequestState.FAILED,
                    "LSPosed Scope Service 未连接"
            );
            return;
        }

        requestStates.put(
                packageName,
                RequestState.REQUESTING
        );

        notifyRequest(
                listener,
                packageName,
                RequestState.REQUESTING,
                null
        );

        try {
            current.requestScope(
                    packageName,
                    new XposedService.OnScopeEventListener() {
                        @Override
                        public void onScopeRequestPrompted(
                                String requestedPackage
                        ) {
                            requestStates.put(
                                    requestedPackage,
                                    RequestState.REQUESTING
                            );

                            notifyRequest(
                                    listener,
                                    requestedPackage,
                                    RequestState.REQUESTING,
                                    null
                            );
                        }

                        @Override
                        public void onScopeRequestApproved(
                                String requestedPackage
                        ) {
                            requestStates.put(
                                    requestedPackage,
                                    RequestState.APPROVED
                            );

                            refreshScope();

                            notifyRequest(
                                    listener,
                                    requestedPackage,
                                    RequestState.APPROVED,
                                    null
                            );

                            notifyServiceListeners();
                        }

                        @Override
                        public void onScopeRequestDenied(
                                String requestedPackage
                        ) {
                            requestStates.put(
                                    requestedPackage,
                                    RequestState.DENIED
                            );

                            notifyRequest(
                                    listener,
                                    requestedPackage,
                                    RequestState.DENIED,
                                    "作用域请求被拒绝或已设为不再询问"
                            );
                        }

                        @Override
                        public void onScopeRequestTimeout(
                                String requestedPackage
                        ) {
                            requestStates.put(
                                    requestedPackage,
                                    RequestState.TIMEOUT
                            );

                            notifyRequest(
                                    listener,
                                    requestedPackage,
                                    RequestState.TIMEOUT,
                                    "作用域请求已超时"
                            );
                        }

                        @Override
                        public void onScopeRequestFailed(
                                String requestedPackage,
                                String message
                        ) {
                            requestStates.put(
                                    requestedPackage,
                                    RequestState.FAILED
                            );

                            notifyRequest(
                                    listener,
                                    requestedPackage,
                                    RequestState.FAILED,
                                    message
                            );
                        }
                    }
            );
        } catch (Throwable throwable) {
            requestStates.put(
                    packageName,
                    RequestState.FAILED
            );

            notifyRequest(
                    listener,
                    packageName,
                    RequestState.FAILED,
                    throwable.getMessage()
            );
        }
    }

    private static void notifyRequest(
            RequestListener listener,
            String packageName,
            RequestState state,
            String message
    ) {
        if (listener != null) {
            listener.onStateChanged(
                    packageName,
                    state,
                    message
            );
        }
    }

    private static void notifyServiceListeners() {
        for (ServiceListener listener
                : listeners) {
            try {
                listener.onServiceStateChanged();
            } catch (Throwable ignored) {
            }
        }
    }
}
