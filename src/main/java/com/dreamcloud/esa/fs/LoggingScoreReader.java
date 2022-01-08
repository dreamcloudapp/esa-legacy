package com.dreamcloud.esa.fs;

import com.dreamcloud.esa.tfidf.DocumentScoreReader;
import com.dreamcloud.esa.tfidf.TfIdfScore;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public class LoggingScoreReader implements DocumentScoreReader {
    private final DocumentScoreReader reader;
    AtomicLong timeTaken = new AtomicLong(0);
    AtomicLong termsRead = new AtomicLong(0);

    public LoggingScoreReader(DocumentScoreReader reader) {
        this.reader = reader;
    }

    public int getDocumentFrequency(String term) throws IOException {
        return reader.getDocumentFrequency(term);
    }

    public TfIdfScore[] getTfIdfScores(String term) throws IOException {
        long startTime = System.nanoTime();
        TfIdfScore[] scores = reader.getTfIdfScores(term);
        timeTaken.addAndGet(System.nanoTime() - startTime);
        termsRead.incrementAndGet();
        return scores;
    }

    public TfIdfScore[] getTfIdfScores(String[] terms) throws IOException {
        long startTime = System.nanoTime();
        TfIdfScore[] scores = reader.getTfIdfScores(terms);
        timeTaken.addAndGet(System.nanoTime() - startTime);
        termsRead.addAndGet(terms.length);
        return scores;
    }

    public double getTermsReadPerSecond() {
        return termsRead.get() / (timeTaken.get() / 1000000000.0d);
    }

    public long getTermsRead() {
        return termsRead.get();
    }
}
