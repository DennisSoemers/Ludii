package approaches.symbolic.generators;

import approaches.random.Generator;
import approaches.symbolic.SymbolMapper;
import approaches.symbolic.nodes.*;
import compiler.Compiler;
import game.Game;
import game.functions.booleans.BooleanConstant;
import game.functions.floats.FloatConstant;
import game.functions.ints.IntConstant;
import grammar.Grammar;
import main.grammar.Description;
import main.grammar.Symbol;

import java.util.*;

public class RandomGenerator {
    final Random random;
    final SymbolMapper symbolMapper;
    public RandomGenerator(SymbolMapper symbolMapper) {
        this.symbolMapper = symbolMapper;
        random = new Random();
    }

    public RandomGenerator(SymbolMapper symbolMapper, long seed) {
        this.symbolMapper = symbolMapper;
        random = new Random(seed);
    }

    public void completeGame(GeneratorNode node, int maxDepth) {
        if (maxDepth == 0) {
            return;
        }

        // Propagate through tree until we find an incomplete node
        if (node.isComplete()) {
            node.parameterSet().forEach(p -> completeGame(p, maxDepth));
            return;
        }

        // TODO
        if (node instanceof PrimitiveNode) {
            ((PrimitiveNode) node).setValue(randomPrimitive((PrimitiveNode) node));
            return;
        }

        if (node instanceof EnumNode)
            return;


        //System.out.println("\nCompleting: " + node.symbol() + " " + node.symbol().ludemeType());

        while (!node.isComplete()) {
            List<GeneratorNode> options = node.nextPossibleParameters(symbolMapper);
            //System.out.println("Options: " + options.stream().map(GeneratorNode::symbol).map(Symbol::path).toList());
            GeneratorNode endNode = options.stream().filter(n -> n instanceof EndOfClauseNode).findFirst().orElse(null);
            if (endNode != null && random.nextBoolean()) {
                node.addParameter(endNode);
                continue;
            }

            GeneratorNode emptyNode = options.stream().filter(n -> n instanceof EmptyNode).findFirst().orElse(null);
            if (emptyNode != null && random.nextBoolean()) {
                node.addParameter(emptyNode);
                continue;
            }

            node.addParameter(options.get(random.nextInt(options.size())));
        }

        //System.out.println("Parameters: " + node.parameterSet());

        node.parameterSet().forEach(p -> completeGame(p, maxDepth - 1));
    }

    public Object randomPrimitive(PrimitiveNode primitiveNode) {
        switch (primitiveNode.symbol().path()) {
            case "game.functions.dim.DimConstant" -> {
                return random.nextInt(10);
            }
            case "java.lang.Integer" -> {
                return random.nextInt(-10, 10);
            }
            case "java.lang.String" -> {
                return UUID.randomUUID().toString();
            }
            case "java.lang.Float" -> {
                return random.nextFloat(10);
            }
            case "java.lang.Boolean" -> {
                return random.nextBoolean();
            }
            case "game.functions.ints.IntConstant" -> {
                return new IntConstant(random.nextInt(-10, 10));
            }
            case "game.functions.floats.FloatConstant" -> {
                return new FloatConstant(random.nextFloat(10));
            }
            case "game.functions.booleans.BooleanConstant" -> {
                return new BooleanConstant(random.nextBoolean());
            }
            default -> throw new RuntimeException("Unknown primitive type: " + primitiveNode.symbol().ludemeType());
        }
    }

    public List<GameNode> compilableCompletions(GameNode root, int completionCount, int maxDepth) {
        List<GameNode> completions = new ArrayList<>(completionCount);

        String gameStr = root.toString();

        for (int i = 0; i < completionCount; i++) {
            System.out.println("Completion " + i);
            GameNode clone = root.copyDown();
            assert clone.toString().equals(gameStr);

            completeGame(clone, maxDepth);
            if (clone.isRecursivelyComplete())
                completions.add(clone);

        }

        return completions;
    }

