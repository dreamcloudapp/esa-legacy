package com.dreamcloud.esa.tfidf;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CollectionInfo {
    protected int numDocs;
    protected Map<String, Integer> documentFrequencies;

    public CollectionInfo(int numDocs) {
        this.numDocs = numDocs;
        this.documentFrequencies = new ConcurrentHashMap<>();
    }

    public CollectionInfo(int numDocs, Map<String, Integer> documentFrequencies) {
        this(numDocs);
        this.documentFrequencies = documentFrequencies;
    }

    public void addDocumentFrequency(String term, int documentFrequency) {
        this.documentFrequencies.put(term, documentFrequency);
    }

    public int getDocumentFrequency(String term) {
        return this.documentFrequencies.getOrDefault(term, 0);
    }

    public boolean hasDocumentFrequency(String term) {
        return this.documentFrequencies.containsKey(term);
    }
}
