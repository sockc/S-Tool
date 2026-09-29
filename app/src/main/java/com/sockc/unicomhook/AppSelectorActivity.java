package com.sockc.unicomhook;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AppSelectorActivity extends Activity {

    public static final String EXTRA_CAPABILITY_ID =
            "capability_id";

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
    private static final int COLOR_SUBCARD =
            Color.rgb(248, 249, 251);

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

    private final List<AppItem> allApps =
            new ArrayList<>();

    private LinearLayout appContainer;
    private EditText searchInput;
    private Switch showSystemSwitch;
    private Switch masterSwitch;
    private TextView selectedSummary;
    private TextView bridgeStatus;

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

        loadInstalledApps();
        setContentView(
                buildContent()
        );
        refreshMasterState();
        refreshSelectedSummary();
        rebuildAppList();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (appContainer != null) {
            rebuildAppList();
            refreshSelectedSummary();
        }
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

        appContainer = vertical();
        root.addView(
                appContainer,
                topMargin(dp(8))
        );

        TextView footer = text(
                "S Tool 中选中 App 只是保存保护配置；目标 App 仍需在 LSPosed 的 S Tool 作用域中勾选。"
                        + " “尚无注入记录”表示更新后还没有记录到该 App 启动。",
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
        LinearLayout card = card();

        LinearLayout row =
                new LinearLayout(this);
        row.setOrientation(
                LinearLayout.HORIZONTAL
        );
        row.setGravity(
                Gravity.CENTER_VERTICAL
        );

        LinearLayout copy = vertical();

        TextView title = text(
                "总开关",
                17,
                COLOR_TEXT,
                Typeface.BOLD
        );
        copy.addView(title);

        bridgeStatus = text(
                crossProcessAvailable
                        ? "跨进程配置桥可用"
                        : "跨进程配置桥不可用",
                12,
                crossProcessAvailable
                        ? COLOR_OK
                        : COLOR_WARN,
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

        row.addView(masterSwitch);
        card.addView(row);

        return card;
    }

    private View buildFilterCard() {
        LinearLayout card = card();

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
        card.addView(searchInput);

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

        return card;
    }

    private void loadInstalledApps() {
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

            allApps.add(
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
                allApps,
                Comparator.comparing(
                        item -> item.label
                                .toLowerCase(
                                        Locale.getDefault()
                                )
                )
        );
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

        for (AppItem item : visible) {
            appContainer.addView(
                    buildAppRow(
                            item
                    ),
                    topMargin(dp(8))
            );
        }
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
            if (item.system
                    && !showSystem) {
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

            result.add(item);
        }

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
        row.setBackground(background);

        LinearLayout copy = vertical();

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

        long injectedAt =
                InjectionStatus
                        .getLastInjectedAt(
                                this,
                                item.packageName
                        );

        TextView injection = text(
                injectedAt > 0L
                        ? "✓ 已注入 "
                        + DateFormat
                        .getTimeInstance(
                                DateFormat.SHORT
                        )
                        .format(
                                new Date(
                                        injectedAt
                                )
                        )
                        : "尚无注入记录 · 请确认 LSPosed Scope",
                11,
                injectedAt > 0L
                        ? COLOR_OK
                        : COLOR_WARN,
                Typeface.NORMAL
        );
        copy.addView(
                injection,
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

        String key =
                FeatureRegistry
                        .universalAppKey(
                                capabilityId,
                                item.packageName
                        );

        Switch toggle =
                new Switch(this);
        toggle.setChecked(
                FeaturePrefs.isEnabled(
                        preferences,
                        key
                )
        );
        toggle.setEnabled(
                crossProcessAvailable
        );

        toggle.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    if (!buttonView.isPressed()) {
                        return;
                    }

                    boolean saved =
                            FeaturePrefs.setEnabled(
                                    preferences,
                                    key,
                                    isChecked
                            );

                    if (!saved) {
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
                }
        );

        row.setOnClickListener(
                view -> {
                    if (toggle.isEnabled()) {
                        toggle.performClick();
                    }
                }
        );

        row.addView(toggle);

        return row;
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

        for (AppItem item : visible) {
            editor.putBoolean(
                    FeatureRegistry.universalAppKey(
                            capabilityId,
                            item.packageName
                    ),
                    enabled
            );
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
                editor.remove(key);
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

    private void refreshSelectedSummary() {
        if (selectedSummary == null) {
            return;
        }

        int count = 0;

        for (Map.Entry<String, ?>
                entry
                : preferences.getAll()
                .entrySet()) {
            if (FeatureRegistry.isUniversalAppKey(
                    capabilityId,
                    entry.getKey()
            )
                    && Boolean.TRUE.equals(
                    entry.getValue()
            )) {
                count++;
            }
        }

        selectedSummary.setText(
                "已选择 "
                        + count
                        + " 个 App"
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
        LinearLayout card = vertical();
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
        card.setBackground(background);
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
