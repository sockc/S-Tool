package com.sockc.unicomhook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
        if (listener != null) {
            listeners.add(
                    listener
            );
        }
    }

    static void removeListener(
            ServiceListener listener
    ) {
        if (listener != null) {
            listeners.remove(
                    listener
            );
        }
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

        requestScopes(
                Collections.singletonList(
                        packageName
                ),
                listener
        );
    }

    static void requestScopes(
            List<String> packageNames,
            RequestListener listener
    ) {
        LinkedHashSet<String> pending =
                new LinkedHashSet<>();

        if (packageNames != null) {
            for (String packageName
                    : packageNames) {
                if (packageName == null
                        || packageName.trim().isEmpty()
                        || isInScope(
                        packageName
                )) {
                    continue;
                }

                pending.add(
                        packageName
                );
            }
        }

        if (pending.isEmpty()) {
            if (packageNames != null) {
                for (String packageName
                        : packageNames) {
                    if (packageName != null
                            && isInScope(
                            packageName
                    )) {
                        notifyRequest(
                                listener,
                                packageName,
                                RequestState.APPROVED,
                                null
                        );
                    }
                }
            }
            return;
        }

        XposedService current =
                service;

        if (current == null) {
            for (String packageName
                    : pending) {
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
            }
            return;
        }

        for (String packageName
                : pending) {
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
        }

        List<String> requested =
                new ArrayList<>(
                        pending
                );

        try {
            current.requestScope(
                    requested,
                    new XposedService.OnScopeEventListener() {
                        @Override
                        public void onScopeRequestApproved(
                                List<String> approved
                        ) {
                            Set<String> approvedSet =
                                    approved == null
                                            ? Collections.emptySet()
                                            : new HashSet<>(
                                            approved
                                    );

                            refreshScope();

                            for (String packageName
                                    : requested) {
                                boolean accepted =
                                        approvedSet.contains(
                                                packageName
                                        )
                                                || isInScope(
                                                packageName
                                        );

                                RequestState state =
                                        accepted
                                                ? RequestState.APPROVED
                                                : RequestState.DENIED;

                                requestStates.put(
                                        packageName,
                                        state
                                );

                                notifyRequest(
                                        listener,
                                        packageName,
                                        state,
                                        accepted
                                                ? null
                                                : "作用域未获批准；S Tool 选择已保留"
                                );
                            }

                            notifyServiceListeners();
                        }

                        @Override
                        public void onScopeRequestFailed(
                                String message
                        ) {
                            for (String packageName
                                    : requested) {
                                requestStates.put(
                                        packageName,
                                        RequestState.FAILED
                                );

                                notifyRequest(
                                        listener,
                                        packageName,
                                        RequestState.FAILED,
                                        message
                                );
                            }
                        }
                    }
            );
        } catch (Throwable throwable) {
            for (String packageName
                    : requested) {
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
