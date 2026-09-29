package com.sockc.unicomhook;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class FeatureRegistry {

    public static final class SubFeature {
        public final String id;
        public final String title;
        public final String summary;

        private SubFeature(
                String id,
                String title,
                String summary
        ) {
            this.id = id;
            this.title = title;
            this.summary = summary;
        }
    }

    public static final class Feature {
        public final String id;
        public final String title;
        public final String summary;
        public final List<String> packages;
        public final List<SubFeature> subFeatures;

        private Feature(
                String id,
                String title,
                String summary,
                List<SubFeature> subFeatures,
                String... packages
        ) {
            this.id = id;
            this.title = title;
            this.summary = summary;
            this.packages = Collections.unmodifiableList(
                    Arrays.asList(packages)
            );
            this.subFeatures = Collections.unmodifiableList(
                    subFeatures
            );
        }
    }

    private static SubFeature sub(
            String id,
            String title,
            String summary
    ) {
        return new SubFeature(id, title, summary);
    }

    private static Feature feature(
            String id,
            String title,
            String summary,
            String... packages
    ) {
        return new Feature(
                id,
                title,
                summary,
                Collections.<SubFeature>emptyList(),
                packages
        );
    }

    private static Feature featureWithSubs(
            String id,
            String title,
            String summary,
            List<SubFeature> subFeatures,
            String... packages
    ) {
        return new Feature(
                id,
                title,
                summary,
                subFeatures,
                packages
        );
    }

    private static final List<Feature> FEATURES =
            Collections.unmodifiableList(Arrays.asList(
                    featureWithSubs(
                            "unicom",
                            "中国联通",
                            "广告净化与隐私保护",
                            Arrays.asList(
                                    sub(
                                            "unicom.cold_splash",
                                            "冷启动广告",
                                            "跳过 WelcomeClient 开屏页"
                                    ),
                                    sub(
                                            "unicom.warm_splash",
                                            "热启动广告",
                                            "从后台切回前台时扫描并点击跳过"
                                    ),
                                    sub(
                                            "unicom.permission_privacy",
                                            "权限保护",
                                            "对定位、联系人和媒体读取权限进行保护"
                                    ),
                                    sub(
                                            "unicom.location_privacy",
                                            "定位保护",
                                            "拦截最近定位并返回保护位置"
                                    ),
                                    sub(
                                            "unicom.screenshot_privacy",
                                            "截图隐私",
                                            "阻止截图回调与媒体库截图监听"
                                    )
                            ),
                            "com.sinovatech.unicom.ui"
                    ),
                    featureWithSubs(
                            "gaode",
                            "高德地图",
                            "首页净化与广告处理",
                            Arrays.asList(
                                    sub(
                                            "gaode.ad_skip",
                                            "广告跳过",
                                            "识别“跳过/关闭广告”按钮并自动点击"
                                    ),
                                    sub(
                                            "gaode.explore_local",
                                            "隐藏探索本地",
                                            "隐藏首页“探索本地”整块内容"
                                    ),
                                    sub(
                                            "gaode.bottom_tabs",
                                            "精简底部导航",
                                            "隐藏探索、AI 对话、路线等指定入口"
                                    ),
                                    sub(
                                            "gaode.float_badges",
                                            "隐藏运营挂件",
                                            "隐藏扫街榜、订周末等右侧运营挂件"
                                    )
                            ),
                            "com.autonavi.minimap"
                    ),
                    featureWithSubs(
                            "taobao",
                            "淘宝",
                            "冷启动与热启动广告处理",
                            Arrays.asList(
                                    sub(
                                            "taobao.cold_splash",
                                            "冷启动广告",
                                            "启动页出现时自动扫描并点击跳过"
                                    ),
                                    sub(
                                            "taobao.warm_splash",
                                            "热启动广告",
                                            "后台切回前台时自动处理广告"
                                    )
                            ),
                            "com.taobao.taobao"
                    ),
                    feature(
                            "tiktok",
                            "TikTok",
                            "区域环境处理与登录界面处理",
                            "com.zhiliaoapp.musically"
                    ),
                    feature(
                            "qq",
                            "QQ",
                            "版本升级 Activity 与升级弹窗处理",
                            "com.tencent.mobileqq"
                    ),
                    feature(
                            "hikvision",
                            "Hik-Connect",
                            "启动广告跳过处理",
                            "com.hikvision.hikconnect"
                    ),
                    feature(
                            "guazi",
                            "瓜子二手车",
                            "启动广告跳过处理",
                            "com.ganji.android.haoche"
                    ),
                    feature(
                            "che300",
                            "车300",
                            "个人版与专业版启动广告处理",
                            "com.car300.activity",
                            "com.che300.price"
                    ),
                    feature(
                            "firsty",
                            "Firsty",
                            "广告关闭按钮检测与自动处理",
                            "com.firsty.app"
                    ),
                    featureWithSubs(
                            "meituan",
                            "美团",
                            "广告处理与隐私隔离",
                            Arrays.asList(
                                    sub(
                                            "meituan.file_privacy",
                                            "文件隐私",
                                            "阻止扫描 DCIM、Pictures、Download"
                                    ),
                                    sub(
                                            "meituan.splash_ad",
                                            "开屏广告",
                                            "识别并自动点击开屏跳过按钮"
                                    ),
                                    sub(
                                            "meituan.permission_privacy",
                                            "权限保护",
                                            "拒绝定位、联系人、相机、媒体等敏感权限"
                                    ),
                                    sub(
                                            "meituan.device_id_privacy",
                                            "设备标识保护",
                                            "保护 Device ID 与 Subscriber ID"
                                    )
                            ),
                            "com.sankuai.meituan"
                    ),
                    featureWithSubs(
                            "pinduoduo",
                            "拼多多",
                            "广告处理与文件系统隐私隔离",
                            Arrays.asList(
                                    sub(
                                            "pinduoduo.file_privacy",
                                            "文件隐私",
                                            "阻止扫描常见媒体与下载目录"
                                    ),
                                    sub(
                                            "pinduoduo.permission_privacy",
                                            "权限保护",
                                            "拒绝存储、媒体、设备状态等敏感权限"
                                    ),
                                    sub(
                                            "pinduoduo.splash_ad",
                                            "开屏广告",
                                            "识别并自动点击开屏跳过按钮"
                                    )
                            ),
                            "com.xunmeng.pinduoduo"
                    ),
                    feature(
                            "clipboard",
                            "剪贴板保护",
                            "对 LSPosed 作用域中的应用隐藏剪贴板内容"
                    ),
                    feature(
                            "zhihuijia",
                            "智汇家",
                            "版本升级弹窗处理",
                            "com.changhong.ssc.cookbook"
                    ),
                    feature(
                            "xianyu",
                            "闲鱼",
                            "开屏广告与 WebView 广告处理",
                            "com.taobao.idlefish"
                    ),
                    feature(
                            "sms_code",
                            "短信验证码",
                            "Google 信息验证码识别并复制到剪贴板",
                            "com.google.android.apps.messaging"
                    ),
                    feature(
                            "yingyongbao",
                            "应用宝",
                            "开屏与弹窗广告处理",
                            "com.tencent.android.qqdownloader"
                    ),
                    feature(
                            "oplus_game",
                            "OPPO 游戏助手",
                            "游戏自动化能力与配置兼容处理",
                            "com.oplus.games"
                    ),
                    feature(
                            "airvoy",
                            "Airvoy",
                            "真实奖励回调完成后自动尝试关闭广告页",
                            "com.airvoy.airvoy"
                    )
            ));

    private FeatureRegistry() {
    }

    public static List<Feature> all() {
        return FEATURES;
    }

    public static List<String> allPreferenceKeys() {
        List<String> keys = new ArrayList<>();

        for (Feature feature : FEATURES) {
            keys.add(feature.id);

            for (SubFeature subFeature : feature.subFeatures) {
                keys.add(subFeature.id);
            }
        }

        return keys;
    }
}
