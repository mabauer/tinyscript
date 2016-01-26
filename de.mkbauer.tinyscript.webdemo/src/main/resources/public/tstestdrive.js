

/**
 * The controller for the Testdrive app
 * @param $scope
 * @param $http
 */
function Tinyscript($scope, $http) {
	
	$scope.codeMirror = initCodeMirror();
	
	$scope.loadScript = function(script) {
		$http.get(script).
		    then(function(data) {
		    	$scope.codeMirror.setValue(data.data);
		    	$scope.codeMirror.focus();
		    }, function() {
		    	$scope.codeMirror.setValue("// Example not available. This should not happen.");
		    	$scope.codeMirror.focus();
		    });
	}
	
	$scope.executing = false;
	$scope.statistics = "";
	$scope.executeScript = function() {
		$scope.result = false;
		$scope.executing = true;
	    $http.post('execute', $scope.codeMirror.getValue()).
    	    success(function(data) {
    	    	$scope.executing = false;
				if (data.errorCode > 0) {
					$scope.codeMirror.markText({line: data.errorLine-1, ch: 0}, { line: data.errorLine, ch: 0}, 
							{clearOnEnter: true, css: "background-color: #f2dede"});	
				}
        	    $scope.result = data;
        	    $scope.statistics = "\n(time=" + $scope.result.statistics.executionTime + "ms"
        	    	+ (($scope.result.statistics.mxCpuTime > 0)? (", cpu=" + Math.round($scope.result.statistics.mxCpuTime / 1000000) +"ms") : "") 
        	    	+ ", stmts=" + $scope.result.statistics.statements 
        	    	+ ", calldepth=" + $scope.result.statistics.callDepth 
        	    	+ (($scope.result.statistics.objectsMax > 0)? (", objs=" + $scope.result.statistics.objectsMax) : "") 
        	    	+ (($scope.result.statistics.memoryMax > 0)? (", umem=" + Math.round($scope.result.statistics.memoryMax / 1024) + "K") : "")
        	    	+ ", creates=" + $scope.result.statistics.objectCreations 
        	    	+ (($scope.result.statistics.mxMAlloc > 0)? (", malloc=" + Math.round($scope.result.statistics.mxMAlloc / 1024) + "K") : "") + ")\n";
				
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
    		indentUnit: 4,
    		theme: "eclipse",
   			mode: {name: "javascript", tinyscript: true},
   			extraKeys: {
   				"Ctrl-Enter": function(cm) {
   						var controller = document.getElementById('main');
   						angular.element(controller).scope().executeScript();
   					}
   			},
  		});
}

angular
	.module('TinyscriptApp', ['ui.bootstrap'])
	.controller('Tinyscript', Tinyscript);

