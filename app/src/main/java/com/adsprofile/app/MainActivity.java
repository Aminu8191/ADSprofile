package com.adsprofile.app;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.CrossProfileApps;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
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
import java.util.List;

public class MainActivity extends Activity {
    AdView banner;
    Switch sw;
    boolean building = true;
    boolean shownHa;
    boolean selWork;
    Button personalBtn, workBtn;
    long lastTap;
    boolean lastTapWork;

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
        shownHa = S.ha(this);

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

        // top bar: settings button (language and contact live inside)
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.END);
        FrameLayout gear = new FrameLayout(this);
        gear.setBackground(Ui.ripple(this, Ui.shape(this, 0x1AFFFFFF, Ui.GOLD, 22), 22));
        gear.setClickable(true);
        gear.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        gear.addView(new IconView(this, IconView.GEAR, Ui.GOLD), new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER));
        bar.addView(gear, new LinearLayout.LayoutParams(dp(44), dp(44)));
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
        TextView s = Ui.tv(this, tr("Choose the profile to reset", "Za\u0253i profile da za a yi reset"), 15, Ui.MINT, false);
        s.setGravity(Gravity.CENTER);
        s.setPadding(0, dp(4), 0, dp(20));
        page.addView(s);

        // Personal and Work: pick which profile gets reset (the gold one is selected)
        selWork = S.selWork(this, S.isManaged(this));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        personalBtn = Ui.half(this, "Personal", !selWork, true);
        personalBtn.setOnClickListener(v -> tap(false));
        workBtn = Ui.half(this, "Work", selWork, false);
        workBtn.setOnClickListener(v -> tap(true));
        row.addView(personalBtn);
        row.addView(workBtn);
        page.addView(row, new LinearLayout.LayoutParams(-1, -2));
        TextView selHint = Ui.tv(this, tr("Tap to select. Double-tap to open its Ads page.",
                "Danna sau \u0257aya don za\u0253a. Sau biyu don bu\u0257e shafin Ads."), 12, Ui.MINT, false);
        selHint.setGravity(Gravity.CENTER);
        page.addView(selHint, new LinearLayout.LayoutParams(-1, -2));

        // Reset Ads ID now
        LinearLayout reset = new LinearLayout(this);
        reset.setOrientation(LinearLayout.HORIZONTAL);
        reset.setGravity(Gravity.CENTER);
        reset.setClickable(true);
        reset.setBackground(Ui.ripple(this, Ui.shape(this, 0x1AFFFFFF, Ui.GOLD, 24), 24));
        reset.addView(new IconView(this, IconView.RESET, Ui.GOLD), new LinearLayout.LayoutParams(dp(24), dp(24)));
        TextView rt = Ui.tv(this, tr("Reset Ads ID now", "Reset Ads ID yanzu"), 19, Ui.GOLD, true);
        rt.setPadding(dp(10), 0, 0, 0);
        reset.addView(rt);
        reset.setOnClickListener(v -> Reset.go(this, false));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, dp(64));
        rp.setMargins(0, dp(8), 0, dp(8));
        page.addView(reset, rp);

        // Floating button: one slim row with a switch
        LinearLayout fr = new LinearLayout(this);
        fr.setOrientation(LinearLayout.HORIZONTAL);
        fr.setGravity(Gravity.CENTER_VERTICAL);
        fr.setClickable(true);
        fr.setPadding(dp(20), dp(12), dp(16), dp(12));
        fr.setBackground(Ui.ripple(this, Ui.shape(this, 0x1AFFFFFF, 0x66F2B705, 24), 24));
        LinearLayout txt = new LinearLayout(this);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(Ui.tv(this, tr("Floating button", "Ma\u0253allin shawagi"), 17, 0xFFFFFFFF, true));
        TextView hint = Ui.tv(this, tr("Tap once to reset the selected profile. Drag it to the X to remove it.",
                "Danna sau \u0257aya don reset \u0257in profile da aka za\u0253a. Ja shi zuwa X don cire shi."), 12, Ui.MINT, false);
        hint.setPadding(0, dp(2), 0, 0);
        txt.addView(hint);
        fr.addView(txt, new LinearLayout.LayoutParams(0, -2, 1f));
        sw = new Switch(this);
        int[][] st = {{android.R.attr.state_checked}, {}};
        sw.setThumbTintList(new ColorStateList(st, new int[]{Ui.GOLD, 0xFFB7DCC4}));
        sw.setTrackTintList(new ColorStateList(st, new int[]{0x88F2B705, 0x33FFFFFF}));
        fr.addView(sw, new LinearLayout.LayoutParams(-2, -2));
        fr.setOnClickListener(v -> sw.toggle());
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
        fp.setMargins(0, dp(8), 0, 0);
        page.addView(fr, fp);
        sw.setOnCheckedChangeListener((btn, on) -> {
            if (building) return;
            if (on) enableFloat();
            else disableFloat();
        });

        View sp = new View(this);
        page.addView(sp, new LinearLayout.LayoutParams(1, 0, 1f));
        TextView pw = Ui.tv(this, "Powered by Al-ameen", 14, Ui.GOLD, false);
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

    // one tap selects; a second tap on the same button within a moment opens that profile's Ads page
    void tap(boolean work) {
        long now = SystemClock.elapsedRealtime();
        boolean dbl = lastTap != 0 && now - lastTap < 380 && lastTapWork == work;
        lastTap = dbl ? 0 : now;
        lastTapWork = work;
        select(work);
        if (dbl) {
            if (work == S.isManaged(this)) Ads.open(this);
            else Reset.remoteAds(this);
        }
    }

    void select(boolean work) {
        selWork = work;
        S.setSelWork(this, work);
        Ui.styleHalf(this, personalBtn, !work);
        Ui.styleHalf(this, workBtn, work);
    }

    void enableFloat() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, tr("Allow \"Display over other apps\", then come back and switch it on.",
                    "Ba da izinin \"Display over other apps\", sannan ka dawo ka kunna."), Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
            setSwitch(false);
            return;
        }
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 9);
        }
        S.setFloat(this, true);
        startForegroundService(new Intent(this, FloatingService.class));
        if (!serviceOn()) {
            Toast.makeText(this, tr("Also turn on Accessibility for ADS profile, or the button cannot tap.",
                    "Kunna Accessibility na ADS profile ma, in ba haka ba maɓallin ba zai iya dannawa ba."), Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }
    }

    void disableFloat() {
        S.setFloat(this, false);
        stopService(new Intent(this, FloatingService.class));
    }

    void setSwitch(boolean on) {
        building = true;
        sw.setChecked(on);
        building = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (S.ha(this) != shownHa) {
            recreate();
            return;
        }
        if (banner != null) banner.resume();
        boolean on = S.floatOn(this);
        if (on && !FloatingService.running && Settings.canDrawOverlays(this)) {
            startForegroundService(new Intent(this, FloatingService.class));
        }
        if (on && !Settings.canDrawOverlays(this)) {
            S.setFloat(this, false);
            on = false;
        }
        setSwitch(on);
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
