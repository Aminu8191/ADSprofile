package com.adsprofile.app;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.Toast;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import java.util.ArrayList;
import java.util.List;

// Opens Google's Ads page and taps "Reset advertising ID" and then "OK". Works only when asked to.
// From the app button a curtain covers the Ads page; from the floating button the button itself spins.
// The Advertising ID is read before and after, so "Reset successful" is only shown when it really changed.
public class ResetService extends AccessibilityService {
    static final int MODE_GREEN = 1;    // green curtain with the logo (app button, or the other profile)
    static final int MODE_FREEZE = 2;   // still picture of your screen + spinning floating button

    interface Listener {
        void busy(boolean b);
    }

    static volatile ResetService instance;
    static volatile Listener listener;   // the floating button listens here to spin
    volatile String before;
    Thread beforeThread;

    // the ID read before a reset (kept per reset, so quick taps one after another do not mix)
    static class Snap {
        volatile String before;
        Thread t;
    }

    Snap snap;
    final Handler h = new Handler(Looper.getMainLooper());
    boolean running, finishing, remoteMode, foundReset;
    int stage;
    long deadline;
    WindowManager wm;
    View curtain;

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

    static void request(Context c, int mode) {
        request(c, mode, false);
    }

    // remote = reset the OTHER profile: this copy opens that profile's Ads page and taps the buttons itself
    static void request(Context c, int mode, boolean remote) {
        ResetService s = instance;
        if (s == null) {
            Toast.makeText(c, S.t(c, "Turn on Accessibility for ADS profile first.",
                    "Kunna Accessibility na ADS profile tukuna."), Toast.LENGTH_LONG).show();
            return;
        }
        s.start(mode, remote);
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

    void start(int mode, boolean remote) {
        if (running) return;
        running = true;
        finishing = false;
        remoteMode = remote;
        foundReset = false;
        stage = 0;
        setBusy(true);
        final Snap sn = new Snap();
        snap = sn;
        if (!remote) {   // the ID of the other profile cannot be read from here
            sn.t = new Thread(new Runnable() {
                public void run() {
                    sn.before = adId();
                }
            });
            sn.t.start();
        }
        if (mode == MODE_FREEZE && Build.VERSION.SDK_INT >= 30) {
            freezeThenGo();
        } else {
            showCurtain(null);
            go();
        }
    }

    // open the Ads page and start tapping
    void go() {
        deadline = System.currentTimeMillis() + 9000;
        if (remoteMode) {
            if (!Reset.launchOtherAds(this)) {
                done(false);
                return;
            }
            h.postDelayed(step, 600);
        } else {
            Ads.open(this);
            h.postDelayed(step, 250);
        }
    }

    // Take a picture of the current screen first, cover the screen with it, then open the Ads page.
    // If the picture cannot be taken (older Android, or a protected app), the green curtain is used.
    @TargetApi(30)
    void freezeThenGo() {
        final boolean[] handled = {false};
        final Runnable fallback = new Runnable() {
            public void run() {
                if (handled[0]) return;
                handled[0] = true;
                showCurtain(null);
                go();
            }
        };
        h.postDelayed(fallback, 900);
        try {
            takeScreenshot(Display.DEFAULT_DISPLAY, getMainExecutor(), new TakeScreenshotCallback() {
                @Override
                public void onSuccess(ScreenshotResult r) {
                    Bitmap sw = null;
                    try {
                        Bitmap hw = Bitmap.wrapHardwareBuffer(r.getHardwareBuffer(), r.getColorSpace());
                        if (hw != null) {
                            sw = hw.copy(Bitmap.Config.ARGB_8888, false);
                            hw.recycle();
                        }
                    } catch (Exception ignored) {
                    }
                    try {
                        r.getHardwareBuffer().close();
                    } catch (Exception ignored) {
                    }
                    if (handled[0]) return;
                    handled[0] = true;
                    h.removeCallbacks(fallback);
                    showCurtain(sw);
                    h.postDelayed(new Runnable() {
                        public void run() {
                            go();
                        }
                    }, sw != null ? 80 : 0);
                }

                @Override
                public void onFailure(int code) {
                    fallback.run();
                }
            });
        } catch (Exception e) {
            fallback.run();
        }
    }

    final Runnable step = new Runnable() {
        public void run() {
            if (!running || finishing) return;
            if (System.currentTimeMillis() > deadline) {
                done(false);
                return;
            }
            work();
            if (running && !finishing) h.postDelayed(this, 70);
        }
    };

    void showCurtain(Bitmap shot) {
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
            View c;
            if (shot != null) {
                FloatingService f = FloatingService.inst;
                boolean showFab = f != null && f.fab != null && f.fab.getWidth() > 0;
                int[] loc = new int[2];
                if (showFab) f.fab.getLocationOnScreen(loc);   // where the button really is on the screen
                c = new FreezeView(this, shot, loc[0], loc[1], showFab ? f.fab.getWidth() : 0,
                        showFab ? f.fab.fill : 0, showFab);
                wm.addView(c, lp);   // appears at once: it looks exactly like your screen
            } else {
                c = new CurtainView(this);
                c.setAlpha(0f);
                wm.addView(c, lp);
                c.animate().alpha(1f).setDuration(90).start();
            }
            curtain = c;
        } catch (Exception e) {
            curtain = null;   // no curtain, the reset still works
        }
    }

