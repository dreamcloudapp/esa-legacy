package com.dreamcloud.esa;

import org.apache.lucene.analysis.CharArraySet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class DictionaryRepository {
    protected CharArraySet source;
    protected File sourceFile;
    protected String sourceFileName;
    protected CharArraySet extendedDictionary;

    public DictionaryRepository() {}

    public DictionaryRepository(CharArraySet source) {
        this.source = source;
    }

    public DictionaryRepository(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    public DictionaryRepository(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    /**
     * Adds additional stopwords beyond the source set (which can be empty).
     */
    public void addExtendedDictionaryWords(CharArraySet words) {
        if (this.extendedDictionary == null) {
            this.extendedDictionary = new CharArraySet(256, true);
        }
        this.extendedDictionary.addAll(words);
    }

    public CharArraySet readDictionaryFromFileName(String sourceFileName) throws IOException {
        return this.readDictionaryFromFile(new File(sourceFileName));
    }

    public CharArraySet readDictionaryFromFile(File sourceFile) throws IOException {
        CharArraySet stopWords = new CharArraySet(1024, true);
        BufferedReader reader;
        reader = new BufferedReader(new FileReader(sourceFile));
        String line = reader.readLine();
        while (line != null) {
            if (!"".equals(line)) {
                stopWords.add(line.toLowerCase());
            }
            line = reader.readLine();
        }
        reader.close();
        return stopWords;
    }

    public CharArraySet getDictionaryWords() throws IOException {
        //Load the stopwords once
        if (source == null) {
            if (this.sourceFile != null) {
                source = this.readDictionaryFromFile(this.sourceFile);
            } else if(this.sourceFileName != null) {
                source = this.readDictionaryFromFileName(this.sourceFileName);
            } else {
                source = new CharArraySet(256, true);
            }
            //Add extended stopwords
            if (extendedDictionary != null) {
                source.addAll(extendedDictionary);
            }
        }
        return source;
    }
}
