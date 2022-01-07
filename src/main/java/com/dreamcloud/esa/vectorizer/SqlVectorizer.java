package com.dreamcloud.esa.vectorizer;

public class SqlVectorizer implements TextVectorizer {
    protected VectorBuilder builder;

    public SqlVectorizer(VectorBuilder builder) {
        this.builder = builder;
    }

    public ConceptVector vectorize(String text) throws Exception {
        return builder.build(text);
    }
}
