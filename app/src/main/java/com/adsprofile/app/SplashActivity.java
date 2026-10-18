package com.adsprofile.app;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
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
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.appopen.AppOpenAd;

// Glowing logo, then an App Open ad on top of the splash, then the app.
public class SplashActivity extends Activity {
    final Handler h = new Handler(Looper.getMainLooper());
    boolean minDone, timeoutDone, wentMain, shown;
    int adState = 0;   // 0 loading, 1 loaded, 2 failed
    AppOpenAd ad;

    boolean isManaged() {
        return Build.VERSION.SDK_INT >= 30 && ((UserManager) getSystemService(USER_SERVICE)).isManagedProfile();
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        // The copy inside the work profile has no screen of its own: it just opens the work Ads page.
        if (isManaged()) {
            Ads.open(this);
            finish();
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
        }, 5000);

        new Thread(new Runnable() {
            public void run() {
                MobileAds.initialize(SplashActivity.this, status -> runOnUiThread(new Runnable() {
                    public void run() {
                        loadAd();
                    }
                }));
            }
        }).start();
    }

    void loadAd() {
        AppOpenAd.load(this, AdIds.APP_OPEN, new AdRequest.Builder().build(), new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(AppOpenAd a) {
                ad = a;
                adState = 1;
                proceed();
            }

            @Override
            public void onAdFailedToLoad(LoadAdError e) {
                adState = 2;
                proceed();
            }
        });
    }

    void proceed() {
        if (wentMain || shown || !minDone) return;
        boolean capOk = System.currentTimeMillis() - S.lastAd(this) > 60000;
        if (adState == 1 && ad != null && capOk && !isFinishing()) {
            shown = true;
            S.setLastAd(this, System.currentTimeMillis());
            ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    goMain();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError e) {
                    goMain();
                }
            });
            ad.show(this);
        } else if (adState == 2 || timeoutDone || (adState == 1 && !capOk)) {
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
