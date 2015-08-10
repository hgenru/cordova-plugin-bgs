/* global cordova, module */
function flatten(e,t){function r(e,c){Object.keys(e).forEach(function(o){var a=e[o],s=t.safe&&Array.isArray(a),y=Object.prototype.toString.call(a),b=isBuffer(a),j="[object Object]"===y||"[object Array]"===y,l=c?c+f+o:o;return t.maxDepth||(n=i+1),!s&&!b&&j&&Object.keys(a).length&&n>i?(++i,r(a,l)):void(u[l]=a)})}t=t||{};var f=t.delimiter||".",n=t.maxDepth,i=1,u={};return r(e),u}function isBuffer(e){return"undefined"==typeof Buffer?!1:Buffer.isBuffer(e)}


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
            config.defaults = config.defaults;
        }
        if (config.defaultRequestParams) {
            config.defaultRequestParams = flatten(config.defaultRequestParams);
        }
        cordova.exec(
            successCallback,
            errorCallback,
            'bgs', 'configure',
            [config]
        );
    }
};
