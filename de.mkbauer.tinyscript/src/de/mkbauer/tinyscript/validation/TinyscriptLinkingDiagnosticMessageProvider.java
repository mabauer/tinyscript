package de.mkbauer.tinyscript.validation;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.StreamSupport;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.diagnostics.DiagnosticMessage;
import org.eclipse.xtext.linking.impl.LinkingDiagnosticMessageProvider;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.IScopeProvider;

import com.google.inject.Inject;

import de.mkbauer.tinyscript.TinyscriptModelUtil;
import de.mkbauer.tinyscript.ts.Block;
import de.mkbauer.tinyscript.ts.Identifier;

/**
 * "Unknown name 'valeus' (did you mean 'values'?)" for a name that is not declared: the
 * suggestion is the most similar name visible at that place. A name that is declared later
 * in the same block (or an enclosing one) is "used before its declaration".
 */
public class TinyscriptLinkingDiagnosticMessageProvider extends LinkingDiagnosticMessageProvider {

	@Inject
	private IScopeProvider scopeProvider;

	@Override
	public DiagnosticMessage getUnresolvedProxyMessage(ILinkingDiagnosticContext context) {
		DiagnosticMessage original = super.getUnresolvedProxyMessage(context);
		String name = context.getLinkText();
		Identifier later = laterDeclaration(context.getContext(), name);
		if (later != null)
			return new DiagnosticMessage(name + " is used before its declaration (line "
					+ TinyscriptModelUtil.getLineOfASTNode(later) + ")",
					original.getSeverity(), original.getIssueCode(), original.getIssueData());
		String message = "Unknown name '" + name + "'";
		String suggestion = suggestion(name, visibleNames(context));
		if (suggestion != null)
			message = message + " (did you mean '" + suggestion + "'?)";
		return new DiagnosticMessage(message, original.getSeverity(), original.getIssueCode(), original.getIssueData());
	}

	// A let / const / class of this name in a block around the reference, after it
	private static Identifier laterDeclaration(EObject reference, String name) {
		for (Block block = TinyscriptModelUtil.containingBlock(reference); block != null;
				block = TinyscriptModelUtil.containingBlock(block.eContainer())) {
			for (Identifier id : TinyscriptModelUtil.declaredVariablesInBlock(block)) {
				if (id.getName().equals(name))
					return id;
			}
		}
		return null;
	}

	private List<String> visibleNames(ILinkingDiagnosticContext context) {
		IScope scope = scopeProvider.getScope(context.getContext(), context.getReference());
		return StreamSupport.stream(scope.getAllElements().spliterator(), false)
				.map(description -> description.getName().toString())
				.distinct()
				.toList();
	}

	/**
	 * The candidate most similar to name, if it is similar enough: at most a third of the
	 * letters differ (at least one); null otherwise.
	 */
	static String suggestion(String name, Collection<String> candidates) {
		int limit = Math.max(1, name.length() / 3);
		return candidates.stream()
				.filter(candidate -> !candidate.equals(name))
				.filter(candidate -> distance(name, candidate) <= limit)
				.min(Comparator.comparingInt((String candidate) -> distance(name, candidate)).thenComparing(Comparator.naturalOrder()))
				.orElse(null);
	}

	/**
	 * The number of edits from a to b: insert, delete or replace a letter, or swap two
	 * neighboring letters (the optimal string alignment distance), so that "Mtah" is one
	 * edit away from "Math".
	 */
	static int distance(String a, String b) {
		int[][] d = new int[a.length() + 1][b.length() + 1];
		for (int i = 0; i <= a.length(); i++)
			d[i][0] = i;
		for (int j = 0; j <= b.length(); j++)
			d[0][j] = j;
		for (int i = 1; i <= a.length(); i++) {
			for (int j = 1; j <= b.length(); j++) {
				int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
				d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
				if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1))
					d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
			}
		}
		return d[a.length()][b.length()];
	}

}
