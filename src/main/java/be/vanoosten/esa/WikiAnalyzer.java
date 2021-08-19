package be.vanoosten.esa;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.TypeTokenFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.miscellaneous.LengthFilter;
import org.apache.lucene.analysis.standard.ClassicFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.Reader;
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

    // null if on 3.1 or later - only for bw compat
    private final Version matchVersion;

    public WikiAnalyzer(Version matchVersion, CharArraySet stopwords) {
        this.matchVersion = matchVersion;
        this.stoptable = CharArraySet.unmodifiableSet(CharArraySet.copy(matchVersion, stopwords));
        this.linkAnalysis = false;
    }

    public WikiAnalyzer(Version matchVersion, CharArraySet stopwords, boolean linkAnalysis) {
        this.matchVersion = matchVersion;
        this.stoptable = CharArraySet.unmodifiableSet(CharArraySet.copy(matchVersion, stopwords));
        this.linkAnalysis = linkAnalysis;
    }

    /**
     * Returns a (possibly reused) {@link TokenStream} which tokenizes all the
     * text in the provided {@link Reader}.
     *
     * @param aReader
     * @return A {@link TokenStream} built from a {@link StandardTokenizer}
     * filtered with {@link StandardFilter}, {@link LowerCaseFilter},
     *   {@link StopFilter}
     */
    @Override
    protected Analyzer.TokenStreamComponents createComponents(String fieldName, Reader aReader) {
        File dictionary = new File("./src/data/en-words.txt");

        Set<String> untokenizedTypes = new HashSet<>();
        untokenizedTypes.add(WikipediaTokenizer.INTERNAL_LINK);

        Set<String> stopTypes = new HashSet<>();
        stopTypes.add(WikipediaTokenizer.EXTERNAL_LINK_URL);
        stopTypes.add(WikipediaTokenizer.EXTERNAL_LINK);
        stopTypes.add(WikipediaTokenizer.CITATION);
        final Tokenizer source;
        if (linkAnalysis) {
           source = new WikipediaTokenizer(aReader, WikipediaTokenizer.UNTOKENIZED_ONLY, untokenizedTypes);
        } else {
            source = new WikipediaTokenizer(aReader);
        }

        TokenStream result = new TypeTokenFilter(matchVersion, source, stopTypes);
        if (!linkAnalysis) {
            result = new StandardFilter(matchVersion, result);
            result = new ASCIIFoldingFilter(result, false);
            result = new LowerCaseFilter(matchVersion, result);
            result = new ClassicFilter(result);
            result = new DictionaryFilter(matchVersion, result, dictionary);
            result = new StopFilter(matchVersion, result, stoptable);
            result = new PorterStemFilter(result);
            result = new StopFilter(matchVersion, result, stoptable);
            result = new LengthFilter(matchVersion, result, 2, 24);
        }

        return new Analyzer.TokenStreamComponents(source, linkAnalysis ? source : result);
    }
}
