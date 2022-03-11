//
//  ResultsViewController.swift
//  TinyscriptDemo
//
//  Created by Markus Bauer on 02.02.16.
//  Copyright © 2016 Markus Bauer. All rights reserved.
//

import UIKit



class ResultsViewController: UIViewController  {
        
    @IBOutlet weak var outputView: UITextView!
    
    var script : String = ""
    var output : String = ""
    var tinyscriptURL : String = ""
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.outputView.text = "// Please wait..."
        crossCompileAndRun(script: script)
    }
    
    func crossCompileAndRun(script: String) {
        let ts = TinyscriptService(delegate: self)
        ts.crossCompileAndRunScript(script)
    }

}

extension ResultsViewController: TinyscriptServiceDelegate {
    
    func didRun(_ service: TinyscriptService, output: String) {
        DispatchQueue.main.async(execute: {
            self.outputView.text = output
        })
    }
}
