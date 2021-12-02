package com.dreamcloud.esa.database;

public class TermScore {
    public long docId;
    public float score;

    public TermScore(long docId, float score) {
        this.docId = docId;
        this.score = score;
    }
}
