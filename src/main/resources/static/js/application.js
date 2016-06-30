var sessionId;

var app = angular.module('main', ['ngTagsInput']);

var users = {};

app.filter('reverse', function() {
    return function(items) {
        return items.slice().reverse();
    };
});

app.controller("general", function($scope, $http, $sce) {

    $scope.mentions = [];

    var soundNotificationsCookie = Cookies.get("soundNotifications");
    var browserNotificationsCookie = Cookies.get("browserNotifications");
    var chatLocationCookie = Cookies.get("chatLocation");

    $scope.soundNotifications = soundNotificationsCookie == undefined ? true : (soundNotificationsCookie == "true");
    $scope.browserNotifications = browserNotificationsCookie == undefined ? true : (browserNotificationsCookie == "true");
    $scope.chatLocation = chatLocationCookie == undefined ? false : (chatLocationCookie == "true");

    $('#switchSound').attr('checked', $scope.soundNotifications).on('switchChange.bootstrapSwitch', function(event, state) {
        $scope.soundNotifications = state;
        Cookies.set('soundNotifications', state);
    });

    $('#switchBrowserNotification').attr('checked', $scope.browserNotifications).on('switchChange.bootstrapSwitch', function(event, state) {
        $scope.browserNotifications = state;
        Cookies.set('browserNotifications', state);
    });

    $('#switchChatLocation').attr('checked', $scope.chatLocation).on('switchChange.bootstrapSwitch', function(event, state) {
        $scope.chatLocation = state;
        Cookies.set('chatLocation', state);
    });

    var cookieTags = Cookies.getJSON('tags');

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

    function containsInUsers(name, channel) {
        if(users[name] === undefined) {
            console.log("unknown username");
            users[name] = Array.of(channel);
            return false;
        }

        for(user in users) {
            if(name === user) {
                var userChannels = users[user];
                for(c in userChannels) {
                    if(channel === userChannels[c]) {
                        return true;
                    }
                }
                users[name].push(channel);
                return false;
            }
        }
    }

    function getLocation(urlFs, urlSc) {
        var result;
        if($scope.chatLocation) {
            if(urlFs == "chat/main") {
                result = "http://funstream.tv/chat/main";
            } else {
                result = "http://funstream.tv/" + urlFs;
            }
        } else {
            if(urlSc == "chat/main") {
                result = "http://funstream.tv/chat/main";
            } else {
                result = "http://sc2tv.ru/" + urlSc;
            }
        }

        return result;
    }

    var socket = null;

    /*
      type = spy
      id = channelid
      name = nickname
      urlFs
      urlSc

      type = def
      id = messageId
      name = nickname
      message
      urlFs
      urlSc
      date
     */

    var new_conn = function() {
        socket = new SockJS('/listen');

        socket.onmessage = function(e) {
            var jsonMessage = JSON.parse(e.data);

            if (jsonMessage.action == "auth") {
                sessionId = jsonMessage.id;

                $scope.changedTags();

            } else if (jsonMessage.action = "mention") {
                //DEFAULT NOTIFICATION
                if(jsonMessage.data.type === "def") {
                    if (!containsInMentions(jsonMessage.data.id)) {
                        var processedText = processReplaces(jsonMessage.data.message);

                        var parsedHtml = $.parseHTML(processedText);

                        var notificationImage;

                        for (var i = 0; i < parsedHtml.length; i++) {
                            if (parsedHtml[i].className == "chat-smile") {
                                notificationImage = $(parsedHtml[i]).attr("src");
                            }
                        }

                        jsonMessage.data.location = getLocation(jsonMessage.data.urlFs, jsonMessage.data.urlSc);

                        if (Notification.permission == "granted") {

                            if ($scope.browserNotifications) {
                                var notification = new Notification(jsonMessage.data.name, {
                                    icon: notificationImage,
                                    body: processedText.replace(/(<([^>]+)>)/ig, "").trim()
                                });

                                setTimeout(function () {
                                    notification.close();
                                }, 10000);

                                notification.onclick = function () {
                                    window.open(jsonMessage.data.location);
                                }
                            }
                        }

                        jsonMessage.data.message = $sce.trustAsHtml(processedText);

                        $scope.mentions.push(
                            jsonMessage.data
                        );

                        if ($scope.soundNotifications)
                            document.getElementById('soundalert').play();

                        $scope.$apply();
                    }
                }
                //SPY NOTIFICATION
                else if(jsonMessage.data.type === "spy") {

                    if(!containsInUsers(jsonMessage.data.name, jsonMessage.data.urlFs)) {

                        var processedText = "Теперь на канале";

                        jsonMessage.data.location = getLocation(jsonMessage.data.urlFs, jsonMessage.data.urlSc);

                        jsonMessage.data.date = Date.now() / 1000;
                        jsonMessage.data.message = $sce.trustAsHtml(processedText + " <a href='" + jsonMessage.data.location + "'>" + jsonMessage.data.location + "</a>");

                        if (Notification.permission == "granted") {

                            if ($scope.browserNotifications) {
                                var notification = new Notification(jsonMessage.data.name, {
                                    body: processedText.replace(/(<([^>]+)>)/ig, "").trim()
                                });

                                setTimeout(function () {
                                    notification.close();
                                }, 10000);

                                notification.onclick = function () {
                                    window.open(jsonMessage.data.location);
                                }
                            }
                        }

                        $scope.mentions.push(
                            jsonMessage.data
                        );

                        if ($scope.soundNotifications)
                            document.getElementById('soundalert').play();

                        $scope.$apply();
                    }
                }
            }
        };

        socket.onclose = function () {
            setTimeout(new_conn, 4000);
        };
    };

    if (Notification.permission !== "granted")
        Notification.requestPermission();

    new_conn();

    $scope.changedTags = function() {

        var tagArray = [];

        for(key in $scope.tags) {
            var tag = $scope.tags[key].text.toLowerCase();
            ga('send', 'event', 'tags', tag);
            tagArray.push(tag);
        }

        Cookies.set("tags", tagArray);

        $http.post("/set", {
            "tags": tagArray,
            "sessionId":  sessionId
        });
    };

    $scope.openSettings = function() {
        $('#settingsModal').modal('show');
    };

    $scope.openInformation = function() {
        $('#informationModal').modal('show');
    };


    $(".switch").bootstrapSwitch({
        size: 'mini'
    });
});