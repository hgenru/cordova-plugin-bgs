package com.jettech.bgs;

import org.json.*;
import java.util.*;
import java.util.Calendar;

import android.util.Log;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Environment;
import android.content.Intent;
import android.content.Context;
import android.app.Service;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.text.format.DateFormat;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;

import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;

import android.provider.Settings;

import cz.msebera.android.httpclient.Header;
import com.loopj.android.http.*;
import com.jettech.bgs.FileLog;


public class BackgroundGeolocationService extends Service implements LocationListener {

    static final String TAG = BackgroundGeolocationService.class.getCanonicalName();
    static final String PREFS_NAME = "BackgroundGeolocationService";

    String serverUrl;
    String userDefaults;
    Float minAccuracity;
    Float distanceFilter;
    Long throttle;
    String deviceId;
    String defaultRequestParams;

    Location lastLocation = null;

    SharedPreferences preferences;
    LocationManager locationManager;
    AsyncHttpClient httpClient;

    @Override
    public void onCreate() {
        FileLog.i(TAG, "onCreate");
        boolean DEBUGGABLE = (this.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (DEBUGGABLE) {
            String appName = this.getPackageName() + "-background-geolocation.log";
            String path = Environment.getExternalStorageDirectory().getPath() + "/" + appName;
            FileLog.open(path, Log.VERBOSE, 1000000);
        }
        preferences = this.getSharedPreferences(PREFS_NAME, 0 | Context.MODE_MULTI_PROCESS);
        userDefaults = preferences.getString("defaults", "");
        serverUrl = preferences.getString("serverUrl", "insert_your_url");
        throttle = preferences.getLong("throttle", 0);
        minAccuracity = preferences.getFloat("minAccuracity", 0);
        distanceFilter = preferences.getFloat("distanceFilter", 0);
        httpClient = new AsyncHttpClient();
        httpClient.setMaxRetriesAndTimeout(3, 2000);
        httpClient.setConnectTimeout(10000);

        defaultRequestParams = preferences.getString("defaultRequestParams", "{}");

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Long minTime = preferences.getLong("minTime", 0);
        Float minDistance = preferences.getFloat("minDistance", 20);
        FileLog.i(TAG, "onCreate, throttle:" + throttle + " minAccuracity:" + minAccuracity + " minDistance:" + minDistance);
        // Android before JellyBean may be ignore minTime and minDistance
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistance, this);
        locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 1000, 10, this);

