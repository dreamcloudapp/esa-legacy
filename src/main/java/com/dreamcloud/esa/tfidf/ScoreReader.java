package com.dreamcloud.esa.tfidf;

import com.dreamcloud.esa.fs.DocumentScoreDataReader;
import com.dreamcloud.esa.fs.TermIndex;
import com.dreamcloud.esa.fs.TermIndexEntry;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Vector;

public class ScoreReader implements DocumentScoreReader {
    protected TermIndex termIndex;
    protected DocumentScoreDataReader scoreFileReader;

    public ScoreReader(TermIndex termIndex, DocumentScoreDataReader scoreFileReader) {
        this.termIndex = termIndex;
        this.scoreFileReader = scoreFileReader;
    }

    public int getDocumentFrequency(String term) {
        TermIndexEntry entry = this.termIndex.getEntry(term);
        if (entry != null) {
            return entry.documentFrequency;
        } else {
            return 0;
        }
    }

    public void getTfIdfScores(String term, Vector<TfIdfScore> outVector) throws IOException {
        TermIndexEntry entry = termIndex.getEntry(term);
        if (entry != null) {
            ByteBuffer byteBuffer = scoreFileReader.readScores(entry.offset, entry.numScores);
            for (int scoreIdx = 0; scoreIdx < entry.numScores; scoreIdx++) {
                int doc = byteBuffer.getInt();
                float score = byteBuffer.getFloat();
                outVector.add(new TfIdfScore(doc, term, score));
            }
        }
    }

    public void getTfIdfScores(String[] terms, Vector<TfIdfScore> outVector) throws IOException {
        for (String term: terms) {
            getTfIdfScores(term, outVector);
        }
    }
}
