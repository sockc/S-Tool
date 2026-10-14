package com.sockc.unicomhook;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Offline-only SMS display labels. A bracket signature is NOT verified identity. */
final class SmsSenderRecognizer {
    private static final Pattern SIGNATURE = Pattern.compile(
            "[【\\[]([^】\\]\\r\\n]{2,20})[】\\]]");
    private static final Pattern INVALID_NAME = Pattern.compile(
            ".*(?:验证码|校验码|动态码|登录码|短信通知|温馨提示|尊敬的|用户|[0-9]{4,}).*");

    private static final Map<String, String> OFFICIAL;
    static {
        Map<String, String> known = new HashMap<>();
        known.put("10010", "中国联通");
        known.put("10086", "中国移动");
        known.put("10000", "中国电信");
        known.put("10099", "中国广电");
        known.put("95588", "工商银行");
        known.put("95533", "建设银行");
        known.put("95566", "中国银行");
        known.put("95599", "农业银行");
        known.put("95555", "招商银行");
        known.put("95528", "浦发银行");
        known.put("95511", "平安银行");
        known.put("95595", "光大银行");
        known.put("95577", "华夏银行");
        known.put("95559", "交通银行");
        OFFICIAL = Collections.unmodifiableMap(known);
    }

    private SmsSenderRecognizer() { }

    /** Only accept service-like numeric senders; never turn arbitrary prose into a number. */
    static String normalizeSender(String input) {
        if (input == null) return null;
        String value = input.replaceAll("[\\s()\\-+]", "");
        if (!value.matches("[0-9]{5,24}")) return null;
        return value;
    }

    static String official(String sender) {
        return OFFICIAL.get(sender);
    }

    static String signature(String preview) {
        if (preview == null || preview.length() > 2000) return null;
        Matcher matcher = SIGNATURE.matcher(preview);
        while (matcher.find()) {
            String candidate = matcher.group(1).trim();
            if (validName(candidate) && !INVALID_NAME.matcher(candidate).matches()) {
                return candidate;
            }
        }
        return null;
    }

    static boolean validName(String value) {
        if (value == null) return false;
        String trimmed = value.trim();
        return trimmed.length() >= 2 && trimmed.length() <= 20
                && !trimmed.contains("=")
                && !trimmed.matches("[0-9\\s\\p{Punct}]+")
                && !trimmed.contains("\n");
    }
}
