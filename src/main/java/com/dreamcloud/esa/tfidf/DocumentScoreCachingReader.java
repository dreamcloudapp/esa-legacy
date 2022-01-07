package com.dreamcloud.esa.tfidf;

import org.apache.commons.collections15.map.FixedSizeMap;

import java.io.IOException;
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

    public int getDocumentFrequency(String term) throws IOException {
        return reader.getDocumentFrequency((term));
    }

    public void clear() {
        cache.clear();
    }

    public TfIdfScore[] getTfIdfScores(String term) throws IOException {
        int termHits = cacheHits.getOrDefault(term, 0) + 1;
        cacheHits.put(term, termHits);
        if (cache.containsKey(term)) {
            return cache.get(term);
        } else {
            TfIdfScore[] scores = reader.getTfIdfScores(term);
            if (cache.size() < capacity) {
                //cache the sucker
                cache.put(term, scores);
            } else {
                //we are at capacity, check the cache hits to see if we can replace anything
                int lowestHits = Integer.MAX_VALUE;
                String lowestHitsTerm = null;
                for (String hitsTerm: cacheHits.keySet()) {
                    int hits = cacheHits.get(hitsTerm);
                    if (hits < lowestHits) {
                        lowestHits = hits;
                        lowestHitsTerm = hitsTerm;
                    }
                }
                if (lowestHitsTerm != null && lowestHits < termHits) {
                    //replace the sucker
                    cache.remove(lowestHitsTerm);
                    cache.put(term, scores);
                }
            }
            return scores;
        }
    }

    public TfIdfScore[] getTfIdfScores(String[] terms) throws IOException {
        Vector<TfIdfScore> allScores = new Vector<>();
        for (String term: terms) {
            TfIdfScore[] scores = this.getTfIdfScores(term);
            allScores.addAll(Arrays.asList(scores));
        }
        return allScores.toArray(TfIdfScore[]::new);
    }
}
