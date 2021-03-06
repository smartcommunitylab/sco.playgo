angular.module('viaggia.controllers.home', [])

  .controller('HomeCtrl', function ($scope, $state, GameSrv, profileService, $rootScope, $ionicPlatform, $timeout, $interval, $filter, $location, $ionicHistory, marketService, notificationService, Config, GeoLocate, mapService, ionicMaterialMotion, ionicMaterialInk, bookmarkService, planService, $ionicLoading, $ionicPopup, trackService, Toast, tutorial, GameSrv, DiaryDbSrv, BT) {

    $scope.challenges = null;
    $scope.buttonProposedEnabled = false;
    $scope.buttonUnlockEnabled = false;
    $scope.expansion = [];
    $scope.buttons = [{
      label: $filter('translate')('menu_news'),
      icon: 'ic_news'
    }, {
      label: $filter('translate')('menu_notifications'),
      icon: 'ic_notification'
    }];
    var typeofChallenges = {};
    updateStatus = function () {
      GameSrv.updateStatus();

    }
    //load from localstorage the id notifications read
    $ionicPlatform.ready(function () {
      document.addEventListener("resume", function () {
        notificationInit();
        updateStatus();
        Config.setWeeklySposnsor();

      }, false);
      Config.setWeeklySposnsor();

    });

    //aggoiorna le notifiche
    var notificationInit = function () {
      //scrico le ultime di una settimana
      if (localStorage.getItem(Config.getAppId() + '_lastUpdateTime') == null) {
        date = new Date();
        //date.setDate(date.getDate() - 7);
        lastUpdateTime = date.getTime();
      } else {
        lastUpdateTime = localStorage.getItem(Config.getAppId() + '_lastUpdateTime');
      }
      notificationService.getNotifications(lastUpdateTime, 0, 10).then(function (items) { //solo le nuove
        if (items) {
          $rootScope.countNotification = items.length;
          //last update time is the last time of notification
          if (items.length > 0) {

            lastUpdateTime = items[0].updateTime + 1;
          }
          localStorage.setItem(Config.getAppId() + '_lastUpdateTime', lastUpdateTime);
        }
      }, function (err) {

        $rootScope.countNotification = 0;

      });
    }
    $scope.openSponsorLink = function (link) {
      window.open(link, '_system', 'location=yes');
      return false;
    }
    var localDataInit = function () {
      typeofChallenges = GameSrv.getTypeChallenge();
      planService.getTrips().then(function () {
        //$ionicLoading.hide();
      }, function () {
        //$ionicLoading.hide();
      });
    }
    var getBlacklistMap = function () {
      GameSrv.getBlacklistMap();
    }
    $scope.programChallenge = function () {
      var date = new Date(new Date().getTime() + (24 * 60 * 60 * 1000));
      $ionicHistory.clearCache().then(function () {
        $state.go("app.home.challenges", {
          challengeEnd: date
        })
      });
    }
    $scope.unlockChallenge = function () {
      $ionicHistory.clearCache().then(function () {
        $state.go("app.home.challenges", {
          unlock: true
        })
      });
    }

    $scope.watch = $rootScope.$watch(function () {
      return profileService.status;
    }, function (newVal, oldVal, scope) {
      $scope.status = profileService.getProfileStatus();
      setUserLevel();
      setUserProgress();
      setChooseButton();
      if ($scope.status && $scope.status.challengeConcept)
        setChallenges();
    });
    $scope.$on("$destroy", function () {
      $scope.watch();
    })
    var initExpansion = function () {
      for (var i = 0; i < $scope.challenges.length; i++) {
        $scope.expansion[i] = false;
      }
    }
    var notGroupProposed = function (challenges) {
      //check if status has in future group challenge
      if ($scope.status && $scope.status.challengeConcept && $scope.status.challengeConcept.challengeData && $scope.status.challengeConcept.challengeData['FUTURE']) {
        for (var i = 0; i < $scope.status.challengeConcept.challengeData.FUTURE.length; i++) {
          if ($scope.status.challengeConcept.challengeData.FUTURE[i].proposerId) {
            return false
          }
        }
      }
      return true;
    }
    var userCanPropose = function () {
      if ($scope.status && $scope.status.inventory && $scope.status.inventory.challengeChoices && $scope.status.inventory.challengeChoices.length > 0)
        return true
      return false
    }

    var setChooseButton = function () {
      //if proposed is not empty enable
      GameSrv.getProposedChallenges(profileService.status).then(function (challenges) {
        if ((challenges && challenges.length) || ($scope.status.canInvite && notGroupProposed() && userCanPropose())) {
          $scope.buttonProposedEnabled = true;

        } else {
          $scope.buttonProposedEnabled = false;
        }
      }, function (err) {
        $scope.buttonProposedEnabled = false;
      });

      //check inventory
      if ($scope.status && $scope.status.inventory && $scope.status.inventory.challengeActivationActions > 0) {
        $scope.buttonUnlockEnabled = true
      } else {
        $scope.buttonUnlockEnabled = false;
      }
    }
    var setChallenges = function () {
      // get the updated active challenges
      GameSrv.getActiveChallenges(profileService.status).then(function (challenges) {
        // if (!!$scope.status && !!$scope.status['challengeConcept']&& !!$scope.status['challengeConcept']['challengeData']) {
        if (challenges) {
          $scope.challenges = challenges;
          if (!$scope.challenges) {
            $scope.challenges = [];
          } else {
            initExpansion();
          }
        } else {
          $scope.challenges = [];
        }
      }, function (error) {
        $scope.challenges = [];
        Toast.show($filter('translate')("pop_up_error_server_template"), "short", "bottom");

      });

    }
    var setUserProgress = function () {
      $scope.userProgress = 0;
      if ($scope.status && $scope.status.levels && $scope.status.levels.length > 0 && $scope.status.levels[0]) {
        var total = $scope.status.levels[0].endLevelScore - $scope.status.levels[0].startLevelScore;
        $scope.toNextLevel = $scope.status.levels[0].toNextLevel;
        var mypos = total - $scope.status.levels[0].toNextLevel;
        $scope.userProgress = (mypos * 100) / total;
      }
    }
    var setUserLevel = function () {
      $scope.level = "";
      if ($scope.status && $scope.status.levels && $scope.status.levels.length > 0 && $scope.status.levels[0].levelValue) {
        $scope.level = $scope.status.levels[0].levelValue;
        $scope.levelNumber = $scope.status.levels[0].levelIndex;
      }
    }
    // var mymap = document.getElementById('map-container');
    $scope.getChallengeTemplate = function (challenge) {
      switch (challenge.type) {
        case typeofChallenges['groupCompetitiveTime'].id: {
          return 'templates/game/challengeTemplates/competitiveTime.html';
          break;
        }
        case typeofChallenges['groupCompetitivePerformance'].id: {
          return 'templates/game/challengeTemplates/competitivePerformance.html';
          break;
        }
        case typeofChallenges['groupCooperative'].id: {
          return 'templates/game/challengeTemplates/cooperative.html';
          break;
        }
        default:
          return 'templates/game/challengeTemplates/default.html';
      }
    }


    $scope.$on("$ionicView.afterEnter", function (scopes, states) {
      $ionicLoading.hide();
      //check timer if passed x time
      var date = new Date();
      if (!localStorage.getItem(Config.getAppId() + "_homeRefresh") || parseInt(localStorage.getItem(Config.getAppId() + "_homeRefresh")) + Config.getCacheRefresh() < new Date().getTime()) {
        Config.init().then(function () {
          $rootScope.title = Config.getAppName();
          bookmarkService.getBookmarksRT().then(function (list) {
            var homeList = [];
            list.forEach(function (e) {
              if (e.home) homeList.push(e);
            });
            $scope.primaryLinks = homeList; //Config.getPrimaryLinks();
          });
          marketService.initMarketFavorites();
          notificationInit();
          initWatch();
          setChallenges();
          setChooseButton();
          updateStatus();
          getBlacklistMap();
          localStorage.setItem(Config.getAppId() + "_homeRefresh", new Date().getTime());
        }, function () {
          //$ionicLoading.hide();
        });
      } else {
        setChooseButton();
      }
    });
    $scope.$on("$ionicView.enter", function (scopes, states) {
      Config.init().then(function () {
        localDataInit();
        if (window.BackgroundGeolocation) {
          $rootScope.syncRunning = true;
          trackService.startup().then(function () {
            $scope.trackingIsOn = trackService.trackingIsGoingOn() && !trackService.trackingIsFinished();
            // $ionicLoading.hide();
            $rootScope.syncRunning = false;
            if ($scope.trackingIsOn) {
              if ($rootScope.GPSAllow === true) {
                updateTrackingInfo();
              }
              var listener = $rootScope.$watch('GPSAllow', function () {
                if ($rootScope.GPSAllow === false) {
                  trackService.cleanTracking();
                  $scope.trackingIsOn = false;
                  //check if it
                  trackService.geolocationDisabledPopup();
                  listener();
                }
              });
            }
          }, function (err) {
            //track service startup not worked.
            // $ionicLoading.hide();
            $rootScope.syncRunning = false;
          });

        };
      });

    }, function (err) {
      $scope.homeCreation = false;
    });

    var translateTransport = function (t) {
      if (t == 'walk') return $filter('translate')('track_walk_action');
      if (t == 'bike') return $filter('translate')('track_bike_action');
      return $filter('translate')('track_other_action');
    }

    function setTrackingInfo() {
      $scope.trackingInfo = {
        transport: translateTransport(trackService.trackedTransport()),
        time: $filter('date')(new Date().getTime() - trackService.trackingTimeStart(), 'HH:mm:ss', '+0000')
      };
    };

    var updateTrackingInfo = function () {
      setTrackingInfo();
      $scope.trackInfoInterval = $interval(function () {
        setTrackingInfo();
      }, 1000);
    }

    var startTransportTrack = function (transportType) {
      $scope.trackingIsOn = true;
      trackService.startTransportTrack(transportType).then(function () {
        updateTrackingInfo();
        if (transportType == 'bus') {
          BT.needBTActivated(function (result) {
            if (result) {
              $ionicPopup.confirm({
                title: $filter('translate')("pop_up_bt_title"),
                template: $filter('translate')("pop_up_bt"),
                buttons: [{
                    text: $filter('translate')("btn_close"),
                    type: 'button-cancel'
                  },
                  {
                    text: $filter('translate')("pop_up_bt_button_enable"),
                    type: 'button-custom',
                    onTap: function () {
                      bluetoothSerial.enable();
                    }
                  }
                ]
              });
            }
          });
        }
      }, function (errorCode) {
        $scope.trackingIsOn = false;
        trackService.geolocationPopup();
        //go back
        // $ionicHistory.goBack();
      }).finally(Config.loaded());
    }



    $scope.track = function (transportType) {
      Config.loading();
      if (!trackService.trackingIsGoingOn() || trackService.trackingIsFinished()) {
        trackService.checkLocalization().then(function () {
          startTransportTrack(transportType);
        }, function (error) {
          Config.loaded();
          if (Config.isErrorLowAccuracy(error)) {
            //popup "do u wanna go on?"
            $ionicPopup.confirm({
              title: $filter('translate')("pop_up_low_accuracy_title"),
              template: $filter('translate')("pop_up_low_accuracy_template"),
              buttons: [{
                  text: $filter('translate')("btn_close"),
                  type: 'button-cancel'
                },
                {
                  text: $filter('translate')("pop_up_low_accuracy_button_go_on"),
                  type: 'button-custom',
                  onTap: function () {
                    startTransportTrack(transportType);
                  }
                }
              ]
            });
          } else if (Config.isErrorGPSNoSignal(error)) {
            //popup "impossible to track" and stop
            var alert = $ionicPopup.alert({
              title: $filter('translate')("pop_up_no_geo_title"),
              template: $filter('translate')("pop_up_no_geo_template"),
              okText: $filter('translate')("btn_close"),
              okType: 'button-cancel'
            });
            alert.then(function (e) {
              trackService.startup();
              if ($state.current.name === 'app.mapTracking') {
                $ionicHistory.nextViewOptions({
                  disableBack: true
                });
                $state.go('app.home.home');
              }
              //$ionicHistory.goBack();
              //$scope.stopTrackingHome();
            });
          }
        });

      } else {
        Config.loaded();
      }
    }
    $scope.trackAndMap = function (transportType) {
      //init multimodal id used for db 
      if ($scope.coronaVirus) {
        $scope.openTrackingCorona();
      } else 
      if (!$rootScope.syncRunning) {
        $scope.startTracking(transportType);
        $state.go('app.mapTracking');
      }

    }
    $scope.stopTrackingHome = function () {
      $scope.trackingIsOn = false;
      $scope.stopTracking();
    }

    $scope.startTracking = function (transportType) {
      if (!$rootScope.syncRunning) {
        $scope.localizationAlwaysAllowed().then(function (loc) {
          if (!loc) {
            $scope.showWarningPopUp();
          } else {
            // else {
            $scope.isBatterySaveMode().then(function (saveMode) {
              if (saveMode) {
                $scope.showSaveBatteryPopUp($scope.track, transportType);
              } else {
                $scope.track(transportType);
              }
            })
          }
        })
      }
    }


    $scope.openSavedTracks = function () {
      planService.getTrips().then(function (trips) {
        if (trips && !angular.equals(trips, {})) {
          $state.go('app.mytrips');
        } else {
          //Toast.show($filter('translate')("no_saved_tracks_to_track"), "short", "bottom");
          var confirmPopup = $ionicPopup.confirm({
            title: $filter('translate')("my_trip_empty_list"),
            template: $filter('translate')("no_saved_tracks_to_track"),
            buttons: [{
                text: $filter('translate')("pop_up_close"),
                type: 'button-cancel'
              },
              {
                text: $filter('translate')("pop_up_plan"),
                type: 'button-custom',
                onTap: function () {
                  confirmPopup.close();
                  planService.setPlanConfigure(null);
                  $state.go('app.plan');
                }
              }
            ]
          });
        }
      });
    }

    $scope.$on('ngLastRepeat.primaryLinks', function (e) {
      $timeout(function () {
        ionicMaterialMotion.ripple();
        ionicMaterialInk.displayEffect()
      }); // No timeout delay necessary.
    });
    var initWatch = function () {
      $scope.$watch('notificationService.notifications', function (newVal, oldVal, scope) {
        notificationInit();
      });
      //     $ionicPlatform.on('resume', function(){
      //        //updatestatus if I come from anotherwebsite
      //   });

    }
    /* DISABLED MAP
        $scope.initMap = function () {
            mapService.initMap('homeMap').then(function (map) {
        
                if (mymap != null) {
                    mapService.resizeElementHeight(mymap, 'homeMap');
                    mapService.refresh('homeMap');
                }
                Config.init().then(function () {
                  mapService.centerOnMe('homeMap', Config.getMapPosition().zoom);
                });
            });
        }
        window.onresize = function () {
            if (mymap != null) {
                mapService.resizeElementHeight(mymap, 'homeMap');
                mapService.refresh('homeMap');
            }
        }
        
        
        $scope.$on('$ionicView.beforeEnter', function(){
          mapService.resizeElementHeight(mymap, 'homeMap');
          mapService.refresh('homeMap');
        });
        
        //just for init
        angular.extend($scope, {
            center: {
                lat: 0,
                lng: 0,
                zoom: 8
            },
            events: {}
        });
    */


    $scope.go = function (state) {
      if (state.indexOf('(') > 0) {
        eval('$scope.' + state);
      } else {
        $location.path(state);
      }

    }
    $scope.goToBookmarks = function () {
      $state.go('app.bookmarks');
      $ionicHistory.nextViewOptions({
        disableBack: true
      });

    }
    $scope.getCountNotification = function (counter) {
      if (counter > 9) {
        return counter + "+";
      }
      return counter;
    }
    $scope.showTutorial = function () {
      tutorial.showTutorial('main', 'main', 5, $scope);
    }
    $scope.expand = function (index) {
      $scope.expansion[index] = !$scope.expansion[index];
    }
    $scope.isExpanded = function (index) {
      return $scope.expansion[index]
    }
    $scope.getWidthUser = function (challenge) {
      return "width:" + ((challenge.status > 100) ? 100 : challenge.status) + "%;"
    }
    $scope.getWidthOther = function (challenge) {
      if (challenge.otherAttendeeData)
        return "width:" + ((challenge.otherAttendeeData.status > 100) ? 100 : challenge.otherAttendeeData.status) + "%;"
      return "width: 1%;"
    }
    $scope.getWidthSeparator = function (challenge) {
      return "width:" + (100 - challenge.otherAttendeeData.status - challenge.status) + "%;background:transparent;"
    }
    var getChallengeByUnit = function (challenge) {
      return GameSrv.getChallengeByUnit(challenge.unit)
    }


    var isKm = function (unit) {
      if (unit != "Walk_Km" && unit != "Bike_Km")
        return false;
      return true
    }
    $scope.getValueUser = function (challenge) {
      var labelChallenge = getChallengeByUnit(challenge);
      return $filter('number')(challenge.row_status, isKm(labelChallenge) ? 2 : 0) + " " + $filter('translate')(labelChallenge);
    }
    $scope.getValueOther = function (challenge) {
      if (challenge.otherAttendeeData) {
        var labelChallenge = getChallengeByUnit(challenge);
        return $filter('number')(challenge.otherAttendeeData.row_status, isKm(labelChallenge) ? 2 : 0) + " " + $filter('translate')(labelChallenge);
      }
      return "";
    }

  })
  .controller('HomeContainerCtrl', function ($scope, $rootScope, profileService, GameSrv, Config, Toast, $filter) {
    $rootScope.currentUser = null;
    $scope.noStatus = false;
    $rootScope.profileImg = null;
    $scope.tmpUrl = 'https://tn.smartcommunitylab.it/playandgo/gamificationweb/player/avatar/' + Config.getAppGameId() + '/'
    $scope.getImage = function () {
      if ($scope.status)
        profileService.getProfileImage($scope.status.playerData.playerId).then(function (image) {
          $rootScope.profileImg = $scope.tmpUrl + $scope.status.playerData.playerId + '?' + (localStorage.getItem(Config.getAppId() + '_timestampImg'));
        }, function (error) {
          $rootScope.profileImg = 'img/game/generic_user.png' + '?' + (localStorage.getItem(Config.getAppId() + '_timestampImg'));
        })
    }
    GameSrv.getLocalStatus().then(
      function (status) {
        $scope.status = status;
        profileService.setProfileStatus(status);
        $rootScope.currentUser = status.playerData;
        if (!localStorage.getItem(Config.getAppId() + '_timestampImg')) {
          localStorage.setItem(Config.getAppId() + '_timestampImg', new Date().getTime());
        }
      },
      function (err) {
        $scope.noStatus = true;
        Toast.show($filter('translate')("pop_up_error_server_template"), "short", "bottom");
      }
    ).finally(function () {
      $scope.getImage();
    });

  })
  .controller('MobilityCtrl', function ($scope, $state, $ionicHistory, $location, bookmarkService) {

    bookmarkService.getBookmarksRT().then(function (list) {
      var homeList = [];
      list.forEach(function (e) {
        if (e.home) homeList.push(e);
      });
      $scope.primaryLinks = homeList; //Config.getPrimaryLinks();
    });
    $scope.go = function (state) {
      if (state.indexOf('(') > 0) {
        eval('$scope.' + state);
      } else {
        $location.path(state);
      }

    }
    $scope.goToBookmarks = function () {
      $state.go('app.bookmarks');
      $ionicHistory.nextViewOptions({
        disableBack: true
      });

    }
  })
