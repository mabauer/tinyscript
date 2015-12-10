package de.mkbauer.tinyscript.interpreter;

import static org.junit.Assert.*;

import com.google.inject.Inject;

import de.mkbauer.tinyscript.TinyscriptInjectorProvider;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.ts.Atomic;
import de.mkbauer.tinyscript.ts.BinaryExpression;
import de.mkbauer.tinyscript.ts.Expression;
import de.mkbauer.tinyscript.ts.IntegerLiteral;
import de.mkbauer.tinyscript.ts.Primary;
import de.mkbauer.tinyscript.ts.Statement;
import de.mkbauer.tinyscript.ts.Tinyscript;
import de.mkbauer.tinyscript.ts.Unary;
import de.mkbauer.tinyscript.ts.Variable;
import de.mkbauer.tinyscript.ts.VariableOrMember;
import de.mkbauer.tinyscript.ts.util.TsSwitch;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.junit4.InjectWith;
import org.eclipse.xtext.junit4.XtextRunner;
import org.eclipse.xtext.junit4.util.ParseHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(TinyscriptInjectorProvider.class)
public class AssociativityTest {
	
	@Inject
	ParseHelper<Tinyscript> parser;
	
	private TsSwitch<String> stringRepr = new TsSwitch<String>() {
		public String caseBinaryExpression(BinaryExpression expr) {
			String result = "(" + doSwitch(expr.getLeft()) + expr.getOp() + doSwitch(expr.getRight()) + ")";
			return result;
		}

		@Override
		public String caseUnary(Unary object) {
			return doSwitch(object.getExpr());
		}
		
		@Override
		public String casePrimary(Primary object) {
			return doSwitch(object.getExpr());
		}

		@Override
		public String caseVariableOrMember(VariableOrMember object) {
			return doSwitch(object.getVar());
		}
		
		@Override
		public String caseVariable(Variable object) {
			return object.getName();
		}

		@Override
		public String caseIntegerLiteral(IntegerLiteral object) {			
			return new Integer(object.getValue()).toString();
		}

		@Override
		public String defaultCase(EObject object) {
			return object.getClass().getSimpleName();
		}
		
	};
	
	String stringRepr(Expression expr) {
		return stringRepr.doSwitch(expr);
	}

	Expression parseExpression(String line) {
		Tinyscript ast = null;
		try {
			 ast = (Tinyscript)parser.parse(line);
		}
		catch (Exception e) {
			fail("Syntax error in: " + line);
			e.printStackTrace();
		}
		EList<Statement> statements = ast.getElements().getStatements();
		Expression expr = (Expression)statements.get(statements.size()-1);
		return expr;
	}
	
	void assertStringRepr(String line, String expected) {
		assertEquals(stringRepr(parseExpression(line)), expected);
	}
	
	@Test
	public void testMultiplication() {
		// Multiplication should be left-associative
		assertStringRepr("1*2*3", "((1*2)*3)");
	}
	
	@Test
	public void testAssignment() {
		// Assignments should be right-associative
		assertStringRepr("var x,y,z; x=y=z", "(x=(y=z))");
	}

}