package be.vanoosten.esa;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;

public class AnalyzerFactory {
    public static Analyzer getAnalyzer() {
        CharArraySet stopWords = EnwikiFactory.getExtendedStopWords();
        return new WikiAnalyzer(stopWords);
    }

    public static Analyzer getVectorizingAnalyzer() {
        CharArraySet stopWords = EnwikiFactory.getExtendedStopWords();
        stopWords.add("dream");
        return new WikiAnalyzer(stopWords);
    }

    public static Analyzer getLinkAnalyzer() {
        CharArraySet stopWords = EnwikiFactory.getExtendedStopWords();
        return new WikiLinkAnalyzer(stopWords);
    }

    public static Analyzer getLemmaAnalyzer() {
        return new WikiLemmaAnalyzer();
    }

    public static Analyzer getPostLemmaAnalyzer() {
        CharArraySet stopWords = EnwikiFactory.getExtendedStopWords();
        return new WikiPostLemmaAnalyzer(stopWords);
    }

    public static Analyzer getDreamAnalyzer() {
        CharArraySet stopWords = EnwikiFactory.getExtendedStopWords();
        stopWords.add("dream");
        return new DreamAnalyzer(stopWords);
    }

    public static Analyzer getDreamPostLemmaAnalyzer() {
        CharArraySet stopWords = EnwikiFactory.getExtendedStopWords();
        stopWords.add("dream");
        return new DreamPostLemmaAnalyzer(stopWords);
    }


}
