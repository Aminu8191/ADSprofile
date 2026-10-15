package com.adsprofile.app;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.widget.Toast;

class Ads {
    // Opens Google's Ads settings page directly.
    static void open(Context c) {
        Intent i = new Intent("com.google.android.gms.settings.ADS_PRIVACY");
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            c.startActivity(i);
        } catch (Exception e) {
            try {
                Intent s = new Intent(Settings.ACTION_SETTINGS);
                s.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(s);
                Toast.makeText(c, S.t(c, "Open Settings > Google > Ads by hand", "Bude Settings > Google > Ads da hannu"), Toast.LENGTH_LONG).show();
            } catch (Exception e2) {
                Toast.makeText(c, S.t(c, "Could not open", "Ba a iya budewa ba"), Toast.LENGTH_LONG).show();
            }
        }
    }
}
