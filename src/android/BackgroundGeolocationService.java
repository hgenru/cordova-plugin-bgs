package com.jettech.bgs;

import org.json.*;
import java.util.Date;
import org.apache.http.Header;

import android.util.Log;
import android.os.Bundle;
import android.os.IBinder;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.app.PendingIntent;
import android.text.format.DateFormat;
import android.content.SharedPreferences;

import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;

import com.loopj.android.http.*;


public class BackgroundGeolocationService extends Service implements LocationListener {

    static final String TAG = BackgroundGeolocationService.class.getCanonicalName();
    static final String PREFS_NAME = "BackgroundGeolocationService";

    String serverUrl;
    Long throttle;

    Long lastLocationTime = 0L;

    SharedPreferences preferences;
    LocationManager locationManager;
    AsyncHttpClient httpClient;

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        preferences = this.getSharedPreferences(PREFS_NAME, 0 | Context.MODE_MULTI_PROCESS);
        serverUrl = preferences.getString("serverUrl", "null");
        throttle = preferences.getLong("throttle", 0);
        httpClient = new AsyncHttpClient();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Long minTime = preferences.getLong("minTime", 0);
        Float minDistance = preferences.getFloat("minDistance", 20);
        String[] providers = new String[] {
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        };
        for (String provider : providers) {
            locationManager.requestLocationUpdates(provider, minTime, minDistance, this);
        }
    }

    public void locationHandler(Location location) {
        final long locationTime = location.getTime();
        CharSequence locationTimeFormatted = DateFormat.format(
            "yyyy-MM-dd kk:mm:ss", locationTime);
        final String provider = location.getProvider();
        final double longitude = location.getLongitude();
        final double latitude = location.getLatitude();
        final float accuracy = location.getAccuracy();

        Log.i(TAG, String.format(
            "%s src:%s; long:%s; lat:%s; acc:%f",
            locationTimeFormatted.toString(), provider,
            latitude, longitude, accuracy)
        );
        if ((locationTime - lastLocationTime) < throttle) {
            Log.d(TAG,
                "locationHandler, reject http request by throttle; lastLocationTime: "
                + lastLocationTime + " throttle: " + throttle);
            return;
        }

        RequestParams params = new RequestParams();
        params.put("provider", provider);
        params.put("longitude", longitude);
        params.put("latitude", latitude);
        params.put("accuracy", accuracy);
        final String requestMsg = String.format(
            "http request url: %s, with params: %s",
            serverUrl, params.toString()
        );

        lastLocationTime = locationTime;

        httpClient.get(serverUrl, params, new AsyncHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d(TAG, "httpClient.get.onStart, " + requestMsg);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                Log.d(TAG, "httpClient.get.onSuccess");
                // 204 will be ignored
                if (statusCode == 204) {
                    return;
                }
                try {
                    String stringResponse = new String(response, "UTF-8");
                    if (stringResponse.length() == 0) {
                        Log.w(TAG, "Ignoring empty response");
                        return;
                    }
                    JSONObject jsonResponse = new JSONObject(stringResponse);
                    if (jsonResponse.has("text")) {
                        showNotification(jsonResponse);
                    } else {
                        Log.d(TAG, "Ignoring wrong JSONObject");
                    }
                } catch (java.io.UnsupportedEncodingException ex) {
                    ex.printStackTrace();
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] error, Throwable ex) {
                Log.d(TAG, "httpClient.get.onFailure");
            }

            @Override
            public void onRetry(int retryNo) {
                Log.w(TAG, "httpClient.get.onRetry, http request url: " + requestMsg);
            }
        });
    }

    public void showNotification(JSONObject options) {
        Log.i(TAG, "showNotification, " + options.toString());
        // Sorry about this long java name shit
        de.appplant.cordova.plugin.notification.Builder builder = new de.appplant.cordova.plugin.notification.Builder(this, options);
        de.appplant.cordova.plugin.notification.Notification notification = builder
            .setTriggerReceiver(de.appplant.cordova.plugin.localnotification.TriggerReceiver.class)
            .setClearReceiver(de.appplant.cordova.plugin.localnotification.ClearReceiver.class)
            .setClickActivity(de.appplant.cordova.plugin.localnotification.ClickActivity.class)
            .build();
        notification.schedule();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onLocationChanged(Location location) {
        locationHandler(location);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.v(TAG, "onProviderDisabled, disabled: " + provider);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.v(TAG, "onProviderEnabled, enabled: " + provider);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.v(TAG, String.format(
            "onStatusChanged, provider: %s status: %i extars: %s",
            provider, status, extras)
        );
    }
}