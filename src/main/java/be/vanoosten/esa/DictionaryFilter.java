package be.vanoosten.esa;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.FilteringTokenFilter;
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
    HashSet<String> words;
    Boolean loaded = false;
    private final CharTermAttribute termAtt = this.addAttribute(CharTermAttribute.class);

    /**
     * Construct a token stream filtering the given input.
     *
     * @param dictionary
     * @param input
     * @param ignoreCase
     */
    public DictionaryFilter(Version version, TokenStream input, File dictionary, Boolean ignoreCase) {
        super(version, input);
        this.dictionary = dictionary;
        this.ignoreCase = ignoreCase;
    }

    /**
     * Construct a token stream filtering the given input.
     *
     * @param dictionary
     * @param input
     */
    public DictionaryFilter(Version version, TokenStream input, File dictionary) {
        super(version, input);
        this.dictionary = dictionary;
        this.ignoreCase = false;
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
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected boolean accept() throws IOException {
        if (!this.loaded) {
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
