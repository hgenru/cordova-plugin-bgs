/* global cordova, module */


module.exports = {
    start: function (successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'bgs', 'start',
            []
        );
    },
    stop: function (successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'bgs', 'stop',
            []
        );
    },
    restart: function (successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'bgs', 'restart',
            []
        );
    },
    configure: function (config, successCallback, errorCallback) {
        var toFloat = ['minAccuracity', 'minDistance', 'minAccuracity', 'distanceFilter'];
        var i;
        for (i = 0; i < toFloat.length; i++) {
            var key = toFloat[i];
            config[key] = parseFloat(config[key]);
        }
        if (config.defaults) {
            config.defaults = JSON.stringify(config.defaults);
        }
        cordova.exec(
            successCallback,
            errorCallback,
            'bgs', 'configure',
            [config]
        );
    }
};
