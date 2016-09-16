//
//  FirstViewController.swift
//  TinyscriptDemo
//
//  Created by Markus Bauer on 26.01.16.
//  Copyright © 2016 Markus Bauer. All rights reserved.
//

import UIKit

class MainScreenController: UIViewController, SourceViewerDelegate {

    // MARK: Properties

    @IBOutlet weak var sourceView: UITextView!
    
    let URL = "http://home.mkbauer.de/tinyscript"
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        sourceView.text = "var hello = \"hello, world\";\nprint(hello);"
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any!) {
        switch(segue.identifier!) {
        case "pickExample":
            let examplePicker = (segue.destination as? ExamplePickerController)!
            examplePicker.tinyscriptURL = URL
            examplePicker.delegate = self
        case "execute":
            let resultsView = (segue.destination as? ResultsViewController)!
            resultsView.defineScript(sourceView.text);
            resultsView.tinyscriptURL = URL + "/xcompile"
        default:
            break
        }
    }
    
    func showSource(_ script: String) {
        sourceView.text = script
    }
    
    
  
}

