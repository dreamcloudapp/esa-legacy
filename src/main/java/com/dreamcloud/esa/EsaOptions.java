package com.dreamcloud.esa;

import com.dreamcloud.esa.documentPreprocessor.DocumentPreprocessor;
import org.apache.lucene.analysis.Analyzer;

public class EsaOptions {
    public Analyzer analyzer;
    public DocumentType documentType;
    public DictionaryRepository dictionaryRepository;
    public StopWordRepository stopWordRepository;
    public DocumentPreprocessor preprocessor;
    public String indexPath;
    public int documentLimit;
}