        // Инициализация рестартера сервиса
        Calendar calendar = Calendar.getInstance();
        Intent intent = new Intent(this, BackgroundGeolocationService.class);
        PendingIntent pintent = PendingIntent.getService(this, 0, intent, 0);
        AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        // Рестартовать сервис раз в 4 часа, так мы себя оберегаем от наедания
        // и рестартимся если что-то стало не так
        alarm.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.getTimeInMillis(),
            4 * 60 * 60 * 1000,
            pintent
        );

        deviceId = Settings.Secure.getString(this.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
    }

    public void locationHandler(Location location) {
        final long locationTime = location.getTime();
        CharSequence locationTimeFormatted = DateFormat.format(
            "yyyy-MM-dd kk:mm:ss", locationTime);
        final String provider = location.getProvider();
        final double longitude = location.getLongitude();
        final double latitude = location.getLatitude();
        final float accuracy = location.getAccuracy();

        FileLog.i(TAG, String.format(
            "%s src:%s; long:%s; lat:%s; acc:%f",
            locationTimeFormatted.toString(), provider,
            latitude, longitude, accuracy)
        );

        if (minAccuracity > 0 && minAccuracity < accuracy) {
            FileLog.d(TAG, "locationHandler, reject http request by minAccuracity");
            return;
        }

        if (lastLocation != null) {
            Long lastLocationTime = lastLocation.getTime();
            if ((locationTime - lastLocationTime) < throttle) {
                FileLog.d(TAG,
                    "locationHandler, reject http request by throttle; lastLocationTime: "
                    + lastLocationTime + " throttle: " + throttle);
                return;
            }
            float distance = lastLocation.distanceTo(location);
            FileLog.d(TAG, "locationHandler, distance to lastLocation:" + distance);
            if (distance < distanceFilter) {
                FileLog.d(TAG, "locationHandler, reject http request by distanceFilter");
                return;
            }
        }

        RequestParams params = new RequestParams();
        try {
            JSONObject defaultsParams = new JSONObject(defaultRequestParams);
            for(Iterator<String> iter = defaultsParams.keys();iter.hasNext();) {
                String key = iter.next();
                String value = value = defaultsParams.getString(key);
                params.put(key, value);
            }
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
        params.put("provider", provider);
        params.put("longitude", longitude);
        params.put("latitude", latitude);
        params.put("accuracy", accuracy);
        params.put("deviceId", deviceId);
        final String requestMsg = String.format(
            "http request url: %s, with params: %s",
            serverUrl, params.toString()
        );

        lastLocation = location;

        httpClient.post(serverUrl, params, new AsyncHttpResponseHandler() {
            @Override
            public void onStart() {
                FileLog.d(TAG, "httpClient.post.onStart, " + requestMsg);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                FileLog.d(TAG, "httpClient.post.onSuccess");
                // 204 will be ignored
                if (statusCode == 204) {
                    FileLog.w(TAG, "Ignoring response with 204 http code");
                    return;
                }
                try {
                    JSONObject defaults;
                    if (userDefaults.length() > 0) {
                        defaults = new JSONObject(userDefaults);
                    } else {
                        defaults = new JSONObject();
                    }
                    defaults.put("sound", defaults.has("sound") ? defaults.optString("sound") : "res://platform_default");
                    defaults.put("icon", defaults.has("icon") ? defaults.optString("icon") : "res://icon");
                    defaults.put("smallIcon", defaults.has("smallIcon") ? defaults.optString("smallIcon") : "res://ic_popup_reminder");
                    defaults.put("ongoing", defaults.has("ongoing") ? defaults.optBoolean("ongoing") : false);
                    defaults.put("autoClear", defaults.has("autoClear") ? defaults.optBoolean("autoClear") : true);
                    defaults.put("led", defaults.has("led") ? defaults.optString("led") : "FFFFFF");
                    String stringResponse = new String(response, "UTF-8");
                    if (stringResponse.length() == 0) {
                        FileLog.w(TAG, "Ignoring empty response");
                        return;
                    }
                    Object jsonResponse = new JSONTokener(stringResponse).nextValue();
                    JSONArray notifyArray = null;
                    if (jsonResponse instanceof JSONObject) {
                        notifyArray = new JSONArray();
                        notifyArray.put(jsonResponse);
                    } else if (jsonResponse instanceof JSONArray) {
                        notifyArray = new JSONArray(jsonResponse);
                    } else {
                        FileLog.w(TAG, "Ignoring invalid response");
                        notifyArray = new JSONArray();
                    }
                    for(int n = 0; n < notifyArray.length(); n++) {
                        JSONObject currentJson = notifyArray.getJSONObject(n);
                        if (currentJson == null) {
                            FileLog.w(TAG, "Ignoring invalid entry (index: " + n + ")");
                        }
                        List<String> defaultsKeys = new ArrayList<String>();
                        Iterator<?> defaultsKeysIterator = defaults.keys();
                        while (defaultsKeysIterator.hasNext()) {
                            String key = (String)defaultsKeysIterator.next();
                            defaultsKeys.add(key);
                        }
                        String[] defaultsKeysArray = defaultsKeys.toArray(new String[defaultsKeys.size()]);
                        JSONObject notifyJson = new JSONObject(defaults, defaultsKeysArray);
                        Iterator<?> currentJsonKeysIterator = currentJson.keys();
                        while (currentJsonKeysIterator.hasNext()) {
                            String key = (String)currentJsonKeysIterator.next();
                            notifyJson.put(key, currentJson.get(key));
                        }
                        if (notifyJson.has("text")) {
                            showNotification(notifyJson);
                        } else {
                            FileLog.w(TAG, "Ignoring JSONObject without `text` property (index: " + n + ")");
                        }
                    }
                } catch (java.io.UnsupportedEncodingException ex) {
                    ex.printStackTrace();
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] error, Throwable ex) {
                FileLog.d(TAG, "httpClient.post.onFailure");
            }

            @Override
            public void onRetry(int retryNo) {
                FileLog.w(TAG, "httpClient.post.onRetry, http request url: " + requestMsg);
            }
        });
    }

    public void showNotification(JSONObject options) {
        FileLog.i(TAG, "showNotification, " + options.toString());
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
        FileLog.d(TAG, "onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            locationHandler(location);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        FileLog.d(TAG, "onDestroy");
        FileLog.close();

        locationManager.removeUpdates(this);

        super.onDestroy();
    }

    @Override
    public void onProviderDisabled(String provider) {
        FileLog.v(TAG, "onProviderDisabled, disabled: " + provider);
    }

    @Override
    public void onProviderEnabled(String provider) {
        FileLog.v(TAG, "onProviderEnabled, enabled: " + provider);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        FileLog.v(TAG, String.format(
            "onStatusChanged, provider: %s status: %i extars: %s",
            provider, status, extras)
        );
    }
}
