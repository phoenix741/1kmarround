package org.shadoware.a1kmarroud;

import android.content.Context;

public class Application extends android.app.Application {
    private static Application self;

    public static Application getApplication() {
        return self;
    }

    public static Context getContext() {
        return getApplication().getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        self = this;
    }
}
