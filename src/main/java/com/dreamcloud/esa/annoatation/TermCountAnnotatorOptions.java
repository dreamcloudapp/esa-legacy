package com.dreamcloud.esa.annoatation;

import org.apache.lucene.analysis.Analyzer;

public class TermCountAnnotatorOptions {
    public Analyzer analyzer;
    public int minimumTerms = 0;
    public int maximumTerms = 0;
}
