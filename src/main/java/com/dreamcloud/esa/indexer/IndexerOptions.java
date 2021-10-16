package com.dreamcloud.esa.indexer;

import com.dreamcloud.esa.analyzer.CommandLineAnalyzerFactory;
import com.dreamcloud.esa.documentPreprocessor.DocumentPreprocessor;
import org.apache.lucene.store.Directory;

public class IndexerOptions {
    public Directory indexDirectory;
    public int minimumTermCount = 0;
    public int maximumTermCount = 0;
    public CommandLineAnalyzerFactory analyzerFactory;
    public DocumentPreprocessor preprocessor;
    public int threadCount = 1;
    public int batchSize = 1;
    public int maximumDocumentCount = 512000;

    public void displayInfo() {
        System.out.println("Indexing options:");
        System.out.println("---------------------------------------");
        System.out.println("Index Directory:\t" + indexDirectory.toString());
        System.out.println("Minimum Terms:\t\t" + minimumTermCount);
        System.out.println("Maximum Terms:\t\t" + maximumTermCount);
        System.out.println("Preprocessors:\t\t[" + (preprocessor != null ? preprocessor.getInfo() : "") + "]");
        System.out.println("Thread Count:\t\t" + threadCount);
        System.out.println("Batch Size:\t\t" + batchSize);
        System.out.println("Max Documents:\t\t" + maximumDocumentCount);
        System.out.println("---------------------------------------");
    }
}
