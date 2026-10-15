package com.adsprofile.app;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;
import java.util.List;

// Opens Google's Ads page and taps "Reset advertising ID" and then "OK". Works only when asked to.
public class ResetService extends AccessibilityService {
    static volatile ResetService instance;
    final Handler h = new Handler(Looper.getMainLooper());
    boolean running;
    int stage;
    long deadline;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public boolean onUnbind(Intent i) {
        instance = null;
        return super.onUnbind(i);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
    }

    @Override
    public void onInterrupt() {
    }

    static void request(Context c) {
        ResetService s = instance;
        if (s == null) {
            Toast.makeText(c, S.t(c, "Turn on Accessibility for ADS profile first.",
                    "Kunna Accessibility na ADS profile tukuna."), Toast.LENGTH_LONG).show();
            return;
        }
        s.start();
    }

    void start() {
        if (running) return;
        running = true;
        stage = 0;
        deadline = System.currentTimeMillis() + 10000;
        Ads.open(this);
        h.postDelayed(step, 900);
    }

    final Runnable step = new Runnable() {
        public void run() {
            if (!running) return;
            if (System.currentTimeMillis() > deadline) {
                done(false);
                return;
            }
            work();
            if (running) h.postDelayed(this, 350);
        }
    };

    AccessibilityNodeInfo clickable(AccessibilityNodeInfo n) {
        AccessibilityNodeInfo x = n;
        int k = 0;
        while (x != null && k < 7) {
            if (x.isClickable()) return x;
            x = x.getParent();
            k++;
        }
        return null;
    }

    void work() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        if (getPackageName().equals(String.valueOf(root.getPackageName()))) return;
        if (stage == 0) {
            List<AccessibilityNodeInfo> l = root.findAccessibilityNodeInfosByText("Reset advertising ID");
            if (l == null) return;
            for (AccessibilityNodeInfo n : l) {
                AccessibilityNodeInfo t = clickable(n);
                if (t != null && t.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    stage = 1;
                    return;
                }
            }
        } else {
            List<AccessibilityNodeInfo> b = root.findAccessibilityNodeInfosByViewId("android:id/button1");
            if (b != null) {
                for (AccessibilityNodeInfo n : b) {
                    if (n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        done(true);
                        return;
                    }
                }
            }
            List<AccessibilityNodeInfo> ok = root.findAccessibilityNodeInfosByText("OK");
            if (ok != null) {
                for (AccessibilityNodeInfo n : ok) {
                    AccessibilityNodeInfo t = clickable(n);
                    if (t != null && t.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        done(true);
                        return;
                    }
                }
            }
        }
    }

    void done(boolean ok) {
        running = false;
        Toast.makeText(this, ok
                ? S.t(this, "Advertising ID reset.", "An yi reset na Advertising ID.")
                : S.t(this, "Could not finish. Try again.", "Ba a iya kammalawa ba. Sake gwadawa."), Toast.LENGTH_LONG).show();
        if (ok) {
            h.postDelayed(new Runnable() {
                public void run() {
                    performGlobalAction(GLOBAL_ACTION_BACK);
                }
            }, 800);
        }
    }
}
