package com.sockc.unicomhook;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "跳过", "关闭", "跳過", "略过", "skip", "close"
    ));

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) return;
        if (!TARGET_PACKAGE.equals(lpparam.processName)) return;

        XposedBridge.log(TAG + "V4 已注入主进程: " + lpparam.processName);

        XposedHelpers.findAndHookMethod(
                Activity.class,
                "onWindowFocusChanged",
                boolean.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Activity activity = (Activity) param.thisObject;
                        boolean hasFocus = (boolean) param.args[0];
                        String cls = activity.getClass().getName();

                        if (!MAIN_ACTIVITY.equals(cls)) return;
                        if (!hasFocus) return;

                        XposedBridge.log(TAG + "命中 MainActivity 焦点，开始扫描");

                        View root = activity.getWindow() == null ? null : activity.getWindow().getDecorView();
                        if (root == null) return;

                        startScan(activity, root);
                    }
                }
        );
    }

    private void startScan(final Activity activity, final View root) {
        final Handler handler = new Handler(Looper.getMainLooper());

        Runnable task = new Runnable() {
            int count = 0;
            boolean done = false;

            @Override
            public void run() {
                if (done) return;

                try {
                    if (activity.isFinishing()) return;

                    count++;
                    XposedBridge.log(TAG + "第 " + count + " 次扫描");

                    // 先找“跳过/关闭”文字控件
                    View textTarget = findKeywordView(root);
                    if (textTarget != null) {
                        XposedBridge.log(TAG + "找到文字候选: " + viewInfo(textTarget));
                        if (clickView(textTarget)) {
                            XposedBridge.log(TAG + "文字候选点击成功");
                            done = true;
                            return;
                        }
                    }

                    // 再找右上角小 clickable View
                    View topRight = findTopRightCandidate(root, root.getWidth(), root.getHeight(), activity);
                    if (topRight != null) {
                        XposedBridge.log(TAG + "找到右上角候选: " + viewInfo(topRight));
                        if (clickView(topRight)) {
                            XposedBridge.log(TAG + "右上角候选点击成功");
                            done = true;
                            return;
                        }
                    }

                    // 只打印可疑控件，别整棵树刷爆
                    dumpInterestingViews(root, 0, root.getWidth(), root.getHeight(), activity);

                    if (count < 15) {
                        handler.postDelayed(this, 200);
                    } else {
                        XposedBridge.log(TAG + "扫描结束，未命中候选控件");
                    }
                } catch (Throwable t) {
                    XposedBridge.log(TAG + "扫描异常: " + t);
                }
            }
        };

        handler.postDelayed(task, 150);
    }

    private boolean clickView(View view) {
        try {
            if (view == null || !view.isShown()) return false;

            if (view.isClickable()) {
                boolean ok = view.performClick();
                XposedBridge.log(TAG + "performClick=" + ok);
                if (ok) return true;
            }

            View parent = findClickableParent(view);
            if (parent != null) {
                boolean ok2 = parent.performClick();
                XposedBridge.log(TAG + "parent.performClick=" + ok2);
                if (ok2) return true;

                return dispatchCenterTap(parent);
            }

            return dispatchCenterTap(view);
        } catch (Throwable t) {
            XposedBridge.log(TAG + "clickView 异常: " + t);
            return false;
        }
    }

    private boolean dispatchCenterTap(View view) {
        try {
            Rect r = new Rect();
            if (!view.getGlobalVisibleRect(r)) return false;

            float x = r.centerX();
            float y = r.centerY();

            long downTime = android.os.SystemClock.uptimeMillis();
            MotionEvent down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0);
            MotionEvent up = MotionEvent.obtain(downTime, downTime + 80, MotionEvent.ACTION_UP, x, y, 0);

            View root = view.getRootView();
            boolean d1 = root.dispatchTouchEvent(down);
            boolean d2 = root.dispatchTouchEvent(up);

            down.recycle();
            up.recycle();

            XposedBridge.log(TAG + "dispatchTap result=" + d1 + "/" + d2 + " @(" + x + "," + y + ")");
            return d1 || d2;
        } catch (Throwable t) {
            XposedBridge.log(TAG + "dispatchCenterTap 异常: " + t);
            return false;
        }
    }

    private View findKeywordView(View view) {
        if (view == null || !view.isShown()) return null;

        CharSequence text = null;
        CharSequence desc = view.getContentDescription();

        if (view instanceof TextView) {
            text = ((TextView) view).getText();
        }

        if (!TextUtils.isEmpty(text) && containsKeyword(text.toString())) {
            return view;
        }

        if (!TextUtils.isEmpty(desc) && containsKeyword(desc.toString())) {
            return view;
        }

        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View found = findKeywordView(vg.getChildAt(i));
                if (found != null) return found;
            }
        }

        return null;
    }

    private View findTopRightCandidate(View view, int screenW, int screenH, Activity activity) {
        if (view == null || !view.isShown()) return null;

        Rect r = new Rect();
        view.getGlobalVisibleRect(r);

        int w = r.width();
        int h = r.height();

        int maxW = dp(activity, 140);
        int maxH = dp(activity, 90);
        int topLimit = dp(activity, 260);
        int rightLimit = dp(activity, 260);

        boolean positionOk = r.top >= 0
                && r.top <= topLimit
                && r.right >= (screenW - rightLimit);

        boolean sizeOk = w > 0 && h > 0 && w <= maxW && h <= maxH;

        boolean candidate = view.isClickable() && positionOk && sizeOk;

        if (candidate) return view;

        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View found = findTopRightCandidate(vg.getChildAt(i), screenW, screenH, activity);
                if (found != null) return found;
            }
        }

        return null;
    }

    private void dumpInterestingViews(View view, int depth, int screenW, int screenH, Activity activity) {
        if (view == null || !view.isShown()) return;

        Rect r = new Rect();
        view.getGlobalVisibleRect(r);

        boolean hasText = false;
        boolean hasDesc = !TextUtils.isEmpty(view.getContentDescription());
        boolean clickable = view.isClickable();

        if (view instanceof TextView) {
            CharSequence t = ((TextView) view).getText();
            hasText = !TextUtils.isEmpty(t);
        }

        int maxW = dp(activity, 160);
        int maxH = dp(activity, 100);
        boolean topRightSmall = clickable
                && r.top >= 0
                && r.top <= dp(activity, 280)
                && r.right >= (screenW - dp(activity, 280))
                && r.width() <= maxW
                && r.height() <= maxH;

        if (hasText || hasDesc || topRightSmall) {
            XposedBridge.log(TAG + viewInfo(view));
        }

        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                dumpInterestingViews(vg.getChildAt(i), depth + 1, screenW, screenH, activity);
            }
        }
    }

    private String viewInfo(View view) {
        Rect rect = new Rect();
        view.getGlobalVisibleRect(rect);

        String text = "";
        String desc = "";
        String idName = "";

        if (view instanceof TextView) {
            CharSequence t = ((TextView) view).getText();
            if (!TextUtils.isEmpty(t)) {
                text = " text=\"" + safe(t.toString()) + "\"";
            }
        }

        CharSequence d = view.getContentDescription();
        if (!TextUtils.isEmpty(d)) {
            desc = " desc=\"" + safe(d.toString()) + "\"";
        }

        int id = view.getId();
        if (id != View.NO_ID) {
            try {
                idName = " id=" + view.getResources().getResourceEntryName(id);
            } catch (Throwable ignored) {
                idName = " id=" + id;
            }
        }

        return view.getClass().getName()
                + " clickable=" + view.isClickable()
                + " rect=[" + rect.left + "," + rect.top + "," + rect.right + "," + rect.bottom + "]"
                + idName + text + desc;
    }

    private String safe(String s) {
        s = s.replace("\n", "\\n").replace("\r", "");
        return s.length() > 60 ? s.substring(0, 60) + "..." : s;
    }

    private boolean containsKeyword(String s) {
        if (s == null) return false;
        String lower = s.trim().toLowerCase();
        for (String k : KEYWORDS) {
            if (lower.contains(k.toLowerCase())) return true;
        }
        return false;
    }

    private View findClickableParent(View view) {
        android.view.ViewParent p = view.getParent();
        while (p instanceof View) {
            View pv = (View) p;
            if (pv.isClickable() && pv.isShown()) return pv;
            p = pv.getParent();
        }
        return null;
    }

    private int dp(Activity activity, int value) {
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        return Math.round(value * dm.density);
    }
}
