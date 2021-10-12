package com.dreamcloud.esa;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.TypeTokenFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.ClassicFilter;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.wikipedia.WikipediaTokenizer;

// copied from DutchAnalyzer, changed StandardTokenizer to WikipediaTokenizer
public final class WikiAnalyzer extends Analyzer {

    /**
     * Contains the stopwords used with the StopFilter.
     */
    private final CharArraySet stoptable;

    public WikiAnalyzer(CharArraySet stopwords) {
        this.stoptable = CharArraySet.unmodifiableSet(CharArraySet.copy(stopwords));
    }

    @Override
    protected Analyzer.TokenStreamComponents createComponents(String fieldName) {
        File dictionary = new File("./src/data/en-words.txt");
        Set<String> stopTypes = new HashSet<>();
        stopTypes.add(WikipediaTokenizer.EXTERNAL_LINK_URL);
        stopTypes.add(WikipediaTokenizer.EXTERNAL_LINK);
        stopTypes.add(WikipediaTokenizer.CITATION);
        final Tokenizer source = new WikipediaTokenizer();
        TokenStream result = new TypeTokenFilter(source, stopTypes);
        result = new ASCIIFoldingFilter(result, false);
        result = new LowerCaseFilter(result);
        result = new ClassicFilter(result);
        result = new DictionaryFilter(result, dictionary);
        result = new StopFilter(result, stoptable);
        result = new PorterStemFilter(result);
        result = new StopFilter(result, stoptable);
        return new Analyzer.TokenStreamComponents(source, result);
    }
}
