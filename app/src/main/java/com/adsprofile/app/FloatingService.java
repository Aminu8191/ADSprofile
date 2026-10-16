package com.adsprofile.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.os.UserManager;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

// Draws the draggable floating button over other apps.
public class FloatingService extends Service {
    static volatile boolean running = false;
    static volatile FloatingService inst;   // so the reset can copy the button onto its still picture
    WindowManager wm;
    FabView fab;
    CloseView close;
    WindowManager.LayoutParams lp, clp;
    float downX, downY;
    int startX, startY;
    boolean moved;

    int dp(float v) {
        return Ui.dp(this, v);
    }

    boolean isManaged() {
        return Build.VERSION.SDK_INT >= 30 && ((UserManager) getSystemService(USER_SERVICE)).isManagedProfile();
    }

    int overlayType() {
        return Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    @Override
    public int onStartCommand(Intent i, int flags, int id) {
        if (i != null && "STOP".equals(i.getAction())) {
            S.setFloat(this, false);
            stopSelf();
            return START_NOT_STICKY;
        }
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel("float", "ADS profile", NotificationManager.IMPORTANCE_LOW));
        PendingIntent open = PendingIntent.getActivity(this, 1, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 2, new Intent(this, FloatingService.class).setAction("STOP"),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(this, "float")
                .setSmallIcon(android.R.drawable.ic_menu_rotate)
                .setContentTitle("ADS profile")
                .setContentText(S.t(this, "Floating button is on. Tap Remove to turn it off.", "Maɓallin shawagi yana aiki. Danna Cire don kashewa."))
                .setContentIntent(open)
                .addAction(new Notification.Action.Builder(null, S.t(this, "Remove", "Cire"), stop).build())
                .setOngoing(true)
                .build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(21, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(21, n);
        }
        if (fab == null) addFab();
        running = true;
        return START_STICKY;
    }

    void addFab() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        int size = dp(56);
        int sw = getResources().getDisplayMetrics().widthPixels;
        lp = new WindowManager.LayoutParams(size, size, overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = S.fx(this, sw - size - dp(10));
        lp.y = S.fy(this, dp(260));
        // green for the main profile, gold-brown for the work profile, so you can tell them apart
        fab = new FabView(this, isManaged() ? 0xFF8A5A00 : 0xFF0A6B38);
        final int slop = ViewConfiguration.get(this).getScaledTouchSlop();
        fab.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = e.getRawX();
                        downY = e.getRawY();
                        startX = lp.x;
                        startY = lp.y;
                        moved = false;
                        fab.setDown(true);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = e.getRawX() - downX, dy = e.getRawY() - downY;
                        if (!moved && Math.hypot(dx, dy) > slop) {
                            moved = true;
                            showClose();
                        }
                        if (moved) {
                            lp.x = (int) (startX + dx);
                            lp.y = (int) (startY + dy);
                            wm.updateViewLayout(fab, lp);
                            if (close != null) close.setHot(overClose());
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        fab.setDown(false);
                        if (moved) {
                            boolean over = overClose();
                            hideClose();
                            if (over) {
                                S.setFloat(FloatingService.this, false);
                                stopSelf();
                            } else {
                                S.setPos(FloatingService.this, lp.x, lp.y);
                            }
                        } else if (e.getAction() == MotionEvent.ACTION_UP) {
                            Reset.go(FloatingService.this, true);
                        }
                        return true;
                }
                return false;
            }
        });
        wm.addView(fab, lp);
        inst = this;
        ResetService.listener = new ResetService.Listener() {
            public void busy(boolean b) {
                if (fab != null) fab.setSpin(b);
            }
        };
    }

    void showClose() {
        if (close != null) return;
        int size = dp(64);
        clp = new WindowManager.LayoutParams(size, size, overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        clp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        clp.y = dp(56);
        close = new CloseView(this);
        wm.addView(close, clp);
    }

    void hideClose() {
        if (close != null) {
            try {
                wm.removeView(close);
            } catch (Exception ignored) {
            }
            close = null;
        }
    }

    boolean overClose() {
        int sw = getResources().getDisplayMetrics().widthPixels;
        int sh = getResources().getDisplayMetrics().heightPixels;
        float fx = lp.x + lp.width / 2f, fy = lp.y + lp.height / 2f;
        float cx = sw / 2f, cy = sh - dp(56) - dp(32);
        return Math.hypot(fx - cx, fy - cy) < dp(70);
    }

    @Override
    public void onDestroy() {
        running = false;
        inst = null;
        ResetService.listener = null;
        hideClose();
        if (fab != null) {
            try {
                wm.removeView(fab);
            } catch (Exception ignored) {
            }
            fab = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }
}
