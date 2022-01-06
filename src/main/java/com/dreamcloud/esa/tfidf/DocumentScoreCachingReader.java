package com.dreamcloud.esa.tfidf;

import org.apache.commons.collections15.map.FixedSizeMap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DocumentScoreCachingReader implements DocumentScoreReader {
    protected static int DEFAULT_CAPACITY = 2048;

    DocumentScoreReader reader;
    protected Map<String, TfIdfScore[]> cache;
    protected Map<String, Integer> cacheHits;
    protected int capacity;

    public DocumentScoreCachingReader(DocumentScoreReader reader, int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0.");
        }
        this.capacity = capacity;
        this.reader = reader;
        this.cache = new ConcurrentHashMap<>(capacity);
        this.cacheHits = new ConcurrentHashMap<>(capacity);
    }

    public DocumentScoreCachingReader(DocumentScoreReader reader) {
        this(reader, DEFAULT_CAPACITY);
    }

    public void clear() {
        cache.clear();
    }

    public TfIdfScore[] getTfIdfScores(String term) {
        cacheHits.put(term, cacheHits.getOrDefault(term, 0) + 1);
        if (cache.containsKey(term)) {
            return cache.get(term);
        } else {
            return reader.getTfIdfScores(term);
        }
    }

    public TfIdfScore[] getTfIdfScores(String[] terms) {
        Vector<TfIdfScore> allScores = new Vector<>();
        for (String term: terms) {
            TfIdfScore[] scores = this.getTfIdfScores(term);
            allScores.addAll(Arrays.asList(scores));
        }
        return allScores.toArray(TfIdfScore[]::new);
    }
}
