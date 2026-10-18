package com.adsprofile.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;

// A still picture of the screen you were on, with a copy of the floating button spinning on it.
// You seem to stay in WhatsApp (or whatever app) while the Ads page works underneath.
class FreezeView extends FrameLayout {
    FabView fab;

    FreezeView(Context c, Bitmap shot, int x, int y, int size, int fill, boolean showFab) {
        super(c);
        ImageView iv = new ImageView(c);
        iv.setImageBitmap(shot);
        iv.setScaleType(ImageView.ScaleType.FIT_XY);
        addView(iv, new FrameLayout.LayoutParams(-1, -1));
        if (showFab) {
            fab = new FabView(c, fill);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size, Gravity.TOP | Gravity.START);
            lp.leftMargin = x;
            lp.topMargin = y;
            addView(fab, lp);
            fab.setSpin(true);
        }
    }

    void setDone() {
        if (fab != null) fab.setSpin(false);
    }
}
