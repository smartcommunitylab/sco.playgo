angular.module('viaggia.controllers.common', [])

  .controller('AppCtrl', function ($scope, Toast, $rootScope, $locale, $q, $state, $ionicSideMenuDelegate, GameSrv, $cordovaCamera, profileService, trackService, $ionicHistory, $location, $timeout, $ionicScrollDelegate, $ionicPopup, $ionicModal, $filter, $ionicLoading, DataManager, Config, planService, Utils, tutorial) {

    $locale.NUMBER_FORMATS.GROUP_SEP = '';
    /* menu group */
    $scope.shownGroup = false;
    $scope.progressCounter = {
      walk: 0,
      bike: 0,
      bus: 0,
      train: 0
    };

    $scope.maxvalues = {
      maxDailywalk: 10000,
      maxDailybike: 20000,
      maxDailybus: 50000,
      maxDailytrain: 50000
    }
    $scope.progressPercent = {
      walk: 0,
      bike: 0,
      bus: 0,
      train: 0
    };
    $scope.toggleGroupRealTime = function () {
      if ($scope.isGroupRealTimeShown()) {
        $scope.shownGroup = false;
      } else {
        $scope.shownGroup = true;
      }
      localStorage.setItem(Config.getAppId() + '_shownGroup', $scope.shownGroup);
    };

    $scope.isGroupRealTimeShown = function () {
      return $scope.shownGroup === true;
    };

    // playGo group
    $scope.shownPlayGroup = false;
    $scope.toggleGroupPlayGo = function () {
      if ($scope.isGroupPlayGoShown()) {
        $scope.shownPlayGroup = false;
      } else {
        $scope.shownPlayGroup = true;
      }
      localStorage.setItem(Config.getAppId() + '_shownPlayGroup', $scope.shownPlayGroup);
    };
    $scope.openProfileOthers = function (profile) {
      $state.go('app.profileOthers', {
        profileId: profile.playerId
      });
    }
    $scope.openProfile = function () {
      $state.go('app.profile');
    }
    $scope.isGroupPlayGoShown = function () {
      return $scope.shownPlayGroup === true;
    };
    $scope.isAccessibilitySet = function () {
      return Config.getAccessibility();
    }

    $ionicModal.fromTemplateUrl('templates/credits.html', {
      id: '3',
      scope: $scope,
      backdropClickToClose: false,
      animation: 'slide-in-up'
    }).then(function (modal) {
      $scope.creditsModal = modal;
    });

    $scope.closeCredits = function () {
      $scope.creditsModal.hide();
    };

    $scope.openCredits = function () {
      $scope.creditsModal.show();
    };
    $scope.goBackView = function () {
      var backView = $ionicHistory.backView();
      if (backView) {
        $ionicHistory.goBack();
      } else {
        $state.go('app.home.home');
        $ionicHistory.nextViewOptions({
          disableBack: true,
          historyRoot: true
        });
      }
    }
    $scope.buttonMapNeed = function () {
      //if tracking is going on and I'm not in the map page show it
      if ($state.current.name != "app.mapTracking" && $state.current.name != "app.login" && trackService.trackingIsGoingOn() && !trackService.trackingIsFinished())
        return true;
      return false
    }
    $scope.isHomePages = function () {
      if ($state.current.name === "app.home.home" || $state.current.name === "app.home.leaderboards" || $state.current.name === "app.home.challenges" || $state.current.name === "app.home.diary" || $state.current.name === "app.home.mobility")
        return true;
      return false;
    }
    $scope.goToMap = function () {
      $state.go('app.mapTracking');
    }
    $scope.notHomeAndMap = function () {
      if ($state.current.name != "app.mapTracking" && $state.current.name != "app.login" && $state.current.name != "app.home.home" && trackService.trackingIsGoingOn() && !trackService.trackingIsFinished())
        return true;
      return false
    }
    $scope.trackingMultimodal = function () {
      return (localStorage.getItem(Config.getAppId() + '_multimodalId') != "null");
    }
    $scope.menuIsOpen = function () {
      return $ionicSideMenuDelegate.isOpen();
    }
    $scope.goHome = function () {
      $state.go('app.home.home');
      $ionicHistory.nextViewOptions({
        disableBack: true,
        historyRoot: true
      });
    }
 
    $scope.changeTracking = function (type, enabled) {
      if (type=="boat"){
        if (!$rootScope.isBetween("03/10/2020 00:00:00","04/10/2020 23:59:59"))
{        $scope.noBoatPopup();
        return;}
      }
      //se type e' uguale al tipo attuale non cambiare
      
      if (enabled) {
        //check if carcappoling
        if (trackService.trackingIsGoingOn() && (type == 'car' || $scope.actualTracking('car'))) {
          $scope.openCarPoolingTrackingProblem();
        }
        else if (!$scope.actualTracking(type) && trackService.trackingIsGoingOn()) {
          //show popup if u want change the tracking mean
          $ionicPopup.show({
            title: $filter('translate')("pop_up_change_free_track_title"),
            template: $filter('translate')("pop_up_change_free_track_template"),
            buttons: [
              {
                text: $filter('translate')("btn_close"),
                type: 'button-cancel'
              },
              {
                text: $filter('translate')("pop_up_change_free_track_go_on"),
                type: 'button-custom',
                onTap: function () {
                  //close track and start another one
                  trackService.stopNoSynch().then(function () {
                    trackService.startTransportTrack(type).then(function () {
                    });
                  }, function () {
                    Toast.show($filter('translate')('pop_up_error_server_template'), "short", "bottom");
                  });
                }
              }]
          })

        }
      } else {
        $scope.openTrackingNotEnabled();
      }
    }

    $scope.stopTracking = function () {
      Config.loading();
      $scope.trackingIsOn = false;
      if (!!$scope.trackInfoInterval) $interval.cancel($scope.trackInfoInterval);
      $scope.trackingInfo = {};
      trackService.computeInfo().then(function (data) {
        Config.loaded();
        var travelForDiary = GameSrv.getTravelForDiary()
        trackService.stop();
        if (data.valid && travelForDiary) {
          GameSrv.addTravelDiary(travelForDiary);
          $ionicPopup.confirm({
            title: $filter('translate')("pop_up_points_title"),
            template: $filter('translate')("pop_up_points_template"),
            buttons: [
              {
                text: $filter('translate')("btn_close"),
                type: 'button-cancel',
                onTap: function () {
                  if ($state.current.name == "app.mapTracking") {
                    $state.go('app.home.home');
                  }
                }
              },
              {
                text: $filter('translate')("pop_up_points_btn"),
                type: 'button-custom',
                onTap: function () {
                  $ionicHistory.clearCache().then(function () {
                    $state.go('app.home.diary');
                  });
                }
              }
            ]
          });
        } else {
          $ionicPopup.confirm({
            title: $filter('translate')("pop_up_invalid_tracking_title"),
            template: $filter('translate')("pop_up_invalid_tracking_template"),
            buttons: [
              {
                text: $filter('translate')("btn_close"),
                type: 'button-cancel',
                onTap: function () {
                  $state.go('app.home.home');
                }
              }
            ]
          })
        }
      }, function () {
        Config.loaded();
        $scope.showErrorServer();
        trackService.stop();
      }).finally(Config.loaded);

    }
    $scope.goPath = function (path) {
      $location.path(path);
    };
    $scope.openNews = function () {
      $state.go('app.news');
    }
    $scope.openNotifications = function () {
      $rootScope.countNotification = 0;
      $state.go('app.notifications');
    }
    $scope.openGamificationBoard = function () {
      $ionicHistory.clearCache().then(function () {
        $state.go('app.home.leaderboards');
      });
    };
    $scope.openBadgeBoard = function () {
      $state.go('app.profile.points');
    };
    $scope.planFromPopup = function () {
      $scope.closePopup();
      $scope.newPlan();
    };

    $scope.myTripsFromPopup = function () {
      $scope.closePopup();
      $state.go('app.mytrips');
    };

    $scope.menuDisabled = function () {
      Toast.show($filter('translate')('menu_disabled'), "short", "bottom");
    }
    $scope.openFAQ = function () {
      if (!!$scope.firstOpenPopup) {
        $scope.firstOpenPopup.close();
      }
      var url = Config.getGamificationURL() + "/faq";
      cordova.InAppBrowser.open(url, "_system", "location=yes");
    }
    $scope.openRules = function () {
      if (!!$scope.firstOpenPopup) {
        $scope.firstOpenPopup.close();
      }

      $scope.openRulesModal();

    };

    $scope.openExtLink = function (link) {
      cordova.InAppBrowser.open(link, "_system", "location=yes");
    }

    $scope.openPrizes = function () {
      if (!!$scope.firstOpenPopup) {
        $scope.firstOpenPopup.close();
      }
      var url = Config.getGamificationURL() + "/prizes";
      cordova.InAppBrowser.open(url, "_system", "location=yes");
    };
    $scope.openSchool = function () {
      if (!!$scope.firstOpenPopup) {
        $scope.firstOpenPopup.close();
      }
      var url = "https://www.smartcommunitylab.it/playgo-high-school-challenge/";
      cordova.InAppBrowser.open(url, "_system", "location=yes");
    };
    $scope.forceTutorial = function () {
      if (!!$scope.firstOpenPopup) {
        $scope.firstOpenPopup.close();
      }
      tutorial.showTutorial('main', 'main', 5, $scope, true);
    }

    $scope.closePopup = function () {
      $scope.firstOpenPopup.close();
    };

    $scope.showPlayAndGoPopup = function () {

      $ionicHistory.nextViewOptions({
        disableBack: true
      });
      $state.go('app.gamemenu');
    };


    $scope.openPlan = function () {
      $state.go('app.plan');
    };

    $scope.openTT = function () {
      $state.go('app.ttlist')
    }

    $scope.openDiary = function () {
      $state.go('app.home.diary');
    };

    $scope.openStatistics = function () {
      $state.go('app.profile.statistics')
    };

    $scope.newPlan = function () {
      planService.setPlanConfigure(null);
      $state.go('app.plan');
    };

    $scope.actualTracking = function (type) {
      var tripId = localStorage.getItem(Config.getAppId() + '_tripId');
      if (tripId && tripId.startsWith(type))
        return true;
      return false;
    }
    $scope.getActualTracking = function () {
      var tripId = localStorage.getItem(Config.getAppId() + '_tripId');
      if (tripId)
        return tripId.substring(0, tripId.indexOf('_'));
      else return null;
    }

    $scope.updateBar = function (location) {
      //TODO best calculation over max 
      if (location && location.extras) {
        $scope.progressCounter[location.extras.transportType]++;
        $scope.progressPercent.walk = ($scope.progressCounter.walk * 20 / $scope.maxvalues.maxDailywalk) * 100;
        $scope.progressPercent.bike = ($scope.progressCounter.bike * 20 / $scope.maxvalues.maxDailybike) * 100;
        $scope.progressPercent.bus = ($scope.progressCounter.bus * 20 / $scope.maxvalues.maxDailybus) * 100;
        $scope.progressPercent.train = ($scope.progressCounter.train * 20 / $scope.maxvalues.maxDailytrain) * 100;
      }

    }
    $scope.isBatterySaveMode = function () {
      var deferred = $q.defer();
      BackgroundGeolocation.isPowerSaveMode(function (isPowerSaveMode) {
        if (isPowerSaveMode) {
          deferred.resolve(true);
        }
        else {
          deferred.resolve(false);
        }

      }, function (err) {
        deferred.reject(err);
      });
      return deferred.promise;
    }

    $scope.localizationAlwaysAllowed = function () {
      var deferred = $q.defer();
      cordova.plugins.diagnostic.getLocationAuthorizationStatus(function (status) {
        switch (status) {
          case cordova.plugins.diagnostic.permissionStatus.NOT_REQUESTED:
            console.log("Permission not requested");
            deferred.resolve(true);
            break;
          case cordova.plugins.diagnostic.permissionStatus.DENIED:
            console.log("Permission denied");
            deferred.resolve(false);

            break;
          case cordova.plugins.diagnostic.permissionStatus.GRANTED:
            console.log("Permission granted always");
            deferred.resolve(true);

            break;
          case cordova.plugins.diagnostic.permissionStatus.GRANTED_WHEN_IN_USE:
            console.log("Permission granted only when in use");
            deferred.resolve(false);

            break;
          case cordova.plugins.diagnostic.permissionStatus.DENIED_ALWAYS:
            console.log("Permission permanently denied");
            deferred.resolve(false);

            break;
        }
      }, function (error) {
        console.error("The following error occurred: " + error);
        deferred.reject();

      });
      return deferred.promise;
    }
    $scope.showSaveBatteryPopUp = function (goOn, transportType) {
      $ionicPopup.confirm({
        title: $filter('translate')("pop_up_battery_save"),
        template: $filter('translate')("pop_up_battery_save_template"),
        buttons: [
          {
            text: $filter('translate')("btn_close"),
            type: 'button-cancel'
          },
          {
            text: $filter('translate')("btn_start_tracking"),
            type: 'button-ok',
            onTap: function () {
              goOn(transportType);
            }
          }
        ]
      });

    }
    $scope.showWarningPopUp = function () {
      //show popup and
      $ionicPopup.confirm({
        title: $filter('translate')("pop_up_always_GPS"),
        template: $filter('translate')("pop_up_always_GPS_template"),
        buttons: [
          {
            text: $filter('translate')("btn_close"),
            type: 'button-cancel'
          },
          {
            text: $filter('translate')("pop_up_always_GPS_go_on"),
            type: 'button-custom',
            onTap: function () {
              if (device.platform == "iOS") {
                cordova.plugins.diagnostic.switchToSettings();
              }
              else {
                cordova.plugins.diagnostic.switchToLocationSettings();
              }
            }
          }
        ]
      });

    }

    $scope.popupLoadingShow = function () {
      $ionicLoading.show({
        template: $filter('translate')("pop_up_loading")
      });
    };

    $scope.popupLoadingHide = function () {
      $ionicLoading.hide();
    };

    $scope.showConfirm = function (template, title, functionOnTap) {
      var confirmPopup = $ionicPopup.confirm({
        title: title,
        template: template,
        buttons: [
          {
            text: $filter('translate')("pop_up_cancel"),
            type: 'button-cancel'
          },
          {
            text: $filter('translate')("pop_up_ok"),
            type: 'button-custom',
            onTap: functionOnTap
          }
        ]
      });
    };

    $scope.showNoConnection = function () {
      var alertPopup = $ionicPopup.alert({
        title: $filter('translate')("pop_up_no_connection_title"),
        template: $filter('translate')("pop_up__no_connection_template"),
        buttons: [
          {
            text: $filter('translate')("pop_up_ok"),
            type: 'button-custom'
          }
        ]
      });
    };

    $scope.getRecurrentDays = function (recurrency) {
      var returnDays = [];
      var empty_rec = Config.getDaysRec()
      for (var k = 0; k < empty_rec.length; k++) {
        if (Utils.contains(recurrency.daysOfWeek, k + 1)) {
          returnDays.push(empty_rec[k]);
        }
      }
      return returnDays;
    };

    $scope.getNotificationTypes = function () {
      var returnNotificationsTypes = [];
      var notifTypes = Config.getNotifTypes()
      for (var k = 0; k < notifTypes.length; k++) {
        returnNotificationsTypes.push(notifTypes[k]);
      }
      return returnNotificationsTypes;
    };

    $scope.openModal = function () {
      $scope.modal.show();
    };

    $scope.hideExpandRulesButton = function () {
      if (!$scope.expandedRules) {
        return false;
      }
      return true;
    };

    $scope.hideCloseRulesButton = function () {
      if ($scope.expandedRules) {
        return false;
      }
      return true;
    };

    $scope.closeModal = function () {
      $scope.modal.hide();
    };

    $scope.openRulesModal = function () {
      $scope.lang = Config.getLang();
      $ionicModal.fromTemplateUrl('templates/rulesModal.html', {
        scope: $scope,
        animation: 'slide-in-up'
      }).then(function (modal) {
        $scope.modal = modal;
        $scope.openModal();
      });
    };

    $scope.scrollTo = function (id) {
      $location.hash(id)
      $ionicScrollDelegate.anchorScroll(true);
    };

    $scope.toggleRules = function () {
      if ($scope.isLongRulesShown()) {
        $scope.expandedRules = false;
        //$scope.scrollTo("firstSeparator");
      } else {
        $scope.expandedRules = true;

      }
    };

    $scope.isLongRulesShown = function () {
      return $scope.expandedRules;
    };

    $scope.showErrorServer = function () {
      var alertPopup = $ionicPopup.alert({
        title: $filter('translate')("pop_up_error_server_title"),
        template: $filter('translate')("pop_up_error_server_template"),
        buttons: [
          {
            text: $filter('translate')("pop_up_ok"),
            type: 'button-custom'
          }
        ]
      });
    };

    Config.init().then(function () {
      $scope.infomenu = Config.getInfoMenu();
      $scope.playmenu = Config.getPlayMenu();
      $scope.version = Config.getVersion();
      $scope.shownGroup = JSON.parse(localStorage.getItem(Config.getAppId() + '_shownGroup')) || false;
      $scope.contactLink = Config.getContactLink();
      $scope.taxiEnabled = (Config.getTaxiId() != 'undefined');
    });

    $scope.chooseAndUploadPhoto = function (callback) {
      Config.loading();
      //get the picture from library
      var options = {
        quality: 90,
        destinationType: navigator.camera.DestinationType.FILE_URI,
        sourceType: navigator.camera.PictureSourceType.PHOTOLIBRARY,
        allowEdit: true, // here it allow to edit pic.
        targetWidth: 600, //what widht you want after capaturing
        targetHeight: 600
      };

      $cordovaCamera.getPicture(options).then(function (imageData) {
        $scope.imgURI = imageData;
        window.localStorage.setItem('image', ($scope.imgURI));
        var options = {
          quality: 75,
          widthRatio: 1,
          heightRatio: 1,
          targetWidth: 600,
          targetHeight: 600
        };
        //crop the picture in a square size
        plugins.crop.promise($scope.imgURI, options)
          .then(function success(newPath) {
            Config.loaded();
            var getFileBlob = function (url, cb) {
              var xhr = new XMLHttpRequest();
              xhr.open("GET", url);
              xhr.responseType = "blob";
              xhr.addEventListener('load', function () {
                cb(xhr.response);
              });
              xhr.send();
            };

            var blobToFile = function (blob, name) {
              blob.lastModifiedDate = new Date();
              blob.name = name;
              return blob;
            };

            var getFileObject = function (filePathOrUrl, cb) {
              getFileBlob(filePathOrUrl, function (blob) {
                cb(blobToFile(blob, 'test.jpg'));
              });
            };
            //send the file
            getFileObject(newPath, function (fileObject) {
              callback(fileObject)
            });

          })
          .catch(function fail(err) {
            Config.loaded();
          })
      }, function (err) {
        // An error occured. Show a message to the user
        Config.loaded();
      });
    }
    var changePrifileImage = function () {
      if (profileService.getProfileStatus())
        profileService.getProfileImage(profileService.getProfileStatus().playerData.playerId).then(function (image) {
          $rootScope.profileImg = profileService.getAvatarUrl() + profileService.getProfileStatus().playerData.playerId + '/big?' + (localStorage.getItem(Config.getAppId() + '_timestampImg'));
          // $scope.refreshProfileImage();
        }, function (error) {
          $rootScope.profileImg = 'img/game/generic_user.png' + '/big?' + (localStorage.getItem(Config.getAppId() + '_timestampImg'));
        })
    }
    $scope.getUserImgBig = function (id) {
      return Config.getServerGamificationURL() + '/gamificationweb/player/avatar/' + Config.getAppGameId() + '/' + id + '/big';
    }
    $scope.getUserImg = function (id) {
      return Config.getServerGamificationURL() + '/gamificationweb/player/avatar/' + Config.getAppGameId() + '/' + id
    }
    $scope.uploadFileImage = function (files) {
      Config.loading();
      profileService.setProfileImage(files).then(function () {
        localStorage.setItem(Config.getAppId() + '_timestampImg', new Date().getTime());
        changePrifileImage();
      }, function (error) {
        if (error == 413)
          console.log("Payload too large");
        return;
        if (error == 415)
          console.log("Unsupported media type");
        return;
        console.log("network error");
      }).finally(Config.loaded)
    };
    $scope.noBoatPopup = function () {
      $scope.titleCorona = $filter('translate')('label_title_noBoat');
      // $scope.labelNoBoath=$filter('translate')('label_message_noBoat');
      $scope.alertPopup = $ionicPopup.alert({
        title: $scope.titleCorona,
        templateUrl: 'templates/noBoatPopup.html',
        scope: $scope,
        cssClass: 'notYetGamePopup'
      });

      $scope.alertPopup.then(function (res) {
      });
    }
    $scope.openCarPoolingTrackingProblem = function () {
      $scope.titleCorona = $filter('translate')('label_title_carpooling_tracking_not');
      $scope.alertPopup = $ionicPopup.alert({
        title: $scope.titleCorona,
        templateUrl: 'templates/notCarPoolingPopup.html',
        scope: $scope,
        cssClass: 'notYetGamePopup'
      });

      $scope.alertPopup.then(function (res) {
      });
    }
    $scope.beforeStartPopup = function () {
      $scope.titleCorona = $filter('translate')('label_title_before_start');
      $scope.alertPopup = $ionicPopup.alert({
        title: $scope.titleCorona,
        templateUrl: 'templates/beforeStartPopup.html',
        scope: $scope,
        cssClass: 'notYetGamePopup'
      });

      $scope.alertPopup.then(function (res) {
      });
    }
    $scope.openTrackingCorona = function(){
      $scope.titleCorona = $filter('translate')('label_title_tracking_corona');
            $scope.alertPopup = $ionicPopup.alert({
                title:$scope.titleCorona,
                templateUrl: 'templates/corona/coronaGamePopup.html',
                scope: $scope,
                cssClass: 'coronaGamePopup'
            });
  
            $scope.alertPopup.then(function (res) {
            });
        }
       
  })
 


  .factory('Toast', function ($rootScope, $timeout, $ionicPopup, $cordovaToast) {
    return {
      show: function (message, duration, position) {
        message = message || "There was a problem...";
        duration = duration || 'short';
        position = position || 'top';

        if (!!window.cordova) {
          // Use the Cordova Toast plugin
          $cordovaToast.show(message, duration, position);
        } else {
          if (duration == 'short') {
            duration = 2000;
          } else {
            duration = 5000;
          }

          var myPopup = $ionicPopup.show({
            template: "<div class='toast'>" + message + "</div>",
            scope: $rootScope,
            buttons: []
          });

          $timeout(function () {
            myPopup.close();
          }, duration);
        }
      }
    };
  })

  .factory('Utils', function ($rootScope, $timeout, $ionicPopup, $cordovaToast) {
    return {
      contains: function (a, obj) {
        for (var i = 0; i < a.length; i++) {
          if (a[i] === obj) {
            return true;
          }
        }
        return false;
      }
    };
  })

  .controller('TutorialCtrl', function ($scope, $ionicLoading) { });
