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