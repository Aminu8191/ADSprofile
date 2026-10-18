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
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

// Draws the draggable floating buttons over other apps: green = Personal, brown-gold = Work.
public class FloatingService extends Service {
    static volatile boolean running = false;
    WindowManager wm;
    CloseView close;
    WindowManager.LayoutParams clp;
    Fab personal, work;

    int dp(float v) {
        return Ui.dp(this, v);
    }

    int overlayType() {
        return Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    class Fab implements View.OnTouchListener {
        final boolean isWork;
        FabView view;
        WindowManager.LayoutParams lp;
        float downX, downY;
        int startX, startY;
        boolean moved;

        Fab(boolean w) {
            isWork = w;
        }

        void add() {
            int size = dp(56);
            int sw = getResources().getDisplayMetrics().widthPixels;
            lp = new WindowManager.LayoutParams(size, size, overlayType(),
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            lp.gravity = Gravity.TOP | Gravity.START;
            lp.x = S.fx(FloatingService.this, isWork, sw - size - dp(10));
            lp.y = S.fy(FloatingService.this, isWork, dp(isWork ? 340 : 260));
            view = new FabView(FloatingService.this, isWork ? 0xFF8A5A00 : 0xFF0A6B38);
            view.setOnTouchListener(this);
            wm.addView(view, lp);
        }

        void remove() {
            if (view != null) {
                try {
                    wm.removeView(view);
                } catch (Exception ignored) {
                }
                view = null;
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
        public boolean onTouch(View v, MotionEvent e) {
            int slop = ViewConfiguration.get(FloatingService.this).getScaledTouchSlop();
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downX = e.getRawX();
                    downY = e.getRawY();
                    startX = lp.x;
                    startY = lp.y;
                    moved = false;
                    view.setDown(true);
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
                        wm.updateViewLayout(view, lp);
                        if (close != null) close.setHot(overClose());
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.setDown(false);
                    if (moved) {
                        boolean over = overClose();
                        hideClose();
                        if (over) {
                            S.setFab(FloatingService.this, isWork, false);
                            sync();
                        } else {
                            S.setPos(FloatingService.this, isWork, lp.x, lp.y);
                        }
                    } else if (e.getAction() == MotionEvent.ACTION_UP) {
                        ResetService.request(FloatingService.this, isWork);
                    }
                    return true;
            }
            return false;
        }
    }

    // Shows or hides each button to match the switches in the app. Stops the service when none is left.
    void sync() {
        if (wm == null) wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (S.fabOn(this, false)) {
            if (personal == null) personal = new Fab(false);
            if (personal.view == null) personal.add();
        } else if (personal != null) {
            personal.remove();
        }
        if (S.fabOn(this, true)) {
            if (work == null) work = new Fab(true);
            if (work.view == null) work.add();
        } else if (work != null) {
            work.remove();
        }
        if (!S.fabOn(this, false) && !S.fabOn(this, true)) stopSelf();
    }

    @Override
    public int onStartCommand(Intent i, int flags, int id) {
        if (i != null && "STOP".equals(i.getAction())) {
            S.setFab(this, false, false);
            S.setFab(this, true, false);
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
        running = true;
        sync();
        return START_STICKY;
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

    @Override
    public void onDestroy() {
        running = false;
        hideClose();
        if (personal != null) personal.remove();
        if (work != null) work.remove();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }
}
