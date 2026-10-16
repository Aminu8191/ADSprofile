package com.adsprofile.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.CrossProfileApps;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.widget.Toast;
import java.util.List;

// Resets the profile picked with the Personal / Work buttons.
class Reset {
    static void go(Context c, boolean fromFloating) {
        boolean managed = S.isManaged(c);
        boolean wantWork = S.selWork(c, managed);
        if (wantWork == managed) {
            // the profile this copy lives in
            ResetService.request(c, fromFloating ? ResetService.MODE_FREEZE : ResetService.MODE_GREEN);
        } else {
            remote(c, fromFloating);
        }
    }

    // Ask the copy in the other profile to do its own reset (it starts, resets and tells you the result).
    static void remote(Context c, boolean fromFloating) {
        if (Build.VERSION.SDK_INT >= 28) {
            try {
                CrossProfileApps cpa = c.getSystemService(CrossProfileApps.class);
                List<UserHandle> l = cpa.getTargetUserProfiles();
                if (!l.isEmpty()) {
                    cpa.startMainActivity(new ComponentName(c, SplashActivity.class), l.get(0));
                    if (fromFloating) {
                        final ResetService.Listener ls = ResetService.listener;
                        if (ls != null) {
                            ls.busy(true);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                public void run() {
                                    ls.busy(false);
                                }
                            }, 5000);
                        }
                    }
                    return;
                }
            } catch (Exception ignored) {
            }
        }
        Toast.makeText(c, S.t(c, "No other profile found, or the app is not installed in it.",
                "Ba a sami wani profile ba, ko kuwa ba a shigar da app a cikinsa ba."), Toast.LENGTH_LONG).show();
    }
}
