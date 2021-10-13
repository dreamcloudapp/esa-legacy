package com.dreamcloud.esa.vectorizer;

public interface TextVectorizer {
    ConceptVector vectorize(String text) throws Exception;
}
