package com.jettech.bgs;

import android.util.Log;
import android.os.Environment;
import android.content.Intent;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;

import com.jettech.bgs.FileLog;


public class BootReceiver extends BroadcastReceiver {

    static final String TAG = BootReceiver.class.getCanonicalName();
    static final String PREFS_NAME = "BackgroundGeolocationService";
    SharedPreferences preferences;

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean DEBUGGABLE = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (DEBUGGABLE) {
            String appName = context.getPackageName() + "-background-geolocation.log";
            String path = Environment.getExternalStorageDirectory().getPath() + "/" + appName;
            FileLog.open(path, Log.VERBOSE, 1000000);
        }
        FileLog.d(TAG, "onReceive");
        preferences = context.getSharedPreferences(PREFS_NAME, 0 | Context.MODE_MULTI_PROCESS);
        Boolean startOnBoot = preferences.getBoolean("startOnBoot", false);
        if (startOnBoot) {
            FileLog.i(TAG, "onReceive, start BackgroundGeolocationService");
            Intent serviceIntent = new Intent(context, BackgroundGeolocationService.class);
            context.startService(serviceIntent);
        } else {
            FileLog.i(TAG, "onReceive, reject start BackgroundGeolocationService, startOnBoot=false");
        }
    }

}
