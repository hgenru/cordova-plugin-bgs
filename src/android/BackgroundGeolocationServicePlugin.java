package com.jettech.bgs;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import android.util.Log;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;


public class BackgroundGeolocationServicePlugin extends CordovaPlugin {

    private static final String TAG = BackgroundGeolocationServicePlugin.class.getCanonicalName();
    public static final String PREFS_NAME = "BackgroundGeolocationService";
    static SharedPreferences preferences;
    static SharedPreferences.Editor preferencesEditor;
    private Intent serviceIntent;

    @Override
    public boolean execute(String action, final JSONArray data, final CallbackContext callbackContext) throws JSONException {
        final Activity activity = this.cordova.getActivity();
        final Context context = activity.getApplicationContext();
        serviceIntent = new Intent(activity, BackgroundGeolocationService.class);

        if (action.equals("start")) {
            Log.d(TAG, "Call `start` BackgroundGeolocationService");
            activity.startService(serviceIntent);
            callbackContext.success();

        } else if (action.equals("stop")) {
            Log.d(TAG, "Call `stop` BackgroundGeolocationService");
            activity.stopService(serviceIntent);
            callbackContext.success();

        } else if (action.equals("restart")) {
            Log.d(TAG, "Call `restart` BackgroundGeolocationService");
            activity.stopService(serviceIntent);
            activity.startService(serviceIntent);
            callbackContext.success();

        } else if (action.equals("configure")) {
            this.cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try {
                        Log.d(TAG, "Call `configure` BackgroundGeolocationService");
                        String jsonConfig = data.getString(0);
                        Log.d(TAG, "Reconfigure service with: " + jsonConfig);
                        JSONObject config = data.getJSONObject(0);
                        preferences = context.getSharedPreferences(PREFS_NAME, 0 | Context.MODE_MULTI_PROCESS);
                        preferencesEditor = preferences.edit();
                        // serverUrl
                        if (config.has("serverUrl")) {
                            String serverUrl = config.getString("serverUrl");
                            preferencesEditor.putString("serverUrl", serverUrl);
                        }
                        // startOnBoot
                        if (config.has("startOnBoot")) {
                            Boolean startOnBoot = config.getBoolean("startOnBoot");
                            preferencesEditor.putBoolean("startOnBoot", startOnBoot);
                        }
                        // throttle
                        if (config.has("throttle")) {
                            Long throttle = config.getLong("throttle");
                            preferencesEditor.putLong("throttle", throttle);
                        }
                        // minTime
                        if (config.has("minTime")) {
                            Long minTime = config.getLong("minTime");
                            preferencesEditor.putLong("minTime", minTime);
                        }
                        // minDistance
                        if (config.has("minDistance")) {
                            Double minDistance = config.getDouble("minDistance");
                            preferencesEditor.putFloat("minDistance", minDistance.floatValue());
                        }
                        // minDistance
                        if (config.has("defaults")) {
                            String defaults = config.getString("defaults");
                            preferencesEditor.putString("defaults", defaults);
                        }

                        preferencesEditor.commit();
                        callbackContext.success();
                    } catch (JSONException e) {
                        Log.w(TAG, "Invalig config object");
                        e.printStackTrace();
                        callbackContext.error(1);
                    }
                }
            });

        } else {
            return false;
        }

        return true;
    }
}
