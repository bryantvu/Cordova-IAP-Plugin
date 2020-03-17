var exec = require('cordova/exec');

exports.coolMethod = function (arg0, success, error) {
    exec(success, error, 'IapWrapper', 'coolMethod', [arg0]);
};

exports.buyNonConsumable = function (success, error) {
    exec(success, error, 'IapWrapper', 'buyNonConsumable', []);
};

exports.getNonConsumables = function (success, error) {
    exec(success, error, 'IapWrapper', 'getNonConsumables', []);
};
