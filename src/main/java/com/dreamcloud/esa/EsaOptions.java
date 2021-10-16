package com.dreamcloud.esa;

import com.dreamcloud.esa.documentPreprocessor.DocumentPreprocessor;
import org.apache.lucene.analysis.Analyzer;

import java.io.IOException;
import java.nio.file.Path;

public class EsaOptions {
    public DocumentType documentType;
    public Analyzer analyzer;
    public DictionaryRepository dictionaryRepository;
    public StopWordRepository stopWordRepository;
    public DocumentPreprocessor preprocessor;
    public Path indexPath;
    public int documentLimit;
    public String indexFile;

    public void displayInfo() throws IOException {
        System.out.println("ESA options:");
        System.out.println("---------------------------------------");
        System.out.println("Document Type\t" + documentType.label);
        System.out.println("Analyzer Class\t" + analyzer.getClass().getSimpleName());
        System.out.println("Stop Words\t" + (stopWordRepository == null ? "(no source)" : stopWordRepository.getStopWords().size()));
        System.out.println("Dictionary Words\t" + (dictionaryRepository == null ? "(no source)" : dictionaryRepository.getDictionaryWords().size()));
        System.out.println("Index Path\t" + indexPath.toString());
        System.out.println("Index File\t" + indexFile);
        System.out.println("Document Limit\t" + documentLimit);
        System.out.println("---------------------------------------");
    }
}
