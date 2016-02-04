//
//  ExamplePickerController.swift
//  TinyscriptDemo
//
//  Created by Markus Bauer on 26.01.16.
//  Copyright © 2016 Markus Bauer. All rights reserved.
//

import UIKit

@objc protocol SourceViewerDelegate {
    func showSource(script: String)
}

class ExamplePickerController:  UITableViewController, UIPopoverPresentationControllerDelegate {
    
    // MARK: Properties
    
    weak var delegate : SourceViewerDelegate?
    var tinyscriptURL : String = ""
    
    
    var examples: [String] = ["Hello World", "Basic Expressions", "Strings", "Control flow", "Functions", "Functions: recursion", "Arrays", "Closures", "Object and Prototypes", "People"]
    var exampleFiles: [String] = ["helloworld.ts", "expressions.ts", "strings.ts", "primes.ts", "fibonacci.ts", "fibonacci_recursive.ts", "arrays.ts", "closures.ts", "oo.ts", "people.ts"]
    
    required init(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)!
    
        // cancel button
        navigationItem.rightBarButtonItem = UIBarButtonItem(barButtonSystemItem: UIBarButtonSystemItem.Cancel, target: self, action: "tapCancel:")
    
        // popover settings
        modalPresentationStyle = .Popover
        popoverPresentationController!.delegate = self
    
        self.preferredContentSize = CGSize(width:320,height:450)
    }
    
    func tapCancel(_ : UIBarButtonItem) {
        dismissViewControllerAnimated(true, completion:nil);
    }
    
    // MARK: UITableViewController
    
    override func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath){

        let example = exampleFiles[indexPath.row]
        print("Selected example: \(example)")
        loadExample(example)
        dismissViewControllerAnimated(true, completion:nil)
    }
    
    override func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return examples.count
    }
    
    override func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) ->   UITableViewCell {
        let cell = UITableViewCell()
        let label = UILabel(frame: CGRect(x:0, y:0, width:320, height:50))
        label.text = examples[indexPath.row]
        cell.addSubview(label)
        return cell
    }
    
    // MARK: UIPopoverPresentationControllerDelegate
    
    func adaptivePresentationStyleForPresentationController(PC: UIPresentationController) ->UIModalPresentationStyle {
    
        return UIModalPresentationStyle.FullScreen
    }
    
    func presentationController(_: UIPresentationController, viewControllerForAdaptivePresentationStyle _: UIModalPresentationStyle) -> UIViewController? {
        return UINavigationController(rootViewController: self)
    
    }
    
    // MARK: Loading example code
    func loadExample(example: String) {
        makeHTTPGetRequest(tinyscriptURL + "/" + example, onCompletion: {data, error -> Void
            in
            if (data != nil) {
                let script : String = NSString(data: data!, encoding:NSUTF8StringEncoding) as! String
                // print(script)
                dispatch_async(dispatch_get_main_queue(), {
                    self.delegate!.showSource(script)
                })
            }
        })
    }
    
    func makeHTTPGetRequest(path: String,
        onCompletion: (NSData?, NSError?) -> Void) {
            
            // Setup the request
            let request = NSMutableURLRequest(URL: NSURL(string: path)!)
            request.HTTPMethod = "GET"
            let session = NSURLSession.sharedSession()
            
            let task = session.dataTaskWithRequest(request, completionHandler: {data, response, error -> Void in
                onCompletion(data, error)
            })
            task.resume()
    }
    
}
