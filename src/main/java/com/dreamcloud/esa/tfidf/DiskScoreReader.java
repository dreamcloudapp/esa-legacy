package com.dreamcloud.esa.tfidf;

import com.dreamcloud.esa.fs.DocumentScoreFileReader;
import com.dreamcloud.esa.fs.TermIndex;
import com.dreamcloud.esa.fs.TermIndexEntry;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Vector;

public class DiskScoreReader implements DocumentScoreReader {
    protected TermIndex termIndex;
    protected DocumentScoreFileReader scoreFileReader;

    public DiskScoreReader(TermIndex termIndex, DocumentScoreFileReader scoreFileReader) {
        this.termIndex = termIndex;
        this.scoreFileReader = scoreFileReader;
    }

    public TfIdfScore[] getTfIdfScores(String term) throws IOException {
        TermIndexEntry entry = termIndex.getEntry(term);
        if (entry == null) {
            return new TfIdfScore[0];
        } else {
            byte[] scoreBytes = scoreFileReader.readScores(entry.offset, entry.numScores);
            ByteBuffer byteBuffer = ByteBuffer.wrap(scoreBytes);
            TfIdfScore[] scores = new TfIdfScore[entry.numScores];
            for (int scoreIdx = 0; scoreIdx < entry.numScores; scoreIdx++) {
                int doc = byteBuffer.getInt();
                float score = byteBuffer.getFloat();
                scores[scoreIdx] = new TfIdfScore(String.valueOf(doc), term, score);
            }
            return scores;
        }
    }

    public TfIdfScore[] getTfIdfScores(String[] terms) throws IOException {
        Vector<TfIdfScore> allScores = new Vector<>();
        for (String term: terms) {
            TfIdfScore[] scores = this.getTfIdfScores(term);
            allScores.addAll(Arrays.asList(scores));
        }
        return allScores.toArray(TfIdfScore[]::new);
    }
}
