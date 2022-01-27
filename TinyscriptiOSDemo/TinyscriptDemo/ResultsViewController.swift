//
//  ResultsViewController.swift
//  TinyscriptDemo
//
//  Created by Markus Bauer on 02.02.16.
//  Copyright © 2016 Markus Bauer. All rights reserved.
//

import UIKit
import JavaScriptCore


class ResultsViewController: UIViewController  {
    
    // MARK: Properties
    
    @IBOutlet weak var outputView: UITextView!
    
    var script : String = ""
    var output : String = ""
    var tinyscriptURL : String = ""
    
 
    //MARK: UIViewController
    override func viewDidLoad() {
        super.viewDidLoad()
        self.outputView.text = "// Please wait..."
        xCompileAndRunScript(script)
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    // MARK: Script execution
    func defineScript(_ script : String) {
        self.script = script
    }
    
    func xCompileAndRunScript(_ script : String) {
        makeHTTPPostRequest(tinyscriptURL, body: script, onCompletion: {data, error -> Void
            in
            var text = ""
            if (data != nil) {
                do {
                    let json: JSON = try JSON(data: data!)
                    if (json["errorCode"] == 0) {
                        if let jsscript = json["script"].string {
                            self.runJSScript(jsscript)
                            text = self.output
                        }
                    }
                    else {
                        if let msg = json["errorMessage"].string {
                            text = "// " + msg
                        }
                        else {
                            text = "// Internal server error"
                        }

                    }
                }
                catch {
                    text = "// Internal server error"
                }
            }
            DispatchQueue.main.async(execute: {
                self.outputView.text = text
            })
        })
    }
    
    func runJSScript(_ script : String) {
        
        let context = JSContext();
        
        // Define a print function
        let printFunction : @convention(block) (String) -> Void =
        {
            (msg: String) in
            
            self.captureMessage(msg)
        }
        
        // ...and register it with the global context
        context?.setObject(unsafeBitCast(printFunction, to: AnyObject.self),
            forKeyedSubscript: "print" as (NSCopying & NSObjectProtocol))
        
        context?.evaluateScript(script)
        // print(result);
        
    }
    
    func captureMessage(_ msg : String) {
        output = output + msg + "\n";
    }
    

    func makeHTTPPostRequest(_ path: String, body: String,
        onCompletion: @escaping (Data?, Error?) -> Void) {

        // Setup the request
        let request = NSMutableURLRequest(url: URL(string: path)!)
        request.httpMethod = "POST"
        request.setValue("text/plain", forHTTPHeaderField: "Content-Type")
        
        // Set the POST body for the request
        request.httpBody = body.data(using: String.Encoding.utf8); // NSJSONSerialization.dataWithJSONObject(body, options: [])
        let session = URLSession.shared
        
        let task = session.dataTask(with: request as URLRequest, completionHandler: { data, response, error -> Void in
            onCompletion(data, error)
            return()
        })
        task.resume()
    }
    

    
}
