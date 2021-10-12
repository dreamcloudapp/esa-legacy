package com.dreamcloud.esa;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.ClassicFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import java.io.File;

public final class DreamPostLemmaAnalyzer extends Analyzer {

    /**
     * Contains the stopwords used with the StopFilter.
     */
    private final CharArraySet stoptable;

    public DreamPostLemmaAnalyzer(CharArraySet stopwords) {
        this.stoptable = CharArraySet.unmodifiableSet(CharArraySet.copy(stopwords));
    }

    protected Analyzer.TokenStreamComponents createComponents(String fieldName) {
        File dictionary = new File("./src/data/en-words.txt");
        Tokenizer source = new StandardTokenizer();
        TokenStream result = new ASCIIFoldingFilter(source, false);
        result = new LowerCaseFilter(result);
        result = new ClassicFilter(result);
        result = new DictionaryFilter(result, dictionary);
        result = new StopFilter(result, stoptable);
        return new Analyzer.TokenStreamComponents(source, result);
    }
}
