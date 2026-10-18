package com.adsprofile.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Toast;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;

// Runs inside the work copy. When the Ads page is opened for this profile, it watches the Advertising ID
// for a while and says "Reset successful" as soon as the ID really changes (whoever tapped the button).
class AdWatch {
    static volatile boolean running;

    static void start(final Context ctx) {
        if (running) return;
        running = true;
        final Context c = ctx.getApplicationContext();
        new Thread(new Runnable() {
            public void run() {
                try {
                    String before = read(c);
                    if (before == null) return;
                    long end = SystemClock.elapsedRealtime() + 15000;
                    while (SystemClock.elapsedRealtime() < end) {
                        Thread.sleep(500);
                        String now = read(c);
                        if (now != null && !now.equals(before)) {
                            say(c, S.t(c, "Work profile: Reset successful", "Work profile: An yi reset cikin nasara"));
                            return;
                        }
                    }
                } catch (Exception ignored) {
                } finally {
                    running = false;
                }
            }
        }).start();
    }

    static String read(Context c) {
        try {
            String id = AdvertisingIdClient.getAdvertisingIdInfo(c).getId();
            if (id == null || id.startsWith("00000000")) return null;
            return id;
        } catch (Exception e) {
            return null;
        }
    }

    static void say(final Context c, final String msg) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                Toast.makeText(c, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
