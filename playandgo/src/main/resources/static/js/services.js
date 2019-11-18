var services = angular.module('webplanner.services', []);

services.factory('geocoder', ['$http',
    function ($http) {
        var getAddresses = function (userAddress) {
            var url = GEOCODER + 'spring/address?address=';
            return $http.get(url + encodeURI(userAddress));
        };

        var geocode = function(lat,lng) {
            var url = GEOCODER + 'spring/location?latlng=';
            return $http.get(url + encodeURI(lat+','+lng));
        };
        
        return {
            getAddresses: getAddresses,
            geocode: geocode
        };
    }
]);
services.factory('parking', ['$http',
  function ($http) {
      var parkings = function(agency) {
          var url = PARKING + 'getparkingsbyagency/';
          return $http.get(url + agency);
      };
      
      var parkingMap = {};
          
      return {
              init: function(agencies) {
            	  agencies.forEach(function(a) {
            		parkings(a).success(function(data) {
            			parkingMap[a] = {};
            			data.forEach(function(p){
            				parkingMap[a][p.name] = p;
            			});
            		});  
            	  });
              },
              getAll: function() {
              	var res = [];
              	for (var a in parkingMap) {
              		for (var p in parkingMap[a]) {
              			var e = parkingMap[a][p];
              			res.push({title:e.name, description: e.description, position:e.position, type:e.extra && e.extra.parkAndRide ? 'parking_pnr' : 'parking'});
              		}
              	}
              	return res;
              },
              getParking : function(agency, id) {
            	if (parkingMap[agency]) return parkingMap[agency][id];   
              }
     };
  }
]);

services.factory('bikesharing', ['$http',
     function ($http) {
         var parkings = function(agency) {
             var url = BIKESHARING + 'bikesharing/';
             return $http.get(url + agency);
         };
         
         var parkingMap = {};
             
         return {
                 init: function(agencies) {
               	  agencies.forEach(function(a) {
               		parkings(a).success(function(data) {
               			parkingMap[a] = {};
               			data.forEach(function(p){
               				parkingMap[a][p.name] = p;
               			});
               		});  
               	  });
                 },
                 getAll: function() {
                	var res = [];
                	for (var a in parkingMap) {
                		for (var p in parkingMap[a]) {
                			var e = parkingMap[a][p];
                  			res.push({title:e.name, description: e.address, position:e.position, type:'bikesharing'});
                		}
                	}
                	return res;
                 },
                 getParking : function(agency, id) {
                	 if (parkingMap[agency]) return parkingMap[agency][id];   
                 }
        };
     }
   ]);


services.factory('taxi', ['$http',
     function ($http) {
         var taxi = function() {
             var url = TAXI + 'getTaxiStation/';
             return $http.get(url);
         };
         
         var taxiMap = {};
             
         return {
                 init: function() {
               		taxi().success(function(data) {
               			taxiMap = {};
               			data.forEach(function(p){
               				taxiMap[p.stationId.id] = p;
               			});
               		});  
                 },
                 getAll: function() {
                	var res = [];
                		for (var p in taxiMap) {
                			var e = taxiMap[p];
                  			res.push({title:e.stationId.id, description: '', position:e.location, type:'taxi'});
                		}
                	return res;
                 },
                 getTaxi : function(id) {
                	 return taxiMap[id];   
                 }
        };
     }
   ]);



