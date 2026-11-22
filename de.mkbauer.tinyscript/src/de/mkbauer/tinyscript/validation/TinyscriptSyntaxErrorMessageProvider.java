package de.mkbauer.tinyscript.validation;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenStream;
import org.eclipse.xtext.nodemodel.SyntaxErrorMessage;
import org.eclipse.xtext.parser.antlr.SyntaxErrorMessageProvider;

import de.mkbauer.tinyscript.ts.FunctionDefinition;

/**
 * Syntax errors in plain words. Constructs that are common in Javascript, but not part of
 * tinyscript or written differently there (a C-style for loop, ===, switch ...), get a
 * hint what to write instead; the other errors of the ANTLR parser are put in plain words.
 */
public class TinyscriptSyntaxErrorMessageProvider extends SyntaxErrorMessageProvider {

	/** The start of the message for a script that ends too early (the REPL then reads on). */
	public static final String UNEXPECTED_END = "Unexpected end of input";

	private static final String INTERFACE = "interface and implements are not supported in tinyscript: "
			+ "describe the methods in a comment; a missing method is reported when it is called";

	private static final Map<String, String> UNSUPPORTED = Map.of(
			"finally", "finally is not supported in tinyscript: there is only try { ... } catch (e) { ... }",
			"switch", "switch is not supported in tinyscript: use if / else if",
			"case", "switch is not supported in tinyscript: use if / else if",
			"var", "tinyscript has no var: use let, or const for a variable that is never assigned again",
			"interface", INTERFACE,
			"implements", INTERFACE);

	// Where an expression starts, a "/" can only be meant as a regular expression
	private static final Set<String> BEFORE_EXPRESSION = Set.of("=", "(", ",", "[", ":", "return", "?", "&&", "||", "!");

	// After these, an expression has to follow
	private static final Set<String> OPERATORS = Set.of("+", "-", "*", "/", "%", "=", "+=", "-=", "*=", "/=", "%=",
			"==", "!=", "<", "<=", ">", ">=", "&&", "||", "??", "!", "?", ":", "(", "[", ",", "return",
			"&", "|", "^", "~", "<<", ">>", "&=", "|=", "^=", "<<=", ">>=");

	private static final Map<String, String> TOKEN_NAMES = Map.of(
			"RULE_ID", "a name",
			"RULE_DOUBLE", "a number",
			"RULE_STRING", "a string",
			"EOF", "the end of input");

	@Override
	public SyntaxErrorMessage getSyntaxErrorMessage(IParserErrorContext context) {
		SyntaxErrorMessage original = super.getSyntaxErrorMessage(context);
		String message = null;
		RecognitionException e = context.getRecognitionException();
		if (e != null && e.token != null && e.input instanceof TokenStream tokens)
			message = hint(context, tokens, e.token, e.index);
		if (message == null)
			message = inPlainWords(context.getDefaultMessage());
		return new SyntaxErrorMessage(message, original.getIssueCode(), original.getIssueData());
	}

	/**
	 * A hint for a construct from Javascript at the offending token, or null.
	 */
	private String hint(IParserErrorContext context, TokenStream tokens, Token offending, int index) {
		String text = offending.getText();
		if (text == null || offending.getType() == Token.EOF)
			return null;
		Token previous = previous(tokens, index);
		String before = previous != null ? previous.getText() : "";
		if (UNSUPPORTED.containsKey(text))
			return UNSUPPORTED.get(text);
		if (UNSUPPORTED.containsKey(before))
			return UNSUPPORTED.get(before);
		String classHint = classHint(context, tokens, index, text, before);
		if (classHint != null)
			return classHint;
		// A keyword where a name is expected: const from = 1; or function f(as) (where the
		// parser reports the "(" before it)
		Token next = next(tokens, index);
		if (isKeyword(context, offending) && NAME_POSITIONS.contains(before))
			return reservedWord(text);
		if ((text.equals("(") || text.equals(",")) && next != null && isKeyword(context, next))
			return reservedWord(next.getText());
		if (text.equals("=") && (before.equals("==") || before.equals("!=")) && previous.getTokenIndex() == offending.getTokenIndex() - 1)
			return "tinyscript has no === and !==: use == and !=, which never convert types";
		if (text.equals(";") && isInForHeader(tokens, index))
			return FOR_WITH_RANGE;
		// for (let i = 0, n) (the Lua-style loop tinyscript had until 0.22.0) or for (let i = 0; …)
		if (text.equals("=") && isInForHeader(tokens, index))
			return FOR_WITH_RANGE;
		if ((text.equals("of") || text.equals("in")) && isInForHeader(tokens, index))
			return "A loop over the elements of an array, string, Map or Set is written for (const x : items) in tinyscript";
		if (isNumberWithDot(context, previous) && isName(context, offending))
			return "A method of a number literal needs parentheses, as in Javascript: ("
					+ before.substring(0, before.length() - 1) + ")." + text + "(…)";
		if (text.equals("`"))
			return "Template strings (`…`) are not supported: build strings with +, e.g. \"Hello \" + name";
		if (text.equals("=") && context.getCurrentContext() instanceof FunctionDefinition && isInParentheses(tokens, index)) {
			return "Parameters cannot have default values: write " + before + " ?? " + (next != null ? next.getText() : "…") + " in the function";
		}
		if (text.equals("++") || text.equals("--"))
			return "++ and -- are statements, not expressions: write i" + text + "; on its own";
		if (text.equals("..."))
			return spreadOrRest(tokens, index);
		if (text.equals("/") && (previous == null || BEFORE_EXPRESSION.contains(before)))
			return "Regular expressions are not supported (std:string has parse, findAll, words and integers)";
		// The ';' is missing at the end of the previous line (the error is reported at the next token)
		if (context.getDefaultMessage().startsWith("missing ';'") && previous != null && previous.getLine() < offending.getLine())
			return "Missing ';' at the end of line " + previous.getLine();
		if (context.getDefaultMessage().startsWith("missing '{'"))
			return "if, else, for and while need a block: if (…) { … }";
		if (context.getDefaultMessage().startsWith("no viable alternative") && OPERATORS.contains(before))
			return "Unexpected '" + text + "', expected an expression";
		return null;
	}

