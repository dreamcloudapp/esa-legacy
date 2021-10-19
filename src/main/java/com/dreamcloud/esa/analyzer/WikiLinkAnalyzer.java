package com.dreamcloud.esa.analyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.TypeTokenFilter;
import org.apache.lucene.analysis.wikipedia.WikipediaTokenizer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public final class WikiLinkAnalyzer extends EsaAnalyzer {

    public WikiLinkAnalyzer(AnalyzerOptions options) {
        super(options);
    }

    protected Analyzer.TokenStreamComponents createComponents(String fieldName) {
        Set<String> untokenizedTypes = new HashSet<>();
        untokenizedTypes.add(WikipediaTokenizer.INTERNAL_LINK);
        Set<String> stopTypes = new HashSet<>();
        stopTypes.add(WikipediaTokenizer.EXTERNAL_LINK_URL);
        stopTypes.add(WikipediaTokenizer.EXTERNAL_LINK);
        stopTypes.add(WikipediaTokenizer.CITATION);
        final Tokenizer source = new WikipediaTokenizer(WikipediaTokenizer.UNTOKENIZED_ONLY, untokenizedTypes);
        TokenStream result = new TypeTokenFilter(source, stopTypes);
        if (options.stopWordsRepository != null) {
            try {
                result = new StopFilter(result, options.stopWordsRepository.getStopWords());
            } catch (IOException e) {
                System.out.println("ESA warning: failed to load stop word dictionary; " + e.getMessage());
            }
        }
        return new Analyzer.TokenStreamComponents(source, result);
    }
}