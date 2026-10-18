package com.adsprofile.app;

import android.content.ComponentName;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

    // Double-tap on the other profile's button: its copy opens its own Ads page (for resetting by hand).
    static void remoteAds(Activity a) {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                CrossProfileApps cpa = a.getSystemService(CrossProfileApps.class);
                List<UserHandle> l = cpa.getTargetUserProfiles();
                if (!l.isEmpty()) {
                    Intent i = new Intent(a, SplashActivity.class);
                    i.setAction(Intent.ACTION_MAIN);
                    i.addCategory(Intent.CATEGORY_LAUNCHER);
                    i.putExtra("cmd", "ads");
                    cpa.startActivity(i, l.get(0), a);
                    return;
                }
            } catch (Exception ignored) {
            }
        }
        Toast.makeText(a, S.t(a, "Could not open the other profile. Open its app by hand.",
                "Ba a iya bu\u0257e ma\u0253allin ba. Bu\u0257e app \u0257in da hannu."), Toast.LENGTH_LONG).show();
    }
}
