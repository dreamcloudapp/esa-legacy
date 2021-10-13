package com.dreamcloud.esa.analyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.TypeTokenFilter;
import org.apache.lucene.analysis.wikipedia.WikipediaTokenizer;

import java.util.HashSet;
import java.util.Set;

/**
 * The WikiAnalyzer is used when we want something fast and simplistic.
 * Used in the preprocessors before lemmatization so as not to modify the text unnecessarily so that Stanford can still understand it.
 */
public class WikiAnalyzer extends Analyzer {
    public WikiAnalyzer() {

    }

    protected Analyzer.TokenStreamComponents createComponents(String fieldName) {
        Set<String> stopTypes = new HashSet<>();
        stopTypes.add(WikipediaTokenizer.EXTERNAL_LINK_URL);
        stopTypes.add(WikipediaTokenizer.EXTERNAL_LINK);
        stopTypes.add(WikipediaTokenizer.CITATION);
        final Tokenizer source = new WikipediaTokenizer();
        TokenStream result = new TypeTokenFilter(source, stopTypes);
        return new Analyzer.TokenStreamComponents(source, result);
    }
}
