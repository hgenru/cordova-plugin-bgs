package com.jettech.bgs;

import android.util.Log;
import android.content.Intent;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.SharedPreferences;


public class BootReceiver extends BroadcastReceiver {

    static final String TAG = BroadcastReceiver.class.getCanonicalName();
    static final String PREFS_NAME = "BackgroundGeolocationService";
    SharedPreferences preferences;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        preferences = context.getSharedPreferences(PREFS_NAME, 0 | Context.MODE_MULTI_PROCESS);
        Boolean startOnBoot = preferences.getBoolean("startOnBoot", false);
        if (startOnBoot) {
            Log.i(TAG, "onReceive, start BackgroundGeolocationService");
            Intent serviceIntent = new Intent(context, BackgroundGeolocationService.class);
            context.startService(serviceIntent);
        } else {
            Log.i(TAG, "onReceive, reject start BackgroundGeolocationService, startOnBoot=false");
        }
    }

}