services.factory('formatter', ['parking', '$rootScope',
  function (parking, $rootScope) {
    var getDateStr = function(date) {
  	  return (date.getMonth() < 9 ? '0':'')+(date.getMonth()+1) +'/'+
  	  		 (date.getDate() < 10 ? '0':'')+date.getDate() +'/' +
  	  		 date.getFullYear();
    };
    var getTimeStr = function(time) {
  	  var am = time.getHours() < 12;
  	  var hour = am ? time.getHours() : time.getHours() - 12;
  	  if (am && hour == 0) hour = 12;
  	  
  	  return (hour < 10 ? '0':'') + hour +':' +
  			 (time.getMinutes() < 10 ? '0' : '') + time.getMinutes() + (am ? 'AM' : 'PM'); 
    };
    
    var getTimeStrSimple = function(time) {
    	  var hour = time.getHours();
    	  return (hour < 10 ? '0':'') + hour +':' +
    			 (time.getMinutes() < 10 ? '0' : '') + time.getMinutes(); 
    };

    var ttMap = {
    		'WALK'		: 'ic_mt_foot',
    		'BICYCLE'	: 'ic_mt_bicycle',
    		'CAR'		: 'ic_mt_car',
    		'BUS'		: 'ic_mt_bus',
    		'EXTRA'		: 'ic_mt_extraurbano',
    		'TRAIN'		: 'ic_mt_train',
    		'PARK'		: 'ic_mt_parking',
    		'TRANSIT'	: 'ic_mt_funivia',
    		'STREET'	: 'ic_price_parking'
    };
    var actionMap = {
    		'WALK'		: 'Cammina',
    		'BICYCLE'	: 'Pedala',
    		'CAR'		: 'Guida',
    		'BUS'		: 'Prendi l\'autobus ',
    		'TRAIN'		: 'Prendi il treno '
    };

    var getImageName = function(tt, agency) {
    	if (tt == 'BUS' && $rootScope.EXTRAURBAN_AGENCIES.indexOf(agency)>=0) {
    		return ttMap['EXTRA'];
    	}
    	return ttMap[tt];
    }
    
    var extractParking = function(leg, extended) {
    	var res = {type: null, cost:null, time: null, note: [], img: null};
    	if (leg.extra != null) {
    		if (leg.extra.costData && leg.extra.costData.fixedCost) {
    			var cost = (leg.extra.costData.fixedCost).replace(',','.').replace(' ','');
    			if (extended == true) {
    				cost = parseFloat(cost) > 0 ? leg.extra.costData.costDefinition : 'gratis';
    			} else {
    				cost = parseFloat(cost) > 0 ? (cost+'\u20AC') : 'gratis';
    			}
    			res.cost = cost;
    			res.note.push(cost);
    		}
    		if (leg.extra.searchTime && leg.extra.searchTime.max > 0) {
    			res.time = leg.extra.searchTime.min+'-'+leg.extra.searchTime.max+'min';
    			res.note.push(res.time);
    		}
    		res.type = 'STREET';
    	}
    	if (leg.to.stopId) {
    		var cost = 'gratis';
    		if (leg.to.stopId.extra && leg.to.stopId.extra.costData && leg.to.stopId.extra.costData.fixedCost) {
    			cost = (leg.to.stopId.extra.costData.fixedCost).replace(',','.').replace(' ','');
    			if (extended == true) {
    				cost = parseFloat(cost) > 0 ? leg.to.stopId.extra.costData.costDefinition : 'gratis';    				
    			} else {
    				cost = parseFloat(cost) > 0 ? (cost+'\u20AC') : 'gratis';
    			}
    		}
			res.cost = cost;
			res.note.push(cost);
    		res.type = 'PARK';
    	}
    	if (leg.to.name) {
    		res.place = leg.to.name;
    	} else if (leg.to.stopId && leg.to.stopId.id) {
    		var parkingPlace = parking.getParking(leg.to.stopId.agencyId, leg.to.stopId.id);
    		res.place = parkingPlace != null ? parkingPlace.description : leg.to.stopId.id;
    	}
    	if (res.type) {
    		res.img = $rootScope.imgBase + 'img/'+getImageName(res.type)+'.png';
    		return res;
    	}
    };
    
    var extractItineraryMeans = function(it) {
    	var means = [];
    	var meanTypes = [];
    	for (var i = 0; i < it.leg.length; i++) {
    		var t = it.leg[i].transport.type;
    		var elem = {note:[],img:null};
    		elem.img = getImageName(t,it.leg[i].transport.agencyId);
    		if (!elem.img) {
    			console.log('UNDEFINED: '+it.leg[i].transport.type);
    			elem.img  = getImageName('BUS');
    		}
    		elem.img = $rootScope.imgBase + 'img/'+elem.img+'.png';
    		
    		if (t == 'BUS' || t == 'TRAIN') {
    			elem.note = [it.leg[i].transport.routeShortName];
    		} else if (t == 'CAR') {
        		if (meanTypes.indexOf('CAR') < 0) {
        			var parking = extractParking(it.leg[i], false);
        			if (parking) {
            			if (parking.type == 'STREET') {
            				elem.note = parking.note;
            			} else {
                			means.push(elem);
            				elem = {img:parking.img, note: parking.note};
            			}
        			}
        		}
    		}
    		
    		var newMt = t + (elem.note.length > 0 ? elem.note.join(','): '');
    		if (meanTypes.indexOf(newMt) >= 0) continue;
    		meanTypes.push(newMt);
        	means.push(elem);
    	}
    	return means;
    };
    
//  var elemColors = ['#FF0000','#FF6600', '#6633FF', '#99FF00', '#339900'];
    var elemColors = {
    		'WALK'		: '#8cc04c',
    		'BICYCLE'	: '#922d66',
    		'CAR'		: '#757575',
    		'BUS'		: '#eb8919',
    		'TRAIN'		: '#cd251c',
    		'TRANSIT'	: '#016a6a'
    };        
    
    var extractMapElements = function(leg, idx, map) {
		var res = [];
    	var path = google.maps.geometry.encoding.decodePath(leg.legGeometery.points);
    	var line = new google.maps.Polyline({
		    path: path,
		    strokeColor: leg.transport.agencyId == '17' ? "#00588e" : elemColors[leg.transport.type],
		    strokeOpacity: 0.8,
		    strokeWeight: 2,
		    map: map
		  });
    	res.push(line);
		return res;
    };
    
    var extractDetails = function(step, leg, idx, from) {
    	step.action = actionMap[leg.transport.type];
    	if (leg.transport.type == 'BICYCLE' && leg.transport.agencyId && leg.transport.agencyId != 'null') {
    		step.fromLabel = "Prendi una bicicletta alla stazione di bike sharing ";
    		if (leg.to.stopId && leg.to.stopId.agencyId && leg.to.stopId.agencyId != 'null') {
        		step.toLabel = "Lascia la bicicletta alla stazione di bike sharing ";
    		} else {
        		step.toLabel = "A ";
    		}
//    	} else if (leg.transport.type == 'CAR' && leg.transport.agencyId && leg.transport.agencyId != 'null') {
    	} else {
    		step.fromLabel = "Da ";
    		step.toLabel = "A ";
    	}
    	if (leg.transport.type == 'BUS' || leg.transport.type == 'TRAIN' || leg.transport.type == 'TRANSIT') {
    		step.actionDetails = leg.transport.routeShortName;
    	}
    	
		if (from != null) step.from = from;
		else {
			step.from = leg.from.name;
		}
		step.to = leg.to.name;
    };
    
    var process = function(plan, from, to, useCoordinates) {
    	plan.steps = [];
    	var nextFrom = !useCoordinates ? from : nextFrom = plan.leg[0].from.name+' ('+plan.leg[0].from.lat+','+plan.leg[0].from.lon+')';
    	
    	for (var i = 0; i < plan.leg.length; i++) {
    		var step = {};
    		step.startime = i == 0 ? plan.startime: plan.leg[i].startime;
    		step.endtime = plan.leg[i].endtime;
    		step.mean = {};

    		extractDetails(step, plan.leg[i], i, nextFrom);
    		nextFrom = null;
    		step.length = getLength(plan.leg[i]);
    		step.cost = getLegCost(plan, i);
    		
    		var t = plan.leg[i].transport.type;
    		step.mean.img = getImageName(t,plan.leg[i].transport.agencyId);
    		if (!step.mean.img) {
    			console.log('UNDEFINED: '+plan.leg[i].transport.type);
    			step.mean.img  = getImageName('BUS');
    		}
    		step.mean.img = $rootScope.imgBase + 'img/'+step.mean.img +'.png';

    		var parkingStep = null;
    		if (t == 'CAR') {
    			var parking = extractParking(plan.leg[i], true);
    			if (parking) {
    				if (parking.type == 'PARK') {
    					step.to = 'parcheggio '+ parking.place;
    					nextFrom = step.to;
        				parkingStep = {
        						startime: plan.leg[i].endtime, 
        						endtime: plan.leg[i].endtime,
        						action: 'Lascia la macchina a ',
        						actionDetails: step.to,
        						parking: parking,
        						mean: {img:parking.img}};
    				} else {
    					step.parking = parking;
    				}
    			}
    		}
    		if (useCoordinates && i == plan.leg.length-1) step.to += ' ('+plan.leg[i].to.lat+','+plan.leg[i].to.lon+')';
    		plan.steps.push(step);
    		if (parkingStep != null) {
        		plan.steps.push(parkingStep);
    		}
    	}
    };
    
    var getLength = function(it) {
    	if (!it.leg && it.length != null) {
    		return (it.length / 1000).toFixed(2);
    	}
    	var l = 0;
    	for (var i = 0; i < it.leg.length; i++) {
    		l += it.leg[i].length;
    	}
    	return (l / 1000).toFixed(2);
    };
    
    var getItineraryCost = function(plan) {
    	var fareMap = {};
    	var total = 0;
    	for (var i = 0; i < plan.leg.length; i++) {
    		if (plan.leg[i].extra) {
        		var fare = plan.leg[i].extra.fare;
        		var fareIdx = plan.leg[i].extra.fareIndex;
        		if (fare && fareMap[fareIdx] == null) {
        			fareMap[fareIdx] = fare;
        			total += fare.cents / 100;
        		}
    		}
    	}
    	return total;
    };
    var getLegCost = function(plan, i) {
    	var fareMap = {};
    	var total = 0;
		if (plan.leg[i].extra) {
    		var fare = plan.leg[i].extra.fare;
    		var fareIdx = plan.leg[i].extra.fareIndex;
    		if (fare && fareMap[fareIdx] == null) {
    			fareMap[fareIdx] = fare;
    			total += fare.cents / 100;
    		}
		}
    	return total;
    };
    
    return {
    	getTimeStrMeridian: getTimeStr,
    	getTimeStr: getTimeStrSimple,
    	getDateStr: getDateStr,
    	extractItineraryMeans: extractItineraryMeans,
    	extractMapElements: extractMapElements,
    	getLength: getLength, 
    	getItineraryCost: getItineraryCost,
    	getLegCost: getLegCost,
    	process: process
    }
}]);

services.factory('planner', ['$http', 'formatter',
	  function ($http, formatter) {
	      
	      var getRequest = function(from, to, means, mode, date, time, policy, wheelchair) {
	          var data = {
	  	        	from: {lat:""+from.lat(),lon: ""+from.lng()},
	  	        	to: {lat: ""+to.lat(),lon: ""+to.lng()},
	  	        	routeType: mode,
	  	        	resultsNumber: 3,
	  	        	date: formatter.getDateStr(date),
	  	        	departureTime: formatter.getTimeStrMeridian(time),
	  	        	transportTypes: means.split(','),
	  	        	wheelchair : wheelchair
	  	          };
	          return data;
	      }; 
	      
	      var plan = function(from, to, means, mode, date, time, policy, wheelchair) {
	          var url = PLANNER + '/plansinglejourney?policyId=' + policy;
	          var data = getRequest(from, to, means, mode, date, time, policy, wheelchair);
	          return $http.post(url,data);
          };
          
          return {
              plan: plan,
              getRequest: getRequest
          };
      }
  ]);
