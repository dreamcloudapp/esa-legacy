package com.dreamcloud.esa.documentPreprocessor;

import com.dreamcloud.esa.EsaOptions;
import com.dreamcloud.esa.analyzer.EsaAnalyzer;
import com.dreamcloud.esa.analyzer.WikiAnalyzer;
import com.dreamcloud.esa.analyzer.WikiLinkAnalyzer;
import com.dreamcloud.esa.indexer.DreamIndexer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;

/**
 * Takes a formatted Wiki article and gets a plain text representation.
 */
public class WikiPreprocessor implements DocumentPreprocessor {
    public WikiPreprocessor() { }

    public String process(String document) throws IOException {
        StringBuilder analyzedText = new StringBuilder();
        Analyzer analyzer = new WikiAnalyzer();
        TokenStream tokenStream = analyzer.tokenStream(DreamIndexer.TEXT_FIELD, document);
        CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        while(tokenStream.incrementToken()) {
            analyzedText.append(termAttribute.toString()).append(" ");
        }
        tokenStream.close();
        return analyzedText.toString();
    }
}
