<!DOCTYPE html>
<html lang="en" ng-app="gameconsole">
<head>
<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="description" content="">
<meta name="author" content="">
<title>Gamification Console</title>
<!-- Custom styles for this template -->
<link href="../css/style.css" rel="stylesheet">
<link href="../css/ng-scrollable.min.css" rel="stylesheet">
<!-- Bootstrap core CSS -->
<link href="../css/bootstrap.min.css" rel="stylesheet">
<script src="../lib/angular/angular.min.js"></script>
<script src="../lib/angular/angular-route.min.js"></script>
<script src="../lib/ui-bootstrap-tpls-0.12.1.min.js"></script>
<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js"></script>
<script src="../lib/bootstrap.min.js"></script>
<script src="../lib/ng-scrollable.min.js"></script>
<script src="https://maps.googleapis.com/maps/api/js?libraries=geometry&v=3.exp"></script>
<script src="../lib/sprintf.min.js"></script>
<script src="../lib/date.js"></script>
<script src="../lib/spin.min.js"></script>
<script src="../js/gamification/gamification.js"></script>
<style type="text/css">
.console {
	padding: 20px;
}

.user-row {
	border: 1px solid #bbb;
	padding: 5px;
	margin-bottom: 10px !important;
}

.user-row a {
	font-size: 24px;
}

.instance-row {
	border: 1px solid #ddd;
	padding: 0 5px;
	position: relative;
}

.instance-row.valid {
	background-color: lightgreen;
}

.instance-row.invalid {
	background-color: lightpink;
}

.instance-row.untracked-valid {
	background-color: lightblue;
}

.instance-row.untracked-invalid {
	background-color: #DB7093;
}

.selected {
	border: 2px solid blue;
}

.itinerary-row {
	padding-top: 5px;
}

table {
	width: 100%;
}

.padding {
	padding: 8px;
}

thead td {
	font-weight: bold;
}

.form-check {
	padding-left: 10px;
}

.form-check input {
	margin-right: 5px;
}

.group-indicator {
	position: absolute;
	bottom: 10px;
	right: 10px;
}

.itinerary-switch {
	margin: 0;
}

.itinerary-def {
	white-space: nowrap;
}

#left-scrollable {
	padding-right: 2px;
}

.right {
	float: right;
}

