package com.dreamcloud.esa.fs;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TermIndex {
    protected int documentCount;
    protected Map<String, TermIndexEntry> termIndex = new HashMap<>();

    public TermIndex(int documentCount) {
        this.documentCount = documentCount;
    }

    public void addEntry(TermIndexEntry entry) {
        termIndex.put(entry.term, entry);
    }

    public TermIndexEntry getEntry(String term) {
        return termIndex.get(term);
    }

    public Set<String> getTerms() {
        return termIndex.keySet();
    }

    public int getDocumentCount() {
        return documentCount;
    }

    public Map<String, Integer> getDocumentFrequencies() {
        Map<String, Integer> documentFrequencies = new HashMap<>();
        for (TermIndexEntry entry: termIndex.values()) {
            documentFrequencies.put(entry.term, entry.documentFrequency);
        }
        return documentFrequencies;
    }
}
