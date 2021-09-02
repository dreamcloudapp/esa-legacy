package be.vanoosten.esa;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.ClassicFilter;
import org.apache.lucene.analysis.standard.ClassicTokenizer;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.Reader;

public final class DreamAnalyzer extends Analyzer {

    /**
     * Contains the stopwords used with the StopFilter.
     */
    private final CharArraySet stoptable;

    public DreamAnalyzer(CharArraySet stopwords) {
        this.stoptable = CharArraySet.unmodifiableSet(CharArraySet.copy(stopwords));
    }

    protected Analyzer.TokenStreamComponents createComponents(String fieldName) {
        File dictionary = new File("./src/data/en-words.txt");
        Tokenizer source = new ClassicTokenizer();
        TokenStream result = new ASCIIFoldingFilter(source, false);
        result = new LowerCaseFilter(result);
        result = new ClassicFilter(result);
        result = new DictionaryFilter(result, dictionary);
        result = new StopFilter(result, stoptable);
        result = new PorterStemFilter(result);
        result = new StopFilter(result, stoptable);
        return new Analyzer.TokenStreamComponents(source, result);
    }
}
