package com.dreamcloud.esa.vectorizer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SqlVectorizer implements TextVectorizer {
    protected VectorBuilder builder;

    public SqlVectorizer(VectorBuilder builder) {
        this.builder = builder;
    }

    public ConceptVector vectorize(String text) throws Exception {
        return builder.build(text);
    }
}
