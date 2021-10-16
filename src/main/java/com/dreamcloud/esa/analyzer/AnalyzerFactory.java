package com.dreamcloud.esa.analyzer;

import org.apache.lucene.analysis.Analyzer;

public interface AnalyzerFactory {
    Analyzer getAnalyzer();
}
