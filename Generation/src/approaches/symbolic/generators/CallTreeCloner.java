package approaches.symbolic.generators;

import approaches.ngram.facade.GramNode;
import approaches.random.Generator;
import approaches.symbolic.SymbolMapper;
import approaches.symbolic.nodes.*;
import compiler.Compiler;
import game.Game;
import game.functions.dim.DimConstant;
import grammar.Grammar;
import main.grammar.*;
import main.options.UserSelections;
import parser.Parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class CallTreeCloner {
    public static GameNode cloneCallTree(Call root, SymbolMapper symbolMapper) {
        assert root.type() == Call.CallType.Class;
        return (GameNode) cloneCallTree(root, List.of(new GameNode(root.symbol())), symbolMapper);
    }

    public static GeneratorNode cloneCallTree(Call call, List<GeneratorNode> options, SymbolMapper symbolMapper) {
        GeneratorNode node = switch (call.type()) {
            case Array -> {
                int nesting = getNesting(call, 0);
                List<Symbol> nestedSymbols = new ArrayList<>();
                getNestedSymbols(call, nestedSymbols);

                optionLoop:
                for (GeneratorNode option : options) {
                    // TODO why is this printing so often? Check if it's a bug
                    if (option.symbol().nesting() != nesting && !(option instanceof EmptyNode)) {
                        //System.out.println(option.symbol() + ": " + option.symbol().nesting() + " incompatible with " + nesting);
                        continue;
                    }

                    for (Symbol nestedSymbol : nestedSymbols) {
                        assert nestedSymbol != null;

                        if (!option.symbol().compatibleWith(nestedSymbol))
                            continue optionLoop;
                    }

                    yield option;
                }
                System.out.println("Could not find a compatible array type " + call.symbol() + " with nesting " + nesting + " and symbols " + nestedSymbols);
                System.out.println("Options: " + options);
                throw new RuntimeException("Could not find a compatible array type ");
            }

            case Class -> {
                for (GeneratorNode option : options) {
                    if (option.symbol().matches(call.symbol())) {
                        yield option;
                    }
                }

                throw new RuntimeException("Could not find a compatible class " + call.symbol());
            }

            case Terminal -> {
                for (GeneratorNode option : options) {
                    if (option.symbol().matches(call.symbol())) {

                        if (option instanceof PrimitiveNode)
                            ((PrimitiveNode) option).setValue(call.object());

                        yield option;
                    }
                }

                throw new RuntimeException("Could not find a compatible terminal class " + call.symbol());
            }

            case Null -> {
                for (GeneratorNode option : options) {
                    if (option instanceof EmptyNode)
                        yield option;
                }

                throw new RuntimeException("null is not an option");
            }
        };

        for (Call childCall : call.args()) {
            assert !(node instanceof EmptyNode);
            //System.out.println(node.symbol() + ": " + node.parameterSet().stream().map(GeneratorNode::symbol).toList());
            //System.out.println("options: " + node.nextPossibleParameters(symbolMapper));
            GeneratorNode child = cloneCallTree(childCall, node.nextPossibleParameters(symbolMapper), symbolMapper);
            node.addParameter(child);
        }

        if (call.type() == Call.CallType.Array || call.type() == Call.CallType.Class) {
            GeneratorNode endNode = node.nextPossibleParameters(symbolMapper).stream().filter(n -> n instanceof EndOfClauseNode).findFirst().orElseThrow();
            node.addParameter(endNode);
        }

        if (!node.isComplete())
            System.out.println(node.symbol().path() + " " + call.object());
        assert node.isComplete();

        return node;
    }

    private static int getNesting(Call call, int nesting) {
        if (call.type() != Call.CallType.Array)
            return nesting;

        if (call.args().size() == 0)
            return nesting + 1;

        return getNesting(call.args().get(0), nesting + 1);
    }

    private static void getNestedSymbols(Call call, List<Symbol> nestedSymbols) {
        if (call.type() != Call.CallType.Array)
            nestedSymbols.add(call.symbol());
        else
            call.args().forEach(child -> getNestedSymbols(child, nestedSymbols));
    }

    static void testLudiiLibrary() throws IOException {
        List<Symbol> symbols = Grammar.grammar().symbols().stream().filter(s -> s.usedInGrammar() || s.usedInDescription() || !s.usedInMetadata()).toList();
        SymbolMapper symbolMapper = new SymbolMapper(symbols);

        String gamesRoot = "../Common/res/lud/board";
        List<Path> paths = Files.walk(Paths.get(gamesRoot)).filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".lud")).limit(2000).toList();
        int count = 0;
        int preCompilation = 0;
        int clone = 0;
        int compile = 0;
        int recompile = 0;
        for (Path path : paths) {
            String gameStr = Files.readString(path);

            if (gameStr.contains("match"))
                continue;

            System.out.println("Loading " + path.getFileName() + " (" + (count + 1) + " of " + paths.size() + " games)");

            Description description = new Description(gameStr);

            final UserSelections userSelections = new UserSelections(new ArrayList<>());
            final Report report = new Report();

            final long startPreCompilation = System.currentTimeMillis();
            try {
                Compiler.compile(description, userSelections, report, false);
            } catch (Exception e) {
                System.out.println("Could not pre-compile " + path.getFileName());
                continue;
            }
            final long endPreCompilation = System.currentTimeMillis();
            //System.out.println("Old compile: " + (endPreCompilation - startPreCompilation) + "ms");

            //Playground.printCallTree(originalGame.description().callTree(), 0);

            GameNode rootNode;
            try {
                rootNode = cloneCallTree(description.callTree(), symbolMapper);
            } catch (Exception e) {
                System.out.println("Could not clone " + path.getFileName());
                continue;
            }
            final long endClone = System.currentTimeMillis();
            //System.out.println("Clone: " + (endClone - endPreCompilation) + "ms");

            try {
                rootNode.compile();
            } catch (Exception e) {
                System.out.println("Could not compile " + path.getFileName());
                continue;
            }
            final long endCompile = System.currentTimeMillis();
            //System.out.println("My Compile: " + (endCompile - endClone) + "ms");

            try {
                rootNode.rulesNode().clearCompilerCache();
                rootNode.compile();
            } catch (Exception e) {
                System.out.println("Could not recompile " + path.getFileName());
                continue;
            }
            final long endRecompile = System.currentTimeMillis();
            //System.out.println("My Recompile: " + (endRecompile - endCompile) + "ms");

            count += 1;
            preCompilation += endPreCompilation - startPreCompilation;
            clone += endClone - endPreCompilation;
            compile += endCompile - endClone;
            recompile += endRecompile - endCompile;
        }

        System.out.println("Games: " + count);
        System.out.println("Pre-compilation: " + preCompilation + "ms");
        System.out.println("Clone: " + clone + "ms");
        System.out.println("Compile: " + compile + "ms");
        System.out.println("Recompile: " + recompile + "ms");
    }

    static void testHex() {
        List<Symbol> symbols = Grammar.grammar().symbols().stream().filter(s -> s.usedInGrammar() || s.usedInDescription() || !s.usedInMetadata()).toList();

        SymbolMapper symbolMapper = new SymbolMapper(symbols);

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

        GameNode rootNode = cloneCallTree(description.callTree(), symbolMapper);

        final long endClone = System.currentTimeMillis();

        Game game1 = rootNode.compile();

        final long endCompile = System.currentTimeMillis();

        //rootNode.equipmentNode().clearCompilerCache();
        rootNode.rulesNode().clearCompilerCache();

        Game game = rootNode.compile();

        final long endRecompile = System.currentTimeMillis();

        System.out.println("\nPreCompilation time: " + (endPreCompilation - startPreCompilation) + "ms");
        System.out.println("Clone time: " + (endClone - endPreCompilation) + "ms");
        System.out.println("Compile time: " + (endCompile - endClone) + "ms");
        System.out.println("Recompile time: " + (endRecompile - endCompile) + "ms");

        System.out.println("\nGAME: " + rootNode);

        System.out.println("\nhasMissingRequirement? " + game.hasMissingRequirement());
        System.out.println("Will it crash? " + game.willCrash());

        System.out.println("Functional? " + Generator.isFunctional(game));
        System.out.println("isPlayable? " + Generator.isPlayable(game));
        System.out.println("isFunctionalAndWithOnlyDecision? " + Generator.isFunctionalAndWithOnlyDecision(game));
    }

    public static void main(String[] args) throws IOException {
        testLudiiLibrary();
    }
}
