package com.dreamcloud.esa.tfidf;

import java.io.IOException;

public interface DocumentScoreReader {
    public int getDocumentFrequency(String term) throws IOException;
    public TfIdfScore[] getTfIdfScores(String term) throws IOException;
    public TfIdfScore[] getTfIdfScores(String[] terms) throws IOException;
}
