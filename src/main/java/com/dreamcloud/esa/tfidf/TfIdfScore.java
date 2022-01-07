package com.dreamcloud.esa.tfidf;

public class TfIdfScore {
    protected int document;
    protected String term;
    protected double score;

    public TfIdfScore(int document, String term, double score) {
        this.document = document;
        this.term = term;
        this.score = score;
    }

    public TfIdfScore(String term, double score) {
        this(-1, term, score);
    }

    public double getScore() {
        return score;
    }

    public Integer getDocument() {
        return document;
    }

    public void setDocument(int document) {
        this.document = document;
    }

    public String getTerm() {
        return term;
    }

    public void normalizeScore(double lengthNorm) {
        this.score = score * lengthNorm;
    }
}
