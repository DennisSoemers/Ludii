package approaches.symbolic.nodes;

import approaches.symbolic.SymbolMapper;
import main.grammar.Symbol;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ArrayNode extends GeneratorNode {
    ArrayNode(Symbol symbol, GeneratorNode parent) {
        super(symbol, parent);

        assert symbol.nesting() > 0;
    }

    Object instantiate() {
        List<Object> arguments = parameterSet.stream().filter(Objects::nonNull).map(GeneratorNode::compile).toList();

        Object array;
        if (symbol.nesting() == 1)
            array = Array.newInstance(symbol.cls(), arguments.size());
        else {
            array = Array.newInstance(arguments.get(0).getClass(), arguments.size());
        }

        for (int i = 0; i < arguments.size(); i++) {
            //System.out.println("Compiling: " + symbol + "-" + symbol.nesting() + " adding: " + arguments.get(i).getClass() + " vs " + array.getClass());
            Array.set(array, i, arguments.get(i));
        }

        return array;
    }

    public List<GeneratorNode> nextPossibleParameters(SymbolMapper symbolMapper) {
        if (!parameterSet.isEmpty() && parameterSet.get(parameterSet.size() - 1) instanceof EndOfClauseNode)
            return List.of();

        List<GeneratorNode> options = new ArrayList<>();
        if (symbol.nesting() == 1) {
            options.addAll(symbolMapper.getCompatibleSymbols(symbol).stream().map(s -> fromSymbol(s, this)).toList());
        } else {
            Symbol childSymbol = new Symbol(symbol);
            childSymbol.setNesting(symbol.nesting() - 1);
            options.add(new ArrayNode(childSymbol, this));
        }

        if (!parameterSet.isEmpty())
            options.add(new EndOfClauseNode(this));

        return options;
    }

    @Override
    public String toString() {
        return "{" + symbol.grammarLabel() + ": " + String.join(", ", parameterSet.stream().map(GeneratorNode::toString).toList()) + "}";
    }

    @Override
    public String buildDescription() {
        return "{" + String.join(" ", parameterSet.stream().map(GeneratorNode::buildDescription).toList()) + "}";
    }


}
