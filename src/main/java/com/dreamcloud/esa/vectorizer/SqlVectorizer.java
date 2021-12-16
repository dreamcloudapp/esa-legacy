package com.dreamcloud.esa.vectorizer;

import java.util.HashMap;
import java.util.Map;

public class SqlVectorizer implements TextVectorizer {
    protected VectorBuilder builder;
    protected Map<String, ConceptVector> conceptVectorCache;

    public SqlVectorizer(VectorBuilder builder) {
        this.builder = builder;
        this.conceptVectorCache = new HashMap<>();
    }

    public ConceptVector vectorize(String text) throws Exception {
        if (!this.conceptVectorCache.containsKey(text)) {
            ConceptVector vector = builder.build(text);
            this.conceptVectorCache.put(text, vector);
        }
        return this.conceptVectorCache.get(text);
    }
}
