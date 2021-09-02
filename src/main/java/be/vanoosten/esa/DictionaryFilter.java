package be.vanoosten.esa;

import org.apache.lucene.analysis.FilteringTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import java.io.*;
import java.util.HashSet;

/**
 * Takes a dictionary of words (one per line) and uses it to filter
 * the input tokens ensuring that all are valid words.
 */
public class DictionaryFilter extends FilteringTokenFilter {
    File dictionary;
    Boolean ignoreCase;
    static HashSet<String> words = new HashSet<String>();
    static Boolean loaded = false;
    private final CharTermAttribute termAtt = this.addAttribute(CharTermAttribute.class);

    /**
     * Construct a token stream filtering the given input.
     *
     * @param dictionary
     * @param input
     * @param ignoreCase
     */
    public DictionaryFilter(TokenStream input, File dictionary, Boolean ignoreCase) {
        super(input);
        this.dictionary = dictionary;
        this.ignoreCase = ignoreCase;
    }

    /**
     * Construct a token stream filtering the given input.
     *
     * @param dictionary
     * @param input
     */
    public DictionaryFilter(TokenStream input, File dictionary) {
        super(input);
        this.dictionary = dictionary;
        this.ignoreCase = true;
    }

    protected void loadDictionary() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(dictionary));
            String line = reader.readLine();
            while (line != null) {
                if (!"".equals(line)) {
                    if (ignoreCase) {
                        line = line.toLowerCase();
                    }
                    words.add(line);
                }
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            loaded = true;
        }
    }

    protected boolean accept() throws IOException {
        if (!loaded) {
            this.loadDictionary();
        }
        //Why is reading from a char array so damn hard?
        char[] buffer = this.termAtt.buffer();
        StringBuilder sb = new StringBuilder(termAtt.length());
        for (int i=0; i<termAtt.length(); i++) {
            sb.append(buffer[i]);
        }
        String word = sb.toString();
        if (ignoreCase) {
            word = word.toLowerCase();
        }
        return words.contains(word);
    }
}
