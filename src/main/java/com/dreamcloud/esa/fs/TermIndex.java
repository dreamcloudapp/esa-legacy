package com.dreamcloud.esa.fs;

import java.util.HashMap;
import java.util.Map;

public class TermIndex {
    Map<String, TermIndexEntry> termIndex = new HashMap<>();

    public TermIndex() {

    }

    public void addEntry(TermIndexEntry entry) {
        termIndex.put(entry.term, entry);
    }

    public TermIndexEntry getEntry(String term) {
        return termIndex.get(term);
    }
}