body {
	padding-bottom: 0 !important;
}
</style>
</head>
<body>
	<div id="console" class="console" ng-controller="GameCtrl">
		<div class="row">
			<div class="col-md-4">
				<form>
					<div class="panel panel-default padding">
						<div class="row">
							<div class="col-md-3">
								<a class="btn btn-primary btn-sm" href="{{'console/report?fromDate=' + fromDate.getTime() + '&toDate=' + toDate.getTime()}}" role="button">Report</a>
							</div>
							<div class="col-md-9 text-right">
								<button class="btn btn-danger btn-sm" data-toggle="modal" data-target="#confirmModal2">Revalidate</button>
								<button class="btn btn-danger btn-sm" data-toggle="modal" data-target="#confirmModal3">Approve all</button>
							</div>
						</div>
					</div>
					<div class="panel panel-default padding">
						<div class="row">
							<div class="col-md-3">
								<button class="btn btn-primary btn-sm" ng-click="reload()">Filter</button>
							</div>
							<div class="col-md-9 text-right">
								<label class="form-check"> <input type="checkbox" ng-click="excludeZeroPoints=!excludeZeroPoints" class="navbar-btn btn-sm">No 0 pts
								</label> <label class="form-check"> <input type="checkbox" ng-click="unapprovedOnly=!unapprovedOnly" class="navbar-btn btn-sm">To approve only
								</label>
								<label class="form-check"> <input type="checkbox" ng-click="toCheck=!toCheck" class="navbar-btn btn-sm">To check 
							</div>
						</div>
						<div class="row padding">
							<div class="col-md-3 text-right">
								<label>From:</label>
							</div>
							<div class="col-md-9">
								<div class="input-group">
									<input id="fromDate" type="text" class="form-control" datepicker-popup="{{format}}" datepicker-options="dateOptions" ng-model="fromDate"
										is-open="openedFrom" close-text="Close"
									/> <span class="input-group-btn">
										<button type="button" class="btn btn-default" ng-click="toggleOpen($event, true)">
											<i class="glyphicon glyphicon-calendar"></i>
										</button>
									</span>
								</div>
							</div>
						</div>
						<div class="row padding">
							<div class="col-md-3 text-right">
								<label>To:</label>
							</div>
							<div class="col-md-9">
								<div class="input-group">
									<input id="toDate" type="text" class="form-control" datepicker-popup="{{format}}" datepicker-options="dateOptions" ng-model="toDate" is-open="openedTo"
										close-text="Close"
									/> <span class="input-group-btn">
										<button type="button" class="btn btn-default" ng-click="toggleOpen($event, false)">
											<i class="glyphicon glyphicon-calendar"></i>
										</button>
									</span>
								</div>
							</div>
						</div>
					</div>
				</form>
				<div id="left-scrollable" ng-scrollable="{scrollX:'none',scrollY:'right'}" style="width: 100%; height: 100%;">
					<div class="">
						<div ng-repeat="user in users" class="panel panel-default user-row">
							<div class="row">
								<div class="col-md-6">
									<a ng-click="selectUser(user)">{{user}} </a>
								</div>
								<div class="col-md-6 pull-right">
								    <strong>({{userTotals[user].total}} tracked, <span style="{{validColor(userTotals[user])}}">{{userTotals[user].failed}} invalid</span>)
									</strong>
								</div>
							</div>
							<div ng-if="selectedUser == user">
								<div ng-repeat="itinerary in userMap[user]" class="itinerary-row">
									<div>
										<div ng-click="selectInstance(itinerary.instance)" class="instance-row"
											ng-class="{'selected':selectedInstance == itinerary.instance, 'valid': getValidityStyle(itinerary.instance), 'invalid': !getValidityStyle(itinerary.instance)}"
										>
											<div class="row">
												<div class="col-md-6 itinerary-def">
													{{itinerary.tripName}} <span ng-show="!itinerary.instance.validationResult.matchedLocations && itinerary.instance.itinerary"
														class="glyphicon glyphicon-move" title="Mismatched locations" data-toggle="tooltip"
													></span> <span ng-show="!itinerary.instance.validationResult.matchedActivities && itinerary.instance.itinerary" class="glyphicon glyphicon-plane"
														title="Mismatched activities" data-toggle="tooltip"
													></span> <span ng-show="itinerary.instance.validationResult.tooFast" class="glyphicon glyphicon-road" title="Too fast" data-toggle="tooltip"></span> <br />
													<i>Type: </i><b>{{itinerary.instance.itinerary == null ? ('Free tracking - ' + itinerary.instance.freeTrackingTransport) : 'Planned'}}</b>
												</div>
												<div class="col-md-6 text-right">
													<b>{{(itinerary.instance.day ? itinerary.instance.day : '--') + " " + (itinerary.startTime|date:'HH:mm')}}</b> <span
														ng-show="itinerary.instance.validationResult.tooFewPoints && itinerary.instance.itinerary" class="glyphicon glyphicon-exclamation-sign"
														title="Too few points" data-toggle="tooltip"
													></span><br /> <b>{{itinerary.instance.itinerary ? itinerary.instance.itinerary.data.customData.estimatedScore :
														itinerary.instance.estimatedScore}} Points</b>
												</div>
											</div>
											<div class="group-indicator">
												<span ng-if="itinerary.instance.groupId != 0" class="label label-primary">group {{itinerary.instance.groupId}}</span>
											</div>
											<label class="itinerary-switch"><b>{{'Switch validity ' + (itinerary.instance.approved ? '(Approved) ' : '')}}</b> <input
												ng-disabled="itinerary.instance.approved" type="checkbox" ng-model="itinerary.instance.switchValidity" 
												class="navbar-btn btn-sm" data-toggle="modal" data-target="#confirmModal1"
											> </label> <label class="itinerary-switch"><b>To check</b> <input ng-disabled="itinerary.instance.approved" type="checkbox"
												ng-checked="itinerary.instance.toCheck" ng-click="toggleToCheck(itinerary.instance)" class="navbar-btn btn-sm"
											> </label>										
											
										</div>
									</div>
								</div>
							</div>
						</div>
						<div></div>
					</div>
				</div>
			</div>
			<div class="col-md-8">
				<div id="map"></div>
				<span ng-hide="!selectedInstance"><label class="navbar-btn"><input type="checkbox" ng-click="fixpaths=!fixpaths; reselectInstance();"
						name="fix-paths" class="navbar-btn btn-sm"
					>&nbsp;Fix paths {{scores}}</label></span>					
				<span ng-hide="!selectedInstance"><label class="navbar-btn">
				<button class="btn btn-danger btn-sm" ng-click="showAllPoints()">Show all points</button>
				</span>					
					<div id="right-scrollable" style="width: 100%; height: 100%;">
						<div class="row" ng-if="selectedInstance != null">
							<div class="col-md-4">
								<h3 style="color: green;">Planned</h3>
								<p>{{selectedInstance.itinerary.data.startime|date:'HH:mm'}} - {{selectedInstance.itinerary.data.endtime|date:'HH:mm'}}</p>
								<p ng-show="selectedInstance.freeTrackingTransport">
									<b>Free tracking tranport:</b> {{selectedInstance.freeTrackingTransport}}
								</p>
								<hr />
								<p ng-repeat="leg in selectedInstance.itinerary.data.leg">{{leg.transport.type}}</p>
							</div>
							<div class="col-md-4">
								<h3>
									<span style="color: blue;">Tracked</span> (valid: <span style="">{{selectedInstance.valid}}</span>)
								</h3>
								<p>{{selectedInstance.geolocationEvents[0].recorded_at|date:'HH:mm'}} -
									{{selectedInstance.geolocationEvents[selectedInstance.geolocationEvents.length-1].recorded_at|date:'HH:mm'}}</p>
								<hr />
								<p ng-repeat="evt in selectedInstance.legs">
									<b>{{evt.activity_type ? evt.activity_type : '??'}}</b> ({{evt.count}} events, {{evt.recorded_at|date:'HH:mm:ss'}}<span
										ng-if="evt.recorded_till != null"
									> -- {{evt.recorded_till|date:'HH:mm:ss'}}</span>)
								</p>
								<!--               <p ng-repeat="evt in selectedInstance.geolocationEvents">{{evt.activity_type ? evt.activity_type : '--'}} ({{evt.recorded_at|date:'HH:mm:ss'}})</p> -->
							</div>
							<div class="col-md-4">
								<h3 style="">Validation</h3>
								<pre>{{selectedInstance.validationResult | json}}</pre>
							</div>
						</div>
						<div class="row" ng-if="selectedInstance != null">
							<div class="col-md-12">
								<h3>Tracked events:</h3>
								<table>
									<thead>
										<td></td>
										<td>i</td>
										<td>When</td>
										<td>Accuracy</td>
										<td>Activity</td>
										<td>Activity Confidence</td>
										<td>Coordinates</td>
										<td>Is moving</td>
										<td>Speed</td>
									</thead>
									<tbody>
										<!-- <tr ng-repeat="evt in selectedInstance.geolocationEvents" ng-click="newMarker(evt.geocoding[1] + ',' + evt.geocoding[0])"> -->
										<tr ng-repeat="evt in selectedInstance.geolocationEvents track by $index" ng-click="lineclick()" class="instance-row">
											<div>
												<!-- <td><span class="glyphicon glyphicon-map-marker" ng-click="newMarker(evt.geocoding[1] + ',' + evt.geocoding[0])"></span></td> -->
												<td><label class="btn" ng-click="newEventMarker(evt.geocoding[1],evt.geocoding[0],$index + 1)"> <span class="glyphicon glyphicon-map-marker"
														aria-hidden="true"
													></span>
												</label></td>
												<td>{{$index + 1}}</td>
												<td>{{evt.recorded_at|date:'HH:mm:ss'}}</td>
												<td>{{evt.accuracy}}</td>
												<td>{{evt.activity_type}}</td>
												<td>{{evt.activity_confidence}}</td>
												<td>{{evt.geocoding[1] + " ," + evt.geocoding[0]}}</td>
												<td>{{evt.is_moving}}</td>
												<td>{{evt.speed}}</td>
											</div>
										</tr>
									</tbody>
								</table>
							</div>
						</div>
					</div>
			</div>
		</div>
		<div id="confirmModal1" class="modal fade" role="dialog">
			<div class="modal-dialog modal-sm">
				<div class="modal-content">
					<div class="modal-header">
						<h4 class="modal-title">Are you sure?</h3>
					</div>
					<div class="modal-body">
						<form id="notification-form">
							<div class="form-group">
								<button class="btn btn-primary" type="button" ng-click="switchCurrentValidity(true)" data-dismiss="modal">Yes</button>
								<button class="btn btn-primary right" type="button" ng-click="switchCurrentValidity(false)" data-dismiss="modal">No</button>
							</div>
						</form>
					</div>
				</div>
			</div>
		</div>
		<div id="confirmModal2" class="modal fade" role="dialog">
			<div class="modal-dialog modal-sm">
				<div class="modal-content">
					<div class="modal-header">
						<h4 class="modal-title">Are you sure?</h3>
					</div>
					<div class="modal-body">
						<form id="notification-form">
							<div class="form-group">
								<button class="btn btn-primary" type="button" ng-click="revalidate()" data-dismiss="modal">Yes</button>
								<button class="btn btn-primary right" type="button" data-dismiss="modal">No</button>
							</div>
						</form>
					</div>
				</div>
			</div>
		</div>
		<div id="confirmModal3" class="modal fade" role="dialog">
			<div class="modal-dialog modal-sm">
				<div class="modal-content">
					<div class="modal-header">
						<h4 class="modal-title">Are you sure?</h3>
					</div>
					<div class="modal-body">
						<form id="notification-form">
							<div class="form-group">
								<button class="btn btn-primary" type="button" ng-click="approveAll()" data-dismiss="modal">Yes</button>
								<button class="btn btn-primary right" type="button" data-dismiss="modal">No</button>
							</div>
						</form>
					</div>
				</div>
			</div>
		</div>				
	</div>
</body>
</html>

