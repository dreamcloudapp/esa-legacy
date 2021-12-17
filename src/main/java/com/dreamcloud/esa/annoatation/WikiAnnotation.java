package com.dreamcloud.esa.annoatation;

public class WikiAnnotation {
    public int incomingLinks = 0;
    public int outgoingLinks = 0;
    public int terms = 0;

    public WikiAnnotation(int incomingLinks, int outgoingLinks, int terms) {
        this.incomingLinks = incomingLinks;
        this.outgoingLinks = outgoingLinks;
        this.terms = terms;
    }
}
