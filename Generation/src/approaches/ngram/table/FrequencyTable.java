package approaches.ngram.table;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public abstract class FrequencyTable {
    public final int maxN;
    protected int total;

    protected FrequencyTable(int maxN) {
        this.maxN = maxN;
    }

    public void incrementAll(List<Integer> ngram) {
        if (ngram.size() != maxN) {
            throw new IllegalArgumentException("ngram must be of size maxN");
        }

        total++;
        incrementRecursively(ngram, 1);
    }
    private void incrementRecursively(List<Integer> ngram, int amount) {
        incrementSingle(ngram, amount);

        if (ngram.size() > 1) {
            incrementRecursively(ngram.subList(1, ngram.size()), amount);
        }
    }
    protected abstract void incrementSingle(List<Integer> ngram, int amount);
    public abstract int getFrequency(List<Integer> ngram);
    public int getCorpusSize() {
        return total;
    }

    public abstract HashMap<List<Integer>, Integer> dumpAllFrequencies();
    public double stupidBackoffScore(List<Integer> ngram, double discount) {
        if (ngram.size() == 1) {
            return getFrequency(ngram) / (double) getCorpusSize();
        }

        int thisFrequency = getFrequency(ngram);
        if (thisFrequency == 0)
            return discount * stupidBackoffScore(ngram.subList(1, ngram.size()), discount);

        int parentFrequency = getFrequency(ngram.subList(0, ngram.size()-1));

        //System.out.println(thisFrequency + " / " + parentFrequency + " -> " + thisFrequency / (double) parentFrequency);

        return thisFrequency / (double) parentFrequency;
    }

    public Stream<String> exportStream() {
        return dumpAllFrequencies().entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).map(entry ->
            String.join(",", entry.getKey().stream().map(Object::toString).toList()) + ":" + entry.getValue().toString()
        );
    }

    public void importStream(Stream<String> stringStream) {
        stringStream.forEach(str -> {
            String[] args = str.split(":");
            List<Integer> ngram = Arrays.stream(args[0].split(",")).map(Integer::parseInt).toList();
            int frequency = Integer.parseInt(args[1]);
            total += frequency;
            incrementRecursively(ngram, frequency);
        });
    }
}
