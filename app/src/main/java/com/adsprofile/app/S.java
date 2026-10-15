package com.adsprofile.app;

import android.content.Context;
import android.content.SharedPreferences;

class S {
    static SharedPreferences sp(Context c) { return c.getSharedPreferences("ads", 0); }
    static boolean ha(Context c) { return !"en".equals(sp(c).getString("lang", "ha")); }
    static void setHa(Context c, boolean ha) { sp(c).edit().putString("lang", ha ? "ha" : "en").apply(); }
    static String t(Context c, String en, String ha) { return ha(c) ? ha : en; }
    static boolean floatOn(Context c) { return sp(c).getBoolean("float", false); }
    static void setFloat(Context c, boolean v) { sp(c).edit().putBoolean("float", v).apply(); }
    static long lastAd(Context c) { return sp(c).getLong("lastAd", 0); }
    static void setLastAd(Context c, long v) { sp(c).edit().putLong("lastAd", v).apply(); }
    static int fx(Context c, int def) { return sp(c).getInt("fx", def); }
    static int fy(Context c, int def) { return sp(c).getInt("fy", def); }
    static void setPos(Context c, int x, int y) { sp(c).edit().putInt("fx", x).putInt("fy", y).apply(); }
}
