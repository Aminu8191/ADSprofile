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
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import java.util.List;

// Opens Google's Ads page and taps "Reset advertising ID" and then "OK". Works only when asked to.
// From the app button a curtain covers the Ads page; from the floating button the button itself spins.
// The Advertising ID is read before and after, so "Reset successful" is only shown when it really changed.
public class ResetService extends AccessibilityService {
    interface Listener {
        void busy(boolean b);
    }

    static volatile ResetService instance;
    static volatile Listener listener;   // the floating button listens here to spin
    volatile String before;
    Thread beforeThread;
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
        setBusy(false);
        return super.onUnbind(i);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
    }

    @Override
    public void onInterrupt() {
    }

    static void request(Context c, boolean withCurtain) {
        ResetService s = instance;
        if (s == null) {
            Toast.makeText(c, S.t(c, "Turn on Accessibility for ADS profile first.",
                    "Kunna Accessibility na ADS profile tukuna."), Toast.LENGTH_LONG).show();
            return;
        }
        s.start(withCurtain);
    }

    void setBusy(boolean b) {
        Listener l = listener;
        if (l != null) l.busy(b);
    }

    // The Advertising ID now, or null if it cannot be read (or ad personalisation is off, which gives zeros).
    String adId() {
        try {
            String id = AdvertisingIdClient.getAdvertisingIdInfo(getApplicationContext()).getId();
            if (id == null || id.startsWith("00000000")) return null;
            return id;
        } catch (Exception e) {
            return null;
        }
    }

    void start(boolean withCurtain) {
        if (running) return;
        running = true;
        finishing = false;
        stage = 0;
        deadline = System.currentTimeMillis() + 9000;
        setBusy(true);
        before = null;
        beforeThread = new Thread(new Runnable() {
            public void run() {
                before = adId();
            }
        });
        beforeThread.start();
        if (withCurtain) showCurtain();
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
            Toast.makeText(this, S.t(this, "Could not finish. Try again.", "Ba a iya kammalawa ba. Sake gwadawa."),
                    Toast.LENGTH_LONG).show();
            hideCurtain();
            setBusy(false);
            running = false;
            finishing = false;
            return;
        }
        // OK was tapped: ring closes, go back, curtain fades, and the ID is checked
        if (curtain != null) curtain.setDone();
        h.postDelayed(new Runnable() {
            public void run() {
                performGlobalAction(GLOBAL_ACTION_BACK);
            }
        }, 250);
        h.postDelayed(new Runnable() {
            public void run() {
                hideCurtain();
            }
        }, 700);
        verify();
    }

    // 1 = ID changed, 2 = same ID, 0 = could not check
    void verify() {
        final Thread bt = beforeThread;
        new Thread(new Runnable() {
            public void run() {
                try {
                    if (bt != null) bt.join(1500);
                } catch (InterruptedException ignored) {
                }
                String b = before;
                int result = 0;
                if (b != null) {
                    String a = null;
                    for (int i = 0; i < 4; i++) {
                        try {
                            Thread.sleep(i == 0 ? 600 : 450);
                        } catch (InterruptedException ignored) {
                        }
                        a = adId();
                        if (a != null && !a.equals(b)) break;
                    }
                    result = a == null ? 0 : (a.equals(b) ? 2 : 1);
                } else {
                    try {
                        Thread.sleep(700);
                    } catch (InterruptedException ignored) {
                    }
                }
                final int r = result;
                h.post(new Runnable() {
                    public void run() {
                        finishVerify(r);
                    }
                });
            }
        }).start();
    }

    void finishVerify(int r) {
        String msg;
        if (r == 1) msg = S.t(this, "Reset successful", "An yi reset cikin nasara");
        else if (r == 2) msg = S.t(this, "Ads ID did not change. Try again.", "Ads ID bai canza ba. Sake gwadawa.");
        else msg = S.t(this, "Reset done (could not verify)", "An yi reset (ba a iya tabbatarwa ba)");
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        setBusy(false);
        running = false;
        finishing = false;
    }
}
