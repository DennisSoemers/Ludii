package approaches.tree;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import main.grammar.ebnf.EBNFClause;
import main.grammar.ebnf.EBNFClauseArg;
import main.grammar.ebnf.EBNFRule;

public class HumanDecisionMaker implements DecisionMaker {
	BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

	@Override
	public EBNFRule chooseRule(List<EBNFRule> rules) {
		System.out.print("\nChoose a rule among: " + rules);	
		return rules.get(inputInt(0, rules.size() - 1));
	}

	@Override
	public EBNFClause chooseClause(List<EBNFClause> clauses) {
		System.out.print("\nChoose a clause among: " + clauses);	
		return clauses.get(inputInt(0, clauses.size() - 1));
	}

	@Override
	public EBNFClauseArg chooseArgument(List<EBNFClauseArg> arguments) {
		System.out.print("\nChoose a argument among: " + arguments);	
		return arguments.get(inputInt(0, arguments.size() - 1));
	}

	@Override
	public boolean chooseExitWithPrimitive() {
		System.out.print("\nExit with a primitive?");	
		return inputBoolean();
	}

	@Override
	public boolean chooseDropArgument() {
		System.out.print("\nDrop an Argument?");	
		return inputBoolean();
	}

	@Override
	public int chooseOrGroup(int max) { // Inclusive max?
		System.out.print("\nChoose an or group:");	
		return inputInt(0, max);
	}

	@Override
	public int chooseItemCount() {
		System.out.print("\nChoose an item count:");	
		return inputInt(1, 4);
	}

	@Override
	public int choosePrimitiveInteger(int min, int max) {
		System.out.print("\nChoose an primitive int:");	
		return inputInt(min, max);
	}

	@Override
	public float choosePrimitiveFloat(float min, float max) {
		System.out.print("\nChoose an primitive float:");	
		return inputFloat(min, max);
	}

	@Override
	public boolean choosePrimitiveBoolean() {
		System.out.print("\nChoose an primitive boolean:");	
		return inputBoolean();
	}

	@Override
	public String choosePrimitiveString(int maxLength) {
		System.out.print("\nChoose an primitive string:");	
		return inputString();
	}

	
	int inputInt(int min, int max) {
		while (true) {
			try {
				System.out.print("\nint between " + min + " and " + max + ": ");
				
				int n = Integer.parseInt(reader.readLine());
				
				if (n < min || n > max) {
			        System.out.println("out of bounds");
			        continue;
				}
				
				return n;
			} catch (NumberFormatException e) {
		        System.out.println("Not an integer");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	float inputFloat(float min, float max) {
		while (true) {
			try {
				System.out.print("\nfloat between " + min + " and " + max + ": ");
				float n = Float.parseFloat(reader.readLine());
				
				if (n < min || n >= max) {
			        System.out.println("out of bounds");
			        continue;
				}
				
				return n;
			} catch (NumberFormatException e) {
		        System.out.println("Not a float");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	boolean inputBoolean() {
		while (true) {
			
			System.out.print("\ntrue (t) or false (f): ");
			String inp;
			try {
				inp = reader.readLine();
				
				if (inp.equalsIgnoreCase("true") || inp.equalsIgnoreCase("t")) {
					return true;
				}
				
				if (inp.equalsIgnoreCase("false") || inp.equalsIgnoreCase("f")) {
					return false;
				}
				
		        System.out.println("Not a boolean. Type true, false, t or f");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	String inputString() {
		while (true) {
			System.out.print("\nstring: ");
			try {
				return reader.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
