
var sessionId;

var app = angular.module('main', ['ngTagsInput', 'ngCookies']);

app.filter('reverse', function() {
    return function(items) {
        return items.slice().reverse();
    };
});

app.controller("general", function($scope, $http, $sce, $cookies, $cookieStore) {

    $scope.mentions = [];

    $scope.sound = true;
    $scope.soundImg = "/img/pause.png";

    var cookieTags = $cookieStore.get("tags");

    if(cookieTags == undefined)
        cookieTags = [];


    $scope.tags = cookieTags;

    function containsInMentions(id) {

        for(key in $scope.mentions) {
            var val =  $scope.mentions[key];

            if(val.id == id)
                return true;

        }

        return false;

    }

    var socket = null;
    var recInterval = null;

    var new_conn = function() {
        socket = new SockJS('/listen');

        socket.onopen = function () {

            clearInterval(recInterval);

            socket.send(
                JSON.stringify({
                    "action": "auth"
                })
            );

        };

        socket.onmessage = function (e) {

            var jsonMessage = JSON.parse(e.data);

            if (jsonMessage.action == "auth") {
                sessionId = jsonMessage.id;

                $scope.changedTags();

            } else if (jsonMessage.action = "mention") {

                if (!containsInMentions(jsonMessage.data.id)) {

                    var processedText = ProcessReplaces(jsonMessage.data.message);

                    var parsedHtml = $.parseHTML(processedText);

                    var notificationImage;

                    for(var i =0; i<parsedHtml.length; i++) {
                        if(parsedHtml[i].className == "chat-smile") {
                            notificationImage = $(parsedHtml[i]).attr("src");
                        }
                    }

                    if(Notification.permission == "granted") {

                        var notification = new Notification(jsonMessage.data.name, {
                            icon: notificationImage,
                            body: processedText.replace(/(<([^>]+)>)/ig,"").trim()
                        });

                        setTimeout(function() {
                            notification.close();
                        }, 10000);

                        notification.onclick = function() {
                            window.open(jsonMessage.data.location);
                        }

                    }



                    jsonMessage.data.message = $sce.trustAsHtml(processedText);

                    $scope.mentions.push(
                        jsonMessage.data
                    );

                    if($scope.sound)
                        document.getElementById('soundalert').play();


                    $scope.$apply();
                }

            }

        };

        socket.onclose = function () {
            new_conn();
        };
    };

    if (Notification.permission !== "granted")
        Notification.requestPermission();

    new_conn();

    $scope.changedTags = function() {

        var tagArray = [];

        for(key in $scope.tags) {
            tagArray.push($scope.tags[key].text.toLowerCase());
        }

        $cookieStore.put("tags", tagArray);

        $http.post("/set", {
            "tags": tagArray,
            "sessionId":  sessionId
        });

    };


    $scope.soundSwitcher = function() {
        $scope.sound = !$scope.sound;
        $scope.soundImg = $scope.sound ? "/img/pause.png" : "/img/play.png" ;
    }



});

