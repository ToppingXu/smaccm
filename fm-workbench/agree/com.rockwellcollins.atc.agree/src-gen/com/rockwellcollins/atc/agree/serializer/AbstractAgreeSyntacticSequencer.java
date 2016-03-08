/*
 * generated by Xtext
 */
package com.rockwellcollins.atc.agree.serializer;

import com.google.inject.Inject;
import com.rockwellcollins.atc.agree.services.AgreeGrammarAccess;
import java.util.List;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.IGrammarAccess;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.serializer.analysis.GrammarAlias.AbstractElementAlias;
import org.eclipse.xtext.serializer.analysis.GrammarAlias.TokenAlias;
import org.eclipse.xtext.serializer.analysis.ISyntacticSequencerPDAProvider.ISynNavigable;
import org.eclipse.xtext.serializer.analysis.ISyntacticSequencerPDAProvider.ISynTransition;
import org.eclipse.xtext.serializer.sequencer.AbstractSyntacticSequencer;

@SuppressWarnings("all")
public abstract class AbstractAgreeSyntacticSequencer extends AbstractSyntacticSequencer {

	protected AgreeGrammarAccess grammarAccess;
	protected AbstractElementAlias match_TermExpr_LeftParenthesisKeyword_9_0_a;
	protected AbstractElementAlias match_TermExpr_LeftParenthesisKeyword_9_0_p;
	
	@Inject
	protected void init(IGrammarAccess access) {
		grammarAccess = (AgreeGrammarAccess) access;
		match_TermExpr_LeftParenthesisKeyword_9_0_a = new TokenAlias(true, true, grammarAccess.getTermExprAccess().getLeftParenthesisKeyword_9_0());
		match_TermExpr_LeftParenthesisKeyword_9_0_p = new TokenAlias(true, false, grammarAccess.getTermExprAccess().getLeftParenthesisKeyword_9_0());
	}
	
	@Override
	protected String getUnassignedRuleCallToken(EObject semanticObject, RuleCall ruleCall, INode node) {
		return "";
	}
	
	
	@Override
	protected void emitUnassignedTokens(EObject semanticObject, ISynTransition transition, INode fromNode, INode toNode) {
		if (transition.getAmbiguousSyntaxes().isEmpty()) return;
		List<INode> transitionNodes = collectNodes(fromNode, toNode);
		for (AbstractElementAlias syntax : transition.getAmbiguousSyntaxes()) {
			List<INode> syntaxNodes = getNodesFor(transitionNodes, syntax);
			if(match_TermExpr_LeftParenthesisKeyword_9_0_a.equals(syntax))
				emit_TermExpr_LeftParenthesisKeyword_9_0_a(semanticObject, getLastNavigableState(), syntaxNodes);
			else if(match_TermExpr_LeftParenthesisKeyword_9_0_p.equals(syntax))
				emit_TermExpr_LeftParenthesisKeyword_9_0_p(semanticObject, getLastNavigableState(), syntaxNodes);
			else acceptNodes(getLastNavigableState(), syntaxNodes);
		}
	}

	/**
	 * Ambiguous syntax:
	 *     '('*
	 *
	 * This ambiguous syntax occurs at:
	 *     (rule start) (ambiguity) 'Get_Property' '(' component=Expr
	 *     (rule start) (ambiguity) 'event' '(' id=NestedDotID
	 *     (rule start) (ambiguity) 'floor' '(' expr=Expr
	 *     (rule start) (ambiguity) 'if' a=Expr
	 *     (rule start) (ambiguity) 'pre' '(' expr=Expr
	 *     (rule start) (ambiguity) 'prev' '(' delay=Expr
	 *     (rule start) (ambiguity) 'real' '(' expr=Expr
	 *     (rule start) (ambiguity) 'this' '.' subThis=NestedDotID
	 *     (rule start) (ambiguity) 'this' (rule start)
	 *     (rule start) (ambiguity) base=[NamedElement|QCPREF]
	 *     (rule start) (ambiguity) op='-'
	 *     (rule start) (ambiguity) op='not'
	 *     (rule start) (ambiguity) val=BooleanLiteral
	 *     (rule start) (ambiguity) val=INTEGER_LIT
	 *     (rule start) (ambiguity) val=REAL_LIT
	 *     (rule start) (ambiguity) {ArrayAccessExpr.array=}
	 *     (rule start) (ambiguity) {BinaryExpr.left=}
	 *     (rule start) (ambiguity) {FnCallExpr.fn=}
	 *     (rule start) (ambiguity) {RecordExpr.record=}
	 *     (rule start) (ambiguity) {RecordUpdateExpr.record=}
	 */
	protected void emit_TermExpr_LeftParenthesisKeyword_9_0_a(EObject semanticObject, ISynNavigable transition, List<INode> nodes) {
		acceptNodes(transition, nodes);
	}
	
	/**
	 * Ambiguous syntax:
	 *     '('+
	 *
	 * This ambiguous syntax occurs at:
	 *     (rule start) (ambiguity) 'Get_Property' '(' component=Expr
	 *     (rule start) (ambiguity) 'if' a=Expr
	 *     (rule start) (ambiguity) 'prev' '(' delay=Expr
	 *     (rule start) (ambiguity) op='-'
	 *     (rule start) (ambiguity) op='not'
	 *     (rule start) (ambiguity) {ArrayAccessExpr.array=}
	 *     (rule start) (ambiguity) {BinaryExpr.left=}
	 *     (rule start) (ambiguity) {RecordUpdateExpr.record=}
	 */
	protected void emit_TermExpr_LeftParenthesisKeyword_9_0_p(EObject semanticObject, ISynNavigable transition, List<INode> nodes) {
		acceptNodes(transition, nodes);
	}
	
}
