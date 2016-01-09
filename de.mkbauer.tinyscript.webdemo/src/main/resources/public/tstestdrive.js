

/**
 * The controller for the Testdrive app
 * @param $scope
 * @param $http
 */
function Tinyscript($scope, $http) {

	$scope.result = {};
	
	$scope.codeMirror = initCodeMirror();

	$scope.executeScript = function() {
	    $http.post('/execute', $scope.codeMirror.getValue()).
    	    success(function(data) {
				if (data.errorCode > 0) {
					$scope.codeMirror.markText({line: data.errorLine-1, ch: 0}, { line: data.errorLine, ch: 0}, 
							{clearOnEnter: true, css: "background-color: red"});	
				}
        	    $scope.result = data;
				
        	});	
	    $scope.codeMirror.focus();
    }
      
}

/**
 * Attach CodeMirror editor to codeEditor div.
 * @returns {CodeMirror}
 */
function initCodeMirror() {
	var elem = document.getElementById("codeEditor");
	return new CodeMirror(elem, 
		{
    		lineNumbers: true,
   			mode: "javascript"
  		});
}

angular
	.module('TinyscriptApp', ['ui.bootstrap'])
	.controller('Tinyscript', Tinyscript);

