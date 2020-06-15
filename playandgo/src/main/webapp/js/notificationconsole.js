var notificationConsole = angular.module('notificationConsole', [,'ui.bootstrap',
'ngRoute',
'notification'
]);

notificationConsole.config(['$routeProvider',
                     function($routeProvider) {
                       $routeProvider.
                         when('/', {
                           templateUrl: '../templates/notificationconsoleinner.html',
                           controller: 'NotificationCtrl'
                         });
                     }]);

notificationConsole.run([function(){
  }]);


