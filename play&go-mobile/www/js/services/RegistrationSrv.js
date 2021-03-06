angular.module('viaggia.services.registration', [])

.factory('registrationService', function ($q, $http, $filter, Config, LoginService) {
    var registrationService = {};

    registrationService.register = function (user) {
        var deferred = $q.defer();
        LoginService.getValidAACtoken().then(function (validToken) {
            var urlReg = Config.getServerGamificationURL() + "/gamificationweb/register?"+
                "nickname=" + user.nickname +
                "&email="+user.mail +
                "&language=" + Config.getLang();
            $http({
                method: 'POST',
                url: urlReg,
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json',
                     'Authorization': 'Bearer ' + validToken,
                    'appId': Config.getAppGameId()
                                },
                data: {
                    "nick_recommandation": user.nick_recommandation
                },
                timeout: 20000
            }).
            success(function (data, status, headers, config) {
                localStorage.userValid = true;
                deferred.resolve(data);
            }).
            error(function (data, status, headers, config) {
                console.log(data + status + headers + config);
                deferred.reject(status);
            });

        });


        return deferred.promise;
    }
    return registrationService;
});
