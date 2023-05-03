package approaches.ngram.table;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SimpleHashTable extends FrequencyTable {
    HashMap<String, Integer> frequencies = new HashMap<>();

    public SimpleHashTable(int maxN) {
        super(maxN);
    }

    @Override
    protected void incrementSingle(List<Integer> ngram, int amount) {
        frequencies.merge(ngramToKey(ngram), amount, Integer::sum);
    }

    @Override
    public int getFrequency(List<Integer> ngram) {
        return frequencies.getOrDefault(ngramToKey(ngram), 0);
    }

    private String ngramToKey(List<Integer> ngram) {
        StringBuilder sb = new StringBuilder();

        for (int n: ngram) {
            sb.append(n);
            sb.append('|');
        }

        return sb.toString();
    }

    public HashMap<List<Integer>, Integer> dumpAllFrequencies() {
        HashMap<List<Integer>, Integer> counts = new HashMap<>();
        for (String key: frequencies.keySet()) {
            List<Integer> ids = Arrays.stream(key.split("\\|")).filter(s -> !s.isEmpty()).map(Integer::parseInt).toList();
            if (ids.size() == maxN)
                counts.put(ids, frequencies.get(key));
        }
        return counts;
    }
    @Override
    public String toString() {
        return dumpAllFrequencies().toString();
    }

    public static void main(String[] args) {
        FrequencyTable table = new SimpleHashTable(3);

        table.incrementAll(Arrays.asList((int) 3, (int) 4, (int) 5));
        System.out.println(table);
        System.out.println(table.getFrequency(Arrays.asList((int) 3, (int) 4, (int) 5)));

        table.incrementAll(Arrays.asList((int) -32768, (int) 0, (int) 32767));
        System.out.println(table);
        System.out.println(table.getFrequency(Arrays.asList((int) -32768, (int) 0, (int) 32767)));

        table.incrementAll(Arrays.asList((int) -8, (int) 0, (int) 32767));
        System.out.println(table);
        System.out.println(table.getFrequency(Arrays.asList((int) 6)));

        table.incrementAll(Arrays.asList((int) 0, (int) 32767, (int) 8));
        System.out.println(table);
    }
}