	/**
	 * Javascript's class features that tinyscript leaves out: extends, static members,
	 * getters and setters, field declarations.
	 */
	private static String classHint(IParserErrorContext context, TokenStream tokens, int index, String text, String before) {
		// class B extends A { … }: the parser reports it at class (or at extends)
		if (text.equals("extends") || (text.equals("class") && extendsFollows(tokens, index)))
			return "Classes in tinyscript have no inheritance (no extends): share code with functions";
		// static make() { … }, get area() { … }: a name before a method (never valid elsewhere)
		Token next = next(tokens, index);
		boolean method = isName(context, tokens.get(index)) && next != null && "(".equals(next.getText());
		if (method && before.equals("static"))
			return "Classes in tinyscript have no static members: write a function or a constant next to the class";
		if (method && (before.equals("get") || before.equals("set")))
			return "Classes in tinyscript have no getters and setters: write a method, e.g. area() { … }";
		if ((text.equals("=") || text.equals(";")) && isInClassBody(tokens, index))
			return "A class has no field declarations: fields get their values in the constructor, this." + before + " = …;";
		return null;
	}

	// Whether the token at index stands directly in the body of a class: the "{" that encloses it
	// follows class Name
	private static boolean isInClassBody(TokenStream tokens, int index) {
		int depth = 0;
		for (int i = index - 1; i >= 0; i--) {
			Token token = tokens.get(i);
			if (token.getChannel() != Token.DEFAULT_CHANNEL)
				continue;
			if ("}".equals(token.getText()))
				depth++;
			else if ("{".equals(token.getText()) && depth-- == 0) {
				Token name = previous(tokens, i);
				Token keyword = name != null ? previous(tokens, name.getTokenIndex()) : null;
				return keyword != null && "class".equals(keyword.getText());
			}
		}
		return false;
	}

	// Whether "extends" comes before the "{" that follows the token at index
	private static boolean extendsFollows(TokenStream tokens, int index) {
		for (int i = index + 1; i < tokens.size() && i < index + 20; i++) {
			String text = tokens.get(i).getText();
			if (text == null || text.equals("{"))
				return false;
			if (text.equals("extends"))
				return true;
		}
		return false;
	}

	private static final String FOR_WITH_RANGE = "tinyscript's for loop goes through items: "
			+ "for (const i : range(n)) { … } counts from 0 to n - 1, range(a, b) from a to b - 1, "
			+ "range(n).reverse() from n - 1 down to 0";

	// Before these, a name can stand (const x, function f, (a, b), let x = y, [a, b], { a })
	private static final Set<String> NAME_POSITIONS = Set.of("let", "const", "function", "(", ",", "=", "[", "{");

	// 5. in 5.toFixed(2): the lexer reads the dot as part of the number
	private static boolean isNumberWithDot(IParserErrorContext context, Token token) {
		return token != null && "RULE_DOUBLE".equals(tokenName(context, token)) && token.getText().endsWith(".");
	}

	private static boolean isName(IParserErrorContext context, Token token) {
		return "RULE_ID".equals(tokenName(context, token));
	}

	private static String tokenName(IParserErrorContext context, Token token) {
		String[] names = context.getTokenNames();
		return token.getType() >= 0 && token.getType() < names.length ? names[token.getType()] : null;
	}

	private static String reservedWord(String word) {
		return "'" + word + "' is a reserved word in tinyscript and cannot be used as a name";
	}

