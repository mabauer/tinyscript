package de.mkbauer.tinyscript.ui.contentassist;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.Assignment;
import org.eclipse.xtext.CrossReference;
import org.eclipse.xtext.ui.editor.contentassist.ContentAssistContext;
import org.eclipse.xtext.ui.editor.contentassist.ICompletionProposalAcceptor;

import de.mkbauer.tinyscript.ts.Reference;
import de.mkbauer.tinyscript.ui.contentassist.AbstractTinyscriptProposalProvider;

/**
 * see http://www.eclipse.org/Xtext/documentation.html#contentAssist on how to customize content assistant
 */
public class TinyscriptProposalProvider extends AbstractTinyscriptProposalProvider {
	

	public void completeReference_Id(Reference ref, Assignment assignment, ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		
		lookupCrossReference(((CrossReference)assignment.getTerminal()), context, acceptor);
		
		
	}
}
