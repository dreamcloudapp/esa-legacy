package com.dreamcloud.esa.tfidf;

public class TfIdfScore {
    protected String document;
    protected String term;
    protected double score;

    public TfIdfScore(String document, String term, double score) {
        this.document = document;
        this.term = term;
        this.score = score;
    }

    public TfIdfScore(String term, double score) {
        this(null, term, score);
    }

    public double getScore() {
        return score;
    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    public String getTerm() {
        return term;
    }

    public void normalizeScore(double lengthNorm) {
        this.score = score * lengthNorm;
    }
}
