package com.dreamcloud.esa.fs;

import java.io.*;

public class TermIndexReader {
    protected DataInputStream inputStream;
    protected int documentCount;
    protected double averageDocumentLength;

    public TermIndexReader() {

    }

    public void open(File termIndex) throws IOException {
        inputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(termIndex)));
        //We're sticking the document count here as it's data we need for TF-IDF
        documentCount = inputStream.readInt();
        averageDocumentLength = inputStream.readFloat();
    }

    public TermIndexEntry readTerm() throws IOException {
        try {
            TermIndexEntry entry = new TermIndexEntry();
            int termLength = inputStream.readInt();
            entry.term = new String(inputStream.readNBytes(termLength));
            entry.documentFrequency = inputStream.readInt();
            entry.offset = inputStream.readInt();
            entry.numScores = inputStream.readInt();
            return entry;
        } catch (EOFException e) {
            return null;
        }
    }

    public TermIndex readIndex() throws IOException {
        TermIndex termIndex = new TermIndex(documentCount, averageDocumentLength);
        while (true) {
            TermIndexEntry entry = readTerm();
            if (entry == null) {
                break;
            }
            termIndex.addEntry(entry);
        }
        return termIndex;
    }

    public void close() throws IOException {
        this.inputStream.close();
    }
}
