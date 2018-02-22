module.controller('ApprovalsListCtrl', function ($scope, realm, Approvals, Notifications, Dialog, $translate) {
    $scope.init = function () {
        $scope.realm = realm;
        $scope.reload();
    };

    $scope.details = function (approval) {
        Dialog.message($translate.instant("detailed-description"), "<pre>" + approval.description + "</pre>", function(){}, function(){})
    };

    $scope.reload = function () {
        $scope.approvals = Approvals.query({realm: realm.realm});
    };

    $scope.approve = function (approval) {
        Dialog.confirm(
            $translate.instant("approve"),
            $translate.instant("approve-confirm") + "<br/>" + "<i>" + approval.action.description + "</i><pre>" + approval.description + "</pre>",
            function(){
                Approvals.approve({realm: realm.realm, approvalId: approval.id}, function () {
                    $scope.reload();
                    Notifications.success($translate.instant("approve-success"));
                })
            },
            function(){})
    };

    $scope.reject = function (approval) {
        Dialog.confirm(
            $translate.instant("reject"),
            $translate.instant("reject-confirm") + "<br/>" + "<i>" + approval.action.description + "</i><pre>" + approval.description + "</pre>",
            function(){
                Approvals.reject({realm: realm.realm, approvalId: approval.id}, function () {
                    $scope.reload();
                    Notifications.success($translate.instant("reject-success"));
                })
            },
            function(){})
    };
});

module.controller('ApprovalListenersCtrl', function ($scope, $route, realm, ApprovalListeners, Notifications, $translate) {
    var oldCopy;
    const secretString = '***************';

    $scope.init = function () {
        $scope.realm = realm;
        $scope.reload();
    };

    $scope.$watch('bpms', function() {
        $scope.changed = !angular.equals($scope.bpms, oldCopy);
    }, true);

    $scope.reload = function () {
        ApprovalListeners.get({realm: realm.realm, listenerId: 'bpms'}, function (bpms) {
            $scope.bpms = bpms;
            $scope.bpms.configs.password = secretString;
            oldCopy = angular.copy($scope.bpms);
            $scope.changed = false;
        });
    };

    $scope.save = function () {
        if ($scope.bpms.configs.password === secretString) {
            delete $scope.bpms.configs.password;
        }
        ApprovalListeners.save({realm: realm.realm, listenerId: 'bpms'}, $scope.bpms, function () {
            $scope.reload();
            Notifications.success($translate.instant('listener-config-saved'));
        });
    };

    $scope.reset = function() {
        $route.reload();
    };
});