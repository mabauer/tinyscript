

/**
 * The controller for the Testdrive app
 * @param $scope
 * @param $http
 */
function Tinyscript($scope, $http) {
	
	$scope.codeMirror = initCodeMirror();
	
	$scope.examples = [ { name: "Hello World", file: "helloworld.ts" }, {name: "Basic Expressions", file: "expressions.ts" } ];
	$scope.loadScript = function(script) {
		$http.get(script).
		    then(function(data) {
		    	$scope.codeMirror.setValue(data.data);
		    }, function() {
		    	$scope.codeMirror.setValue("// Example not available. This should not happen.");
		    });
	}
	
	$scope.statistics = "";
	$scope.executeScript = function() {
		$scope.result = false;
	    $http.post('execute', $scope.codeMirror.getValue()).
    	    success(function(data) {
				if (data.errorCode > 0) {
					$scope.codeMirror.markText({line: data.errorLine-1, ch: 0}, { line: data.errorLine, ch: 0}, 
							{clearOnEnter: true, css: "background-color: #f2dede"});	
				}
        	    $scope.result = data;
        	    $scope.statistics = "\n(time=" + $scope.result.statistics.executionTime + "ms"
        	    	+ ", stmts=" + $scope.result.statistics.statements 
        	    	+ ", calldepth=" + $scope.result.statistics.callDepth 
        	    	+ ", objs=" + $scope.result.statistics.objectCreations + ")\n";
				
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

