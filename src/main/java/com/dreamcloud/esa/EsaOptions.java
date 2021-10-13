package com.dreamcloud.esa;

import com.dreamcloud.esa.documentPreprocessor.DocumentPreprocessor;
import org.apache.lucene.analysis.Analyzer;

import java.nio.file.Path;

public class EsaOptions {
    public Analyzer analyzer;
    public DocumentType documentType;
    public DictionaryRepository dictionaryRepository;
    public StopWordRepository stopWordRepository;
    public DocumentPreprocessor preprocessor;
    public Path indexPath;
    public int documentLimit;
    public String indexFile;
}
