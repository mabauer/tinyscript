//
//  TinyscriptService.swift
//  TinyscriptDemo
//
//  Created by Markus Bauer on 11.03.22.
//  Copyright © 2022 Markus Bauer. All rights reserved.
//

import Foundation
import JavaScriptCore

protocol TinyscriptServiceDelegate {
    func didLoadExample(_ service: TinyscriptService, script: String)
    func didRun(_ service: TinyscriptService, output: String)
    func didFailWithError(_ service: TinyscriptService, error: Error)
}

extension TinyscriptServiceDelegate {
    func didLoadExample(_ service: TinyscriptService, script: String) {}
    func didRun(_ service: TinyscriptService, output: String) {}
    func didFailWithError(_ service: TinyscriptService, error: Error) {}
}

class TinyscriptService {
    
    var delegate: TinyscriptServiceDelegate
    var output = ""
    
    init(delegate: TinyscriptServiceDelegate) {
        self.delegate = delegate
    }

    // MARK: Load example code
    func loadExample(_ example: String)  {
        makeHTTPGetRequest(K.TinyscriptServiceUrl + "/" + example, onCompletion: {data, error -> Void in
            if (data != nil) {
                let script : String = NSString(data: data!, encoding:String.Encoding.utf8.rawValue)! as String
                // print(script)
                self.delegate.didLoadExample(self, script: script)
                
            }
        })
    }

    func makeHTTPGetRequest(_ path: String,
        onCompletion: @escaping (Data?, Error?) -> Void) {
            
            // Setup the request
            var request = NSMutableURLRequest(url: URL(string: path)!) as URLRequest
            request.httpMethod = "GET"
            let session = URLSession.shared
        
            let task = session.dataTask(with: request, completionHandler: {data, response, error -> Void in
                onCompletion(data, error)
                return ()
            })
            task.resume()
    }
    
    // MARK: Cross compile and run code
    
    func crossCompileAndRunScript(_ script : String) {
        makeHTTPPostRequest(K.TinyscriptServiceUrl + "/xcompile", body: script, onCompletion: {data, error -> Void in
            self.output = ""
            if (data != nil) {
                do {
                    let json: JSON = try JSON(data: data!)
                    if (json["errorCode"] == 0) {
                        if let jsscript = json["script"].string {
                            self.runJSScript(jsscript)
                        }
                    }
                    else {
                        if let msg = json["errorMessage"].string {
                            self.output = "// " + msg
                        }
                        else {
                            self.output = "// Internal server error"
                        }
                    }
                }
                catch {
                    self.output = "// Internal server error"
                }
            }
            self.delegate.didRun(self, output: self.output)
            
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