    // fade out, then remove
    void hideCurtain() {
        final View c = curtain;
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
        View c = curtain;
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

    // every window we can see: the active one first, then the others (this helps to reach the work profile's window)
    List<AccessibilityNodeInfo> candidateRoots() {
        List<AccessibilityNodeInfo> l = new ArrayList<AccessibilityNodeInfo>();
        AccessibilityNodeInfo a = getRootInActiveWindow();
        if (a != null) l.add(a);
        try {
            List<AccessibilityWindowInfo> ws = getWindows();
            if (ws != null) {
                for (AccessibilityWindowInfo w : ws) {
                    AccessibilityNodeInfo r = w.getRoot();
                    if (r != null) l.add(r);
                }
            }
        } catch (Exception ignored) {
        }
        return l;
    }

    void work() {
        for (AccessibilityNodeInfo root : candidateRoots()) {
            String pk = String.valueOf(root.getPackageName());
            if (getPackageName().equals(pk) || "com.android.systemui".equals(pk)) continue;
            if (handle(root)) return;
        }
    }

    // true when something was tapped
    boolean handle(AccessibilityNodeInfo root) {
        if (stage == 0) {
            List<AccessibilityNodeInfo> l = root.findAccessibilityNodeInfosByText("Reset advertising ID");
            if (l != null) {
                for (AccessibilityNodeInfo n : l) {
                    foundReset = true;
                    AccessibilityNodeInfo t = clickable(n);
                    if (t != null && t.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        stage = 1;
                        return true;
                    }
                }
            }
            // Any other language: on Google's Ads page the first tappable text row is "Reset advertising ID".
            String pk = String.valueOf(root.getPackageName());
            if (pk.startsWith("com.google.android.gms") || "com.android.settings".equals(pk)) {
                AccessibilityNodeInfo row = firstRow(root);
                if (row != null) {
                    foundReset = true;
                    if (row.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        stage = 1;
                        return true;
                    }
                }
            }
        } else {
            List<AccessibilityNodeInfo> b = root.findAccessibilityNodeInfosByViewId("android:id/button1");
            if (b != null) {
                for (AccessibilityNodeInfo n : b) {
                    if (n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        done(true);
                        return true;
                    }
                }
            }
            List<AccessibilityNodeInfo> ok = root.findAccessibilityNodeInfosByText("OK");
            if (ok != null) {
                for (AccessibilityNodeInfo n : ok) {
                    AccessibilityNodeInfo t = clickable(n);
                    if (t != null && t.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        done(true);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // the tappable text row nearest the top of the page (not a switch, not a toolbar button)
    AccessibilityNodeInfo firstRow(AccessibilityNodeInfo root) {
        List<AccessibilityNodeInfo> rows = new ArrayList<AccessibilityNodeInfo>();
        collectRows(root, rows, 0);
        AccessibilityNodeInfo best = null;
        int bestTop = Integer.MAX_VALUE;
        Rect r = new Rect();
        for (AccessibilityNodeInfo n : rows) {
            n.getBoundsInScreen(r);
            if (r.top < bestTop) {
                bestTop = r.top;
                best = n;
            }
        }
        return best;
    }

    void collectRows(AccessibilityNodeInfo n, List<AccessibilityNodeInfo> out, int depth) {
        if (n == null || depth > 25) return;
        if (n.isClickable() && n.isVisibleToUser() && !n.isCheckable() && hasText(n, 0) && !hasSwitch(n, 0)) {
            out.add(n);
            return;
        }
        for (int i = 0; i < n.getChildCount(); i++) {
            collectRows(n.getChild(i), out, depth + 1);
        }
    }

    boolean hasText(AccessibilityNodeInfo n, int d) {
        if (n == null || d > 6) return false;
        CharSequence t = n.getText();
        if (t != null && t.length() > 0) return true;
        for (int i = 0; i < n.getChildCount(); i++) {
            if (hasText(n.getChild(i), d + 1)) return true;
        }
        return false;
    }

    boolean hasSwitch(AccessibilityNodeInfo n, int d) {
        if (n == null || d > 6) return false;
        if (n.isCheckable()) return true;
        CharSequence cn = n.getClassName();
        if (cn != null && cn.toString().contains("Switch")) return true;
        for (int i = 0; i < n.getChildCount(); i++) {
            if (hasSwitch(n.getChild(i), d + 1)) return true;
        }
        return false;
    }

    void done(boolean ok) {
        if (finishing) return;
        finishing = true;
        if (!ok) {
            String msg = S.t(this, "Could not finish. Try again.", "Ba a iya kammalawa ba. Sake gwadawa.");
            if (remoteMode && !foundReset) {
                msg = S.t(this, "The Ads page of the other profile was not seen by the main app.",
                        "Main app bai ga shafin Ads na \u0257ayan profile ba.");
            }
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            hideCurtain();
            setBusy(false);
            running = false;
            finishing = false;
            return;
        }
        // OK was tapped: ring closes, go back at once, the curtain fades, and the ID is checked
        if (curtain instanceof CurtainView) ((CurtainView) curtain).setDone();
        else if (curtain instanceof FreezeView) ((FreezeView) curtain).setDone();
        h.postDelayed(new Runnable() {
            public void run() {
                performGlobalAction(GLOBAL_ACTION_BACK);
            }
        }, 120);
        h.postDelayed(new Runnable() {
            public void run() {
                hideCurtain();
                setBusy(false);
                running = false;      // ready for the next tap right away
                finishing = false;
            }
        }, 380);
        if (!remoteMode) verify();
    }

    // 1 = ID changed, 2 = same ID, 0 = could not check
    void verify() {
        final Snap sn = snap;
        new Thread(new Runnable() {
            public void run() {
                try {
                    if (sn.t != null) sn.t.join(1500);
                } catch (InterruptedException ignored) {
                }
                String b = sn.before;
                int result = 0;
                if (b != null) {
                    String a = null;
                    for (int i = 0; i < 5; i++) {
                        try {
                            Thread.sleep(i == 0 ? 350 : 400);
                        } catch (InterruptedException ignored) {
                        }
                        a = adId();
                        if (a != null && !a.equals(b)) break;
                    }
                    result = a == null ? 0 : (a.equals(b) ? 2 : 1);
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
    }
}
