package com.sockc.unicomhook;

import android.app.Activity;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XianyuHook implements IXposedHookLoadPackage {
    private static final String TAG = "Sockc_Xianyu: ";
    private static final String TARGET_PACKAGE = "com.taobao.idlefish";
    private static final String MAIN_ACTIVITY = "com.taobao.idlefish.maincontainer.activity.MainActivity";
    private static final String INIT_ACTIVITY = "com.taobao.fleamarket.home.activity.InitActivity";

    private static final Set<String> BLOCK_KEYS = new HashSet<>(Arrays.asList(
            "splash", "startup", "launch", "advert", "advertise", "adservice",
            "screenad", "openad", "startad", "preloadad", "jumpad",
            "开屏", "广告", "启动图"
    ));

    private static volatile WeakReference<Activity> sTopActivity = new WeakReference<>(null);
    private static volatile long sStartupBegin = 0L;

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) return;
        if (!TARGET_PACKAGE.equals(lpparam.processName)) return;

        XposedBridge.log(TAG + "V5 已注入主进程: " + lpparam.processName);

        hookActivityLifecycle();
        hookAndroidWebView();
        hookUcWebViewIfExists(lpparam.classLoader);
        hookAddView();
    }

    private void hookActivityLifecycle() {
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                String cls = activity.getClass().getName();

                sTopActivity = new WeakReference<>(activity);

                if (MAIN_ACTIVITY.equals(cls) || INIT_ACTIVITY.equals(cls)) {
                    sStartupBegin = SystemClock.elapsedRealtime();
                    XposedBridge.log(TAG + "进入启动窗口: " + cls);
                }
            }
        });
    }

    private void hookAndroidWebView() {
        XposedBridge.hookAllMethods(WebView.class, "loadUrl", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args == null || param.args.length == 0) return;
                Object arg0 = param.args[0];
                if (!(arg0 instanceof String)) return;

                String url = (String) arg0;
                if (shouldBlockUrl(url)) {
                    XposedBridge.log(TAG + "拦截 Android WebView URL: " + url);
                    param.setResult(null);
                }
            }
        });

        XposedBridge.hookAllMethods(WebView.class, "loadDataWithBaseURL", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args == null || param.args.length < 1) return;
                Object baseUrl = param.args[0];
                if (baseUrl instanceof String && shouldBlockUrl((String) baseUrl)) {
                    XposedBridge.log(TAG + "拦截 Android WebView BaseURL: " + baseUrl);
                    param.setResult(null);
                }
            }
        });
    }

    private void hookUcWebViewIfExists(ClassLoader cl) {
        try {
            Class<?> ucWebView = XposedHelpers.findClass("com.uc.webview.export.WebView", cl);

            XposedBridge.hookAllMethods(ucWebView, "loadUrl", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args == null || param.args.length == 0) return;
                    Object arg0 = param.args[0];
                    if (!(arg0 instanceof String)) return;

                    String url = (String) arg0;
                    if (shouldBlockUrl(url)) {
                        XposedBridge.log(TAG + "拦截 UC WebView URL: " + url);
                        param.setResult(null);
                    }
                }
            });

            XposedBridge.log(TAG + "已挂上 UC WebView");
        } catch (Throwable ignored) {
            XposedBridge.log(TAG + "未发现 UC WebView，跳过");
        }
    }

    private void hookAddView() {
        XposedBridge.hookAllMethods(ViewGroup.class, "addView", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Activity top = sTopActivity.get();
                if (top == null) return;

                String actName = top.getClass().getName();
                if (!MAIN_ACTIVITY.equals(actName) && !INIT_ACTIVITY.equals(actName)) return;

                long delta = SystemClock.elapsedRealtime() - sStartupBegin;
                if (delta < 0 || delta > 8000) return;

                if (param.args == null || param.args.length == 0) return;
                Object first = param.args[0];
                if (!(first instanceof View)) return;

                View child = (View) first;

                if (shouldBlockStartupView(top, child)) {
                    XposedBridge.log(TAG + "拦截启动广告 View: " + viewInfo(child));
                    param.setResult(null);
                }
            }
        });
    }

    private boolean shouldBlockUrl(String url) {
        if (TextUtils.isEmpty(url)) return false;

        String lower = url.toLowerCase();

        // 只拦看起来像广告/开屏的 URL，避免误伤正常页面
        for (String k : BLOCK_KEYS) {
            if (lower.contains(k.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldBlockStartupView(Activity activity, View view) {
        if (view == null) return false;

        String cls = view.getClass().getName().toLowerCase();
        String desc = view.getContentDescription() == null ? "" : view.getContentDescription().toString().toLowerCase();
        String idName = getIdName(view).toLowerCase();

        boolean keywordHit = containsBlockKey(cls) || containsBlockKey(desc) || containsBlockKey(idName);
        boolean webLike = cls.contains("webview") || cls.contains("windvane") || cls.contains("h5");

        ViewGroup.LayoutParams lp = view.getLayoutParams();
        int width = 0;
        int height = 0;
        if (lp != null) {
            width = lp.width;
            height = lp.height;
        }

        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        int sw = dm.widthPixels;
        int sh = dm.heightPixels;

        boolean fullScreenLike =
                (width == ViewGroup.LayoutParams.MATCH_PARENT || width >= sw * 0.8f) &&
                (height == ViewGroup.LayoutParams.MATCH_PARENT || height >= sh * 0.5f);

        return keywordHit || (webLike && fullScreenLike);
    }

    private boolean containsBlockKey(String s) {
        if (TextUtils.isEmpty(s)) return false;
        String lower = s.toLowerCase();
        for (String k : BLOCK_KEYS) {
            if (lower.contains(k.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String getIdName(View view) {
        int id = view.getId();
        if (id == View.NO_ID) return "";
        try {
            return view.getResources().getResourceEntryName(id);
        } catch (Throwable ignored) {
            return String.valueOf(id);
        }
    }

    private String viewInfo(View view) {
        String idName = getIdName(view);
        String desc = view.getContentDescription() == null ? "" : view.getContentDescription().toString();
        ViewGroup.LayoutParams lp = view.getLayoutParams();

        int w = lp == null ? 0 : lp.width;
        int h = lp == null ? 0 : lp.height;

        return view.getClass().getName()
                + " id=" + idName
                + " desc=" + desc
                + " lp=[" + w + "," + h + "]";
    }
}