    public static void main(String[] args) {
        List<Symbol> symbols = Grammar.grammar().symbols().stream().filter(s -> s.usedInGrammar() || s.usedInDescription() || !s.usedInMetadata()).toList();
        SymbolMapper symbolMapper = new SymbolMapper(symbols);
        RandomGenerator randomGenerator = new RandomGenerator(symbolMapper, 0);

        String str =
                "(game \"Hex\" \n" +
                        "    (players 2) \n" +
                        "    (equipment { \n" +
                        "        (board (hex Diamond 11)) \n" +
                        "        (piece \"Marker\" Each)\n" +
                        "        (regions P1 {(sites Side NE) (sites Side SW) })\n" +
                        "        (regions P2 {(sites Side NW) (sites Side SE) })\n" +
                        "    }) \n" +
                        "    (rules \n" +
                        "        (play (move Add (to (sites Empty))))\n" +
                        "        (end (if (is Connected Mover) (result Mover Win))) \n" +
                        "    )\n" +
                        ")";

        final long startPreCompilation = System.currentTimeMillis();
        final Description description = new Description(str);
        Game originalGame = (Game) Compiler.compileTest(description, false);
        final long endPreCompilation = System.currentTimeMillis();

        GameNode rootNode = CallTreeCloner.cloneCallTree(description.callTree(), symbolMapper);

        final long endClone = System.currentTimeMillis();

        Game game1 = rootNode.compile();

        final long endCompile = System.currentTimeMillis();

        rootNode.rulesNode().find("play").find("move").clearParameters();

        int completionCount = 100;
        List<GameNode> completions = randomGenerator.compilableCompletions(rootNode, completionCount, 20);

        System.out.println("Complete: " + completions.size() / (double) completionCount * 100 + "%");

        int i = 0;
        int compilable = 0;
        int functional = 0;
        int playable = 0;
        int onlyDecisions = 0;
        for (GameNode completion : completions) {
            System.out.println("Compiling: " + (i++));
            try {
                Game game = completion.compile();
                compilable ++;
                if (Generator.isFunctional(game)) {
                    functional ++;
                    if (Generator.isFunctionalAndWithOnlyDecision(game)) {
                        onlyDecisions++;
                        if (Generator.isPlayable(game))
                            playable++;
                    }
                }
            } catch (Throwable ignored) {}

            // Release memory
            completion.clearCompilerCache();
        }

        System.out.println("Compilable: " + compilable / (double) completionCount * 100 + "%");
        System.out.println("Functional: " + functional / (double) completionCount * 100 + "%");
        System.out.println("Playable: " + playable / (double) completionCount * 100 + "%");
        System.out.println("Only decisions: " + onlyDecisions / (double) completionCount * 100 + "%");

        //Playground.printCallTree(description.callTree(), 0);


//        randomGenerator.completeGame(rootNode, 2);
//
//        System.out.println("\nGAME: " + rootNode.rulesNode().find("play").find("move").find("to"));
//
//        if (rootNode.isRecursivelyComplete())
//            System.out.println("Recursively complete!");
//        else {
//            System.out.println("Not recursively complete!");
//            return;
//        }
//
//        Game game = rootNode.compile();
//
//        final long endRecompile = System.currentTimeMillis();
//
//        System.out.println("\nPreCompilation time: " + (endPreCompilation - startPreCompilation) + "ms");
//        System.out.println("Clone time: " + (endClone - endPreCompilation) + "ms");
//        System.out.println("Compile time: " + (endCompile - endClone) + "ms");
//        System.out.println("Recompile time: " + (endRecompile - endCompile) + "ms");
//
//
//        System.out.println("\nhasMissingRequirement? " + game.hasMissingRequirement());
//        System.out.println("Will it crash? " + game.willCrash());
//
//        System.out.println("\nFunctional? " + Generator.isFunctional(game));
//        System.out.println("isPlayable? " + Generator.isPlayable(game));
//        System.out.println("isFunctionalAndWithOnlyDecision? " + Generator.isFunctionalAndWithOnlyDecision(game));
    }
}
