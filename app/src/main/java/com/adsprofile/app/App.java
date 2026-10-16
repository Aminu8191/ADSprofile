package com.adsprofile.app;

import android.app.Application;

// Starts the ads SDK and the App Open ad as early as possible, before any screen opens.
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppOpen.init(this);
    }
}
