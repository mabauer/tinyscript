//
//  ExamplePickerController.swift
//  TinyscriptDemo
//
//  Created by Markus Bauer on 26.01.16.
//  Copyright © 2016 Markus Bauer. All rights reserved.
//

import UIKit

protocol ExamplePickerDelegate {
    func didSelectExample(_ example: String)
}

class ExamplePickerController:  UITableViewController, UIPopoverPresentationControllerDelegate {
    
    // MARK: Properties
    
    var delegate : ExamplePickerDelegate?
    
    var examples: [String] = ["Hello World", "Basic Expressions", "Strings", "Control flow", "Functions", "Functions: recursion", "Arrays", "Closures", "Object and Prototypes", "People"]
    var exampleFiles: [String] = ["helloworld.ts", "expressions.ts", "strings.ts", "primes.ts", "fibonacci.ts", "fibonacci_recursive.ts", "arrays.ts", "closures.ts", "oo.ts", "people.ts"]
    
    required init(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)!
    
        // cancel button
        navigationItem.rightBarButtonItem = UIBarButtonItem(barButtonSystemItem: UIBarButtonItem.SystemItem.cancel, target: self, action: #selector(ExamplePickerController.tapCancel(_:)))
    
        // popover settings
        modalPresentationStyle = .popover
        popoverPresentationController!.delegate = self
    
        self.preferredContentSize = CGSize(width:320,height:450)
    }
    
    @objc func tapCancel(_ : UIBarButtonItem) {
        dismiss(animated: true, completion:nil);
    }
    
    // MARK: UITableViewController
    
    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {

        let example = exampleFiles[(indexPath as NSIndexPath).row]
        print("Selected example: \(example)")
        delegate!.didSelectExample(example)
        dismiss(animated: true, completion:nil)
    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return examples.count
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) ->   UITableViewCell {
        let cell = UITableViewCell()
        let label = UILabel(frame: CGRect(x:0, y:0, width:320, height:50))
        label.text = examples[(indexPath as NSIndexPath).row]
        cell.addSubview(label)
        return cell
    }
    
    // MARK: UIPopoverPresentationControllerDelegate
    
    func adaptivePresentationStyle(for PC: UIPresentationController) ->UIModalPresentationStyle {
    
        return UIModalPresentationStyle.fullScreen
    }
    
    func presentationController(_: UIPresentationController, viewControllerForAdaptivePresentationStyle _: UIModalPresentationStyle) -> UIViewController? {
        return UINavigationController(rootViewController: self)
    
    }
    
}
