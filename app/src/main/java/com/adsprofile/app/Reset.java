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
        if (S.isManaged(c)) {
            // work copy: the main copy decides and does everything, following ITS Personal / Work choice
            if (launchOtherAds(c) && fromFloating) spinFor(5000);
            return;
        }
        int mode = fromFloating ? ResetService.MODE_FREEZE : ResetService.MODE_GREEN;
        if (S.selWork(c, false)) {
            // work profile selected: this copy's Accessibility does the tapping there
            ResetService.request(c, mode, true);
        } else {
            ResetService.request(c, mode);
        }
    }

    // keeps the floating button spinning for a while (the other copy does the work and cannot tell us when it is done)
    static void spinFor(long ms) {
        final ResetService.Listener ls = ResetService.listener;
        if (ls == null) return;
        ls.busy(true);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            public void run() {
                ls.busy(false);
            }
        }, ms);
    }

    // Opens the Ads page inside the other profile (its copy of the app starts and shows the page).
    // The tapping is then done by this copy's own Accessibility service.
    static boolean launchOtherAds(Context c) {
        if (Build.VERSION.SDK_INT >= 28) {
            try {
                CrossProfileApps cpa = c.getSystemService(CrossProfileApps.class);
                List<UserHandle> l = cpa.getTargetUserProfiles();
                if (!l.isEmpty()) {
                    cpa.startMainActivity(new ComponentName(c, SplashActivity.class), l.get(0));
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        Toast.makeText(c, S.t(c, "No other profile found, or the app is not installed in it.",
                "Ba a sami wani profile ba, ko kuwa ba a shigar da app a cikinsa ba."), Toast.LENGTH_LONG).show();
        return false;
    }

    // Double-tap on the other profile's button: its copy opens its own Ads page (for resetting by hand).
    // Uses the same cross-profile start that the work reset uses (it is known to work); the other copy shows its Ads page.
    static void remoteAds(Activity a) {
        launchOtherAds(a);
    }

    // Sends a command to the copy in the other profile: "ads" opens its Ads page, "reset-local" resets its own profile.
    static void remoteCmd(Activity a, String cmd) {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                CrossProfileApps cpa = a.getSystemService(CrossProfileApps.class);
                List<UserHandle> l = cpa.getTargetUserProfiles();
                if (!l.isEmpty()) {
                    Intent i = new Intent(a, SplashActivity.class);
                    i.setAction(Intent.ACTION_MAIN);
                    i.addCategory(Intent.CATEGORY_LAUNCHER);
                    i.putExtra("cmd", cmd);
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
