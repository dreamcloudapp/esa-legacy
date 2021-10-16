package com.dreamcloud.esa;

import org.apache.lucene.analysis.CharArraySet;
import java.io.*;

public class StopWordRepository {
    protected CharArraySet source;
    protected File sourceFile;
    protected String sourceFileName;
    protected CharArraySet extendedStopWords;

    public StopWordRepository() {

    }

    public StopWordRepository(CharArraySet source) {
        this.source = source;
    }

    public StopWordRepository(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    public StopWordRepository(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    /**
     * Adds additional stopwords beyond the source set (which can be empty).
     */
    public void addExtendedStopWords(CharArraySet stopwords) {
        if (this.extendedStopWords == null) {
            this.extendedStopWords = new CharArraySet(256, true);
        }
        this.extendedStopWords.addAll(stopwords);
    }

    public CharArraySet readStopWordsFromFileName(String sourceFileName) throws IOException {
        return this.readStopWordsFromFile(new File(sourceFileName));
    }

    public CharArraySet readStopWordsFromFile(File sourceFile) throws IOException {
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

    public CharArraySet getStopWords() throws IOException {
        //Load the stopwords once
        if (source == null) {
            if (this.sourceFile != null) {
                source = this.readStopWordsFromFile(this.sourceFile);
            } else if(this.sourceFileName != null) {
                source = this.readStopWordsFromFileName(this.sourceFileName);
            } else {
                source = new CharArraySet(256, true);
            }
            //Add extended stopwords
            if (this.extendedStopWords != null) {
                source.addAll(extendedStopWords);
            }
        }
        return source;
    }
}
