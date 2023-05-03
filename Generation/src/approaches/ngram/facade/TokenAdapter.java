package approaches.ngram.facade;

import approaches.ngram.table.FrequencyTable;
import approaches.ngram.table.SimpleHashTable;
import main.grammar.Description;
import main.grammar.Report;
import main.grammar.Token;
import main.options.UserSelections;
import parser.Parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class TokenAdapter {
    final Pattern isInteger = Pattern.compile("-?\\d+");
    final Pattern isFloat = Pattern.compile("-?\\d*\\.\\d+");
    final Pattern isString = Pattern.compile("\".+\"");
    public final FrequencyTable verticalTable = new SimpleHashTable(3);
    HashMap<String, Integer> symbolToId = new HashMap<>();
    List<String> idToSymbol = new ArrayList<>();

    public TokenAdapter() {
        addSymbol("%start%");
        addSymbol("%end%");
        addSymbol("%array%");
        addSymbol("%int%");
        addSymbol("%float%");
        addSymbol("%string%");
        addSymbol("%unknown%");
    }

    GramNode buildGameTree(Token rootToken) {
        GramNode rootNode = new GramNode(toGram(rootToken));

        for (Token childToken: rootToken.arguments()) {
            buildGameTree(childToken, rootNode);
        }

        return rootNode;
    }

    private void buildGameTree(Token token, GramNode parent) {
        GramNode root = new GramNode(toGram(token), parent);
        for (Token childToken: token.arguments()) {
            buildGameTree(childToken, root);
        }
    }

    void incrementLudiiLibrary() throws IOException {
        String gamesRoot = "../Common/res/lud/board";
        Stream<Path> paths = Files.walk(Paths.get(gamesRoot)).filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".lud")).limit(2000);
        paths.forEach(path -> {

            try {

                final Description description = new Description(Files.readString(path));
                final UserSelections userSelections = new UserSelections(new ArrayList<String>());
                final Report report = new Report();

                Parser.expandAndParse(description, userSelections, report, false);
                //Game game = (Game) Compiler.compile(description, userSelections, report, false);
                Token rootToken = description.tokenForest().tokenTree();
                if (rootToken == null) {
                    System.out.println("Null token, skipped " + path);
                    return;
                }

                GramNode root = buildGameTree(description.tokenForest().tokenTree());
                root.recursivelyIncrementNgrams(verticalTable);
            } catch (Exception e) {
                System.out.println("\n\nSomething went wrong, skipped " + path);
                e.printStackTrace();
            }
        });
    }

    List<Double> evaluateChildSets(List<Token> ancestors, List<List<Token>> childSets) {
        GramNode rootNode = new GramNode(toGram(ancestors.get(0)));
        GramNode parent = rootNode;
        for (Token child: ancestors.subList(1, ancestors.size())) {
            parent = new GramNode(toGram(child), parent);
        }

        List<Double> evaluations = new ArrayList<>(childSets.size());
        for (List<Token> childSet: childSets) {
            for (Token token: childSet) {
                parent.stupidBackoffScore(toGram(token), verticalTable, 0.4);
            }
        }

        return evaluations;
    }

    int toGram(Token token) {
        String name = token.name();

        if (token.isArray())
            name = "%array%";
        else if (token.isTerminal()) {
            if (isInteger.matcher(name).matches())
                name = "%int%";
            else if (isFloat.matcher(name).matches())
                name = "%float%";
            else if (isString.matcher(name).matches()) {
                name = "%string%";
            }
        }

        Integer index = symbolToId.get(name);

        if (index == null)
            return addSymbol(name);

        return index;
    }

    int addSymbol(String symbol) {
        idToSymbol.add(symbol);
        symbolToId.put(symbol, idToSymbol.size());
        return idToSymbol.size();
    }

    @Override
    public String toString() {
        HashMap<List<Integer>, Integer> counts = verticalTable.dumpAllFrequencies();
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<List<Integer>, Integer> e : counts.entrySet()) {
            sb.append(e.getKey().stream().map(s -> idToSymbol.get(s)).toList());
            sb.append(": ").append(e.getValue());
            sb.append(", ");
        }

        return sb.toString();
    }

    public static void main(String[] args) throws IOException {
        /*
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

        final Description description = new Description(str);
        final UserSelections userSelections = new UserSelections(new ArrayList<String>());
        final Report report = new Report();

        Parser.expandAndParse(description, userSelections, report, false);
        Game game = (Game) Compiler.compileTest(description, false);

        //System.out.println("raw: " + description.raw());
        System.out.println("tokenForest: " + description.tokenForest().tokenTree());
        //System.out.println("parseTree: " + description.parseTree());
        //System.out.println("callTree: " + description.callTree());

        TokenAdapter tokenAdapter = new TokenAdapter();
        GramNode hexRoot = tokenAdapter.buildGameTree(description.tokenForest().tokenTree());
        */
        TokenAdapter tokenAdapter = new TokenAdapter();
        Path path = Paths.get("/Users/alex/Documents/Marble/tokengrams.txt");
        if (path.toFile().exists())
            tokenAdapter.verticalTable.importStream(Files.readAllLines(path).stream());
        else {
            tokenAdapter.incrementLudiiLibrary();
            Files.write(path, (Iterable<String>)tokenAdapter.verticalTable.exportStream()::iterator);
        }

        System.out.println("tokenAdapter: " + tokenAdapter.verticalTable.dumpAllFrequencies().size());
    }
}
