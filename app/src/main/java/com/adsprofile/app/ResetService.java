package com.adsprofile.app;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;
import java.util.List;

// Opens Google's Ads page and taps "Reset advertising ID" and then "OK". Works only when asked to.
// While it works, a curtain covers the screen so the Ads page is not seen. No message is shown when it succeeds.
public class ResetService extends AccessibilityService {
    static volatile ResetService instance;
    final Handler h = new Handler(Looper.getMainLooper());
    boolean running, finishing;
    int stage;
    long deadline;
    WindowManager wm;
    CurtainView curtain;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public boolean onUnbind(Intent i) {
        instance = null;
        dropCurtain();
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
        finishing = false;
        stage = 0;
        deadline = System.currentTimeMillis() + 9000;
        showCurtain();
        Ads.open(this);
        h.postDelayed(step, 450);
    }

    final Runnable step = new Runnable() {
        public void run() {
            if (!running || finishing) return;
            if (System.currentTimeMillis() > deadline) {
                done(false);
                return;
            }
            work();
            if (running && !finishing) h.postDelayed(this, 150);
        }
    };

    void showCurtain() {
        if (curtain != null) return;
        try {
            wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            lp.gravity = Gravity.TOP | Gravity.START;
            if (Build.VERSION.SDK_INT >= 28) {
                lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }
            CurtainView c = new CurtainView(this);
            c.setAlpha(0f);
            wm.addView(c, lp);
            c.animate().alpha(1f).setDuration(90).start();
            curtain = c;
        } catch (Exception e) {
            curtain = null;   // no curtain, the reset still works
        }
    }

    // fade out, then remove
    void hideCurtain() {
        final CurtainView c = curtain;
        if (c == null) return;
        curtain = null;
        c.animate().alpha(0f).setDuration(160).withEndAction(new Runnable() {
            public void run() {
                try {
                    wm.removeView(c);
                } catch (Exception ignored) {
                }
            }
        }).start();
    }

    // remove at once
    void dropCurtain() {
        CurtainView c = curtain;
        curtain = null;
        if (c != null) {
            try {
                wm.removeView(c);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onDestroy() {
        dropCurtain();
        super.onDestroy();
    }

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
        if (finishing) return;
        finishing = true;
        if (!ok) {
            // only a failure is announced
            Toast.makeText(this, S.t(this, "Could not finish. Try again.", "Ba a iya kammalawa ba. Sake gwadawa."),
                    Toast.LENGTH_LONG).show();
            hideCurtain();
            running = false;
            finishing = false;
            return;
        }
        // success: no message. Ring closes, go back, then the curtain fades away.
        if (curtain != null) curtain.setDone();
        h.postDelayed(new Runnable() {
            public void run() {
                performGlobalAction(GLOBAL_ACTION_BACK);
            }
        }, 250);
        h.postDelayed(new Runnable() {
            public void run() {
                hideCurtain();
                running = false;
                finishing = false;
            }
        }, 700);
    }
}
