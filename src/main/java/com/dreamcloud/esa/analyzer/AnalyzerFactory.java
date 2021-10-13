package com.dreamcloud.esa.analyzer;

import com.dreamcloud.esa.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.wikipedia.WikipediaTokenizer;

import java.util.HashSet;
import java.util.Set;

public class AnalyzerFactory {
    DocumentType type;
    public AnalyzerFactory(DocumentType type) {
        this.type = type;
    }

    public AnalyzerOptions getAnalyzerOptions() {
        AnalyzerOptions options = new AnalyzerOptions();
        switch(type) {
            case WIKI:
                //You don't get to customize these if you are coming from command line
                options.tokenizer = new WikipediaTokenizer();
                Set<String> stopTokenTypes = new HashSet<>();
                stopTokenTypes.add(WikipediaTokenizer.EXTERNAL_LINK_URL);
                stopTokenTypes.add(WikipediaTokenizer.EXTERNAL_LINK);
                stopTokenTypes.add(WikipediaTokenizer.CITATION);
                options.stopTokenTypes = stopTokenTypes;
                break;
            case DREAM:
                options.tokenizer = new StandardTokenizer();
                break;
        }
        return options;
    }

    public Analyzer getAnalyzer() {
        AnalyzerOptions options = this.getAnalyzerOptions();
        return new EsaAnalyzer(options);
    }
}
