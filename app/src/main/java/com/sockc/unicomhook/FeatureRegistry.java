package com.sockc.unicomhook;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class FeatureRegistry {

    public static final class Feature {
        public final String id;
        public final String title;
        public final String summary;
        public final List<String> packages;

        private Feature(
                String id,
                String title,
                String summary,
                String... packages
        ) {
            this.id = id;
            this.title = title;
            this.summary = summary;
            this.packages = Collections.unmodifiableList(
                    Arrays.asList(packages)
            );
        }
    }

    private static final List<Feature> FEATURES =
            Collections.unmodifiableList(Arrays.asList(
                    new Feature(
                            "unicom",
                            "中国联通",
                            "开屏净化、热启动广告处理与隐私保护",
                            "com.sinovatech.unicom.ui"
                    ),
                    new Feature(
                            "screenshot_privacy",
                            "联通截图隐私",
                            "阻止联通客户端监听系统截图与媒体库截图变化",
                            "com.sinovatech.unicom.ui"
                    ),
                    new Feature(
                            "gaode",
                            "高德地图",
                            "首页净化、运营卡片处理与开屏广告处理",
                            "com.autonavi.minimap"
                    ),
                    new Feature(
                            "taobao",
                            "淘宝",
                            "冷启动与热启动广告处理",
                            "com.taobao.taobao"
                    ),
                    new Feature(
                            "tiktok",
                            "TikTok",
                            "区域环境处理与登录界面处理",
                            "com.zhiliaoapp.musically"
                    ),
                    new Feature(
                            "qq",
                            "QQ",
                            "版本升级 Activity 与升级弹窗处理",
                            "com.tencent.mobileqq"
                    ),
                    new Feature(
                            "hikvision",
                            "Hik-Connect",
                            "启动广告跳过处理",
                            "com.hikvision.hikconnect"
                    ),
                    new Feature(
                            "guazi",
                            "瓜子二手车",
                            "启动广告跳过处理",
                            "com.ganji.android.haoche"
                    ),
                    new Feature(
                            "che300",
                            "车300",
                            "个人版与专业版启动广告处理",
                            "com.car300.activity",
                            "com.che300.price"
                    ),
                    new Feature(
                            "firsty",
                            "Firsty",
                            "广告关闭按钮检测与自动处理",
                            "com.firsty.app"
                    ),
                    new Feature(
                            "meituan",
                            "美团",
                            "广告处理与本地文件隐私隔离",
                            "com.sankuai.meituan"
                    ),
                    new Feature(
                            "pinduoduo",
                            "拼多多",
                            "广告处理与文件系统隐私隔离",
                            "com.xunmeng.pinduoduo"
                    ),
                    new Feature(
                            "clipboard",
                            "剪贴板保护",
                            "对 LSPosed 作用域中的应用隐藏剪贴板内容"
                    ),
                    new Feature(
                            "zhihuijia",
                            "智汇家",
                            "版本升级弹窗处理",
                            "com.changhong.ssc.cookbook"
                    ),
                    new Feature(
                            "xianyu",
                            "闲鱼",
                            "开屏广告与 WebView 广告处理",
                            "com.taobao.idlefish"
                    ),
                    new Feature(
                            "sms_code",
                            "短信验证码",
                            "Google 信息验证码识别并复制到剪贴板",
                            "com.google.android.apps.messaging"
                    ),
                    new Feature(
                            "yingyongbao",
                            "应用宝",
                            "开屏与弹窗广告处理",
                            "com.tencent.android.qqdownloader"
                    ),
                    new Feature(
                            "oplus_game",
                            "OPPO 游戏助手",
                            "游戏自动化能力与配置兼容处理",
                            "com.oplus.games"
                    ),
                    new Feature(
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
}
