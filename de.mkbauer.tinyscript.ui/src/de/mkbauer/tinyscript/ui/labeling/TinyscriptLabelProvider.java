/*
* generated by Xtext
*/
package de.mkbauer.tinyscript.ui.labeling;

import org.eclipse.emf.ecore.EObject;

import com.google.inject.Inject;

import de.mkbauer.tinyscript.ts.BinaryExpression;
import de.mkbauer.tinyscript.ts.FunctionDefinition;
import de.mkbauer.tinyscript.ts.Identifier;
import de.mkbauer.tinyscript.ts.NumberLiteral;
import de.mkbauer.tinyscript.ts.ComputedPropertyAccessSuffix;
import de.mkbauer.tinyscript.ts.DotPropertyAccessSuffix;
import de.mkbauer.tinyscript.ts.PropertyName;
import de.mkbauer.tinyscript.ts.Reference;
import de.mkbauer.tinyscript.ts.StringLiteral;

/**
 * Provides labels for a EObjects.
 * 
 * see http://www.eclipse.org/Xtext/documentation.html#labelProvider
 */
public class TinyscriptLabelProvider extends org.eclipse.xtext.ui.label.DefaultEObjectLabelProvider {

	@Inject
	public TinyscriptLabelProvider(org.eclipse.emf.edit.ui.provider.AdapterFactoryLabelProvider delegate) {
		super(delegate);
	}
	
	public String text(final BinaryExpression ele) {
		return ele.getOp();
	}
	
	public String text(final FunctionDefinition ele) {
		if (ele.getId() != null)
			return "Function " + ele.getId().getName();
		return "Function";
	}
	
	public String text(final DotPropertyAccessSuffix ele) {
		PropertyName key = ele.getKey();
		if (key.getName() != null)
			return "." + key.getName();
		else if (key.getExpr() != null)
			return "." + key.getExpr().getValue();
		return ".";
	}
	
	public String text(final ComputedPropertyAccessSuffix ele) {
		return "[]";
	}
	
	public String text(final Identifier ele) {
		return ele.getName();
	}
	
	public String text(final Reference ele) {
		if (ele.getId().getName() != null)
			return "->" + ele.getId().getName();
		return "-> *unresolved*";
	}
	
	public String text(final StringLiteral ele) {
		return "\"" + ele.getValue() +"\"";
	}
	
	public String text(final NumberLiteral ele) {
		return Double.valueOf(ele.getValue()).toString();
	}
	
	public String text(final EObject ele) {
		Class<? extends EObject> _class = ele.getClass();
		String simpleName = _class.getSimpleName().replace("Impl", "");
		return simpleName;
	}

}
