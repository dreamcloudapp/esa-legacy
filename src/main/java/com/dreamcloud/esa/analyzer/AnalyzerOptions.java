package com.dreamcloud.esa.analyzer;

import com.dreamcloud.esa.DictionaryRepository;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.Tokenizer;

import java.util.Set;

/**
 * Tokenization options for the Lucene analyzers.
 */
public class AnalyzerOptions {
    public boolean porterStemmerFilter = false;
    public int porterStemmerFilterDepth = 1;
    public boolean lowerCaseFilter = false;
    public boolean classicFilter = false;
    public boolean asciiFoldingFilter = false;

    public CharArraySet stopWords;
    public DictionaryRepository dictionaryRepository;
    public Set<String> stopTokenTypes;
    public Tokenizer tokenizer;

    public AnalyzerOptions() {}
}
