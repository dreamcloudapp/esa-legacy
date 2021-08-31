package be.vanoosten.esa;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.util.CharArraySet;

import static org.apache.lucene.util.Version.LUCENE_48;

public class AnalyzerFactory {
    public static Analyzer getAnalyzer() {
        CharArraySet stopWords = EnwikiFactory.getExtendedStopWords();
        return new WikiAnalyzer(LUCENE_48, stopWords, false);
    }

    public static Analyzer getVectorizingAnalyzer() {
        CharArraySet stopWords = EnwikiFactory.getExtendedStopWords();
        stopWords.add("dream");
        return new WikiAnalyzer(LUCENE_48, stopWords, false);
    }

    public static Analyzer getLinkAnalyzer() {
        CharArraySet stopWords = EnwikiFactory.getExtendedStopWords();
        return new WikiAnalyzer(LUCENE_48, stopWords, true);
    }

    public static Analyzer getDreamAnalyzer() {
        CharArraySet stopWords = EnwikiFactory.getExtendedStopWords();
        stopWords.add("dream");
        return new WikiAnalyzer(LUCENE_48, stopWords, false);
    }
}
