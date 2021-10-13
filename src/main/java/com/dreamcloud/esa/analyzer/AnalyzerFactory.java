package com.dreamcloud.esa.analyzer;

import com.dreamcloud.esa.*;
import org.apache.commons.cli.CommandLine;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.wikipedia.WikipediaTokenizer;

import java.util.HashSet;
import java.util.Set;

public class AnalyzerFactory {
    EsaOptions esaOptions;
    public AnalyzerFactory(EsaOptions esaOptions) {
        this.esaOptions = esaOptions;
    }

    public AnalyzerOptions getAnalyzerOptions(CommandLine cmd) {
        AnalyzerOptions options = new AnalyzerOptions();
        options.stopWordsRepository = esaOptions.stopWordRepository;
        options.dictionaryRepository = esaOptions.dictionaryRepository;

        String[] filters = cmd.getOptionValues("filter");
        for(String filter: filters) {
            switch(filter) {
                case "stemmer":
                    options.porterStemmerFilter = true;
                    break;
                case "classic":
                    options.classicFilter = true;
                    break;
                case "lower":
                    options.lowerCaseFilter = true;
                    break;
                case "ascii":
                    options.asciiFoldingFilter = true;
                    break;
            }
        }

        String stemmerDepth = cmd.getOptionValue("stemmer-depth");
        if (!"".equals(stemmerDepth)) {
            options.porterStemmerFilter = true;
            options.porterStemmerFilterDepth = Integer.parseInt(stemmerDepth);
        }

        switch(esaOptions.documentType) {
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

    public Analyzer getAnalyzer(CommandLine cmd) {
        AnalyzerOptions options = this.getAnalyzerOptions(cmd);
        return new EsaAnalyzer(options);
    }
}
