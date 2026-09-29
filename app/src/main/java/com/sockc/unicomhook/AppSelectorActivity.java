package com.sockc.unicomhook;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class AppSelectorActivity
        extends Activity {

    public static final String EXTRA_CAPABILITY_ID =
            "capability_id";

    private static final int FILTER_ALL = 0;
    private static final int FILTER_SELECTED = 1;
    private static final int FILTER_NEEDS_SCOPE = 2;

    private static final long APP_CACHE_TTL_MS =
            60_000L;

    private static final Object APP_CACHE_LOCK =
            new Object();

    private static List<AppItem> cachedApps =
            Collections.emptyList();

    private static long cachedAppsAt;

    private static final int COLOR_BG =
            Color.rgb(246, 247, 249);
    private static final int COLOR_CARD =
            Color.WHITE;
    private static final int COLOR_TEXT =
            Color.rgb(28, 30, 34);
    private static final int COLOR_SUBTEXT =
            Color.rgb(103, 109, 120);
    private static final int COLOR_ACCENT =
            Color.rgb(30, 112, 255);
    private static final int COLOR_OK =
            Color.rgb(26, 143, 86);
    private static final int COLOR_WARN =
            Color.rgb(197, 119, 0);

    private static final class AppItem {
        final String label;
        final String packageName;
        final boolean system;

        AppItem(
                String label,
                String packageName,
                boolean system
        ) {
            this.label = label;
            this.packageName = packageName;
            this.system = system;
        }
    }

    private SharedPreferences preferences;
    private boolean crossProcessAvailable;
    private String capabilityId;
    private int filterMode =
            FILTER_ALL;

    private final List<AppItem> allApps =
            new ArrayList<>();

    private LinearLayout appContainer;
    private EditText searchInput;
    private Switch showSystemSwitch;
    private Switch masterSwitch;
    private TextView selectedSummary;
    private TextView bridgeStatus;

    private ScopeServiceBridge.ServiceListener
            scopeServiceListener;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        capabilityId =
                getIntent().getStringExtra(
                        EXTRA_CAPABILITY_ID
                );

        if (!FeatureRegistry.isUniversalCapability(
                capabilityId
        )) {
            finish();
            return;
        }

        getWindow().setStatusBarColor(
                Color.WHITE
        );
        getWindow().setNavigationBarColor(
                COLOR_BG
        );
        getWindow()
                .getDecorView()
                .setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                );

        FeaturePrefs.OpenResult openResult =
                FeaturePrefs.open(this);

        preferences =
                openResult.preferences;
        crossProcessAvailable =
                openResult.crossProcessAvailable;

        scopeServiceListener =
                () -> runOnUiThread(
                        () -> {
                            if (isFinishing()
                                    || isDestroyed()) {
                                return;
                            }

                            refreshBridgeStatus();
                            refreshSelectedSummary();
                            rebuildAppList();
                        }
                );

        ScopeServiceBridge.addListener(
                scopeServiceListener
        );
        ScopeServiceBridge.initialize();
        ScopeServiceBridge.refreshScope();

        loadInstalledApps();

        setContentView(
                buildContent()
        );

        refreshMasterState();
        refreshBridgeStatus();
        refreshSelectedSummary();
        rebuildAppList();
    }

    @Override
    protected void onResume() {
        super.onResume();

        ScopeServiceBridge.refreshScope();

        if (appContainer != null) {
            refreshBridgeStatus();
            refreshSelectedSummary();
            rebuildAppList();
        }
    }

    @Override
    protected void onDestroy() {
        ScopeServiceBridge.removeListener(
                scopeServiceListener
        );
        super.onDestroy();
    }

    private View buildContent() {
        ScrollView scrollView =
                new ScrollView(this);

        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(
                COLOR_BG
        );

        LinearLayout root = vertical();
        root.setPadding(
                dp(18),
                dp(18),
                dp(18),
                dp(32)
        );

        scrollView.addView(
                root,
                new ScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        TextView back = text(
                "‹ 返回",
                15,
                COLOR_ACCENT,
                Typeface.BOLD
        );
        back.setPadding(
                0,
                dp(4),
                0,
                dp(8)
        );
        back.setOnClickListener(
                view -> finish()
        );
        root.addView(back);

        TextView title = text(
                capabilityTitle(),
                28,
                COLOR_TEXT,
                Typeface.BOLD
        );
        root.addView(
                title,
                topMargin(dp(4))
        );

        TextView subtitle = text(
                capabilitySummary(),
                13,
                COLOR_SUBTEXT,
                Typeface.NORMAL
        );
        subtitle.setLineSpacing(
                0,
                1.15f
        );
        root.addView(
                subtitle,
                topMargin(dp(6))
        );

        root.addView(
                buildMasterCard(),
                topMargin(dp(18))
        );

        root.addView(
                buildFilterCard(),
                topMargin(dp(14))
        );

        selectedSummary = text(
                "",
                14,
                COLOR_TEXT,
                Typeface.BOLD
        );
        root.addView(
                selectedSummary,
                topMargin(dp(18))
        );

        appContainer =
                vertical();

        root.addView(
                appContainer,
                topMargin(dp(8))
        );

        TextView footer = text(
                "选择 App 后，S Tool 会检查 LSPosed 的真实 Scope；"
                        + "未加入时会直接发起 LSPosed 原生“作用域请求”，无需进入管理器手动勾选。"
                        + "取消当前功能只会取消该 App 的本项配置，不会擅自移除 LSPosed Scope。",
                12,
                COLOR_SUBTEXT,
                Typeface.NORMAL
        );
        footer.setLineSpacing(
                0,
                1.18f
        );

        root.addView(
                footer,
                topMargin(dp(18))
        );

        return scrollView;
    }

    private View buildMasterCard() {
        LinearLayout card =
                card();

        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );
        row.setGravity(
                Gravity.CENTER_VERTICAL
        );

        LinearLayout copy =
                vertical();

        TextView title = text(
                "总开关",
                17,
                COLOR_TEXT,
                Typeface.BOLD
        );
        copy.addView(title);

        bridgeStatus = text(
                "",
                12,
                COLOR_SUBTEXT,
                Typeface.NORMAL
        );

        copy.addView(
                bridgeStatus,
                topMargin(dp(3))
        );

        row.addView(
                copy,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        masterSwitch =
                new Switch(this);

        masterSwitch.setEnabled(
                crossProcessAvailable
        );

        masterSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    if (!buttonView.isPressed()) {
                        return;
                    }

                    if (!setCapabilityEnabled(
                            isChecked
                    )) {
                        refreshMasterState();

                        Toast.makeText(
                                this,
                                "保存失败",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        row.addView(
                masterSwitch
        );
        card.addView(row);

        return card;
    }

    private View buildFilterCard() {
        LinearLayout card =
                card();

        searchInput =
                new EditText(this);

        searchInput.setHint(
                "搜索应用或包名"
        );
        searchInput.setSingleLine(true);
        searchInput.setTextSize(14);
        searchInput.setImeOptions(
                EditorInfo.IME_ACTION_DONE
        );

        searchInput.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {
                        rebuildAppList();
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                }
        );

        card.addView(
                searchInput
        );

        LinearLayout options =
                new LinearLayout(this);

        options.setOrientation(
                LinearLayout.HORIZONTAL
        );
        options.setGravity(
                Gravity.CENTER_VERTICAL
        );

        showSystemSwitch =
                new Switch(this);
        showSystemSwitch.setText(
                "显示系统应用"
        );
        showSystemSwitch.setTextSize(12);
        showSystemSwitch.setChecked(false);

        showSystemSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) ->
                        rebuildAppList()
        );

        options.addView(
                showSystemSwitch,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        Button selectVisible =
                actionButton(
                        "全选当前"
                );

        selectVisible.setOnClickListener(
                view -> setVisibleSelections(
                        true
                )
        );

        options.addView(
                selectVisible
        );

        Button clear =
                actionButton(
                        "清空"
                );

        clear.setOnClickListener(
                view -> clearSelections()
        );

        options.addView(
                clear
        );

        card.addView(
                options,
                topMargin(dp(8))
        );

        LinearLayout filters =
                new LinearLayout(this);

        filters.setOrientation(
                LinearLayout.HORIZONTAL
        );

        filters.addView(
                filterButton(
                        "全部",
                        FILTER_ALL
                ),
                weightedButtonParams(
                        1f,
                        0
                )
        );

        filters.addView(
                filterButton(
                        "已选择",
                        FILTER_SELECTED
                ),
                weightedButtonParams(
                        1f,
                        dp(6)
                )
        );

        filters.addView(
                filterButton(
                        "待 Scope",
                        FILTER_NEEDS_SCOPE
                ),
                weightedButtonParams(
                        1f,
                        dp(6)
                )
        );

        card.addView(
                filters,
                topMargin(dp(6))
        );

        return card;
    }

    private Button filterButton(
            String value,
            int mode
    ) {
        Button button =
                actionButton(
                        value
                );

        button.setOnClickListener(
                view -> {
                    filterMode =
                            mode;
                    rebuildAppList();
                }
        );

        return button;
    }

    private void loadInstalledApps() {
        long now =
                SystemClock.elapsedRealtime();

        synchronized (APP_CACHE_LOCK) {
            if (!cachedApps.isEmpty()
                    && now - cachedAppsAt
                    < APP_CACHE_TTL_MS) {
                allApps.addAll(
                        cachedApps
                );
                return;
            }
        }

        PackageManager packageManager =
                getPackageManager();

        List<ApplicationInfo> applications;

        try {
            applications =
                    packageManager
                            .getInstalledApplications(
                                    0
                            );
        } catch (Throwable throwable) {
            applications =
                    Collections.emptyList();
        }

        List<AppItem> loaded =
                new ArrayList<>();

        for (ApplicationInfo info
                : applications) {
            if (shouldSkipPackage(
                    info.packageName
            )) {
                continue;
            }

            CharSequence labelValue =
                    packageManager
                            .getApplicationLabel(
                                    info
                            );

            String label =
                    labelValue == null
                            ? info.packageName
                            : labelValue
                            .toString()
                            .trim();

            boolean system =
                    (info.flags
                            & ApplicationInfo.FLAG_SYSTEM)
                            != 0;

            loaded.add(
                    new AppItem(
                            label.isEmpty()
                                    ? info.packageName
                                    : label,
                            info.packageName,
                            system
                    )
            );
        }

        Collections.sort(
                loaded,
                Comparator.comparing(
                        item -> item.label
                                .toLowerCase(
                                        Locale.getDefault()
                                )
                )
        );

        allApps.addAll(
                loaded
        );

        synchronized (APP_CACHE_LOCK) {
            cachedApps =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    loaded
                            )
                    );
            cachedAppsAt =
                    now;
        }
    }

    private boolean shouldSkipPackage(
            String packageName
    ) {
        return packageName == null
                || getPackageName().equals(
                packageName
        )
                || "android".equals(
                packageName
        )
                || "com.android.systemui".equals(
                packageName
        )
                || "com.android.settings".equals(
                packageName
        );
    }

    private void rebuildAppList() {
        if (appContainer == null) {
            return;
        }

        appContainer.removeAllViews();

        List<AppItem> visible =
                filteredApps();

        if (visible.isEmpty()) {
            TextView empty = text(
                    "没有符合条件的应用",
                    13,
                    COLOR_SUBTEXT,
                    Typeface.NORMAL
            );

            appContainer.addView(
                    empty,
                    topMargin(dp(8))
            );
            return;
        }

        List<AppItem> selected =
                new ArrayList<>();
        List<AppItem> other =
                new ArrayList<>();

        for (AppItem item : visible) {
            if (isSelected(
                    item.packageName
            )) {
                selected.add(
                        item
                );
            } else {
                other.add(
                        item
                );
            }
        }

        if (!selected.isEmpty()) {
            appContainer.addView(
                    sectionLabel(
                            "已选择 "
                                    + selected.size()
                    )
            );

            for (AppItem item : selected) {
                appContainer.addView(
                        buildAppRow(
                                item
                        ),
                        topMargin(dp(8))
                );
            }
        }

        if (!other.isEmpty()) {
            appContainer.addView(
                    sectionLabel(
                            selected.isEmpty()
                                    ? "应用"
                                    : "其他应用"
                    ),
                    topMargin(
                            selected.isEmpty()
                                    ? 0
                                    : dp(14)
                    )
            );

            for (AppItem item : other) {
                appContainer.addView(
                        buildAppRow(
                                item
                        ),
                        topMargin(dp(8))
                );
            }
        }
    }

    private TextView sectionLabel(
            String value
    ) {
        return text(
                value,
                13,
                COLOR_SUBTEXT,
                Typeface.BOLD
        );
    }

    private List<AppItem> filteredApps() {
        String query =
                searchInput == null
                        ? ""
                        : searchInput
                        .getText()
                        .toString()
                        .trim()
                        .toLowerCase(
                                Locale.getDefault()
                        );

        boolean showSystem =
                showSystemSwitch != null
                        && showSystemSwitch
                        .isChecked();

        List<AppItem> result =
                new ArrayList<>();

        for (AppItem item : allApps) {
            boolean selected =
                    isSelected(
                            item.packageName
                    );

            if (item.system
                    && !showSystem
                    && !selected) {
                continue;
            }

            if (!query.isEmpty()
                    && !item.label
                    .toLowerCase(
                            Locale.getDefault()
                    )
                    .contains(query)
                    && !item.packageName
                    .toLowerCase(
                            Locale.getDefault()
                    )
                    .contains(query)) {
                continue;
            }

            if (filterMode
                    == FILTER_SELECTED
                    && !selected) {
                continue;
            }

            if (filterMode
                    == FILTER_NEEDS_SCOPE
                    && (!selected
                    || ScopeServiceBridge.isInScope(
                    item.packageName
            ))) {
                continue;
            }

            result.add(
                    item
            );
        }

        result.sort(
                (left, right) -> {
                    boolean leftSelected =
                            isSelected(
                                    left.packageName
                            );
                    boolean rightSelected =
                            isSelected(
                                    right.packageName
                            );

                    if (leftSelected
                            != rightSelected) {
                        return leftSelected
                                ? -1
                                : 1;
                    }

                    return left.label
                            .compareToIgnoreCase(
                                    right.label
                            );
                }
        );

        return result;
    }

    private View buildAppRow(
            AppItem item
    ) {
        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );
        row.setGravity(
                Gravity.CENTER_VERTICAL
        );
        row.setPadding(
                dp(14),
                dp(12),
                dp(10),
                dp(12)
        );

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                COLOR_CARD
        );
        background.setCornerRadius(
                dp(14)
        );
        background.setStroke(
                dp(1),
                Color.rgb(
                        232,
                        234,
                        238
                )
        );

        row.setBackground(
                background
        );

        boolean selected =
                isSelected(
                        item.packageName
                );

        LinearLayout copy =
                vertical();

        TextView title = text(
                item.label,
                15,
                COLOR_TEXT,
                Typeface.BOLD
        );
        copy.addView(title);

        TextView packageView = text(
                item.packageName
                        + (item.system
                        ? " · 系统应用"
                        : ""),
                11,
                COLOR_SUBTEXT,
                Typeface.NORMAL
        );

        copy.addView(
                packageView,
                topMargin(dp(2))
        );

        TextView scopeStatus =
                buildScopeStatusView(
                        item,
                        selected
                );

        copy.addView(
                scopeStatus,
                topMargin(dp(3))
        );

        if (FeatureRegistry.UNIVERSAL_SPLASH_SKIP.equals(
                capabilityId
        )) {
            SplashSkipStatus.Entry entry =
                    SplashSkipStatus.read(
                            this,
                            item.packageName
                    );

            String statusText;

            if (entry.lastSuccessAt > 0L) {
                statusText =
                        "今日成功 "
                                + entry.todayCount
                                + " 次 · 最近 "
                                + DateFormat
                                .getTimeInstance(
                                        DateFormat.SHORT
                                )
                                .format(
                                        new Date(
                                                entry.lastSuccessAt
                                        )
                                )
                                + " · 评分 "
                                + entry.lastScore;
            } else {
                statusText =
                        "尚未记录到成功跳过";
            }

            TextView splashStatus =
                    text(
                            statusText,
                            11,
                            entry.lastSuccessAt > 0L
                                    ? COLOR_OK
                                    : COLOR_SUBTEXT,
                            Typeface.NORMAL
                    );

            copy.addView(
                    splashStatus,
                    topMargin(dp(3))
            );
        }

        row.addView(
                copy,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        String key =
                FeatureRegistry
                        .universalAppKey(
                                capabilityId,
                                item.packageName
                        );

        Switch toggle =
                new Switch(this);

        toggle.setChecked(
                selected
        );
        toggle.setEnabled(
                crossProcessAvailable
        );

        toggle.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    boolean saved =
                            FeaturePrefs.setEnabled(
                                    preferences,
                                    key,
                                    isChecked
                            );

                    if (!saved) {
                        buttonView.setOnCheckedChangeListener(
                                null
                        );
                        buttonView.setChecked(
                                !isChecked
                        );

                        Toast.makeText(
                                this,
                                "保存失败",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    refreshSelectedSummary();

                    if (isChecked) {
                        requestScopeIfNeeded(
                                item.packageName,
                                true
                        );
                    }

                    rebuildAppList();
                }
        );

        row.setOnClickListener(
                view -> {
                    if (toggle.isEnabled()) {
                        toggle.performClick();
                    }
                }
        );

        row.addView(
                toggle
        );

        return row;
    }

    private TextView buildScopeStatusView(
            AppItem item,
            boolean selected
    ) {
        boolean inScope =
                ScopeServiceBridge.isInScope(
                        item.packageName
                );

        ScopeServiceBridge.RequestState state =
                ScopeServiceBridge.getRequestState(
                        item.packageName
                );

        StringBuilder value =
                new StringBuilder();

        int color =
                COLOR_SUBTEXT;

        if (inScope) {
            value.append(
                    selected
                            ? "✓ 已在 LSPosed 作用域"
                            : "✓ Scope 已有 · 当前未选择"
            );
            color =
                    COLOR_OK;
        } else if (selected) {
            color =
                    COLOR_WARN;

            switch (state) {
                case REQUESTING:
                    value.append(
                            "⏳ 等待 LSPosed 作用域授权"
                    );
                    break;
                case DENIED:
                    value.append(
                            "⚠ Scope 未授权 · 点此重新请求"
                    );
                    break;
                case TIMEOUT:
                    value.append(
                            "⚠ Scope 请求超时 · 点此重试"
                    );
                    break;
                case FAILED:
                    value.append(
                            ScopeServiceBridge.isConnected()
                                    ? "⚠ Scope 请求失败 · 点此重试"
                                    : "⚠ Scope Service 未连接"
                    );
                    break;
                default:
                    value.append(
                            ScopeServiceBridge.isConnected()
                                    ? "⚠ 尚未加入 Scope · 点此请求"
                                    : "⏳ Scope Service 连接中"
                    );
                    break;
            }
        } else {
            value.append(
                    ScopeServiceBridge.isConnected()
                            ? "未选择"
                            : "Scope 状态暂不可用"
            );
        }

        long injectedAt =
                InjectionStatus
                        .getLastInjectedAt(
                                this,
                                item.packageName
                        );

        if (injectedAt > 0L) {
            value.append(
                    inScope
                            ? " · 已注入 "
                            : " · 历史注入 "
            ).append(
                    DateFormat
                            .getTimeInstance(
                                    DateFormat.SHORT
                            )
                            .format(
                                    new Date(
                                            injectedAt
                                    )
                            )
            );
        } else if (inScope) {
            value.append(
                    " · 尚无注入记录"
            );
        }

        TextView view = text(
                value.toString(),
                11,
                color,
                Typeface.NORMAL
        );

        if (selected
                && !inScope) {
            view.setClickable(true);
            view.setOnClickListener(
                    ignored ->
                            requestScopeIfNeeded(
                                    item.packageName,
                                    true
                            )
            );
        }

        return view;
    }

    private void requestScopeIfNeeded(
            String packageName,
            boolean showFeedback
    ) {
        if (ScopeServiceBridge.isInScope(
                packageName
        )) {
            return;
        }

        ScopeServiceBridge.requestScope(
                packageName,
                (requestedPackage,
                 state,
                 message) ->
                        runOnUiThread(
                                () -> {
                                    if (isFinishing()
                                            || isDestroyed()) {
                                        return;
                                    }

                                    if (showFeedback) {
                                        if (state
                                                == ScopeServiceBridge
                                                .RequestState.APPROVED) {
                                            Toast.makeText(
                                                    this,
                                                    "已加入 LSPosed 作用域",
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        } else if (state
                                                == ScopeServiceBridge
                                                .RequestState.DENIED) {
                                            Toast.makeText(
                                                    this,
                                                    "Scope 未授权，S Tool 选择已保留",
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        } else if ((state
                                                == ScopeServiceBridge
                                                .RequestState.FAILED
                                                || state
                                                == ScopeServiceBridge
                                                .RequestState.TIMEOUT)
                                                && message != null) {
                                            Toast.makeText(
                                                    this,
                                                    message,
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        }
                                    }

                                    refreshBridgeStatus();
                                    rebuildAppList();
                                }
                        )
        );
    }

    private void setVisibleSelections(
            boolean enabled
    ) {
        if (!crossProcessAvailable) {
            return;
        }

        List<AppItem> visible =
                filteredApps();

        SharedPreferences.Editor editor =
                preferences.edit();

        List<String> newlySelected =
                new ArrayList<>();

        for (AppItem item : visible) {
            boolean wasSelected =
                    isSelected(
                            item.packageName
                    );

            editor.putBoolean(
                    FeatureRegistry.universalAppKey(
                            capabilityId,
                            item.packageName
                    ),
                    enabled
            );

            if (enabled
                    && !wasSelected
                    && !ScopeServiceBridge.isInScope(
                    item.packageName
            )) {
                newlySelected.add(
                        item.packageName
                );
            }
        }

        editor.putLong(
                FeaturePrefs.KEY_LAST_CHANGED_AT,
                System.currentTimeMillis()
        );

        if (!editor.commit()) {
            Toast.makeText(
                    this,
                    "保存失败",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        refreshSelectedSummary();
        rebuildAppList();

        if (enabled
                && !newlySelected.isEmpty()) {
            prepareBatchScopeRequests(
                    newlySelected
            );
        }
    }

    private void prepareBatchScopeRequests(
            List<String> packages
    ) {
        LinkedHashSet<String> pending =
                new LinkedHashSet<>();

        for (String packageName : packages) {
            if (!ScopeServiceBridge.isInScope(
                    packageName
            )) {
                pending.add(
                        packageName
                );
            }
        }

        if (pending.isEmpty()) {
            return;
        }

        if (!ScopeServiceBridge.isConnected()) {
            Toast.makeText(
                    this,
                    "配置已保存，LSPosed Scope Service 暂未连接",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        if (pending.size() == 1) {
            requestScopeBatch(
                    new ArrayList<>(
                            pending
                    )
            );
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(
                        "批量作用域请求"
                )
                .setMessage(
                        "已选择应用，其中 "
                                + pending.size()
                                + " 个尚未加入 S Tool 的 LSPosed 作用域。"
                                + "是否一次提交给 LSPosed 发起原生作用域请求？"
                )
                .setPositiveButton(
                        "开始请求",
                        (dialog, which) ->
                                requestScopeBatch(
                                        new ArrayList<>(
                                                pending
                                        )
                                )
                )
                .setNegativeButton(
                        "稍后",
                        null
                )
                .show();
    }

    private void requestScopeBatch(
            List<String> packages
    ) {
        ScopeServiceBridge.requestScopes(
                packages,
                (requestedPackage,
                 state,
                 message) ->
                        runOnUiThread(
                                () -> {
                                    if (isFinishing()
                                            || isDestroyed()) {
                                        return;
                                    }

                                    if (state
                                            == ScopeServiceBridge
                                            .RequestState.FAILED
                                            && message != null) {
                                        Toast.makeText(
                                                this,
                                                message,
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    }

                                    refreshBridgeStatus();
                                    refreshSelectedSummary();
                                    rebuildAppList();
                                }
                        )
        );
    }

    private void clearSelections() {
        if (!crossProcessAvailable) {
            return;
        }

        SharedPreferences.Editor editor =
                preferences.edit();

        for (String key
                : preferences.getAll().keySet()) {
            if (FeatureRegistry
                    .isUniversalAppKey(
                            capabilityId,
                            key
                    )) {
                editor.remove(
                        key
                );
            }
        }

        editor.putLong(
                FeaturePrefs.KEY_LAST_CHANGED_AT,
                System.currentTimeMillis()
        );

        if (!editor.commit()) {
            Toast.makeText(
                    this,
                    "保存失败",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        refreshSelectedSummary();
        rebuildAppList();
    }

    private boolean isSelected(
            String packageName
    ) {
        String key =
                FeatureRegistry.universalAppKey(
                        capabilityId,
                        packageName
                );

        return FeaturePrefs.isEnabled(
                preferences,
                key
        );
    }

    private boolean setCapabilityEnabled(
            boolean enabled
    ) {
        return preferences.edit()
                .putBoolean(
                        capabilityId,
                        enabled
                )
                .putLong(
                        FeaturePrefs.KEY_LAST_CHANGED_AT,
                        System.currentTimeMillis()
                )
                .commit();
    }

    private void refreshMasterState() {
        if (masterSwitch == null) {
            return;
        }

        masterSwitch.setChecked(
                FeaturePrefs.isEnabled(
                        preferences,
                        capabilityId
                )
        );
    }

    private void refreshBridgeStatus() {
        if (bridgeStatus == null) {
            return;
        }

        String configText =
                crossProcessAvailable
                        ? "配置桥可用"
                        : "配置桥不可用";

        String scopeText =
                ScopeServiceBridge.isConnected()
                        ? "Scope Service 已连接"
                        : "Scope Service 连接中";

        bridgeStatus.setText(
                configText
                        + " · "
                        + scopeText
        );

        bridgeStatus.setTextColor(
                crossProcessAvailable
                        && ScopeServiceBridge.isConnected()
                        ? COLOR_OK
                        : COLOR_WARN
        );
    }

    private void refreshSelectedSummary() {
        if (selectedSummary == null) {
            return;
        }

        int count = 0;
        int inScopeCount = 0;

        for (Map.Entry<String, ?>
                entry
                : preferences.getAll()
                .entrySet()) {
            if (!FeatureRegistry.isUniversalAppKey(
                    capabilityId,
                    entry.getKey()
            )
                    || !Boolean.TRUE.equals(
                    entry.getValue()
            )) {
                continue;
            }

            count++;

            String packageName =
                    FeatureRegistry
                            .packageFromUniversalAppKey(
                                    capabilityId,
                                    entry.getKey()
                            );

            if (ScopeServiceBridge.isInScope(
                    packageName
            )) {
                inScopeCount++;
            }
        }

        selectedSummary.setText(
                "已选择 "
                        + count
                        + " 个 App"
                        + (ScopeServiceBridge.isConnected()
                        ? " · Scope "
                        + inScopeCount
                        + "/"
                        + count
                        : " · Scope 状态连接中")
        );
    }

    private String capabilityTitle() {
        return FeatureRegistry.universalCapabilityTitle(
                capabilityId
        );
    }

    private String capabilitySummary() {
        if (FeatureRegistry.UNIVERSAL_LOCATION.equals(
                capabilityId
        )) {
            return "阻断所选 App 的 Android 原生定位入口。地图、导航、打车等依赖真实定位的 App 建议不要开启。";
        }

        if (FeatureRegistry.UNIVERSAL_SCREENSHOT.equals(
                capabilityId
        )) {
            return "阻止所选 App 注册 Android 14+ 截图回调和常见媒体库截图监听。";
        }

        if (FeatureRegistry.UNIVERSAL_CLIPBOARD.equals(
                capabilityId
        )) {
            return "让所选 App 看不到当前剪贴板内容与剪贴板状态。需要粘贴、验证码读取的 App 请谨慎开启。";
        }

        if (FeatureRegistry.UNIVERSAL_DEVICE_ID.equals(
                capabilityId
        )) {
            return "保护所选 App 读取 IMEI、MEID、IMSI、SIM 序列号、Android ID 和设备序列号。部分登录或风控功能可能受影响。";
        }

        if (FeatureRegistry.UNIVERSAL_FILE_MEDIA.equals(
                capabilityId
        )) {
            return "隐藏所选 App 对 DCIM、Pictures、Download、Movies、Screenshots 等公共目录的常规 File 扫描。不会拦截系统文件选择器。";
        }

        if (FeatureRegistry.UNIVERSAL_APP_LIST.equals(
                capabilityId
        )) {
            return "限制所选 App 批量枚举已安装应用和软件包。仅处理批量枚举 API，降低对正常打开其它 App 的影响。";
        }

        if (FeatureRegistry.UNIVERSAL_SPLASH_SKIP.equals(
                capabilityId
        )) {
            return "严格模式：只在目标 App 冷启动后的前 10 秒扫描可见 UI；"
                    + "综合“跳过/关闭广告”文字、resource-id、广告上下文和屏幕位置评分，达到阈值后最多执行一次 performClick()。"
                    + "不 Hook 广告 SDK、不修改启动回调或状态机。";
        }

        return FeatureRegistry.universalCapabilitySummary(
                capabilityId
        );
    }

    private LinearLayout vertical() {
        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        return layout;
    }

    private LinearLayout card() {
        LinearLayout card =
                vertical();

        card.setPadding(
                dp(16),
                dp(15),
                dp(16),
                dp(15)
        );

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                COLOR_CARD
        );
        background.setCornerRadius(
                dp(15)
        );
        background.setStroke(
                dp(1),
                Color.rgb(
                        232,
                        234,
                        238
                )
        );

        card.setBackground(
                background
        );

        return card;
    }

    private TextView text(
            String value,
            int sp,
            int color,
            int style
    ) {
        TextView view =
                new TextView(this);

        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(
                Typeface.DEFAULT,
                style
        );

        return view;
    }

    private Button actionButton(
            String value
    ) {
        Button button =
                new Button(this);

        button.setText(value);
        button.setTextSize(12);
        button.setAllCaps(false);
        button.setTextColor(
                COLOR_ACCENT
        );

        return button;
    }

    private LinearLayout.LayoutParams weightedButtonParams(
            float weight,
            int leftMargin
    ) {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        0,
                        dp(42),
                        weight
                );

        params.leftMargin =
                leftMargin;

        return params;
    }

    private LinearLayout.LayoutParams wrapMatch() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams topMargin(
            int margin
    ) {
        LinearLayout.LayoutParams params =
                wrapMatch();

        params.topMargin =
                margin;

        return params;
    }

    private int dp(
            int value
    ) {
        return Math.round(
                value
                        * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
}
