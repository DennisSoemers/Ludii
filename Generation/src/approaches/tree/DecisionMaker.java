package approaches.tree;

import java.util.List;

import main.grammar.ebnf.EBNFClause;
import main.grammar.ebnf.EBNFClauseArg;
import main.grammar.ebnf.EBNFRule;

public interface DecisionMaker {
	
	// return rule from list
	EBNFRule chooseRule(List<EBNFRule> rules);

	// return clause from list
	EBNFClause chooseClause(List<EBNFClause> clauses);
	
	// return argument from list
	EBNFClauseArg chooseArgument(List<EBNFClauseArg> arguments);
	
	boolean chooseExitWithPrimitive();
	boolean chooseDropArgument();

	// return int between 0 and max (inclusive)
	int chooseOrGroup(int max);
	
	// return int between 1 and 4 (inclusive)
	int chooseItemCount();
	
	int choosePrimitiveInteger(int min, int max);
	float choosePrimitiveFloat(float min, float max);
	boolean choosePrimitiveBoolean();
	String choosePrimitiveString(int maxLength);

	//TODO choose label
}
