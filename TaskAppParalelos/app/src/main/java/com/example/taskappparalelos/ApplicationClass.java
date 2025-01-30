package com.example.taskappparalelos;

import android.app.AlertDialog;
import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;
import com.onesignal.Continue;

public class ApplicationClass extends Application {

    private static final String ONESIGNAL_APP_ID = "668406ab-e8b1-4098-97cb-3ad0ee154297";

    @Override
    public void onCreate() {
        super.onCreate();

        // Verbose Logging set to help debug issues, remove before releasing your app.
        OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);

        // OneSignal Initialization
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID);

        // requestPermission will show the native Android notification permission prompt.
        // NOTE: It's recommended to use a OneSignal In-App Message to prompt instead.
        OneSignal.getNotifications().requestPermission(false, Continue.none());
        // Obtener el userId (Player ID)

    }
}
