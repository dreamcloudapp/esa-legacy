package com.dreamcloud.esa;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.FilteringTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import java.io.*;

/**
 * Takes a dictionary of words (one per line) and uses it to filter
 * the input tokens ensuring that all are valid words.
 */
public class DictionaryFilter extends FilteringTokenFilter {
    DictionaryRepository repository;
    Boolean ignoreCase;
    private final CharTermAttribute termAtt = this.addAttribute(CharTermAttribute.class);

    /**
     * Construct a token stream filtering the given input.
     *
     * @param dictionary
     * @param input
     * @param ignoreCase
     */
    public DictionaryFilter(TokenStream input, DictionaryRepository repository, Boolean ignoreCase) {
        super(input);
        this.repository = repository;
        this.ignoreCase = ignoreCase;
    }

    /**
     * Construct a token stream filtering the given input.
     *
     * @param dictionary
     * @param input
     */
    public DictionaryFilter(TokenStream input, DictionaryRepository repository) {
        super(input);
        this.repository = repository;
        this.ignoreCase = true;
    }

    protected boolean accept() throws IOException {
        CharArraySet words = repository.getDictionaryWords();
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
