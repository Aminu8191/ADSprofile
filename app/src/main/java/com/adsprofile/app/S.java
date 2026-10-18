package com.adsprofile.app;

import android.content.Context;
import android.content.SharedPreferences;

class S {
    static SharedPreferences sp(Context c) { return c.getSharedPreferences("ads", 0); }
    static boolean ha(Context c) { return !"en".equals(sp(c).getString("lang", "ha")); }
    static void setHa(Context c, boolean ha) { sp(c).edit().putString("lang", ha ? "ha" : "en").apply(); }
    static String t(Context c, String en, String ha) { return ha(c) ? ha : en; }
    static long lastAd(Context c) { return sp(c).getLong("lastAd", 0); }
    static void setLastAd(Context c, long v) { sp(c).edit().putLong("lastAd", v).apply(); }

    // two floating buttons: Personal (work=false) and Work (work=true)
    static boolean fabOn(Context c, boolean work) { return sp(c).getBoolean(work ? "fabW" : "fabP", false); }
    static void setFab(Context c, boolean work, boolean v) { sp(c).edit().putBoolean(work ? "fabW" : "fabP", v).apply(); }
    static int fx(Context c, boolean work, int def) { return sp(c).getInt(work ? "fxW" : "fxP", def); }
    static int fy(Context c, boolean work, int def) { return sp(c).getInt(work ? "fyW" : "fyP", def); }
    static void setPos(Context c, boolean work, int x, int y) {
        sp(c).edit().putInt(work ? "fxW" : "fxP", x).putInt(work ? "fyW" : "fyP", y).apply();
    }
}
