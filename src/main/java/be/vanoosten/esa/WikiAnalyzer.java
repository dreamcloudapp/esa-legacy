package be.vanoosten.esa;

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
    private boolean linkAnalysis;

    public WikiAnalyzer(CharArraySet stopwords) {
        this.stoptable = CharArraySet.unmodifiableSet(CharArraySet.copy(stopwords));
        this.linkAnalysis = false;
    }

    public WikiAnalyzer(CharArraySet stopwords, boolean linkAnalysis) {
        this.stoptable = CharArraySet.unmodifiableSet(CharArraySet.copy(stopwords));
        this.linkAnalysis = linkAnalysis;
    }

    @Override
    protected Analyzer.TokenStreamComponents createComponents(String fieldName) {
        File dictionary = new File("./src/data/en-words.txt");

        Set<String> untokenizedTypes = new HashSet<>();
        untokenizedTypes.add(WikipediaTokenizer.INTERNAL_LINK);

        Set<String> stopTypes = new HashSet<>();
        stopTypes.add(WikipediaTokenizer.EXTERNAL_LINK_URL);
        stopTypes.add(WikipediaTokenizer.EXTERNAL_LINK);
        stopTypes.add(WikipediaTokenizer.CITATION);
        final Tokenizer source;
        if (linkAnalysis) {
           source = new WikipediaTokenizer(WikipediaTokenizer.UNTOKENIZED_ONLY, untokenizedTypes);
        } else {
            source = new WikipediaTokenizer();
        }

        TokenStream result = new TypeTokenFilter(source, stopTypes);
        if (!linkAnalysis) {
            result = new ASCIIFoldingFilter(result, false);
            result = new LowerCaseFilter(result);
            result = new ClassicFilter(result);
            result = new DictionaryFilter(result, dictionary);
            result = new StopFilter(result, stoptable);
            result = new PorterStemFilter(result);
            result = new StopFilter(result, stoptable);
        }

        return new Analyzer.TokenStreamComponents(source, result);
    }
}
