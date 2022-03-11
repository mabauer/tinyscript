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

class ExamplePickerController:  UIViewController {
    
   
    @IBOutlet weak var exampleTable: UITableView!
    
    var delegate : ExamplePickerDelegate?
    
    var examples: [String] = ["Hello World", "Basic Expressions", "Strings", "Control flow", "Functions", "Functions: recursion", "Arrays", "Closures", "Object and Prototypes", "People"]
    var exampleFiles: [String] = ["helloworld.ts", "expressions.ts", "strings.ts", "primes.ts", "fibonacci.ts", "fibonacci_recursive.ts", "arrays.ts", "closures.ts", "oo.ts", "people.ts"]
    
    override func viewDidLoad() {
        super.viewDidLoad()
        exampleTable.dataSource = self
        exampleTable.delegate = self
    }
    
    func close() {
        self.navigationController?.popViewController(animated: true)
    }
}

extension ExamplePickerController: UITableViewDataSource, UITableViewDelegate {
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {

        let example = exampleFiles[(indexPath as NSIndexPath).row]
        print("Selected example: \(example)")
        delegate!.didSelectExample(example)
        // self.dismiss(animated: true, completion:nil)
        self.close()
        
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return examples.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) ->   UITableViewCell {
        let cell = UITableViewCell()
        let label = UILabel(frame: CGRect(x:10, y:0, width:320, height:50))
        label.text = examples[(indexPath as NSIndexPath).row]
        cell.addSubview(label)
        return cell
    }
    
    
}