	// A keyword of the grammar (like from, as, import), not a name
	private static boolean isKeyword(IParserErrorContext context, Token token) {
		String[] names = context.getTokenNames();
		return token.getText() != null && token.getText().matches("[a-z]+")
				&& token.getType() >= 0 && token.getType() < names.length && names[token.getType()].startsWith("'");
	}

	/**
	 * ... is only allowed for rest parameters; the message depends on where else it stands:
	 * in a pattern, in an array or object literal, or in a call.
	 */
	private static String spreadOrRest(TokenStream tokens, int index) {
		int open = openingBracket(tokens, index);
		String bracket = open >= 0 ? tokens.get(open).getText() : "";
		Token before = open >= 0 ? previous(tokens, open) : null;
		boolean inDeclaration = before != null && Set.of("let", "const").contains(before.getText());
		if ((bracket.equals("[") || bracket.equals("{")) && inDeclaration)
			return "Rest elements (...) in patterns are not supported (for an array, use slice: items.slice(1))";
		if (bracket.equals("["))
			return "Spread (...) in array literals is not supported: concatenate arrays with +, e.g. a + b";
		if (bracket.equals("{"))
			return "Spread (...) in object literals is not supported: copy the properties, e.g. for (const key : Object.keys(o))";
		return "... is only allowed for rest parameters: to call a function with an array, use f.apply(null, array)";
	}

	// The index of the bracket ( [ or { that encloses the token at index, -1 if there is none
	private static int openingBracket(TokenStream tokens, int index) {
		int depth = 0;
		for (int i = index - 1; i >= 0; i--) {
			String text = tokens.get(i).getText();
			if (text == null || tokens.get(i).getChannel() != Token.DEFAULT_CHANNEL)
				continue;
			if (text.equals(")") || text.equals("]") || text.equals("}"))
				depth++;
			else if (text.equals("(") || text.equals("[") || text.equals("{")) {
				if (depth == 0)
					return i;
				depth--;
			}
		}
		return -1;
	}

	// Whether the token at index is inside the parentheses of for ( ... )
	private static boolean isInForHeader(TokenStream tokens, int index) {
		int open = openingParenthesis(tokens, index);
		Token before = open >= 0 ? previous(tokens, open) : null;
		return before != null && before.getText().equals("for");
	}

	private static boolean isInParentheses(TokenStream tokens, int index) {
		return openingParenthesis(tokens, index) >= 0;
	}

	// The index of the "(" that encloses the token at index, -1 if there is none in the statement
	private static int openingParenthesis(TokenStream tokens, int index) {
		int depth = 0;
		for (int i = index - 1; i >= 0; i--) {
			String text = tokens.get(i).getText();
			if (text == null || tokens.get(i).getChannel() != Token.DEFAULT_CHANNEL)
				continue;
			if (text.equals(")"))
				depth++;
			else if (text.equals("(")) {
				if (depth == 0)
					return i;
				depth--;
			}
			else if (depth == 0 && (text.equals("{") || text.equals("}")))
				return -1;
		}
		return -1;
	}

	private static Token previous(TokenStream tokens, int index) {
		for (int i = index - 1; i >= 0; i--) {
			if (tokens.get(i).getChannel() == Token.DEFAULT_CHANNEL)
				return tokens.get(i);
		}
		return null;
	}

	private static Token next(TokenStream tokens, int index) {
		for (int i = index + 1; i < tokens.size(); i++) {
			if (tokens.get(i).getChannel() == Token.DEFAULT_CHANNEL)
				return tokens.get(i);
		}
		return null;
	}

	private static final Pattern MISSING = Pattern.compile("missing (.+) at (.+)");
	private static final Pattern MISMATCHED = Pattern.compile("(?:mismatched|extraneous) input (.+) expecting (.+)");
	private static final Pattern NO_VIABLE_ALTERNATIVE = Pattern.compile("no viable alternative at input (.+)");

	/**
	 * The ANTLR messages in plain words: "missing ';' at 'print'" becomes "Missing ';' before 'print'".
	 */
	static String inPlainWords(String message) {
		Matcher m = MISSING.matcher(message);
		if (m.matches())
			return m.group(2).equals("'<EOF>'") ? "Missing " + name(m.group(1)) + " at the end of the input"
					: "Missing " + name(m.group(1)) + " before " + m.group(2);
		m = MISMATCHED.matcher(message);
		if (m.matches())
			return (m.group(1).equals("'<EOF>'") ? UNEXPECTED_END : "Unexpected " + m.group(1)) + ", expected " + name(m.group(2));
		m = NO_VIABLE_ALTERNATIVE.matcher(message);
		if (m.matches())
			return m.group(1).equals("'<EOF>'") ? UNEXPECTED_END : "Unexpected " + m.group(1);
		return message;
	}

	private static String name(String token) {
		return TOKEN_NAMES.getOrDefault(token, token);
	}

}
