package be.vanoosten.esa;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;

public class AnalyzerFactory {
    public static Analyzer getAnalyzer() {
        CharArraySet stopWords = EnwikiFactory.getExtendedStopWords();
        return new WikiAnalyzer(stopWords, false);
    }

    public static Analyzer getVectorizingAnalyzer() {
        CharArraySet stopWords = EnwikiFactory.getExtendedStopWords();
        stopWords.add("dream");
        return new WikiAnalyzer(stopWords, false);
    }

    public static Analyzer getLinkAnalyzer() {
        CharArraySet stopWords = EnwikiFactory.getExtendedStopWords();
        return new WikiAnalyzer(stopWords, true);
    }

    public static Analyzer getDreamAnalyzer() {
        CharArraySet stopWords = EnwikiFactory.getExtendedStopWords();
        stopWords.add("dream");
        return new DreamAnalyzer(stopWords);
    }
}
