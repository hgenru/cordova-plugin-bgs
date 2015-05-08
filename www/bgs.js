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
        cordova.exec(
            successCallback,
            errorCallback,
            'bgs', 'configure',
            [config]
        );
    }
};
