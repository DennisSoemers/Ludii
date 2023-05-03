package approaches.symbolic.nodes;

import approaches.symbolic.SymbolMapper;
import game.Game;
import game.equipment.Equipment;
import game.mode.Mode;
import game.players.Players;
import game.rules.Rules;
import grammar.Grammar;
import main.grammar.Symbol;

import java.util.ArrayList;
import java.util.List;

public class GameNode extends GeneratorNode {
    static final Symbol gameSymbol = Grammar.grammar().findSymbolByPath("game.Game");
    static Symbol nameSymbol = Grammar.grammar().findSymbolByPath("java.lang.String");
    static Symbol playersSymbol = Grammar.grammar().findSymbolByPath("game.players.Players");
    static Symbol equipmentSymbol = Grammar.grammar().findSymbolByPath("game.equipment.Equipment");
    static Symbol modeSymbol = Grammar.grammar().findSymbolByPath("game.mode.Mode");
    static Symbol rulesSymbol = Grammar.grammar().findSymbolByPath("game.rules.Rules");

    public GameNode(Symbol symbol) {
        super(symbol, null);
        assert symbol.path().equals("game.Game");
    }

    public GameNode() {
        super(gameSymbol, null);
    }

    @Override
    public Game compile() {
        if (compilerCache != null) return (Game) compilerCache;

        boolean skipEquipment = parameterSet.get(3).compilerCache != null;

        //System.out.println("Skipping equipment: " + skipEquipment);

        Game game = instantiate();

        if (skipEquipment) game.createGame();
        else game.create();

        //System.out.println("totalDefaultSites: " + game.equipment().totalDefaultSites());


        compilerCache = game;
        return (Game) compilerCache;
    }

    @Override
    Game instantiate() {
        return new Game((String) nameNode().compile(), (Players) playersNode().compile(), modeNode() != null ? (Mode) modeNode().compile() : null, (Equipment) equipmentNode().compile(), (Rules) rulesNode().compile());
    }

    @Override
    public List<GeneratorNode> nextPossibleParameters(SymbolMapper symbolMapper) {
        switch (parameterSet.size()) {
            case 0 -> {
                return List.of(new PrimitiveNode(nameSymbol, this));
            }
            case 1 -> {
                return List.of(new ClassNode(playersSymbol, this));
            }
            case 2 -> {
                ArrayList<GeneratorNode> options = new ArrayList<>(2);
                options.add(new EmptyNode(this));
                options.add(new ClassNode(modeSymbol, this));
                return options;
            }
            case 3 -> {
                return List.of(new ClassNode(equipmentSymbol, this));
            }
            case 4 -> {
                return List.of(new ClassNode(rulesSymbol, this));
            }
            case 5 -> {
                return List.of(new EndOfClauseNode(this));
            }
            default -> {
                return List.of();
            }
        }
    }

    @Override
    public void clearCompilerCache() {
        compilerCache = null;
    }

    @Override
    public String toString() {
        return "(" + symbol.grammarLabel() + ": " + String.join(", ", parameterSet.stream().map(GeneratorNode::toString).toList()) + ")";
    }

    @Override
    public String buildDescription() {
        return "(" + symbol.token() + " " + String.join(" ", parameterSet.stream().filter(s -> !(s instanceof EmptyNode || s instanceof EndOfClauseNode)).map(GeneratorNode::buildDescription).toList()) + ")";
    }

    @Override
    public GameNode copyDown() {
        GameNode clone = new GameNode(symbol);
        clone.parameterSet.addAll(parameterSet.stream().map(GeneratorNode::copyDown).toList());
        clone.complete = complete;
        clone.compilerCache = compilerCache;
        return clone;
    }

    @Override
    public GameNode copyUp() {
        GameNode clone = new GameNode(symbol);
        clone.parameterSet.addAll(parameterSet);
        clone.complete = complete;
        clone.compilerCache = compilerCache;
        return clone;
    }

    public GeneratorNode nameNode() {
        return parameterSet.get(0);
    }

    public GeneratorNode playersNode() {
        return parameterSet.get(1);
    }

    public GeneratorNode modeNode() {
        return parameterSet.get(2);
    }

    public GeneratorNode equipmentNode() {
        return parameterSet.get(3);
    }

    public GeneratorNode rulesNode() {
        return parameterSet.get(4);
    }

    @Override
    public void setParent(GeneratorNode parent) {
        throw new RuntimeException("Cannot set parent of GameNode");
    }
}
