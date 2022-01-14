package com.dreamcloud.esa.tfidf;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CollectionInfo {
    private double averageDocumentLength;
    protected int numDocs;
    protected Map<String, Integer> documentFrequencies;

    public CollectionInfo(int numDocs, double averageDocumentLength, Map<String, Integer> documentFrequencies) {
        this.numDocs = numDocs;
        this.averageDocumentLength = averageDocumentLength;
        this.documentFrequencies = documentFrequencies;
    }

    public CollectionInfo(int numDocs, double averageDocumentLength) {
        this(numDocs, averageDocumentLength, new ConcurrentHashMap<>());
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

    public Map<String, Integer> getDocumentFrequencies() {
        return documentFrequencies;
    }

    public int getDocumentCount() {
        return this.numDocs;
    }

    public double getAverageDocumentLength() {
        return averageDocumentLength;
    }
}
