package approaches.symbolic.generators.string;

import approaches.symbolic.SymbolMapper;
import approaches.symbolic.nodes.GameNode;
import approaches.symbolic.nodes.GeneratorNode;
import game.Game;
import other.GameLoader;

import java.util.*;

import static approaches.symbolic.generators.CallTreeCloner.cloneCallTree;

/**
 * StringGenerator is a class that processes a game description in string format,
 * tokenizes it and appends tokens to generate a game state.
 */
public class StringGenerator {
    final SymbolMapper symbolMapper;
    GenerationState generationState;

    public StringGenerator() {
        this(new SymbolMapper());
    }

    public StringGenerator(SymbolMapper symbolMapper) {
        this.symbolMapper = symbolMapper;
        generationState = new GenerationState();
    }

    public Map<String, GenerationState> filter(List<String> tokens) {
        Map<String, GenerationState> result = new HashMap<>();

        for (String token : tokens) {
            GenerationState newState = generationState.append(token);
            if (newState == null)
                continue;

            result.put(token, newState);
        }

        return result;
    }

    /**
     * Appends a token to the current state, updating the current state.
     *
     * @param token The token to be appended.
     * @return True if the token was successfully appended, false otherwise.
     */
    public boolean append(String token) {
        GenerationState newState = generationState.append(token);
        if (newState == null)
            return false;

        generationState = newState;
        return true;
    }


    /**
     * Updates the current state with a new state.
     *
     * @param newState The new state to be set.
     */
    public void append(GenerationState newState) {
        generationState = newState;
    }

    /**
     * GenerationState represents the state of the generator while processing tokens.
     * It maintains a list of GenerationPath objects that represent possible paths
     * through the tree of game elements based on the input tokens.
     */
    class GenerationState {
        // Sometimes multiple options match the same string. In such a case, the generation path is cloned and all options
        // are considered. Eventually, a string that is only compatible with one of the generation paths will be appended.
        public final List<GenerationPath> generationPaths;

        GenerationState() {
            generationPaths = List.of(new GenerationPath(symbolMapper));
        }

        GenerationState(List<GenerationPath> generationPaths) {
            this.generationPaths = Collections.unmodifiableList(generationPaths);
        }

        /**
         * Appends a token to the current state, generating a new state with updated generation paths.
         *
         * @param token The token to be appended.
         * @return A new GenerationState object with updated generation paths,
         *         or null if the token is not compatible with any generation path.
         */
        private GenerationState append(String token) {
            List<GenerationPath> newGenerationPaths = new ArrayList<>();

            for (GenerationPath generationPath : generationPaths) {
                List<GenerationPath> generationPathsForPath = generationPath.append(token);
                assert generationPathsForPath.stream().map(GenerationPath::toString).distinct().count() == generationPathsForPath.size();
                newGenerationPaths.addAll(generationPathsForPath);
            }

            if (newGenerationPaths.isEmpty())
                return null;

            return new GenerationState(newGenerationPaths);
        }
    }

    /* ------ Tests from here on ----- */

    /**
     * Splits a game description string into a list of tokens. So far only used for testing.
     *
     * @param gameDescription The game description string to be split.
     * @return A list of tokens generated from the game description.
     */
    public static List<String> splitGameDescription(String gameDescription) {
        // Preprocess the input string
        gameDescription = gameDescription.replaceAll("\\s+", " ");
        gameDescription = gameDescription.replaceAll("\\( ", "(");
        gameDescription = gameDescription.replaceAll("\\{ ", "{");
        gameDescription = gameDescription.replaceAll("\\(\\{ ", "( {");
        gameDescription = gameDescription.replaceAll("\\{\\( ", "{ (");

        // Initialize variables for tokenization
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        Random random = new Random();

        // Tokenize the input string
        for (int i = 0; i < gameDescription.length(); i++) {
            char c = gameDescription.charAt(i);
            if (c == ' ' || c == '(' || c == ')' || c == '[' || c == ']' || c == '{' || c == '}') {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current.setLength(0);
                }
                if (c == ' ' && (result.isEmpty() || result.get(result.size() - 1).equals(" "))) {
                    continue;
                }
                result.add(String.valueOf(c));
            } else {
                current.append(c);
                if (i < gameDescription.length() - 1 && random.nextBoolean()) {
                    result.add(current.toString());
                    current.setLength(0);
                }
            }
        }

        // Add the last token if there is any
        if (current.length() > 0) {
            result.add(current.toString());
        }

        return result;
    }

    static void descriptionTest() {
        SymbolMapper symbolMapper = new SymbolMapper();
        Game hex = GameLoader.loadGameFromName("Hex.lud");
        System.out.println(hex.description().gameOptions());
        GameNode rootNode = cloneCallTree(hex.description().callTree(), symbolMapper);
        String gameDescription = rootNode.buildDescription();
        System.out.println(gameDescription);
        List<String> tokens = splitGameDescription(gameDescription);
        System.out.println(tokens);
        StringGenerator generator = new StringGenerator();
        for (String token : tokens) {
            if (!generator.append(token)) {
                System.out.println("\nFailed to append token: " + token);
                break;
            }

            System.out.println("\nAppended token: " + token);
            System.out.println("Path count: " + generator.generationState.generationPaths.size());
            //System.out.println("Current paths: " + generator.state.generationPaths.stream().limit(100).toList());

        }
        System.out.println("Terminated on: " + generator.generationState.generationPaths.stream().map(p -> p.current).toList());
        System.out.println("Final result: " + generator.generationState.generationPaths.stream().map(p -> p.current.root().buildDescription()).toList());
        System.out.println("Expected: " + gameDescription);
        System.out.println("Identical? " + generator.generationState.generationPaths.get(0).current.buildDescription().equals(gameDescription));
    }

    static void randomTest() {
        List<String> tokens = List.of(" ", "(", ")", "[", "]", "{", "}", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".", ":", "\"");
        Random random = new Random(0);
        StringGenerator generator = new StringGenerator();
        StringBuilder tokenSequence = new StringBuilder();
        while (true) {
            Map<String, GenerationState> validTokens = generator.filter(tokens);
            if (validTokens.isEmpty()) {
                System.out.println("\nNo valid tokens left");
                break;
            }

            String token = validTokens.keySet().stream().skip(random.nextInt(validTokens.size())).findFirst().get();
            tokenSequence.append(token);
            generator.append(validTokens.get(token));
        }
        System.out.println("Token sequence:\n" + tokenSequence);

        if (generator.generationState.generationPaths.isEmpty()) {
            System.out.println("No generation paths");
            return;
        }

        if (generator.generationState.generationPaths.size() > 1) {
            System.out.println("WARNING: Multiple generation paths");
            generator.generationState.generationPaths.forEach(p -> System.out.println(p.current.root()));
        }

        GeneratorNode root = generator.generationState.generationPaths.get(0).current.root();
        System.out.println("Root:\n" + root);
        root.assertRecursivelyComplete();
        System.out.println("isRecursivelyComplete? " + root.isRecursivelyComplete());
        System.out.println("compiles? " + root.compile());

    }

    public static void main(String[] args) {
        randomTest();
        //descriptionTest();
    }
}
