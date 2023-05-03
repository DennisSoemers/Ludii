package approaches.model;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import game.Game;
import main.grammar.Token;


/* LudiiTokens to NumericTokens */

public class Tokenizer {

	TokenizationParameters parameters;

	private ArrayList<String> previousStrings;
	private ArrayList<String> previousComponents;
	private List<Integer> tokens;
	private HashSet<String> componentNames;
	private HashSet<String> containerNames;


	public Tokenizer(TokenizationParameters parameters) {
		this.parameters = parameters;
	}

	public List<Integer> tokenizeGame(Game game) {
		
		
		previousStrings = new ArrayList<>();
		previousComponents = new ArrayList<>();
		tokens = new ArrayList<>();
		
		componentNames = (HashSet<String>) Arrays.stream(game.equipment().components()).map(p -> extractLabel(p.name())).collect(Collectors.toSet());
		containerNames = (HashSet<String>) Arrays.stream(game.equipment().containers()).map(p -> extractLabel(p.name())).collect(Collectors.toSet());
		
		tokenizeTree(game.description().tokenForest().tokenTrees(), tokens);

		return tokens;
	}

	private void tokenizeTree(Collection<Token> tree, List<Integer> tokens) {

		for (Token token : tree) {
			
			if (token.parameterLabel() != null) 
				tokens.add(tokenizeClause(token.parameterLabel()));
			
			if (token.isClass())
				tokens.add(TokenizationParameters.openClassToken);
			else if (token.isArray())
				tokens.add(TokenizationParameters.openArrayToken);

			String name = token.name();
			if (name != null) {
				tokenizeLudiiToken(name);
			}

			if (!token.isTerminal()) 
				tokenizeTree(token.arguments(), tokens);

			if (token.isClass())
				tokens.add(TokenizationParameters.closeClassToken);
			else if (token.isArray())
				tokens.add(TokenizationParameters.closeArrayToken);

		}
	}
	
	private void tokenizeLudiiToken(String name) {
		
		try {
			tokens.add(tokenizeSymbol(name));
			return;
		} catch (NullPointerException ignored) {}
		
		try {
			tokens.add(tokenizeInt(Integer.parseInt(name)));
			return;
		} catch (NumberFormatException ignored) {}

		try {
			int[] floatTokens = tokenizeFloat(Float.parseFloat(name));
			tokens.add(floatTokens[0]);
			tokens.add(floatTokens[1]);
			tokens.add(floatTokens[2]);
			return;
		} catch (NumberFormatException ignored) {}

		if (name.toLowerCase().equals("true"))
			tokens.add(tokenizeBoolean(true));
		else if (name.toLowerCase().equals("false"))
			tokens.add(tokenizeBoolean(false));
		
		// "1,E,N1,W"
		else if (name.matches("\\\"\\S*,\\S*\\\"")) {
			tokens.add(TokenizationParameters.stringedTokensDelimiter);
			
			String[] subTokens = name.substring(1, name.length()-1).split(",");
			for (String subToken: subTokens) {
				tokenizeLudiiToken(subToken);
			}
			
			tokens.add(TokenizationParameters.stringedTokensDelimiter);
		}
		
		// N4 or "B2"
		else if (name.matches("\\\"?[A-Z]{1,2}[0-9]*\\\"?")) {	
			String letters = name.replaceAll("[^A-Z]", "");
			String numbers = name.replaceAll("[^0-9]", "");
			
			//System.out.println(name + ", " + letters + ", " + numbers);

			
			boolean hasQuotes = name.charAt(0) == '"';

			if (hasQuotes)
				tokens.add(TokenizationParameters.stringedTokensDelimiter);
			
			
			tokens.add(tokenizeSymbol(letters.substring(0, 1)));
						
			if (letters.length() == 2) {
				tokens.add(tokens.size() - 1, TokenizationParameters.coordinateJoiner);
				tokens.add(tokenizeSymbol(letters.substring(1)));
			}
			
			if (numbers.length() > 0) {
				tokens.add(tokens.size() - 1, TokenizationParameters.coordinateJoiner);
				tokens.add(tokenizeInt(Integer.parseInt(numbers)));
			}
			
			if (hasQuotes)
				tokens.add(TokenizationParameters.stringedTokensDelimiter);
		}
		
		else if (name.charAt(0) == '"') {
			String label = extractLabel(name);
			int playerIndex = extractPlayerIndex(name);
			
			if (playerIndex >= parameters.maxPlayers)
				throw new RuntimeException("Too many players " + name);
			
			if (componentNames.contains(label))
				tokens.add(tokenizeComponent(label, playerIndex));
			
			else if (containerNames.contains(label))
				tokens.add(tokenizeContainer(label, playerIndex));
			
			else
				tokens.add(tokenizeString(label, playerIndex));
		}
		else
			throw new RuntimeException("Failed to tokenize " + name);
	}

