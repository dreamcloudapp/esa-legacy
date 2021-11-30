package com.dreamcloud.esa.analyzer;

import com.dreamcloud.esa.*;
import org.apache.commons.cli.CommandLine;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.wikipedia.WikipediaTokenizer;

import java.util.HashSet;
import java.util.Set;

public class CommandLineAnalyzerFactory implements AnalyzerFactory {
    EsaOptions esaOptions;
    CommandLine cmd;


    public static boolean nonEmpty(String s) {
        return s != null && !s.equals("");
    }

    public CommandLineAnalyzerFactory(CommandLine cmd, EsaOptions esaOptions) {
        this.esaOptions = esaOptions;
        this.cmd = cmd;
    }

    public AnalyzerOptions getAnalyzerOptions() {
        AnalyzerOptions options = new AnalyzerOptions();
        options.stopWordsRepository = esaOptions.stopWordRepository;
        options.rareWordsRepository = esaOptions.rareWordRepository;
        options.dictionaryRepository = esaOptions.dictionaryRepository;

        String[] filters = cmd.getOptionValues("filter");
        if (filters != null) {
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
                    case "singular":
                        options.singularCaseFilter = true;
                    case "ascii":
                        options.asciiFoldingFilter = true;
                        break;
                }
            }
        }

        String stemmerDepth = cmd.getOptionValue("stemmer-depth");
        if (nonEmpty(stemmerDepth)) {
            options.porterStemmerFilter = true;
            options.porterStemmerFilterDepth = Integer.parseInt(stemmerDepth);
        }

        String minimumWordLength = cmd.getOptionValue("min-word-length");
        if (nonEmpty(minimumWordLength)) {
            options.minimumWordLength = Integer.parseInt(minimumWordLength);
        }

        switch(esaOptions.documentType) {
            case WIKI:
                //You don't get to customize these if you are coming from command line
                options.tokenizerFactory = new TokenizerFactory() {
                    public Tokenizer getTokenizer() {
                        return new WikipediaTokenizer();
                    }
                };
                Set<String> stopTokenTypes = new HashSet<>();
                stopTokenTypes.add(WikipediaTokenizer.EXTERNAL_LINK_URL);
                stopTokenTypes.add(WikipediaTokenizer.EXTERNAL_LINK);
                stopTokenTypes.add(WikipediaTokenizer.CITATION);
                options.stopTokenTypes = stopTokenTypes;
                break;
            case DREAM:
                options.tokenizerFactory = new TokenizerFactory() {
                    public Tokenizer getTokenizer() {
                        return new StandardTokenizer();
                    }
                };
                break;
        }
        return options;
    }

    public Analyzer getAnalyzer() {
        AnalyzerOptions options = this.getAnalyzerOptions();
        return new EsaAnalyzer(options);
    }
}
