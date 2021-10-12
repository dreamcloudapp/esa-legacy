package com.dreamcloud.esa;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.TypeTokenFilter;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.wikipedia.WikipediaTokenizer;

public final class WikiLemmaAnalyzer extends Analyzer {
    @Override
    protected Analyzer.TokenStreamComponents createComponents(String fieldName) {
        Set<String> stopTypes = new HashSet<>();
        stopTypes.add(WikipediaTokenizer.EXTERNAL_LINK_URL);
        stopTypes.add(WikipediaTokenizer.EXTERNAL_LINK);
        stopTypes.add(WikipediaTokenizer.CITATION);
        stopTypes.add(WikipediaTokenizer.HEADING);
        stopTypes.add(WikipediaTokenizer.SUB_HEADING);
        final Tokenizer source = new WikipediaTokenizer();
        TokenStream result = new TypeTokenFilter(source, stopTypes);
        result = new ASCIIFoldingFilter(source);
        return new Analyzer.TokenStreamComponents(source, result);
    }
}
