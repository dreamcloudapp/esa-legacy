package be.vanoosten.esa;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.ClassicFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.Reader;

public final class DreamAnalyzer extends Analyzer {

    /**
     * Contains the stopwords used with the StopFilter.
     */
    private final CharArraySet stoptable;

    // null if on 3.1 or later - only for bw compat
    private final Version matchVersion;

    public DreamAnalyzer(Version matchVersion, CharArraySet stopwords) {
        this.matchVersion = matchVersion;
        this.stoptable = CharArraySet.unmodifiableSet(CharArraySet.copy(matchVersion, stopwords));
    }

    protected Analyzer.TokenStreamComponents createComponents(String fieldName, Reader aReader) {
        File dictionary = new File("./src/data/en-words.txt");
        Tokenizer source = new StandardTokenizer(matchVersion, aReader);
        TokenStream result = new StandardFilter(matchVersion, source);
        result = new ASCIIFoldingFilter(result, false);
        result = new LowerCaseFilter(matchVersion, result);
        result = new ClassicFilter(result);
        result = new DictionaryFilter(matchVersion, result, dictionary);
        result = new StopFilter(matchVersion, result, stoptable);
        result = new PorterStemFilter(result);
        result = new StopFilter(matchVersion, result, stoptable);
        return new Analyzer.TokenStreamComponents(source, result);
    }
}
