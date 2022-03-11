//
//  FirstViewController.swift
//  TinyscriptDemo
//
//  Created by Markus Bauer on 26.01.16.
//  Copyright © 2016 Markus Bauer. All rights reserved.
//

import UIKit

class MainScreenController: UIViewController {

    @IBOutlet weak var sourceView: UITextView!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        sourceView.text = "var hello = \"hello, world\";\nprint(hello);"
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any!) {
        switch(segue.identifier!) {
        case K.pickExampleSegue:
            let examplePicker = (segue.destination as? ExamplePickerController)!
            examplePicker.delegate = self
        case K.executeSegue:
            let resultsView = (segue.destination as? ResultsViewController)!
            resultsView.script = sourceView.text;
        default:
            break
        }
    }
    
}

extension MainScreenController: ExamplePickerDelegate {
    
    func didSelectExample(_ example: String) {
        let ts = TinyscriptService(delegate: self)
        ts.loadExample(example)
    }
    
}

extension MainScreenController: TinyscriptServiceDelegate {
    
    func didLoadExample(_ service: TinyscriptService, script: String) {
        DispatchQueue.main.async(execute: {
            self.sourceView.text = script
        })
    }
  
}

