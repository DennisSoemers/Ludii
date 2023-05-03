package approaches.ngram.table;

import java.util.*;

public class TreeMapTrie extends FrequencyTable {
    Node root = new Node();
    public TreeMapTrie(int maxN) {
        super(maxN);
    }
    @Override
    protected void incrementSingle(List<Integer> ngram, int amount) {
        root.increment(ngram, amount);
    }
    @Override
    public int getFrequency(List<Integer> ngram) {
        return root.getFrequency(ngram);
    }

    @Override
    public HashMap<List<Integer>, Integer> dumpAllFrequencies() {
        HashMap<List<Integer>, Integer> counts = new HashMap<>();
        root.getFrequencies(counts, new ArrayList<>());
        return counts;
    }

    @Override
    public String toString() {
        return dumpAllFrequencies().toString();
    }

    private class Node {
        int count;
        TreeMap<Integer, Node> children = new TreeMap<>();
        public Node() {
        }
        public Node(int gram, Node parent) {
            parent.children.put(gram, this);
        }

        void increment(List<Integer> ngram, int amount) {
            if (ngram.size() == 0) {
                count += amount;
                return;
            }

            Node child = children.get(ngram.get(0));
            if (child == null)
                child = new Node(ngram.get(0), this);

            child.increment(ngram.subList(1, ngram.size()), amount);
        }
        int getFrequency(List<Integer> ngram) {
            if (ngram.size() == 0)
                return count;

            Node child = children.get(ngram.get(0));
            if (child != null) {
                return child.getFrequency(ngram.subList(1, ngram.size()));
            }

            return 0;
        }

        void getFrequencies(HashMap<List<Integer>, Integer> counts, List<Integer> ngram) {
            if (ngram.size() == maxN) {
                counts.put(new ArrayList<>(ngram), count);
            }

            else if (children.size() > 0) {
                for (Map.Entry<Integer, Node> childEntry : children.entrySet()) {
                    ngram.add(childEntry.getKey());
                    childEntry.getValue().getFrequencies(counts, ngram);
                    ngram.remove(ngram.size()-1);
                }
            }
        }
    }
}
