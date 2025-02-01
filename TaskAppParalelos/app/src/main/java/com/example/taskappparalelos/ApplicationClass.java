package com.example.taskappparalelos;

import android.app.AlertDialog;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.onesignal.OneSignal;

public class ApplicationClass extends Application {

    private static final String ONESIGNAL_APP_ID = "668406ab-e8b1-4098-97cb-3ad0ee154297";
    private static final String PREFS_NAME = "MyPrefs";
    private static final String PLAYER_ID_KEY = "player_id";
    @Override
    public void onCreate() {
        super.onCreate();

        // Inicializar OneSignal
        OneSignal.initWithContext(this);
        OneSignal.setAppId(ONESIGNAL_APP_ID);


        OneSignal.disablePush(false);
        OneSignal.promptForPushNotifications();

        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);
        OneSignal.setNotificationOpenedHandler(result ->
                Log.d("OneSignal", "Notificación abierta: " + result.toString()));


        // Esperar hasta que OneSignal obtenga el playerId y guardarlo en SharedPreferences
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            String playerId = OneSignal.getDeviceState().getUserId();
            if (playerId != null) {
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                prefs.edit().putString(PLAYER_ID_KEY, playerId).apply();
                Log.d("OneSignal", "Player ID guardado: " + playerId);
            }
        }, 5000); // Espera 5 segundos para asegurarse de que OneSignal lo genera

    }
}
