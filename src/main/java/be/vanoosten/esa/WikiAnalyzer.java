package be.vanoosten.esa;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.TypeTokenFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.miscellaneous.LengthFilter;
import org.apache.lucene.analysis.sinks.TokenTypeSinkFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.ClassicFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.WordlistLoader;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.wikipedia.WikipediaTokenizer;

// copied from DutchAnalyzer, changed StandardTokenizer to WikipediaTokenizer
public final class WikiAnalyzer extends Analyzer {

    /**
     * Contains the stopwords used with the StopFilter.
     */
    private final CharArraySet stoptable;

    // null if on 3.1 or later - only for bw compat
    private final Version matchVersion;

    public WikiAnalyzer(Version matchVersion, CharArraySet stopwords) {
        this.matchVersion = matchVersion;
        this.stoptable = CharArraySet.unmodifiableSet(CharArraySet.copy(matchVersion, stopwords));
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
        Set<String> stopTypes = new HashSet<>();
        stopTypes.add(WikipediaTokenizer.EXTERNAL_LINK_URL);
        stopTypes.add(WikipediaTokenizer.EXTERNAL_LINK);
        //stopTypes.add(WikipediaTokenizer.INTERNAL_LINK);
        stopTypes.add(WikipediaTokenizer.CITATION);
        /*skip.add(WikipediaTokenizer.BOLD);
        skip.add(WikipediaTokenizer.BOLD_ITALICS);
        skip.add(WikipediaTokenizer.CATEGORY);
        skip.add(WikipediaTokenizer.HEADING);
        skip.add(WikipediaTokenizer.ITALICS);
        skip.add(WikipediaTokenizer.SUB_HEADING);*/
        final Tokenizer source = new WikipediaTokenizer(aReader);
        TokenStream result = new TypeTokenFilter(matchVersion, source, stopTypes);
        result = new StandardFilter(matchVersion, result);
        result = new TypeTokenFilter(matchVersion, result, stopTypes);
        result = new ASCIIFoldingFilter(result, false);
        result = new LowerCaseFilter(matchVersion, result);
        result = new ClassicFilter(result);
        result = new StopFilter(matchVersion, result, stoptable);
        result = new PorterStemFilter(result);
        result = new LengthFilter(matchVersion, result, 2, 24);
        return new Analyzer.TokenStreamComponents(source, result);
    }
}
