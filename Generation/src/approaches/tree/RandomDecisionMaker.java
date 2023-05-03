package approaches.tree;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;

import main.grammar.Baptist;
import main.grammar.ebnf.EBNFClause;
import main.grammar.ebnf.EBNFClauseArg;
import main.grammar.ebnf.EBNFRule;

public class RandomDecisionMaker implements DecisionMaker {

	@Override
	public EBNFRule chooseRule(List<EBNFRule> rules) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public EBNFClause chooseClause(List<EBNFClause> clauses) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public EBNFClauseArg chooseArgument(List<EBNFClauseArg> arguments) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean chooseExitWithPrimitive() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean chooseDropArgument() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int chooseOrGroup(int max) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int chooseItemCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int choosePrimitiveInteger(int min, int max) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float choosePrimitiveFloat(float min, float max) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean choosePrimitiveBoolean() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String choosePrimitiveString(int maxLength) {
		// TODO Auto-generated method stub
		return null;
	}
	/*
	
	static String instantiateStrings(final String input, final Random rng)
	{
		String str = input.trim();
		
		// Instantiate 'string' placeholders
		int c = 0;
		while (true)
		{
			// Find next occurrence of 'string'
			c = str.indexOf("string", c + 1);
			if (c < 0)
				break;
			
			final char chPrev = str.charAt(c - 1);
			final char chNext = str.charAt(c + 6);
			
			if 
			(
				chPrev != ' ' && chPrev != ':' && chPrev != '{'
				||
				chNext != ' ' && chNext != ')' && chNext != '}'
			)
			{
				continue;  // is not an actual string placeholder
			}
			
			final String owner = enclosingLudemeName(str, c);
			//System.out.println("owner='" + owner + "'");
			
			String replacement = null;
			if 
			(
				owner.equalsIgnoreCase("game")
				||
				owner.equalsIgnoreCase("match")
				||
				owner.equalsIgnoreCase("subgame")
			)
			{
				// Create a name for this game
				replacement = Baptist.baptist().name(str.hashCode(), 4);
			}
			
			if (replacement == null)
			{
				// Look for a ludeme from the stringPool
				for (int group = 0; group < stringPool.length && replacement == null; group++)
					for (int n = 0; n < stringPool[group][0].length && replacement == null; n++)
						if (owner.equalsIgnoreCase(stringPool[group][0][n]))
							replacement = stringPool[group][1][rng.nextInt(stringPool[group][1].length)];
			}
			
			if (replacement == null)
			{
				// Create random coordinate
				replacement = (char)('A' + rng.nextInt(26)) + ("" + rng.nextInt(26));
			}
			
			str = str.substring(0, c) + "\"" + replacement + "\"" + str.substring(c + 6);
		}
						
		return str;
	}
	
	static String instantiateIntegers(final String input, final Random rng)
	{
		String str = input.trim();
		
		// Instantiate 'int' placeholders
		int c = 0;
		while (true)
		{
			// Find next occurrence of 'int'
			c = str.indexOf("int", c + 1);
			if (c < 0)
				break;
			
			final char chPrev = str.charAt(c - 1);
			final char chNext = str.charAt(c + 3);
			
			if 
			(
				chPrev != ' ' && chPrev != ':' && chPrev != '{'
				||
				chNext != ' ' && chNext != ')' && chNext != '}'
			)
			{
				continue;  // is not an actual int placeholder
			}
			
			int num = lowBiasedRandomInteger(rng, true);
			
			final String owner = enclosingLudemeName(str, c);
			if (owner.equalsIgnoreCase("players") && rng.nextInt(4) != 0)
			{
				// Restrict number of players
				num = num % 4 + 1;
			}
			
			str = str.substring(0, c) + num + str.substring(c + 3);
		}
						
		return str;
	}
	
	static String instantiateDims(final String input, final Random rng)
	{
		String str = input.trim();
		
		// Instantiate 'dim' placeholders
		int c = 0;
		while (true)
		{
			// Find next occurrence of 'dim'
			c = str.indexOf("dim", c + 1);
			if (c < 0)
				break;
			
			final char chPrev = str.charAt(c - 1);
			final char chNext = str.charAt(c + 3);
			
			if 
			(
				chPrev != ' ' && chPrev != ':' && chPrev != '{'
				||
				chNext != ' ' && chNext != ')' && chNext != '}'
			)
			{
				continue;  // is not an actual int placeholder
			}
			
			// Positive number within reasonable bounds
			int num = Math.abs(lowBiasedRandomInteger(rng, true)) % 20;
			
			final String owner = enclosingLudemeName(str, c);
			if (owner.equalsIgnoreCase("players") && rng.nextInt(4) != 0)
			{
				// Restrict number of players
				num = num % 4 + 1;
			}
			
			str = str.substring(0, c) + num + str.substring(c + 3);
		}
						
		return str;
	}
	
	static String instantiateFloats(final String input, final Random rng)
	{
		String str = input.trim();
		
		final DecimalFormat df = new DecimalFormat("#.##");
		
		// Instantiate 'int' placeholders
		int c = 0;
		while (true)
		{
			// Find next occurrence of 'float'
			c = str.indexOf("float", c + 1);
			if (c < 0)
				break;
			
			final char chPrev = str.charAt(c - 1);
			final char chNext = str.charAt(c + 5);
			
			if 
			(
				chPrev != ' ' && chPrev != ':' && chPrev != '{'
				||
				chNext != ' ' && chNext != ')' && chNext != '}'
			)
			{
				continue;  // is not an actual int placeholder
			}
			
			final double num = lowBiasedRandomInteger(rng, true) / 4.0;
						
			str = str.substring(0, c) + df.format(num) + str.substring(c + 5);	
		}
						
		return str;
	}*/
	
	//-------------------------------------------------------------------------

	/**
	 * @return Name of enclosing ludeme (if any).
	 */
	static String enclosingLudemeName(final String str, final int fromIndex)
	{
		int c = fromIndex;
		while (c >= 0 && str.charAt(c) != '(')
			c--;
		
		if (c < 0)
			return str.split(" ")[0];
		
		int cc = c + 1;
		while (cc < str.length() && str.charAt(cc) != ' ')
			cc++;
		
		return str.substring(c + 1, cc);
	}
	
	//-------------------------------------------------------------------------

	/**
	 * @return Random number (biased towards lower values) that is sometimes negative.
	 */
	private static int lowBiasedRandomInteger(final Random rng, final boolean negate)
	{
		int r = 0;
		switch (rng.nextInt(10))
		{
		case  0: r = rng.nextInt(2) + 1;  break;
		case  1: r = rng.nextInt(4);      break;
		case  2: r = rng.nextInt(3) + 1;  break;
		case  3: r = rng.nextInt(4) + 1;  break;
		case  4: r = rng.nextInt(5) + 1;  break;
		case  5: r = rng.nextInt(6) + 1;  break;
		case  6: r = rng.nextInt(8);      break;
		case  7: r = rng.nextInt(16);     break;
		case  8: r = rng.nextInt(100);    break;
		case  9: r = rng.nextInt(1000);   break;
		default: r = 0;
		}
		
		if (negate && rng.nextInt(20) == 0)
			r = -r;
		
		return r;
	}

}
