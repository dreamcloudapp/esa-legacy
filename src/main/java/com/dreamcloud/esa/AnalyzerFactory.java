package com.dreamcloud.esa;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;

import java.io.IOException;

public class AnalyzerFactory {
    StopWordRepository stopWordRepository;

    public AnalyzerFactory(StopWordRepository stopWordRepository) {
        this.stopWordRepository = stopWordRepository;
    }

    public Analyzer getAnalyzer() throws IOException {
        return new WikiAnalyzer(this.stopWordRepository.getStopWords());
    }

    public Analyzer getVectorizingAnalyzer() throws IOException {
        return new WikiAnalyzer(this.stopWordRepository.getStopWords());
    }

    public Analyzer getLinkAnalyzer() throws IOException {
        return new WikiLinkAnalyzer(this.stopWordRepository.getStopWords());
    }

    public Analyzer getLemmaAnalyzer() {
        return new WikiLemmaAnalyzer();
    }

    public Analyzer getPostLemmaAnalyzer() throws IOException {
        return new WikiPostLemmaAnalyzer(this.stopWordRepository.getStopWords());
    }

    public Analyzer getDreamAnalyzer() throws IOException {
        return new DreamAnalyzer(this.stopWordRepository.getStopWords());
    }

    public Analyzer getDreamPostLemmaAnalyzer() throws IOException {
        return new DreamPostLemmaAnalyzer(this.stopWordRepository.getStopWords());
    }
}
