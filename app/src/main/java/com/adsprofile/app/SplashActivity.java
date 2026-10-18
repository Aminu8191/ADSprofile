package com.adsprofile.app;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserManager;
import android.view.Gravity;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.appopen.AppOpenAd;

// Glowing logo, then an App Open ad on top of the splash, then the app.
public class SplashActivity extends Activity {
    final Handler h = new Handler(Looper.getMainLooper());
    boolean minDone, timeoutDone, wentMain, shown;
    final Runnable ping = new Runnable() {
        public void run() {
            proceed();
        }
    };

    boolean launchedFromSelf() {
        Uri r = getReferrer();
        return r != null && getPackageName().equals(r.getHost());
    }

    boolean isManaged() {
        return Build.VERSION.SDK_INT >= 30 && ((UserManager) getSystemService(USER_SERVICE)).isManagedProfile();
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        // The other copy (main or work) sent a command.
        String cmd = getIntent() == null ? null : getIntent().getStringExtra("cmd");
        if (cmd != null || launchedFromSelf()) {
            boolean managed = S.isManaged(this);
            if ("ads".equals(cmd) || (managed && cmd == null)) {
                // show the Ads page of this profile; the copy that asked taps the buttons
                Ads.open(this);
                AdWatch.start(this);   // says "Reset successful" when the ID really changes
            } else if ("reset-local".equals(cmd)) {
                ResetService.request(this, ResetService.MODE_GREEN, false);
            } else {
                // the work copy asked: do what the main copy's Personal / Work choice says
                Reset.go(this, false);
            }
            finish();
            overridePendingTransition(0, 0);
            return;
        }
        getWindow().setStatusBarColor(0xFF02200F);
        getWindow().setNavigationBarColor(0xFF0A6B38);

        FrameLayout root = new FrameLayout(this);
        root.setBackground(Ui.bg());
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);

        FrameLayout box = new FrameLayout(this);
        final GlowView glow = new GlowView(this);
        box.addView(glow, new FrameLayout.LayoutParams(-1, -1));
        final ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.logo);
        box.addView(logo, new FrameLayout.LayoutParams(Ui.dp(this, 150), Ui.dp(this, 150), Gravity.CENTER));
        col.addView(box, new LinearLayout.LayoutParams(Ui.dp(this, 300), Ui.dp(this, 300)));

        TextView t = Ui.tv(this, "ADS profile", 28, 0xFFFFFFFF, true);
        t.setGravity(Gravity.CENTER);
        col.addView(t);
        TextView p = Ui.tv(this, "Powered by Al-ameen", 14, 0xFFF2B705, false);
        p.setGravity(Gravity.CENTER);
        p.setPadding(0, Ui.dp(this, 8), 0, 0);
        col.addView(p);
        root.addView(col, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        // logo pops in, glow pulses
        logo.setScaleX(0.6f);
        logo.setScaleY(0.6f);
        ObjectAnimator sx = ObjectAnimator.ofFloat(logo, "scaleX", 0.6f, 1f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(logo, "scaleY", 0.6f, 1f);
        sx.setDuration(800);
        sy.setDuration(800);
        sx.setInterpolator(new OvershootInterpolator());
        sy.setInterpolator(new OvershootInterpolator());
        sx.start();
        sy.start();
        ValueAnimator va = ValueAnimator.ofFloat(0.15f, 0.9f);
        va.setDuration(900);
        va.setRepeatCount(ValueAnimator.INFINITE);
        va.setRepeatMode(ValueAnimator.REVERSE);
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator a) {
                glow.setA((Float) a.getAnimatedValue());
            }
        });
        va.start();

        h.postDelayed(new Runnable() {
            public void run() {
                minDone = true;
                proceed();
            }
        }, 1800);
        h.postDelayed(new Runnable() {
            public void run() {
                timeoutDone = true;
                proceed();
            }
        }, 6000);

        // The ad is normally already loading (App.java). This listens for it and asks again if it never started.
        AppOpen.listener = ping;
        AppOpen.preload(this);
    }

    @Override
    protected void onDestroy() {
        h.removeCallbacksAndMessages(null);
        if (AppOpen.listener == ping) AppOpen.listener = null;
        super.onDestroy();
    }

    void proceed() {
        if (wentMain || shown || !minDone || isFinishing()) return;
        boolean capOk = System.currentTimeMillis() - S.lastAd(this) > 60000;
        if (capOk && AppOpen.ready()) {
            shown = true;
            AppOpenAd a = AppOpen.take();
            a.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdShowedFullScreenContent() {
                    S.setLastAd(SplashActivity.this, System.currentTimeMillis());
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    AppOpen.preload(SplashActivity.this);
                    goMain();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError e) {
                    goMain();
                }
            });
            a.show(this);
        } else if (!capOk || AppOpen.failed || timeoutDone) {
            goMain();
        }
    }

    void goMain() {
        if (wentMain) return;
        wentMain = true;
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
