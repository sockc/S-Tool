package com.sockc.unicomhook;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class MainActivity extends Activity {

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

    private SharedPreferences preferences;
    private boolean crossProcessAvailable;
    private boolean updatingSwitches;

    private TextView configStatus;
    private TextView enabledSummary;
    private TextView installedSummary;
    private TextView changedSummary;

    private final Map<String, Switch> switches =
            new LinkedHashMap<>();

    private final Map<String, TextView> subSummaryViews =
            new LinkedHashMap<>();

    private final Map<String, LinearLayout> subContainers =
            new LinkedHashMap<>();

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

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

        setContentView(buildContent());
        refreshSummary();
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
                dp(20),
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

        TextView title = text(
                "S Tool",
                30,
                COLOR_TEXT,
                Typeface.BOLD
        );
        root.addView(title);

        TextView subtitle = text(
                "V1.2.2 · 稳定配置桥与固定签名",
                14,
                COLOR_SUBTEXT,
                Typeface.NORMAL
        );

        LinearLayout.LayoutParams subtitleParams =
                wrapMatch();
        subtitleParams.topMargin = dp(2);
        root.addView(
                subtitle,
                subtitleParams
        );

        root.addView(
                buildStatusCard(),
                topMargin(dp(18))
        );

        root.addView(
                sectionTitle("快速操作"),
                topMargin(dp(22))
        );

        root.addView(
                buildQuickActions(),
                topMargin(dp(10))
        );

        root.addView(
                sectionTitle("功能模块"),
                topMargin(dp(24))
        );

        for (FeatureRegistry.Feature feature
                : FeatureRegistry.all()) {
            root.addView(
                    buildFeatureCard(feature),
                    topMargin(dp(10))
            );
        }

        TextView footer = text(
                "总开关关闭时会停止整个模块，子功能原设置会保留。"
                        + " 配置通过只读 ConfigProvider 跨进程读取；"
                        + " 修改开关后请强制停止并重新打开对应目标 App。"
                        + " 配置桥读取失败时 Hook 默认开启，避免升级后功能意外失效。",
                12,
                COLOR_SUBTEXT,
                Typeface.NORMAL
        );

        footer.setLineSpacing(
                0,
                1.18f
        );

        LinearLayout.LayoutParams footerParams =
                topMargin(dp(20));
        footerParams.bottomMargin = dp(12);

        root.addView(
                footer,
                footerParams
        );

        return scrollView;
    }

    private View buildStatusCard() {
        LinearLayout card = card();

        TextView header = text(
                "运行状态",
                18,
                COLOR_TEXT,
                Typeface.BOLD
        );
        card.addView(header);

        configStatus = text(
                "",
                14,
                COLOR_SUBTEXT,
                Typeface.NORMAL
        );
        card.addView(
                configStatus,
                topMargin(dp(12))
        );

        enabledSummary = text(
                "",
                14,
                COLOR_SUBTEXT,
                Typeface.NORMAL
        );
        card.addView(
                enabledSummary,
                topMargin(dp(8))
        );

        installedSummary = text(
                "",
                14,
                COLOR_SUBTEXT,
                Typeface.NORMAL
        );
        card.addView(
                installedSummary,
                topMargin(dp(8))
        );

        changedSummary = text(
                "",
                13,
                COLOR_SUBTEXT,
                Typeface.NORMAL
        );
        card.addView(
                changedSummary,
                topMargin(dp(8))
        );

        if (!crossProcessAvailable) {
            TextView warning = text(
                    "配置桥自检失败。请确认 S Tool 已正常安装后重新打开；"
                            + "目标 App 内仍会回退到旧 XSharedPreferences 或默认开启。",
                    13,
                    COLOR_WARN,
                    Typeface.BOLD
            );

            warning.setLineSpacing(
                    0,
                    1.15f
            );

            card.addView(
                    warning,
                    topMargin(dp(12))
            );
        }

        return card;
    }

    private View buildQuickActions() {
        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );

        Button enableAll =
                actionButton("全部开启");
        Button disableAll =
                actionButton("全部关闭");
        Button reset =
                actionButton("恢复默认");

        enableAll.setEnabled(
                crossProcessAvailable
        );
        disableAll.setEnabled(
                crossProcessAvailable
        );
        reset.setEnabled(
                crossProcessAvailable
        );

        enableAll.setOnClickListener(
                view -> setAllFeatures(true)
        );

        disableAll.setOnClickListener(
                view -> setAllFeatures(false)
        );

        reset.setOnClickListener(
                view -> resetDefaults()
        );

        row.addView(
                enableAll,
                weightedButtonParams(
                        1f,
                        0
                )
        );

        row.addView(
                disableAll,
                weightedButtonParams(
                        1f,
                        dp(8)
                )
        );

        row.addView(
                reset,
                weightedButtonParams(
                        1f,
                        dp(8)
                )
        );

        return row;
    }

    private View buildFeatureCard(
            FeatureRegistry.Feature feature
    ) {
        LinearLayout card = card();

        LinearLayout firstRow =
                new LinearLayout(this);

        firstRow.setOrientation(
                LinearLayout.HORIZONTAL
        );
        firstRow.setGravity(
                Gravity.CENTER_VERTICAL
        );

        LinearLayout nameColumn = vertical();

        TextView title = text(
                feature.title,
                17,
                COLOR_TEXT,
                Typeface.BOLD
        );
        nameColumn.addView(title);

        TextView target = text(
                buildTargetStatus(feature),
                12,
                COLOR_SUBTEXT,
                Typeface.NORMAL
        );

        nameColumn.addView(
                target,
                topMargin(dp(3))
        );

        firstRow.addView(
                nameColumn,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        Switch masterSwitch =
                createSwitch(
                        feature.id,
                        FeaturePrefs.isEnabled(
                                preferences,
                                feature.id
                        )
                );

        masterSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    if (updatingSwitches) {
                        return;
                    }

                    saveSwitch(
                            feature.id,
                            isChecked
                    );

                    updateChildSwitchStates(
                            feature
                    );
                    updateSubSummary(
                            feature
                    );
                    refreshSummary();
                }
        );

        firstRow.addView(masterSwitch);
        card.addView(firstRow);

        TextView summary = text(
                feature.summary,
                13,
                COLOR_SUBTEXT,
                Typeface.NORMAL
        );

        summary.setLineSpacing(
                0,
                1.12f
        );

        card.addView(
                summary,
                topMargin(dp(10))
        );

        if (!feature.subFeatures.isEmpty()) {
            LinearLayout childContainer =
                    vertical();

            childContainer.setVisibility(
                    View.GONE
            );

            TextView subSummary = text(
                    "",
                    13,
                    COLOR_ACCENT,
                    Typeface.BOLD
            );

            subSummary.setPadding(
                    0,
                    dp(4),
                    0,
                    dp(2)
            );

            subContainers.put(
                    feature.id,
                    childContainer
            );
            subSummaryViews.put(
                    feature.id,
                    subSummary
            );

            subSummary.setOnClickListener(
                    view -> {
                        boolean expand =
                                childContainer
                                        .getVisibility()
                                        != View.VISIBLE;

                        childContainer.setVisibility(
                                expand
                                        ? View.VISIBLE
                                        : View.GONE
                        );

                        updateSubSummary(
                                feature
                        );
                    }
            );

            card.addView(
                    subSummary,
                    topMargin(dp(12))
            );

            for (FeatureRegistry.SubFeature subFeature
                    : feature.subFeatures) {
                childContainer.addView(
                        buildSubFeatureRow(
                                feature,
                                subFeature
                        ),
                        topMargin(dp(8))
                );
            }

            card.addView(
                    childContainer,
                    topMargin(dp(2))
            );

            updateChildSwitchStates(
                    feature
            );
            updateSubSummary(
                    feature
            );
        }

        return card;
    }

    private View buildSubFeatureRow(
            FeatureRegistry.Feature feature,
            FeatureRegistry.SubFeature subFeature
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
                dp(12),
                dp(10),
                dp(10),
                dp(10)
        );

        GradientDrawable background =
                new GradientDrawable();
        background.setColor(
                COLOR_SUBCARD
        );
        background.setCornerRadius(
                dp(11)
        );
        row.setBackground(background);

        LinearLayout copy = vertical();

        TextView title = text(
                subFeature.title,
                14,
                COLOR_TEXT,
                Typeface.BOLD
        );
        copy.addView(title);

        TextView summary = text(
                subFeature.summary,
                12,
                COLOR_SUBTEXT,
                Typeface.NORMAL
        );

        summary.setLineSpacing(
                0,
                1.10f
        );

        copy.addView(
                summary,
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

        Switch childSwitch =
                createSwitch(
                        subFeature.id,
                        FeaturePrefs.isEnabled(
                                preferences,
                                subFeature.id
                        )
                );

        childSwitch.setEnabled(
                crossProcessAvailable
                        && FeaturePrefs.isEnabled(
                        preferences,
                        feature.id
                )
        );

        childSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    if (updatingSwitches) {
                        return;
                    }

                    saveSwitch(
                            subFeature.id,
                            isChecked
                    );

                    updateSubSummary(
                            feature
                    );
                    refreshSummary();
                }
        );

        row.addView(childSwitch);

        return row;
    }

    private Switch createSwitch(
            String key,
            boolean checked
    ) {
        Switch toggle =
                new Switch(this);

        toggle.setChecked(checked);
        toggle.setEnabled(
                crossProcessAvailable
        );

        switches.put(
                key,
                toggle
        );

        return toggle;
    }

    private void saveSwitch(
            String key,
            boolean enabled
    ) {
        boolean saved =
                FeaturePrefs.setEnabled(
                        preferences,
                        key,
                        enabled
                );

        if (!saved) {
            Toast.makeText(
                    this,
                    "保存失败",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void updateChildSwitchStates(
            FeatureRegistry.Feature feature
    ) {
        boolean masterEnabled =
                crossProcessAvailable
                        && FeaturePrefs.isEnabled(
                        preferences,
                        feature.id
                );

        for (FeatureRegistry.SubFeature subFeature
                : feature.subFeatures) {
            Switch child =
                    switches.get(
                            subFeature.id
                    );

            if (child != null) {
                child.setEnabled(
                        masterEnabled
                );
            }
        }
    }

    private void updateSubSummary(
            FeatureRegistry.Feature feature
    ) {
        TextView label =
                subSummaryViews.get(
                        feature.id
                );

        LinearLayout container =
                subContainers.get(
                        feature.id
                );

        if (label == null
                || container == null) {
            return;
        }

        int rawEnabled = 0;

        for (FeatureRegistry.SubFeature subFeature
                : feature.subFeatures) {
            if (FeaturePrefs.isEnabled(
                    preferences,
                    subFeature.id
            )) {
                rawEnabled++;
            }
        }

        boolean masterEnabled =
                FeaturePrefs.isEnabled(
                        preferences,
                        feature.id
                );

        boolean expanded =
                container.getVisibility()
                        == View.VISIBLE;

        String arrow =
                expanded ? " ▴" : " ▾";

        if (masterEnabled) {
            label.setText(
                    "子功能 "
                            + rawEnabled
                            + "/"
                            + feature.subFeatures.size()
                            + " 已开启 · 点击"
                            + (expanded
                            ? "收起"
                            : "展开")
                            + arrow
            );
        } else {
            label.setText(
                    "总开关已关闭 · 保留 "
                            + rawEnabled
                            + "/"
                            + feature.subFeatures.size()
                            + " 个子设置 · 点击"
                            + (expanded
                            ? "收起"
                            : "展开")
                            + arrow
            );
        }
    }

    private String buildTargetStatus(
            FeatureRegistry.Feature feature
    ) {
        if (feature.packages.isEmpty()) {
            return "作用范围：当前 LSPosed Scope";
        }

        StringBuilder builder =
                new StringBuilder();

        int installed = 0;

        for (String packageName
                : feature.packages) {
            String version =
                    getVersion(
                            packageName
                    );

            if (version == null) {
                continue;
            }

            installed++;

            if (builder.length() > 0) {
                builder.append(" · ");
            }

            builder.append(
                    "已安装 "
            ).append(version);
        }

        if (installed == 0) {
            return feature.packages.size() == 1
                    ? "目标 App 未安装"
                    : "目标 App 均未安装";
        }

        if (installed
                < feature.packages.size()) {
            builder.append(" · ")
                    .append(installed)
                    .append("/")
                    .append(
                            feature.packages.size()
                    )
                    .append(" 个目标已安装");
        }

        return builder.toString();
    }

    private String getVersion(
            String packageName
    ) {
        try {
            PackageInfo info =
                    getPackageManager()
                            .getPackageInfo(
                                    packageName,
                                    0
                            );

            String version =
                    info.versionName;

            if (version == null
                    || version.trim().isEmpty()) {
                return "版本未知";
            }

            return "v" + version;
        } catch (
                PackageManager.NameNotFoundException ignored
        ) {
            return null;
        }
    }

    private void refreshSummary() {
        int enabledModules = 0;
        int totalModules =
                FeatureRegistry
                        .all()
                        .size();

        int enabledSubFeatures = 0;
        int totalSubFeatures = 0;

        Set<String> targets =
                new HashSet<>();

        for (FeatureRegistry.Feature feature
                : FeatureRegistry.all()) {
            boolean masterEnabled =
                    FeaturePrefs.isEnabled(
                            preferences,
                            feature.id
                    );

            if (masterEnabled) {
                enabledModules++;
            }

            for (FeatureRegistry.SubFeature subFeature
                    : feature.subFeatures) {
                totalSubFeatures++;

                if (masterEnabled
                        && FeaturePrefs.isEnabled(
                        preferences,
                        subFeature.id
                )) {
                    enabledSubFeatures++;
                }
            }

            targets.addAll(
                    feature.packages
            );

            updateSubSummary(
                    feature
            );
        }

        int installedTargets = 0;

        for (String packageName : targets) {
            if (getVersion(packageName)
                    != null) {
                installedTargets++;
            }
        }

        configStatus.setText(
                crossProcessAvailable
                        ? "跨进程配置桥：可用"
                        : "跨进程配置桥：不可用"
        );

        configStatus.setTextColor(
                crossProcessAvailable
                        ? COLOR_OK
                        : COLOR_WARN
        );

        String enabledText =
                String.format(
                        Locale.getDefault(),
                        "已启用模块：%d / %d",
                        enabledModules,
                        totalModules
                );

        if (totalSubFeatures > 0) {
            enabledText +=
                    String.format(
                            Locale.getDefault(),
                            " · 生效子功能：%d / %d",
                            enabledSubFeatures,
                            totalSubFeatures
                    );
        }

        enabledSummary.setText(
                enabledText
        );

        installedSummary.setText(
                String.format(
                        Locale.getDefault(),
                        "已安装目标：%d / %d",
                        installedTargets,
                        targets.size()
                )
        );

        long changedAt =
                preferences.getLong(
                        FeaturePrefs
                                .KEY_LAST_CHANGED_AT,
                        0L
                );

        changedSummary.setText(
                changedAt <= 0L
                        ? "配置修改：尚未修改"
                        : "最近修改："
                        + DateFormat
                        .getDateTimeInstance(
                                DateFormat.SHORT,
                                DateFormat.SHORT
                        )
                        .format(
                                new Date(
                                        changedAt
                                )
                        )
        );
    }

    private void setAllFeatures(
            boolean enabled
    ) {
        SharedPreferences.Editor editor =
                preferences.edit();

        for (String key
                : FeatureRegistry
                .allPreferenceKeys()) {
            editor.putBoolean(
                    key,
                    enabled
            );
        }

        editor.putLong(
                FeaturePrefs
                        .KEY_LAST_CHANGED_AT,
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

        updatingSwitches = true;

        try {
            for (Switch toggle
                    : switches.values()) {
                toggle.setChecked(
                        enabled
                );
            }
        } finally {
            updatingSwitches = false;
        }

        refreshChildSwitchStates();
        refreshSummary();
    }

    private void resetDefaults() {
        SharedPreferences.Editor editor =
                preferences.edit();

        for (String key
                : FeatureRegistry
                .allPreferenceKeys()) {
            editor.remove(key);
        }

        editor.remove(
                "screenshot_privacy"
        );

        editor.putLong(
                FeaturePrefs
                        .KEY_LAST_CHANGED_AT,
                System.currentTimeMillis()
        );

        if (!editor.commit()) {
            Toast.makeText(
                    this,
                    "恢复失败",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        updatingSwitches = true;

        try {
            for (Switch toggle
                    : switches.values()) {
                toggle.setChecked(true);
            }
        } finally {
            updatingSwitches = false;
        }

        refreshChildSwitchStates();
        refreshSummary();

        Toast.makeText(
                this,
                "已恢复默认：总开关与子功能全部开启",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void refreshChildSwitchStates() {
        for (FeatureRegistry.Feature feature
                : FeatureRegistry.all()) {
            updateChildSwitchStates(
                    feature
            );
        }
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

        card.setBackground(
                background
        );

        return card;
    }

    private TextView sectionTitle(
            String value
    ) {
        return text(
                value,
                15,
                COLOR_SUBTEXT,
                Typeface.BOLD
        );
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
        button.setTextSize(13);
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
                        dp(48),
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

    private int dp(int value) {
        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        return Math.round(
                value * density
        );
    }
}
