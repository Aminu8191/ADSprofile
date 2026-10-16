package com.adsprofile.app;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.appopen.AppOpenAd;

// Keeps one App Open ad ready, so the splash does not have to wait for it to download.
class AppOpen {
    static AppOpenAd ad;
    static long loadedAt;
    static boolean initDone, loading, failed;
    static Runnable listener;   // the splash listens here (always called on the main thread)

    static void init(final Application app) {
        new Thread(new Runnable() {
            public void run() {
                MobileAds.initialize(app, status -> new Handler(Looper.getMainLooper()).post(new Runnable() {
                    public void run() {
                        initDone = true;
                        preload(app);
                    }
                }));
            }
        }).start();
    }

    static boolean ready() {
        if (ad != null && SystemClock.elapsedRealtime() - loadedAt > 3L * 60 * 60 * 1000) ad = null;   // ads go stale after 4h
        return ad != null;
    }

    static AppOpenAd take() {
        AppOpenAd a = ad;
        ad = null;
        return a;
    }

    static void preload(Context c) {
        if (!initDone || loading || ready()) return;
        loading = true;
        failed = false;
        AppOpenAd.load(c.getApplicationContext(), AdIds.APP_OPEN, new AdRequest.Builder().build(),
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(AppOpenAd a) {
                        ad = a;
                        loadedAt = SystemClock.elapsedRealtime();
                        loading = false;
                        failed = false;
                        ping();
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError e) {
                        loading = false;
                        failed = true;
                        ping();
                    }
                });
    }

    static void ping() {
        Runnable l = listener;
        if (l != null) l.run();
    }
}
