package com.dreamcloud.esa.analyzer;

import com.dreamcloud.esa.DictionaryRepository;
import com.dreamcloud.esa.StopWordRepository;
import org.apache.lucene.analysis.Tokenizer;

import java.util.Set;

/**
 * Tokenization options for the Lucene analyzers.
 */
public class AnalyzerOptions {
    public boolean porterStemmerFilter = false;
    public int porterStemmerFilterDepth = 1;
    public int minimumWordLength = 0;
    public boolean lowerCaseFilter = false;
    public boolean classicFilter = false;
    public boolean asciiFoldingFilter = false;
    public boolean singularCaseFilter = false;


    public StopWordRepository stopWordsRepository;
    public StopWordRepository rareWordsRepository;
    public DictionaryRepository dictionaryRepository;
    public Set<String> stopTokenTypes;
    public TokenizerFactory tokenizerFactory;

    public AnalyzerOptions() {}
}
