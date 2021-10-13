package com.dreamcloud.esa.indexer;

import com.dreamcloud.esa.documentPreprocessor.DocumentPreprocessor;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.store.Directory;

public class IndexerOptions {
    public Directory indexDirectory;
    public int minimumTermCount = 0;
    public int maximumTermCount = 0;
    public Analyzer analyzer;
    public DocumentPreprocessor preprocessor;
    public int threadCount = 1;
    public int batchSize = 1;
    public int maximumDocumentCount = 512000;
}
