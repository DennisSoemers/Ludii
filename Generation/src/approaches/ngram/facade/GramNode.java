package approaches.ngram.facade;

import approaches.ngram.table.FrequencyTable;

import java.util.*;

public class GramNode {
    public static final int startGram = 0;
    public static final int endGram = 1;
    private int gram;
    private GramNode parent;
    private List<GramNode> children = new LinkedList<>();

    // root node
    GramNode(int gram) {
        this.gram = gram;
    }

    // Normal node
    GramNode(int gram, GramNode parent) {
        this.gram = gram;
        this.parent = parent;
        parent.children.add(this);
    }

    void recursivelyIncrementNgrams(FrequencyTable verticalFrequencyTable) {
        verticalFrequencyTable.incrementAll(getVerticalNGram(verticalFrequencyTable.maxN));

        for (GramNode child : children) {
            child.recursivelyIncrementNgrams(verticalFrequencyTable);
        }

        if (children.size() == 0) {
            List<Integer> ngram = getVerticalNGram(verticalFrequencyTable.maxN - 1);
            ngram.add(endGram);
            verticalFrequencyTable.incrementAll(ngram);
        }
    }

    private List<Integer> getVerticalNGram(int n) {
        ArrayList<Integer> verticalNGrams = new ArrayList<>();
        getVerticalNGram(verticalNGrams, n);
        return verticalNGrams;
    }
    private void getVerticalNGram(List<Integer> verticalNGram, int n) {
        if (verticalNGram.size() >= n)
            return;

        verticalNGram.add(0, gram);

        if (parent != null) {
            parent.getVerticalNGram(verticalNGram, n);
        } else {
            for (int i=verticalNGram.size(); i < n; i++) {
                verticalNGram.add(0, startGram);
            }
        }
    }
    public double stupidBackoffScore(int childGram, FrequencyTable verticalFrequencyTable, double discount) {
        List<Integer> ngram = getVerticalNGram(verticalFrequencyTable.maxN - 1);
        ngram.add(childGram);
        return verticalFrequencyTable.stupidBackoffScore(ngram, discount);
    }

    public static void main(String[] args) {
        /*
        GramNode the = new GramNode("the");
        GramNode car = new GramNode("car", the);
        GramNode is = new GramNode("is", car);
        GramNode down = new GramNode("down", is);
        GramNode the2 = new GramNode("the", down);
        GramNode road = new GramNode("road", the2);

        GramNode up = new GramNode("up", is);
        GramNode the3 = new GramNode("the", up);
        GramNode road2 = new GramNode("road", the3);

        GramNode a = new GramNode("a", is);
        GramNode car2 = new GramNode("car", a);
        GramNode not = new GramNode("not", car2);
        GramNode a2 = new GramNode("a", not);
        GramNode truck = new GramNode("truck", a2);
        GramNode but = new GramNode("but", truck);
        GramNode a3 = new GramNode("a", but);
        GramNode car3 = new GramNode("car", a3);


        System.out.println(the.getVerticalNGram(4));
        System.out.println(is.getVerticalNGram(4));
        System.out.println(road.getVerticalNGram(4));

        FrequencyTable frequencyTable = new HashTableTrie(5);
        the.recursivelyIncrementNgrams(frequencyTable);

        System.out.println(frequencyTable);

        System.out.println(the2.getVerticalNGram(5) + " ...");
        System.out.println("road: " + the2.stupidBackoffScore("road", frequencyTable, 0.4));
        System.out.println("car: " + the2.stupidBackoffScore("car", frequencyTable, 0.4));
        System.out.println("a: " + the2.stupidBackoffScore("a", frequencyTable, 0.4));
        System.out.println("jam: " + the2.stupidBackoffScore("jam", frequencyTable, 0.4));
*/

    }
}
