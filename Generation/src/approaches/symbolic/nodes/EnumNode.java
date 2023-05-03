package approaches.symbolic.nodes;

import approaches.symbolic.SymbolMapper;
import main.grammar.Symbol;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class EnumNode extends GeneratorNode {
    EnumNode(Symbol symbol, GeneratorNode parent) {
        super(symbol, parent);
    }

    Object instantiate() {
        try {
            return symbol.cls().getMethod("valueOf", String.class).invoke(null, symbol.name());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to compile enum node: " + e);
        }
    }

    public List<GeneratorNode> nextPossibleParameters(SymbolMapper symbolMapper) {
        return List.of();
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public void addParameter(GeneratorNode param) {
        throw new RuntimeException("Enum nodes are terminal");
    }

    @Override
    public String toString() {
        return symbol.grammarLabel();
    }
}
