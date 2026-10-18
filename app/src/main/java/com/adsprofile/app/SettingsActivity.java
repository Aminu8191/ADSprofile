package com.adsprofile.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

// Settings: language, contact, and the Telegram channel.
public class SettingsActivity extends Activity {
    static final String MAIL = "aminulawanzakariyyah@gmail.com";
    static final String TG = "https://t.me/al_ameen_developer";

    TextView accSub;

    String tr(String en, String ha) {
        return S.t(this, en, ha);
    }

    int dp(float v) {
        return Ui.dp(this, v);
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(0xFF02200F);
        getWindow().setNavigationBarColor(0xFF0A6B38);

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setBackground(Ui.bg());
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(20), dp(22), dp(18));
        sv.addView(page, new FrameLayout.LayoutParams(-1, -1));

        // header: back + title
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout back = new FrameLayout(this);
        back.setBackground(Ui.ripple(this, Ui.shape(this, 0x1AFFFFFF, Ui.GOLD, 22), 22));
        back.setClickable(true);
        back.setOnClickListener(v -> finish());
        back.addView(new IconView(this, IconView.BACK, Ui.GOLD), new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER));
        head.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView title = Ui.tv(this, tr("Settings", "Saituna"), 26, 0xFFFFFFFF, true);
        title.setPadding(dp(14), 0, 0, 0);
        head.addView(title);
        page.addView(head, new LinearLayout.LayoutParams(-1, -2));

        // language
        page.addView(label(tr("Language", "Harshe")));
        LinearLayout seg = new LinearLayout(this);
        seg.setOrientation(LinearLayout.HORIZONTAL);
        seg.setPadding(dp(6), dp(6), dp(6), dp(6));
        seg.setBackground(Ui.shape(this, 0x1AFFFFFF, 0x55F2B705, 26));
        seg.addView(langBtn("English", false));
        seg.addView(langBtn("Hausa", true));
        page.addView(seg, new LinearLayout.LayoutParams(-1, -2));

        // accessibility: the reset buttons need it
        page.addView(label(tr("Accessibility", "Accessibility")));
        page.addView(card(IconView.TOGGLE, tr("Accessibility service", "Accessibility service"), "", v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))));
        accSub = lastSub;
        refreshAcc();

        // contact
        page.addView(label(tr("Contact or Help", "Tuntu\u0253a ko Taimako")));
        page.addView(card(IconView.MAIL, tr("Contact or Help", "Tuntu\u0253a ko Taimako"), MAIL, v -> openMail()));

        // push the Telegram card to the bottom
        View sp = new View(this);
        page.addView(sp, new LinearLayout.LayoutParams(1, 0, 1f));

        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
        tp.topMargin = dp(24);
        page.addView(card(IconView.TELEGRAM, tr("For more details and apps", "Don \u0199arin bayani da apps"),
                "t.me/al_ameen_developer", v -> openTelegram()), tp);

        TextView pw = Ui.tv(this, "Powered by Al-ameen", 14, Ui.GOLD, false);
        pw.setGravity(Gravity.CENTER);
        pw.setPadding(0, dp(20), 0, dp(2));
        page.addView(pw, new LinearLayout.LayoutParams(-1, -2));
        TextView ver = Ui.tv(this, tr("Version ", "Sigar ") + version(), 12, Ui.MINT, false);
        ver.setGravity(Gravity.CENTER);
        page.addView(ver, new LinearLayout.LayoutParams(-1, -2));

        setContentView(sv);
    }

    String version() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "";
        }
    }

    TextView label(String text) {
        TextView t = Ui.tv(this, text, 14, Ui.GOLD, true);
        t.setLetterSpacing(0.06f);
        t.setPadding(dp(4), dp(26), 0, dp(10));
        return t;
    }

    TextView langBtn(String name, final boolean ha) {
        boolean sel = S.ha(this) == ha;
        TextView v = Ui.tv(this, name, 16, sel ? Ui.DARK : Ui.GOLD, true);
        v.setGravity(Gravity.CENTER);
        v.setClickable(true);
        v.setBackground(sel ? Ui.gold(this, 20) : Ui.ripple(this, Ui.shape(this, 0x00000000, 0, 20), 20));
        v.setLayoutParams(new LinearLayout.LayoutParams(0, dp(48), 1f));
        v.setOnClickListener(x -> {
            if (S.ha(this) == ha) return;
            S.setHa(this, ha);
            recreate();
        });
        return v;
    }

    TextView lastSub;

    boolean accOn() {
        try {
            String v = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return v != null && v.contains(getPackageName()) && v.contains("ResetService");
        } catch (Exception e) {
            return false;
        }
    }

    void refreshAcc() {
        if (accSub == null) return;
        if (S.isManaged(this)) {
            accSub.setText(tr("Not needed in this profile. The main app taps for you.",
                    "Ba a bu\u0199ata a nan. Main app ne ke dannawa."));
            accSub.setTextColor(0xFF7DDC9A);
        } else if (accOn()) {
            accSub.setText(tr("On", "A kunne"));
            accSub.setTextColor(0xFF7DDC9A);
        } else {
            accSub.setText(tr("Off. Tap here to turn it on.", "A kashe. Danna nan don kunnawa."));
            accSub.setTextColor(Ui.GOLD);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAcc();
    }

    // icon in a gold circle, a title and a line under it
    LinearLayout card(int icon, String title, String sub, View.OnClickListener click) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.HORIZONTAL);
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.setClickable(true);
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        c.setBackground(Ui.ripple(this, Ui.shape(this, 0x1AFFFFFF, 0x55F2B705, 24), 24));
        c.setOnClickListener(click);

        FrameLayout badge = new FrameLayout(this);
        badge.setBackground(Ui.gold(this, 26));
        badge.addView(new IconView(this, icon, Ui.DARK), new FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER));
        c.addView(badge, new LinearLayout.LayoutParams(dp(52), dp(52)));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), 0, 0, 0);
        col.addView(Ui.tv(this, title, 17, 0xFFFFFFFF, true));
        TextView s = Ui.tv(this, sub, 14, Ui.GOLD, false);
        s.setPadding(0, dp(3), 0, 0);
        col.addView(s);
        lastSub = s;
        c.addView(col, new LinearLayout.LayoutParams(0, -2, 1f));
        return c;
    }

    // opens Gmail directly; any other mail app if Gmail is missing; copies the address as a last resort
    void openMail() {
        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + MAIL));
        i.putExtra(Intent.EXTRA_SUBJECT, "ADS profile");
        try {
            i.setPackage("com.google.android.gm");
            startActivity(i);
            return;
        } catch (Exception ignored) {
        }
        try {
            i.setPackage(null);
            startActivity(i);
        } catch (Exception e) {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("email", MAIL));
            Toast.makeText(this, tr("Email copied", "An kwafi email \u0257in"), Toast.LENGTH_SHORT).show();
        }
    }

    // opens the Telegram app directly; the browser if Telegram is not installed
    void openTelegram() {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(TG));
        try {
            i.setPackage("org.telegram.messenger");
            startActivity(i);
            return;
        } catch (Exception ignored) {
        }
        try {
            i.setPackage(null);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, tr("Could not open", "Ba a iya bu\u0257ewa ba"), Toast.LENGTH_SHORT).show();
        }
    }
}
