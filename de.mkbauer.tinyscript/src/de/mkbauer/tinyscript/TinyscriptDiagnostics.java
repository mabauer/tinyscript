package de.mkbauer.tinyscript;

import java.util.Comparator;
import java.util.List;

import org.eclipse.xtext.diagnostics.Diagnostic;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.validation.Issue;

/**
 * Which of the issues of a script to report, and how: the REPL, the web demo and module
 * loading report errors the same way.
 */
public final class TinyscriptDiagnostics {

	private TinyscriptDiagnostics() {
	}

	/**
	 * The errors worth reporting. After a syntax error, the parser recovers somehow and the
	 * rest of the script is often misread, which causes further syntax errors, unresolved
	 * names and validation errors; so if there are syntax errors, only the first one is
	 * reported. Otherwise all errors, in the order of the source.
	 */
	public static List<Issue> relevantErrors(List<Issue> issues) {
		List<Issue> errors = issues.stream()
				.filter(issue -> issue.getSeverity() == Severity.ERROR)
				.sorted(Comparator.comparing((Issue issue) -> issue.getOffset() != null ? issue.getOffset() : 0))
				.toList();
		return errors.stream()
				.filter(issue -> Diagnostic.SYNTAX_DIAGNOSTIC.equals(issue.getCode()))
				.findFirst()
				.map(List::of)
				.orElse(errors);
	}

	/**
	 * The warnings, in the order of the source, as lines "Warning: … (line 3)"; none if there
	 * are errors (they come first).
	 */
	public static List<String> warnings(List<Issue> issues) {
		if (!relevantErrors(issues).isEmpty())
			return List.of();
		return issues.stream()
				.filter(issue -> issue.getSeverity() == Severity.WARNING)
				.sorted(Comparator.comparing((Issue issue) -> issue.getOffset() != null ? issue.getOffset() : 0))
				.map(issue -> "Warning: " + issue.getMessage() + " (line " + issue.getLineNumber() + ")")
				.toList();
	}

	/**
	 * The first error worth reporting as an exception, or null if there is none.
	 */
	public static TinyscriptSyntaxError firstError(List<Issue> issues, String filename) {
		List<Issue> errors = relevantErrors(issues);
		if (errors.isEmpty())
			return null;
		Issue error = errors.get(0);
		return new TinyscriptSyntaxError(error.getMessage(), filename, error.getLineNumber(), column(error));
	}

	public static int column(Issue issue) {
		return issue.getColumn() != null ? issue.getColumn() : 0;
	}

	/**
	 * The source line where a runtime error happened, with a marker (see below); empty if the
	 * place is unknown.
	 */
	public static String excerpt(TinyscriptRuntimeException e) {
		if (e.getNode() == null || NodeModelUtils.getNode(e.getNode()) == null)
			return "";
		String source = NodeModelUtils.getNode(e.getNode()).getRootNode().getText();
		return excerpt(source, e.getAffectedLine(), e.getAffectedColumn());
	}

	/**
	 * The source line with a marker under the column (both 1-based), indented, e.g.
	 * <pre>
	 *     if (x === 1) { print(x); }
	 *           ^
	 * </pre>
	 * Tabs before the column are kept, so that the marker lines up. Empty if there is no such line.
	 */
	public static String excerpt(String source, int line, int column) {
		String[] lines = source.split("\r?\n", -1);
		if (line < 1 || line > lines.length)
			return "";
		String text = lines[line - 1];
		StringBuilder marker = new StringBuilder();
		for (int i = 0; i < Math.min(column - 1, text.length()); i++)
			marker.append(text.charAt(i) == '\t' ? '\t' : ' ');
		String indent = "    ";
		return indent + text + "\n" + indent + marker + "^\n";
	}

}
