package com.dreamcloud.esa.tfidf;

public interface DocumentScoreReader {
    public TfIdfScore[] getTfIdfScores(String term);
    public TfIdfScore[] getTfIdfScores(String[] term);
}
