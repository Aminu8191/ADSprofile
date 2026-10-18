package com.adsprofile.app;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.CrossProfileApps;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import java.util.List;

public class MainActivity extends Activity {
    AdView banner;
    Switch swP, swW;
    boolean building = true;

    String tr(String en, String ha) {
        return S.t(this, en, ha);
    }

    int dp(float v) {
        return Ui.dp(this, v);
    }

    boolean serviceOn() {
        String s = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return s != null && s.contains(getPackageName());
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(0xFF02200F);
        getWindow().setNavigationBarColor(0xFF04180F);
        new Thread(new Runnable() {
            public void run() {
                MobileAds.initialize(MainActivity.this, status -> { });
            }
        }).start();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(Ui.bg());

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setPadding(dp(26), dp(20), dp(26), dp(12));
        sv.addView(page);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));

        // language switch
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.END);
        bar.addView(langPill("Hausa", true));
        bar.addView(langPill("English", false));
        page.addView(bar, new LinearLayout.LayoutParams(-1, -2));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.logo);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(130), dp(130));
        ip.topMargin = dp(10);
        ip.bottomMargin = dp(14);
        page.addView(logo, ip);

        TextView t = Ui.tv(this, "ADS profile", 32, 0xFFFFFFFF, true);
        t.setGravity(Gravity.CENTER);
        page.addView(t);
        TextView s = Ui.tv(this, tr("Choose your profile", "Zabi profile dinka"), 15, 0xFFB7DCC4, false);
        s.setGravity(Gravity.CENTER);
        s.setPadding(0, dp(4), 0, dp(20));
        page.addView(s);

        Button personal = Ui.button(this, "Personal", true);
        personal.setOnClickListener(v -> Ads.open(this));
        page.addView(personal);
        Button work = Ui.button(this, "Work", false);
        work.setOnClickListener(v -> openWork());
        page.addView(work);
        Button resetP = Ui.button(this, tr("Reset Personal Ads ID", "Reset Ads ID na Personal"), false);
        resetP.setOnClickListener(v -> ResetService.request(this, false));
        page.addView(resetP);
        Button resetW = Ui.button(this, tr("Reset Work Ads ID", "Reset Ads ID na Work"), false);
        resetW.setOnClickListener(v -> ResetService.request(this, true));
        page.addView(resetW);

        // floating button card
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(Ui.shape(this, 0x22FFFFFF, 0, 18));
        swP = floatSwitch(tr("Floating button: Personal", "Maɓallin shawagi: Personal"), false);
        card.addView(swP);
        swW = floatSwitch(tr("Floating button: Work", "Maɓallin shawagi: Work"), true);
        swW.setPadding(0, dp(12), 0, 0);
        card.addView(swW);
        TextView hint = Ui.tv(this, tr("A button on your screen (green = Personal, gold = Work). Tap it once to reset that Ads ID. Drag it anywhere, or drag it to the X at the bottom to remove it. Both are controlled from this app, so you only turn on Accessibility here.",
                "Maɓalli a kan allonka (kore = Personal, zinariya = Work). Ka danna shi sau ɗaya don reset na wannan Ads ID. Ka ja shi ko'ina, ko ka ja shi zuwa X a ƙasa don cire shi. Duka ana sarrafa su daga wannan app ɗin, don haka Accessibility a nan kawai za ka kunna."), 13, 0xFFB7DCC4, false);
        hint.setPadding(0, dp(8), 0, 0);
        card.addView(hint);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.topMargin = dp(14);
        page.addView(card, cp);

        View sp = new View(this);
        page.addView(sp, new LinearLayout.LayoutParams(1, 0, 1f));
        TextView pw = Ui.tv(this, "Powered by Al-ameen", 14, 0xFFF2B705, false);
        pw.setGravity(Gravity.CENTER);
        pw.setPadding(0, dp(18), 0, dp(6));
        page.addView(pw);

        // banner ad at the bottom
        FrameLayout adBox = new FrameLayout(this);
        adBox.setBackgroundColor(0xFF04180F);
        banner = new AdView(this);
        banner.setAdSize(AdSize.BANNER);
        banner.setAdUnitId(AdIds.BANNER);
        adBox.addView(banner, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        root.addView(adBox, new LinearLayout.LayoutParams(-1, -2));
        banner.loadAd(new AdRequest.Builder().build());

        setContentView(root);
        building = false;
    }

    TextView langPill(String name, boolean ha) {
        TextView v = Ui.pill(this, name, S.ha(this) == ha);
        v.setOnClickListener(x -> {
            S.setHa(this, ha);
            recreate();
        });
        return v;
    }

    void openWork() {
        if (Build.VERSION.SDK_INT >= 28) {
            CrossProfileApps cpa = getSystemService(CrossProfileApps.class);
            List<UserHandle> l = cpa.getTargetUserProfiles();
            if (!l.isEmpty()) {
                cpa.startMainActivity(new ComponentName(this, SplashActivity.class), l.get(0));
                return;
            }
        }
        Toast.makeText(this, tr("No work profile found, or the app is not installed in it.",
                "Ba a sami Work profile ba, ko kuwa ba a shigar da app a cikinsa ba."), Toast.LENGTH_LONG).show();
    }

    Switch floatSwitch(String label, final boolean work) {
        Switch sw = new Switch(this);
        sw.setText(label);
        sw.setTextColor(0xFFFFFFFF);
        sw.setTextSize(16);
        sw.setTypeface(null, android.graphics.Typeface.BOLD);
        sw.setOnCheckedChangeListener((btn, on) -> {
            if (building) return;
            if (on) enableFloat(work);
            else disableFloat(work);
        });
        return sw;
    }

    void enableFloat(boolean work) {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, tr("Allow \"Display over other apps\", then come back and switch it on.",
                    "Ba da izinin \"Display over other apps\", sannan ka dawo ka kunna."), Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
            refreshSwitches();
            return;
        }
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 9);
        }
        S.setFab(this, work, true);
        startForegroundService(new Intent(this, FloatingService.class));
        if (!serviceOn()) {
            Toast.makeText(this, tr("Also turn on Accessibility for ADS profile, or the button cannot tap.",
                    "Kunna Accessibility na ADS profile ma, in ba haka ba maɓallin ba zai iya dannawa ba."), Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }
    }

    void disableFloat(boolean work) {
        S.setFab(this, work, false);
        if (S.fabOn(this, false) || S.fabOn(this, true)) {
            startForegroundService(new Intent(this, FloatingService.class));   // re-sync the buttons
        } else {
            stopService(new Intent(this, FloatingService.class));
        }
    }

    void refreshSwitches() {
        building = true;
        swP.setChecked(S.fabOn(this, false));
        swW.setChecked(S.fabOn(this, true));
        building = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (banner != null) banner.resume();
        if (!Settings.canDrawOverlays(this)) {
            S.setFab(this, false, false);
            S.setFab(this, true, false);
        }
        if ((S.fabOn(this, false) || S.fabOn(this, true)) && !FloatingService.running) {
            startForegroundService(new Intent(this, FloatingService.class));
        }
        refreshSwitches();
    }

    @Override
    protected void onPause() {
        if (banner != null) banner.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (banner != null) banner.destroy();
        super.onDestroy();
    }
}
