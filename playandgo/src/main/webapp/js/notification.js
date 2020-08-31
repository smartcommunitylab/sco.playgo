var notification = angular.module('notification', [ 'ngMask', 'textAngular' ]);

var PAGE_SIZE = 5;

$(document).ready(function(){
	$('#agencyId-dropdown').tooltip({placement: 'left', title: '<span align="left"><p><b>5</b>: Verona Brennero<br/><b>6</b>: Trento Bassano<br/><b>10</b>: Trento-Mal&#232;<br/><b>12</b>: Trento Urbano<br/><b>16</b>: Rovereto Urbano<br/><b>17</b>: Extraurbano</p>', html: true});   
});

notification.controller('NotificationCtrl', function($scope, $http) {
	
	$scope.updateAgency = function($event) {
		$scope.agencyId = $event.target.innerText;
		if ($scope.agencyId != '-') {
			$http.get("notification/announcements/routesIds/" + $scope.agencyId).success(function(data) {	
				var routes = ["-"];
				for (var i = 0; i < data.length; i++) {
					routes.push(data[i].id.id + " / " + data[i].routeShortName + " / " + data[i].routeLongName);
				}
				$scope.routeIds = routes;
			});		
		} else {
			$scope.agencyId = null;
			$scope.routeIds = ["-"];
		}
		$scope.routeId = null;
	};	
	
	$scope.updateRoute = function($event) {
		$scope.routeId = $event.target.innerText;
		if ($scope.routeId == '-') {
			$scope.routeId = null;
		}
	}
	
	$scope.what = "News";
	
    $scope.notifyMessage = " ";
	$scope.notifyError = " "; 	
	
	$scope.skipAnnouncement = 0;
	
	$scope.prevAnnouncementVisible = false;
	$scope.nextAnnouncementVisible = false;
	
	$scope.prevText  = "";
	$scope.currentText  = "";
	$scope.nextText  = "";
	
	$scope.init = function() {
		$http.get("notification/announcements/appId").success(function(data) {	
			$scope.appId = data;
		});
		$http.get("notification/announcements/agencyIds").success(function(data) {	
			$scope.agencyIds = data;
		});		
		$scope.resetNews();
	}
	
	$scope.updateAnnouncement = function() {
		$scope.nextAnnouncementVisible = true;
		
		$http.get("notification/announcements/" + $scope.what.toLowerCase() + "?skip=" + $scope.skipAnnouncement + "&limit=" + PAGE_SIZE).success(function(data) {
			$scope.msgss = data.announcement;
			$scope.prevText = ($scope.skipAnnouncement - PAGE_SIZE + 1) + '-' + ($scope.skipAnnouncement - 1 + 1);
			$scope.currentText = ($scope.skipAnnouncement + 1) + '-' + ($scope.skipAnnouncement + (PAGE_SIZE - 1 + 1));
			if ($scope.skipAnnouncement > 0) {
				$scope.prevAnnouncementVisible = true;
			} else {
				$scope.prevAnnouncementVisible = false;
			}				
			if ($scope.msgss.length < PAGE_SIZE) {
				$scope.nextAnnouncementVisible = false;
			}	
			
			$scope.nextText = ($scope.skipAnnouncement + PAGE_SIZE + 1) + '-' + ($scope.skipAnnouncement + (2 * PAGE_SIZE - 1) + 1);
		});

	}
	
	$scope.resetNews = function() {
		$scope.what = "News";
		$scope.skipAnnouncement = 0;
		$scope.updateAnnouncement();
	}
	
	$scope.resetNotifications = function() {
		$scope.what = "Notifications";
		$scope.skipAnnouncement = 0;
		$scope.updateAnnouncement();
	}	
	
	$scope.init();
	
	$scope.nextAnnouncement = function() {
		$scope.skipAnnouncement += PAGE_SIZE;
		$scope.updateAnnouncement();
	}
	
	$scope.prevAnnouncement = function() {
		$scope.skipAnnouncement -= PAGE_SIZE;
		$scope.updateAnnouncement();
	}	
	
	$scope.notify = function() {
		if ($scope.validate()) {
			$http.post("notification/notify", {
				'title' : $scope.form.title,
				'description' : $scope.form.description,
				'html' : $scope.form.html,
				'from' : $scope.form.from,
				'to' : $scope.form.to,
				'notification' : $scope.form.notification,
				'news' : $scope.form.news,
				'agencyId' : $scope.agencyId,
				'routeId' : $scope.routeId
			}).success(function(data) {
				$scope.notifyMessage = data.message;
				$scope.notifyError = data.error;
				$scope.resetNews();
			}).error(function(error) {
				$scope.notifyMessage = data.message;
				$scope.notifyError = data.error;
				$scope.resetNews();
			});
		}
	}
	$scope.validate = function() {
		if ($scope.form.news == false && $scope.form.notification == false) {
			$scope.notifyMessage = "";
			$scope.notifyError = "One of 'News' or 'Notification' must be checked!";
		} else if (!$scope.form) {
			$scope.notifyMessage = "";
			$scope.notifyError = "No data!";
		} else if (!$scope.form.title || $scope.form.title.length == 0) {
			$scope.notifyMessage = "";
			$scope.notifyError = "'Title' cannot be empty!";
		} else if (Date.parse($scope.form.to) < Date.parse($scope.form.from)) {
			$scope.notifyMessage = "";
			$scope.notifyError = "'To' must be greater or equal to 'From'!";

		} else {
			return true;
		}
		return false;
	}
	
	$scope.reset = function() {
        $scope.form = angular.copy({});
        $scope.notifyMessage = null;
		$scope.notifyError = null;   
		$scope.form.news = true;
		$scope.form.notification = false;
		$scope.agencyId = null;
		$scope.routeId = null;
      };
	
});