	// Modified version of Arrays.binarySearch
	private int tokenizeInt(int n) {
		int low = 0;
        int high = parameters.ints.length - 1;
        
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = parameters.ints[mid];

            if (midVal < n)
                low = mid + 1;
            else if (midVal > n)
                high = mid - 1;
            else
                return parameters.intStart + mid; // key found
        }
        
        if (parameters.approximateContinuousValiables)
        	return parameters.intStart + low;
        			
		throw new RuntimeException(n + " int is not supported");
	}

	private int[] tokenizeFloat(float f) {
		int integerPart = (int) f;

		if (f < 0)
			integerPart--;
		
		int[] tokens = {TokenizationParameters.floatJoiner, tokenizeInt(integerPart), 0};
		
		double decimalPart = f - integerPart;
		//System.out.println("float: "+f + " int:" + integerPart + " decimalPart:" + decimalPart);
		
		int low = 0;
        int high = parameters.decimals.length - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            double midVal = parameters.decimals[mid];

            if (midVal < decimalPart)
                low = mid + 1;  // Neither val is NaN, thisVal is smaller
            else if (midVal > decimalPart)
                high = mid - 1; // Neither val is NaN, thisVal is larger
            else {
                long midBits = Double.doubleToLongBits(midVal);
                long keyBits = Double.doubleToLongBits(decimalPart);
                //System.out.println(midBits + " == " + keyBits);
                if (midBits == keyBits) {
                	tokens[2] = parameters.decimalStart + mid;
                	return tokens;
                }
                else if (midBits < keyBits) // (-0.0, 0.0) or (!NaN, NaN)
                    low = mid + 1;
                else                        // (0.0, -0.0) or (NaN, !NaN)
                    high = mid - 1;
            }
        }
        
        //System.out.println(parameters.decimals[low] + ", " + parameters.decimals[high]);
        
        double low_error = Math.abs(parameters.decimals[low] - decimalPart);
        double high_error = Math.abs(parameters.decimals[high] - decimalPart);
        int bestIndex = low;
        if (low_error > high_error) {
        	bestIndex = high;
        }
        
		if (parameters.approximateContinuousValiables || Math.abs(parameters.decimals[bestIndex] - decimalPart) <= TokenizationParameters.floatPrecision) {
			tokens[2] = parameters.decimalStart + bestIndex;
			return tokens;
		}
		
		throw new RuntimeException(f + " float is not supported");
	}

	private int tokenizeBoolean(boolean bool) {
		return parameters.booleanStart + (bool ? 1 : 0);
	}

	private int tokenizeComponent(String label, int playerIndex) {
		int nameIndex = previousComponents.indexOf(label);

		if (nameIndex < 0) {
			nameIndex = previousComponents.size();
			previousComponents.add(label);
		}

		int finaIndex = nameIndex * parameters.maxPlayers + playerIndex;

		if (finaIndex >= parameters.componentTokens)
			throw new RuntimeException("Too many unique components " + previousComponents.size());

		return parameters.componentStart + finaIndex;
	}
	
	private int tokenizeContainer(String label, int playerIndex) {
		int nameIndex = Arrays.binarySearch(parameters.containers, label);
		
		if (nameIndex < 0) {
			System.out.println(Arrays.toString(parameters.containers) + "|" + label + "|" + nameIndex);

			throw new RuntimeException("Not a known container " + label);
		}

		int finaIndex = nameIndex * parameters.maxPlayers + playerIndex;

		return parameters.containerStart + finaIndex;
	}

	// TODO understand this better. Can I really replace Track1, Track2, Track with A, A, A or A1, A2, A?
	private int tokenizeString(String label, int playerIndex) {

		int nameIndex = previousStrings.indexOf(label);

		if (nameIndex < 0) {
			nameIndex = previousStrings.size();
			previousStrings.add(label);
		}

		int finaIndex = nameIndex * parameters.maxPlayers + playerIndex;

		if (finaIndex >= parameters.stringTokens)
			throw new RuntimeException("Too many unique strings " + previousStrings.size());

		return parameters.stringStart + finaIndex;
	}
	
	private int tokenizeSymbol(String name) {
		return parameters.symbolStart + parameters.symbolToId.get(name);
	}
	
	private int tokenizeClause(String parameterLabel) {
		return parameters.clauseStart + parameters.clauseToId.get(parameterLabel);
	}
	
	private int extractPlayerIndex(String string) {
		if (string.indexOf(' ') >= 0 || string.matches(".*\\d.*\\d.*"))
			return 0;
		
		try {
			return Integer.parseInt(string.replaceAll("[^0-9]", ""));
		} catch (NumberFormatException ignored) {}
		return 0;
	}
	
	private String extractLabel(String string) {
		return string.replaceAll("\\d","").replace("\"", "");
	}

}
