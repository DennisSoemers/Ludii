package approaches.symbolic.generators.string;

import approaches.symbolic.SymbolMapper;
import approaches.symbolic.nodes.*;
import game.util.graph.Face;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class GenerationPath {
    static final String naturalNumber = "(0|([1-9]\\d{0,9}))";
    static final Pattern integerPattern = Pattern.compile("-?" + naturalNumber);
    static final Pattern dimPattern = Pattern.compile(naturalNumber);
    static final Pattern floatPattern = Pattern.compile("-?" + naturalNumber + "\\." + naturalNumber);
    static final Pattern stringPattern = Pattern.compile("\"\\w[\\w\\s]{0,50}\\w\"");
    static final Pattern booleanPattern = Pattern.compile("true|false");

    static final Map<String, Set<String>> equivalenceFilters = Map.of(
            "game.functions.ints.IntConstant", Set.of("java.lang.Integer", "game.functions.dim.DimConstant"),
            "java.lang.Integer", Set.of("game.functions.dim.DimConstant"),
            "game.functions.floats.FloatConstant", Set.of("java.lang.Float"),
            "game.functions.booleans.BooleanConstant", Set.of("java.lang.Boolean")
    );

    final SymbolMapper symbolMapper;

    // Current leaf node being completed
    GeneratorNode current;
    // As strings are appended, the partial parameter is built up until it matches an option.
    String partialParameter = "";

    // The options and nulls lists are kept in sync. The nulls list keeps track of the number of nulls preceding each option.
    List<GeneratorNode> options;
    List<Integer> nulls;

    // Keeps track of whether the last token ended with a bracket. If so, enforce the required space between parameters.
    boolean closedByBracket = false;

    boolean gameComplete = false;

    public GenerationPath(SymbolMapper symbolMapper) {
        this.symbolMapper = symbolMapper;
        findOptions();
    }

    /**
     * Generates the next possible non-null options. Keeps track of the number of nulls preceding each option using the
     * nulls list. Note: it assumes the partialParameter string is null.
     */
    private void findOptions() {
        assert partialParameter.isEmpty();
        //System.out.println("Finding options for " + current);
        if (current == null) {
            options = List.of(new GameNode());
            nulls = List.of(0);
            return;
        }

        options = new ArrayList<>();
        options.addAll(filterParameters(current.nextPossibleParameters(symbolMapper)));
        // add a zero to the nulls list for each initial option
        nulls = new ArrayList<>(Collections.nCopies(options.size(), 0));


        List<GeneratorNode> emptyNodes = new ArrayList<>();
        while (true) {
            //System.out.println("Options - in progress: " + options);
            //System.out.println("Nulls - in progress: " + nulls);
            GeneratorNode emptyNode = options.stream().filter(node -> node instanceof EmptyNode).findFirst().orElse(null);
            if (emptyNode == null)
                break;
            int index = options.indexOf(emptyNode);
            nulls.remove(index);
            options.remove(index);
            emptyNodes.add(emptyNode);

            List<GeneratorNode> newOptions = filterParameters(current.nextPossibleParameters(symbolMapper, emptyNodes));
            options.addAll(newOptions);
            nulls.addAll(Collections.nCopies(newOptions.size(), emptyNodes.size()));
        }

        //System.out.println("Options: " + options);
        //System.out.println("Nulls: " + nulls);
    }

    public List<GeneratorNode> filterParameters(List<GeneratorNode> parameters) {
        Stream<GeneratorNode> filteredParameters = parameters.stream();

        for (GeneratorNode parameter : parameters) {
            Set<String> unnecessaryPaths = equivalenceFilters.get(parameter.symbol().path());

            if (unnecessaryPaths != null)
                filteredParameters = filteredParameters.filter(p -> !unnecessaryPaths.contains(p.symbol().path()));
        }

        return filteredParameters.toList();
    }

    /**
     * @param token the token to append
     * @return true if the token was successfully appended, false otherwise
     */
    public List<GenerationPath> append(String token) {
        if (token.length() == 0)
            throw new RuntimeException("Empty token");

        if (token.length() != 1 && (token.indexOf('(') != -1 || token.indexOf(')') != -1 || token.indexOf('{') != -1 || token.indexOf('}') != -1 || token.indexOf(' ') != -1))
            throw new RuntimeException("Multi-character Token contains a special character: " + token);

        if (gameComplete)
            return List.of();

        assert options != null;
        assert nulls != null;

        if (closedByBracket && !token.equals(")") && !token.equals("}")) {
            closedByBracket = false;
            if (token.equals(" "))
                return List.of(this);
            return List.of();
        }

        return switch (token) {
            case " " -> completeParameter();
            case ")", "}" -> completeCurrent(token);
            case "{" -> newList(token);
            default -> extendPartialParameter(token);
        };
    }

    // Completes a parameter and returns the new GenerationPath list
    private List<GenerationPath> completeParameter() {
        if (partialParameter.isEmpty())
            return List.of();

        List<GenerationPath> newPaths = new ArrayList<>();
        for (GeneratorNode option : options) {
            if (matches(partialParameter, option)) {
                GenerationPath path = this.copy();
                path.appendOption(option);
                //System.out.println("New Path Space: " + path);
                newPaths.add(path);
            }
        }
        return newPaths;
    }


    // Completes the current node and returns the new GenerationPath list
    private List<GenerationPath> completeCurrent(String token) {
        // Complete the current parameter if there wasn't a space preceding the bracket
        List<GenerationPath> possiblePaths = partialParameter.isEmpty()? List.of(this) : completeParameter();

        // Identify and complete valid generation paths
        List<GenerationPath> newPaths = new ArrayList<>();
        for (GenerationPath path : possiblePaths) {
            // TODO used to be an if. not sure what could trigger it
            assert path.partialParameter.isEmpty();

            // Verify that the current node can be closed
            GeneratorNode endNode = path.options.stream().filter(node -> node instanceof EndOfClauseNode).findFirst().orElse(null);
            if (endNode == null)
                continue;

            //Verify the bracket corresponds to the correct node type
            if (token.equals(")") && !(path.current instanceof ClassNode || path.current instanceof GameNode))
                continue;

            if (token.equals("}") && !(path.current instanceof ArrayNode))
                continue;

            path.closedByBracket = true;

            // Add the end of clause parameter and it's preceding nulls
            for (int i = 0; i < path.nulls.get(path.options.indexOf(endNode)); i++) {
                path.current.addParameter(new EmptyNode(path.current));
            }
            path.current.addParameter(endNode);

            // Complete current node, moving up the tree until a node is incomplete
            path.completeCurrent();

            //System.out.println("New Path Complete: " + path);

            // Add this path as a possible path
            newPaths.add(path);
        }

        return newPaths;
    }

    // Creates a new list of GenerationPath instances for ArrayNode options
    private List<GenerationPath> newList(String token) {
        // We can't start a new list if we already started building a parameter
        if (!partialParameter.isEmpty())
            return List.of();

        List<GenerationPath> possiblePaths = new ArrayList<>();
        for (GeneratorNode option : options) {
            if (option instanceof ArrayNode) {
                GenerationPath path = this.copy();
                path.appendOption(option);
                possiblePaths.add(path);
            }
        }
        return possiblePaths;
    }

    // Extends the partial parameter and filters out any options that are not consistent with the new partial parameter
    private List<GenerationPath> extendPartialParameter(String token) {
        // check if the new string is consistent with any of the options
        String newPartialParameter = partialParameter + token;
        //System.out.println("New partial parameter: " + newPartialParameter);
        for (GeneratorNode option : options) {
            if (!(option instanceof EndOfClauseNode) && consistent(newPartialParameter, option)) {
                GenerationPath path = this.copy();
                path.partialParameter = newPartialParameter;
                return List.of(path);
            }
        }

        return List.of();
    }

    private void completeCurrent() {
        assert current.isComplete();
        partialParameter = "";
        if (current.parent() == null) {
            gameComplete = true;
            return;
        }

        current = current.parent();
        //System.out.println("Moving up to: " + current);
        if (current.isComplete()) {
            completeCurrent();
        } else {
            findOptions();
        }
    }

    private void appendOption(GeneratorNode option) {
        if (option instanceof PrimitiveNode)
            ((PrimitiveNode) option).setUnparsedValue(partialParameter.replace("\"", ""));

        //System.out.println("Appending option: " + option);
        if (current == null)
            current = option;
        else {
            for (int i = 0; i < nulls.get(options.indexOf(option)); i++) {
                current.addParameter(new EmptyNode(current));
            }
            // Necessary in case the parent was cloned after the option was found
            option.setParent(current);
            current.addParameter(option);
            assert current.parameterSet().contains(option);
            assert current.parent() == null || current.parent().parameterSet().contains(current);
        }

        //System.out.println("Appended: " + current);

        if (!option.isComplete())
            current = option;

        partialParameter = "";
        findOptions();
    }

    public GenerationPath copy() {
        GenerationPath clone = new GenerationPath(symbolMapper);

        if (current != null) {
            //System.out.println("current: "+current);
            clone.current = current.copyUp();
        }

        clone.options = new ArrayList<>(options);
        clone.nulls = nulls != null ? new ArrayList<>(nulls) : null;
        clone.partialParameter = partialParameter;
        clone.closedByBracket = closedByBracket;
        return clone;
    }

    @Override
    public String toString() {
        return "{partialParameter: \"" + partialParameter + "\", current: " + current + ", options: " + options + "}";
    }

    /**
     * @param string the string to check
     * @param node the node to check against
     * @return true if the string matches the node or could match it if more characters where appended, false otherwise
     */
    private boolean consistent(String string, GeneratorNode node) {
        assert !(node instanceof EmptyNode) && !(node instanceof EndOfClauseNode);
        //System.out.println("Consistent? " + node + ", " + string);

        if (node instanceof PrimitiveNode) {
            Matcher matcher = primitiveMatcher((PrimitiveNode) node, string);
            return matcher.matches() || matcher.hitEnd();
        }

        String token = node.symbol().token();

        if (node instanceof ClassNode || node instanceof GameNode)
            token = '(' + token;

        if (node instanceof ArrayNode)
            token = '{' + token;

        return token.startsWith(string);
    }

    /**
     * @param newString the string to check
     * @param node the node to check against
     * @return true if the string entirely matches the node, false otherwise
     */
    private boolean matches(String newString, GeneratorNode node) {
        assert !(node instanceof EmptyNode);
        //System.out.println("Matching: " + node + ", " + newString);

        if (node instanceof EndOfClauseNode)
            return newString.strip().equals(")");

        if (node instanceof PrimitiveNode) {
            //System.out.println("Matching Primitive: " + node + ", " + newString + ", " + primitiveMatcher((PrimitiveNode) node, newString).matches());
            return primitiveMatcher((PrimitiveNode) node, newString).matches();
        }

        String token = node.symbol().token();

        if (!(node instanceof EnumNode))
            token = "(" + token;

        return token.equals(newString);
    }

    private Matcher primitiveMatcher(PrimitiveNode primitiveNode, String newString) {
        //System.out.println("Primitive node: " + primitiveNode.getType() + ", " + newString);
        switch (primitiveNode.getType()) {
            case INT -> {
                return integerPattern.matcher(newString);
            }
            case DIM -> {
                return dimPattern.matcher(newString);
            }
            case FLOAT -> {
                return floatPattern.matcher(newString);
            }
            case BOOLEAN -> {
                return booleanPattern.matcher(newString);
            }
            case STRING -> {
                return stringPattern.matcher(newString);
            }
            default -> {
                throw new RuntimeException("Unexpected primitive type: " + primitiveNode.symbol().path());
            }
        }
    }
}
