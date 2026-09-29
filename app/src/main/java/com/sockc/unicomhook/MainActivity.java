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
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class MainActivity extends Activity {

    private static final int COLOR_BG = Color.rgb(246, 247, 249);
    private static final int COLOR_CARD = Color.WHITE;
    private static final int COLOR_TEXT = Color.rgb(28, 30, 34);
    private static final int COLOR_SUBTEXT = Color.rgb(103, 109, 120);
    private static final int COLOR_ACCENT = Color.rgb(30, 112, 255);
    private static final int COLOR_OK = Color.rgb(26, 143, 86);
    private static final int COLOR_WARN = Color.rgb(197, 119, 0);

    private SharedPreferences preferences;
    private boolean crossProcessAvailable;
    private boolean updatingSwitches;

    private TextView configStatus;
    private TextView enabledSummary;
    private TextView installedSummary;
    private TextView changedSummary;

    private final Map<String, Switch> switches =
            new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(COLOR_BG);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );

        FeaturePrefs.OpenResult openResult =
                FeaturePrefs.open(this);
        preferences = openResult.preferences;
        crossProcessAvailable =
                openResult.crossProcessAvailable;

        setContentView(buildContent());
        refreshSummary();
    }

    private View buildContent() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(COLOR_BG);

        LinearLayout root = vertical();
        root.setPadding(dp(18), dp(20), dp(18), dp(32));
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
                "V1.2.0 · LSPosed 模块管理",
                14,
                COLOR_SUBTEXT,
                Typeface.NORMAL
        );
        LinearLayout.LayoutParams subtitleParams =
                wrapMatch();
        subtitleParams.topMargin = dp(2);
        root.addView(subtitle, subtitleParams);

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
                "修改开关后，请强制停止并重新打开对应目标 App。"
                        + " 未读取到配置时，为避免影响现有功能，Hook 默认保持开启。",
                12,
                COLOR_SUBTEXT,
                Typeface.NORMAL
        );
        footer.setLineSpacing(0, 1.15f);
        LinearLayout.LayoutParams footerParams =
                topMargin(dp(20));
        footerParams.bottomMargin = dp(12);
        root.addView(footer, footerParams);

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
                    "当前只能写入本机私有配置。请先在 LSPosed 中启用 S Tool，"
                            + "然后重新打开本应用，功能开关才会真正作用于 Hook。",
                    13,
                    COLOR_WARN,
                    Typeface.BOLD
            );
            warning.setLineSpacing(0, 1.15f);
            card.addView(
                    warning,
                    topMargin(dp(12))
            );
        }

        return card;
    }

    private View buildQuickActions() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        Button enableAll = actionButton("全部开启");
        Button disableAll = actionButton("全部关闭");
        Button reset = actionButton("恢复默认");

        enableAll.setEnabled(crossProcessAvailable);
        disableAll.setEnabled(crossProcessAvailable);
        reset.setEnabled(crossProcessAvailable);

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
                weightedButtonParams(1f, 0)
        );
        row.addView(
                disableAll,
                weightedButtonParams(1f, dp(8))
        );
        row.addView(
                reset,
                weightedButtonParams(1f, dp(8))
        );

        return row;
    }

    private View buildFeatureCard(
            FeatureRegistry.Feature feature
    ) {
        LinearLayout card = card();

        LinearLayout firstRow = new LinearLayout(this);
        firstRow.setOrientation(LinearLayout.HORIZONTAL);
        firstRow.setGravity(Gravity.CENTER_VERTICAL);

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

        Switch toggle = new Switch(this);
        toggle.setChecked(
                FeaturePrefs.isEnabled(
                        preferences,
                        feature.id
                )
        );
        toggle.setEnabled(crossProcessAvailable);
        switches.put(feature.id, toggle);

        toggle.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    if (updatingSwitches) {
                        return;
                    }

                    boolean saved =
                            FeaturePrefs.setEnabled(
                                    preferences,
                                    feature.id,
                                    isChecked
                            );

                    if (!saved) {
                        Toast.makeText(
                                this,
                                "保存失败",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    refreshSummary();
                }
        );

        firstRow.addView(toggle);
        card.addView(firstRow);

        TextView summary = text(
                feature.summary,
                13,
                COLOR_SUBTEXT,
                Typeface.NORMAL
        );
        summary.setLineSpacing(0, 1.12f);
        card.addView(
                summary,
                topMargin(dp(10))
        );

        return card;
    }

    private String buildTargetStatus(
            FeatureRegistry.Feature feature
    ) {
        if (feature.packages.isEmpty()) {
            return "作用范围：当前 LSPosed Scope";
        }

        StringBuilder builder = new StringBuilder();
        int installed = 0;

        for (String packageName : feature.packages) {
            String version = getVersion(packageName);
            if (version != null) {
                installed++;
                if (builder.length() > 0) {
                    builder.append(" · ");
                }
                builder.append("已安装 ").append(version);
            }
        }

        if (installed == 0) {
            return feature.packages.size() == 1
                    ? "目标 App 未安装"
                    : "目标 App 均未安装";
        }

        if (installed < feature.packages.size()) {
            builder.append(" · ")
                    .append(installed)
                    .append("/")
                    .append(feature.packages.size())
                    .append(" 个目标已安装");
        }

        return builder.toString();
    }

    private String getVersion(String packageName) {
        try {
            PackageInfo info = getPackageManager()
                    .getPackageInfo(packageName, 0);
            String version = info.versionName;
            if (version == null || version.trim().isEmpty()) {
                return "版本未知";
            }
            return "v" + version;
        } catch (PackageManager.NameNotFoundException ignored) {
            return null;
        }
    }

    private void refreshSummary() {
        int enabled = 0;
        int total = FeatureRegistry.all().size();

        for (FeatureRegistry.Feature feature
                : FeatureRegistry.all()) {
            if (FeaturePrefs.isEnabled(
                    preferences,
                    feature.id
            )) {
                enabled++;
            }
        }

        int installedTargets = 0;
        int targetCount = 0;

        for (FeatureRegistry.Feature feature
                : FeatureRegistry.all()) {
            for (String packageName : feature.packages) {
                targetCount++;
                if (getVersion(packageName) != null) {
                    installedTargets++;
                }
            }
        }

        configStatus.setText(
                crossProcessAvailable
                        ? "跨进程配置：可用"
                        : "跨进程配置：不可用"
        );
        configStatus.setTextColor(
                crossProcessAvailable
                        ? COLOR_OK
                        : COLOR_WARN
        );

        enabledSummary.setText(
                String.format(
                        Locale.getDefault(),
                        "已启用模块：%d / %d",
                        enabled,
                        total
                )
        );

        installedSummary.setText(
                String.format(
                        Locale.getDefault(),
                        "已安装目标：%d / %d",
                        installedTargets,
                        targetCount
                )
        );

        long changedAt = preferences.getLong(
                FeaturePrefs.KEY_LAST_CHANGED_AT,
                0L
        );

        changedSummary.setText(
                changedAt <= 0L
                        ? "配置修改：尚未修改"
                        : "最近修改："
                        + DateFormat.getDateTimeInstance(
                                DateFormat.SHORT,
                                DateFormat.SHORT
                        ).format(new Date(changedAt))
        );
    }

    private void setAllFeatures(boolean enabled) {
        SharedPreferences.Editor editor =
                preferences.edit();

        for (FeatureRegistry.Feature feature
                : FeatureRegistry.all()) {
            editor.putBoolean(feature.id, enabled);
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

        updatingSwitches = true;
        try {
            for (Switch toggle : switches.values()) {
                toggle.setChecked(enabled);
            }
        } finally {
            updatingSwitches = false;
        }

        refreshSummary();
    }

    private void resetDefaults() {
        SharedPreferences.Editor editor =
                preferences.edit();

        for (FeatureRegistry.Feature feature
                : FeatureRegistry.all()) {
            editor.remove(feature.id);
        }

        editor.putLong(
                FeaturePrefs.KEY_LAST_CHANGED_AT,
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
            for (Switch toggle : switches.values()) {
                toggle.setChecked(true);
            }
        } finally {
            updatingSwitches = false;
        }

        refreshSummary();

        Toast.makeText(
                this,
                "已恢复默认：全部开启",
                Toast.LENGTH_SHORT
        ).show();
    }

    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
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
        background.setColor(COLOR_CARD);
        background.setCornerRadius(dp(15));
        background.setStroke(
                dp(1),
                Color.rgb(232, 234, 238)
        );
        card.setBackground(background);

        return card;
    }

    private TextView sectionTitle(String value) {
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
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(
                Typeface.DEFAULT,
                style
        );
        return view;
    }

    private Button actionButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setTextColor(COLOR_ACCENT);
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
        params.leftMargin = leftMargin;
        return params;
    }

    private LinearLayout.LayoutParams wrapMatch() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams topMargin(int margin) {
        LinearLayout.LayoutParams params = wrapMatch();
        params.topMargin = margin;
        return params;
    }

    private int dp(int value) {
        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;
        return Math.round(value * density);
    }
}
