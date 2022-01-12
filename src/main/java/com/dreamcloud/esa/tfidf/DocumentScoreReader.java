package com.dreamcloud.esa.tfidf;

import java.io.IOException;
import java.util.Vector;

public interface DocumentScoreReader {
    public int getDocumentFrequency(String term) throws IOException;
    public void getTfIdfScores(String term, Vector<TfIdfScore> outVector) throws IOException;
    public void getTfIdfScores(String[] terms, Vector<TfIdfScore> outVector) throws IOException;
}
