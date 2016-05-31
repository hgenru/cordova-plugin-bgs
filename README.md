# BackgroundGeolocationService with notification for Android

Simple cordova plugin for capture geodata in background mode, send to server and notify (if needed) user with `local notification`. Plugin use awesome [Cordova Local Notification Plugin](https://github.com/katzer/cordova-plugin-local-notifications) builder it is making available to you all its API.


# Example usage

    var config = {
        serverUrl: 'https://example.com?my_param1=123&my_param2=321',
        startOnBoot: true,  // Load service on boot
        // Android LocationListener params, see [this](https://developer.android.com/reference/android/location/LocationManager.html#requestLocationUpdates(java.lang.String, long, float, android.location.LocationListener, android.os.Looper)) article
        minDistance: 10,
        minTime: 24 * 60 * 60 * 1000,
        minAccuracity: 150,
        distanceFilter: 100,
        throttle: 1 * 60 * 1000,  // Throttle http requests, ms
        // Default Notification property
        defaults: {smallIcon: 'res://icon'},
        defaultRequestParams: {access_key: 'ADSsad2129casjas8'} // default post params
    };
    window.bgs.configure(config, function() {
        window.bgs.start(
            function() { console.log('bgs init successful'); },
            function() { console.log('bgs init failure'); }
        );
    });


# Example server request

    POST https://example.com?my_param1=123&my_param2=321
    BODY latitude=33&longitude=34&accuracy=100&access_key=ADSsad2129casjas8


# Example server response

You must response json data on request. Response json format similiar to Cordova Local Notification Plugin `schedule` function api. See more info [there](https://github.com/katzer/cordova-plugin-local-notifications/wiki/04.-Scheduling). If you not want to show notification response to client with http code 204.

### for on notification

    {"text": "Hello World"}

### or for multiplie notification

    [{"text": "Hello One"}, {"text": "Hello Two"}]


# Capture event in cordova code

You may pass additional data and capture click event in client JavaScript code. See more info [there](https://github.com/katzer/cordova-plugin-local-notifications/wiki/09.-Events).

    {"text": "My Custom Notification", "data": {"myDataId": 123}}

    cordova.plugins.notification.local.on('click', function(notification) {
        if (notification.data) {
            if (typeof notification.data === 'string') {
                // BUG: https://github.com/katzer/cordova-plugin-local-notifications/issues/538
                notification.data = JSON.parse(notification.data);
            }
            if (notification.data.myDataId) {
                alert('Capture click with myDataId: ' + myDataId);
            }
        }
    });


# Api Overview

## `window.bgs.*`

#### `start()`:
start BackgroundGeolocation service

#### `stop()`:
stop BackgroundGeolocation service

#### `restart()`:
stop BackgroundGeolocation service

#### `configure(config)`:

configure BackgroundGeolocation service
> Note: you must restart bgs after configure

    config = {
        serverUrl: 'https://example.com?my_param1=123&my_param2=321',
        startOnBoot: true,  // Load service on boot
        // Android LocationListener params, see [this](https://developer.android.com/reference/android/location/LocationManager.html#requestLocationUpdates(java.lang.String, long, float, android.location.LocationListener, android.os.Looper)) article
        minDistance: 10,
        minTime: 24 * 60 * 60 * 1000,
        throttle: 1 * 60 * 1000,  // Throttle http requests, ms
        // Default Notification property
        defaults: {smallIcon: 'res://icon', sound: 'file://sound.mp3'}
    }


# Note

In IOS you may wake up app and process response in JavaScript code.

You may use this plugin with [Cordova BackgroundGeoLocation Plugin](https://github.com/christocracy/cordova-plugin-background-geolocation) for setup similiar behavior on IOS. Just remove `src/android` from BackgroundGeoLocation plugin directory.
