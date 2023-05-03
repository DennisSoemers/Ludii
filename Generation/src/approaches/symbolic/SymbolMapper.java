package approaches.symbolic;

import grammar.Grammar;
import main.grammar.Clause;
import main.grammar.ClauseArg;
import main.grammar.Symbol;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SymbolMapper {
    public static final Symbol emptySymbol = new EmptySymbol();
    public static final Symbol endOfClauseSymbol = new EndOfClauseSymbol();

    // TODO do I need to add RegionConstant?
    private static final String[] primitives = {
            "java.lang.Integer", "game.functions.ints.IntConstant", "game.functions.dim.DimConstant",
            "java.lang.Float", "game.functions.floats.FloatConstant",
            "java.lang.String",
            "java.lang.Boolean", "game.functions.booleans.BooleanConstant"
    };
    private final Set<Symbol> symbols = new HashSet<>();
    private final Set<String> paths = new HashSet<>();
    private final Map<String, List<Symbol>> compatibilityMap = new HashMap<>();

    // To obtain every possible set of symbols which can be used to initialize another symbol, you would need replace
    // each base-symbol with it's corresponding source symbols and take their cartesian product. Unfortunately,
    // this is too intensive to pre-compute.
    // Maps symbols to every possible set of base-symbols (aka parameters) that can be used to initialize them.
    // eg game.util.graph.Graph can be initialized using [<Float>, null], [<Float>, <Integer>], [], or [<graph>]
    private final Map<String, List<List<Symbol>>> parameterMap = new HashMap<>();
    private final HashMap<String, Symbol> equivalenceMap = new HashMap<>();

    public SymbolMapper() {
        this(Grammar.grammar().symbols().stream().filter(s ->
                (s.usedInGrammar()  // Includes most symbols, including types
                || !s.usedInMetadata())  // Includes even more types and many constants
                && !s.path().equals("game.rules.start.set.SetStartSitesType.Phase")  // Excluded because its grammar label collides with game.rules.phase.Phase
                && !s.path().equals("game.rules.meta")  // Excluded because I don't think it's necessary
        ).toList());
    }

    public SymbolMapper(Collection<Symbol> symbols) {
        this.symbols.addAll(symbols);
        this.paths.addAll(symbols.stream().map(Symbol::path).toList());

        buildSymbolMap();
        buildCompatibilityMap();

        System.out.println(symbols.stream().map(Symbol::path).toList().contains("game.rules.start.set.SetStartSitesType.Phase"));
        System.out.println(compatibilityMap.values().stream().anyMatch(l -> l.stream().map(Symbol::path).toList().contains("game.rules.start.set.SetStartSitesType.Phase")));

        //parameterMap.get("game.functions.region.sites.Sites").stream().map(s -> s.path()).forEach(System.out::println);
        //compatibilityMap.get("game.functions.region.sites.around.Around").forEach(System.out::println);
    }

    public List<Symbol> nextPossibilities(Symbol parent, List<Symbol> partialArguments) {
        assert !partialArguments.contains(endOfClauseSymbol);
        Stream<List<Symbol>> parameterSets = parameterMap.get(parent.path()).stream();

        parameterSets = parameterSets.filter(completeArguments -> {
            if (partialArguments.size() >= completeArguments.size()) return false;

            for (int i = 0; i < partialArguments.size(); i++) {
                if (!completeArguments.get(i).compatibleWith(partialArguments.get(i))) return false;
            }

            return true;
        });


        Map<String, Symbol> possibilities = new HashMap<>();
        parameterSets.forEach(args -> {
            Symbol argSymbol = args.get(partialArguments.size());

            if (argSymbol.nesting() > 0) {
                possibilities.put(argSymbol.path() + "|" + argSymbol.nesting(), argSymbol);
            } else {
                for (Symbol symbol : compatibilityMap.get(argSymbol.path())) {
                    possibilities.put(symbol.path(), symbol);
                }
            }
        });

        assert possibilities.values().stream().map(Symbol::grammarLabel).distinct().count() == possibilities.size();

        return possibilities.values().stream().sorted(Comparator.comparing(Symbol::path)).toList();
    }

    static Symbol cloneSymbol(Symbol symbol, int nesting) {
        symbol = new Symbol(symbol);
        symbol.setNesting(nesting);
        return symbol;
    }

    public List<Symbol> getCompatibleSymbols(Symbol symbol) {
        return Collections.unmodifiableList(compatibilityMap.get(symbol.path()));
    }

    private void buildCompatibilityMap() {
        for (Symbol symbol : symbols) {
            compatibilityMap.put(symbol.path(), new ArrayList<>());
        }

        for (Symbol symbol : symbols) {
            for (Symbol other : symbols) {
                boolean isCompatible = symbol.compatibleWith(other);
                boolean isSubLudeme = other.ludemeType() == Symbol.LudemeType.SubLudeme;
                boolean isInitializable =
                        !Modifier.isAbstract(other.cls().getModifiers())
                        && !Modifier.isInterface(other.cls().getModifiers())
                        && (
                                other.cls().getConstructors().length > 0
                                || Arrays.stream(other.cls().getMethods()).anyMatch(m -> m.getName().equals("construct"))
                        );
                boolean isEnumValue = other.cls().isEnum() && !other.cls().getTypeName().equals(other.path());
                boolean isPrimitive = Arrays.stream(primitives).anyMatch(p -> p.equals(other.path()));
                boolean inGrammar = other.usedInGrammar();
                boolean hasRule = other.rule() != null;

                if (isCompatible && !isSubLudeme && (isInitializable || isEnumValue) && ((inGrammar && hasRule) || isPrimitive || isEnumValue)) {
                    compatibilityMap.get(symbol.path()).add(other);
                }
            }
        }

        compatibilityMap.put(emptySymbol.path(), List.of(emptySymbol));
        compatibilityMap.put(endOfClauseSymbol.path(), List.of(endOfClauseSymbol));
    }

    private void buildSymbolMap() {
        for (Symbol symbol : symbols) {
            List<List<Symbol>> parameterSets = new ArrayList<>(findParameterSets(symbol));
            parameterSets.sort(Comparator.comparing(List::toString));
            parameterMap.put(symbol.path(), parameterSets);
        }
    }

    private List<List<Symbol>> findParameterSets(Symbol symbol) {
        List<List<Symbol>> constructorSets = new ArrayList<>();

        if (symbol.isTerminal()) {
            //System.out.println("Symbol " + symbol.name() + " is terminal " + symbol.ludemeType());
            return constructorSets;
        }

        if (symbol.rule() == null) {
            //System.out.println("Symbol " + symbol.name() + " has no rule " + symbol.ludemeType() + " " + symbol.isAbstract());
            return constructorSets;
        }

        for (Clause clause : symbol.rule().rhs()) {
            if (clause.args() == null) {
                continue;
            }
            // Find flags
            // eg.                    *           *              *
            // orGroup:           [0, 1, 1, 0, 0, 2, 2, 2, 2, 2, 3, 3, 0, 0, 0]
            // argument.optional: [0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]
            // optionalFlags ->   [0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 1, 0, 1, 1, 1]
            // mandatoryFlags ->  [1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
            BitSet optionalFlags = new BitSet();
            BitSet mandatoryFlags = new BitSet();
            int nextOrGroup = 1;
            int nextAndGroup = 1;
            for (int i = 0; i < clause.args().size(); i++) {
                ClauseArg arg = clause.args().get(i);

                if (!arg.symbol().usedInGrammar()) System.out.println("Symbol " + arg.symbol().path() + " is not used in grammar");

                // TODO is if-else correct or should it just be a bunch of ifs?
                if (arg.orGroup() == nextOrGroup) {
                    optionalFlags.set(i, arg.optional());
                    mandatoryFlags.set(i, !arg.optional());
                    nextOrGroup++;
                } else if (arg.andGroup() == nextAndGroup) {
                    optionalFlags.set(i, arg.optional());
                    mandatoryFlags.set(i, !arg.optional());
                    nextAndGroup++;
                } else if (arg.andGroup() == 0 && arg.orGroup() == 0) {
                    optionalFlags.set(i, arg.optional());
                    mandatoryFlags.set(i, !arg.optional());
                }
            }

            //System.out.println("optional flags:\n" + IntStream.range(0, clause.args().size()).mapToObj(i -> optionalFlags.get(i)? 1:0).toList());
            //System.out.println("mandatory flags:\n" + IntStream.range(0, clause.args().size()).mapToObj(i -> mandatoryFlags.get(i)? 1:0).toList());

            // Permute optional flags
            // optionalIndexes: [3, 4, 5, 10, 12, 13, 14]
            // possibleSets: [[1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0], [1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0], ..., [1, 0, 0, 1, 1, 1, 0, 0, 0, 0, 1, 0, 1, 1, 1] ]
            List<BitSet> flagPermutations = permuteFlags(optionalFlags, mandatoryFlags);
            //System.out.println("permuted flags: ");
            //flagPermutations.forEach(set -> System.out.println(IntStream.range(0, clause.args().size()).mapToObj(i -> set.get(i)? 1:0).toList()));

            // Complete selected andGroups
            completeAndGroups(clause.args(), flagPermutations);

            // Shift selection in or groups
            // MUST BE for i as we are modifying the list
            List<BitSet> shiftedSets = new ArrayList<>();
            for (BitSet flagPermutation : flagPermutations) {
                //System.out.println("Shifting: "  + IntStream.range(0, clause.args().size()).mapToObj(i -> flagPermutation.get(i)? 1:0).toList());
                recursivelyShiftOrGroups(flagPermutation, 1, clause.args(), shiftedSets);
            }

            //System.out.println("shifted: ");
            //shiftedSets.forEach(set -> System.out.println(IntStream.range(0, clause.args().size()).mapToObj(i -> set.get(i)? 1:0).toList()));


            constructorSets.addAll(shiftedSets.stream().map(set -> {
                List<Symbol> clauseSymbols = new ArrayList<>(clause.args().size() + 1);
                for (int i = 0; i < clause.args().size(); i++) {
                    if (set.get(i)) {
                        clauseSymbols.add(cloneSymbol(clause.args().get(i).symbol(), clause.args().get(i).nesting()));
                    } else clauseSymbols.add(emptySymbol);
                }

                clauseSymbols.add(endOfClauseSymbol);
                return clauseSymbols;
            }).toList());
        }

        // filter for out-of-vocabulary symbols and duplicates
        Stream<List<Symbol>> parameterStream = constructorSets.stream().distinct();
        parameterStream = parameterStream.filter(l -> paths.containsAll(l.stream().filter(s -> s != emptySymbol && s != endOfClauseSymbol).map(Symbol::path).toList()));

        return parameterStream.toList();
    }

    private List<BitSet> permuteFlags(BitSet optionalFlags, BitSet mandatoryFlags) {
        List<BitSet> possibleSets = new ArrayList<>();

        int optionalParams = optionalFlags.cardinality();
        int[] optionalIndexes = IntStream.range(0, optionalFlags.length()).filter(optionalFlags::get).toArray();

        int initialPermutations = (int) Math.pow(2, optionalParams);
        for (int i = 0; i < initialPermutations; i++) {
            BitSet set = (BitSet) mandatoryFlags.clone();
            for (int j = 0; j < optionalIndexes.length; j++) {
                // Decide whether to set the optional parameter based on the jth digit of i in binary form
                set.set(optionalIndexes[j], ((i >> j) & 1) == 1);
            }

            possibleSets.add(set);
        }

        return possibleSets;
    }

    private void completeAndGroups(List<ClauseArg> clauseArgs, List<BitSet> possibleSets) {
        for (BitSet currentSet : possibleSets) {
            int currentAndGroup = 0;
            boolean active = false;
            for (int j = 0; j < clauseArgs.size(); j++) {
                ClauseArg arg = clauseArgs.get(j);

                if (arg.andGroup() == currentAndGroup && active) {
                    currentSet.set(j);
                }

                if (arg.andGroup() == currentAndGroup + 1) {
                    currentAndGroup++;
                    active = currentSet.get(j);
                }
            }
        }
    }

    private void recursivelyShiftOrGroups(BitSet currentSet, int currentOrGroup, List<ClauseArg> clauseArgs, List<BitSet> possibleSets) {
        int groupIndex = IntStream.range(0, clauseArgs.size()).filter(i -> clauseArgs.get(i).orGroup() == currentOrGroup).findFirst().orElse(-1);

        if (groupIndex == -1) {
            //System.out.println(IntStream.range(0, clauseArgs.size()).mapToObj(i -> currentSet.get(i)? 1:0).toList());
            possibleSets.add(currentSet);
            return;
        }

        if (!currentSet.get(groupIndex)) {
            recursivelyShiftOrGroups(currentSet, currentOrGroup + 1, clauseArgs, possibleSets);
            return;
        }

        for (int i = groupIndex; i < clauseArgs.size(); i++) {
            ClauseArg arg = clauseArgs.get(i);
            if (arg.orGroup() != currentOrGroup)
                break;

            BitSet newSet = (BitSet) currentSet.clone();
            newSet.set(groupIndex, false);
            newSet.set(i);
            recursivelyShiftOrGroups(newSet, currentOrGroup + 1, clauseArgs, possibleSets);
        }

    }

    static class EmptySymbol extends Symbol {
        private EmptySymbol() {
            super(null, "mapper.unused", null, SymbolMapper.class);
        }

        @Override
        public boolean compatibleWith(final Symbol other) {
            return other instanceof EmptySymbol;
        }

        @Override
        public Symbol returnType() {
            return this;
        }
    }

    static class EndOfClauseSymbol extends Symbol {
        private EndOfClauseSymbol() {
            super(null, "mapper.endOfClause", null, SymbolMapper.class);
        }

        @Override
        public boolean compatibleWith(final Symbol other) {
            return other instanceof EndOfClauseSymbol;
        }

        @Override
        public Symbol returnType() {
            return this;
        }
    }

    public static void main(String[] args) {

        System.out.println(Grammar.grammar().symbols().stream().filter(s -> !s.usedInGrammar() && s.usedInDescription()).toList());
        System.out.println(Grammar.grammar().symbols().stream().filter(s -> s.usedInGrammar() && !s.usedInDescription()).toList());
        System.out.println(Grammar.grammar().symbols().stream().filter(s -> !s.usedInGrammar() && !s.usedInMetadata()).toList());

//        SymbolMapper symbolMapper = new SymbolMapper();
//        System.out.println("Finished mapping symbols. Found " + symbolMapper.parameterMap.values().stream().mapToInt(List::size).sum() + " parameter sets.");
//
//        // TODO WHY is it used in grammar??
//        Symbol trackStep = Grammar.grammar().findSymbolByPath("game.util.equipment.TrackStep");
//        //Symbol trackStep = Grammar.grammar().findSymbolByPath("game.functions.trackStep.TrackStep");
//        System.out.println(trackStep);
//        System.out.println(trackStep.rule());
//        System.out.println(trackStep.usedInGrammar());
//        System.out.println(symbolMapper.nextPossibilities(trackStep, List.of()));

        // TODO, is int handled correctly? I don't think so.
//        Grammar.grammar().symbols().stream().max(Comparator.comparingInt(s -> s.rule() == null? 0:s.rule().rhs().size())).ifPresent(s -> System.out.println(s.path() + " " + s.rule().rhs()));
//        System.out.println(symbolMapper.parameterMap.get("int"));
//        System.out.println(symbolMapper.nextPossibilities(Grammar.grammar().findSymbolByPath("java.lang.Integer"), new ArrayList<>()));


//        ArrayList<Symbol> partialSymbols = new ArrayList<>();
//        partialSymbols.add(Grammar.grammar().findSymbolByPath("java.lang.String"));
//        partialSymbols.add(endOfClauseSymbol);
//        System.out.println(symbolMapper.nextPossibilities(Grammar.grammar().findSymbolByPath("game.Game"), partialSymbols));
    }
}
