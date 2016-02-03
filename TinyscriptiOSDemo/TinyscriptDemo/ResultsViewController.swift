//
//  ResultsViewController.swift
//  TinyscriptDemo
//
//  Created by Markus Bauer on 02.02.16.
//  Copyright © 2016 Markus Bauer. All rights reserved.
//

import UIKit
import JavaScriptCore


class ResultsViewController: UIViewController, UIWebViewDelegate {
    
    // MARK: Properties
    
    @IBOutlet weak var outputView: UITextView!
    var webView : UIWebView = UIWebView()
    
    var script : String = ""
    var output : String = ""
    var tinyscriptURL : String = ""
    
 
    //MARK: UIViewController
    override func viewDidLoad() {
        super.viewDidLoad()
        self.webView.delegate = self
        self.outputView.text = "// Please wait..."
        xCompileAndRunScript(script)
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    // MARK: Script execution
    func defineScript(script : String) {
        self.script = script
    }
    
    func xCompileAndRunScript(script : String) {
        makeHTTPPostRequest(tinyscriptURL, body: script, onCompletion: {data, error -> Void
            in
            if (data != nil) {
                var json: JSON = JSON(data: data!)
                if (json["errorCode"] == 0) {
                    if let jsscript = json["script"].string {
                        self.runJSScript(jsscript)
                        dispatch_async(dispatch_get_main_queue(), {
                            self.outputView.text = self.output
                        })
                    }
                }
                else {
                    if let msg = json["errorMessage"].string {
                        dispatch_async(dispatch_get_main_queue(), {
                            self.outputView.text = "// " + msg
                        })
                    }

                }
            }
        })
    }
    
    func runJSScript(script : String) {
        
        let context = self.webView.valueForKeyPath("documentView.webView.mainFrame.javaScriptContext") as! JSContext
        
        // Define a print function
        let printFunction : @convention(block) (String) -> Void =
        {
            (msg: String) in
            
            self.captureMessage(msg)
        }
        
        // ...and register it with the global context
        context.setObject(unsafeBitCast(printFunction, AnyObject.self),
            forKeyedSubscript: "print")
        
        let result : JSValue = context.evaluateScript(script)
        // print(result);
    }
    
    func captureMessage(msg : String) {
        output = output + msg + "\n";
    }
    

    func makeHTTPPostRequest(path: String, body: String,
        onCompletion: (NSData?, NSError?) -> Void) {

        // Setup the request
        let request = NSMutableURLRequest(URL: NSURL(string: path)!)
        request.HTTPMethod = "POST"
        request.setValue("text/plain", forHTTPHeaderField: "Content-Type")
        
        // Set the POST body for the request
        request.HTTPBody = body.dataUsingEncoding(NSUTF8StringEncoding); // NSJSONSerialization.dataWithJSONObject(body, options: [])
        let session = NSURLSession.sharedSession()
        
        let task = session.dataTaskWithRequest(request, completionHandler: {data, response, error -> Void in
            onCompletion(data, error)
        })
        task.resume()
    }
    

    
}
