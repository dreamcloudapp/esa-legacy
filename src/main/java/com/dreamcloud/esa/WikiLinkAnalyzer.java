package com.dreamcloud.esa;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.TypeTokenFilter;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.wikipedia.WikipediaTokenizer;

// copied from DutchAnalyzer, changed StandardTokenizer to WikipediaTokenizer
public final class WikiLinkAnalyzer extends Analyzer {

    /**
     * Contains the stopwords used with the StopFilter.
     */
    private final CharArraySet stoptable;

    public WikiLinkAnalyzer(CharArraySet stopwords) {
        this.stoptable = CharArraySet.unmodifiableSet(CharArraySet.copy(stopwords));
    }

    @Override
    protected Analyzer.TokenStreamComponents createComponents(String fieldName) {
        Set<String> untokenizedTypes = new HashSet<>();
        untokenizedTypes.add(WikipediaTokenizer.INTERNAL_LINK);
        Set<String> stopTypes = new HashSet<>();
        stopTypes.add(WikipediaTokenizer.EXTERNAL_LINK_URL);
        stopTypes.add(WikipediaTokenizer.EXTERNAL_LINK);
        stopTypes.add(WikipediaTokenizer.CITATION);
        final Tokenizer source = new WikipediaTokenizer(WikipediaTokenizer.UNTOKENIZED_ONLY, untokenizedTypes);
        TokenStream result = new TypeTokenFilter(source, stopTypes);
        return new Analyzer.TokenStreamComponents(source, result);
    }
}
