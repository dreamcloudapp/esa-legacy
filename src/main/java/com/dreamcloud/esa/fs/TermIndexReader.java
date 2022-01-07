package com.dreamcloud.esa.fs;

import java.io.*;
import java.nio.ByteBuffer;

public class TermIndexReader {
    InputStream inputStream;
    int documentCount;

    public TermIndexReader() {

    }

    public void open(File termIndex) throws IOException {
        inputStream = new FileInputStream(termIndex);
        inputStream = new BufferedInputStream(inputStream);

        //We're sticking the document count here as it's data we need for TF-IDF
        byte[] documentCountBytes = inputStream.readNBytes(FileSystem.OFFSET_BYTES);
        ByteBuffer buffer = ByteBuffer.wrap(documentCountBytes);
        documentCount = buffer.getInt();
    }

    public TermIndexEntry readTerm() throws IOException {
        byte[] termLengthBytes = inputStream.readNBytes(FileSystem.TERM_LENGTH_BYTES);
        if (termLengthBytes.length == 0) {
            //EOF
            return null;
        }

        int termLength = ByteBuffer.wrap(termLengthBytes).getInt();
        TermIndexEntry entry = new TermIndexEntry();
        entry.term = new String(inputStream.readNBytes(termLength));
        entry.documentFrequency = ByteBuffer.wrap(inputStream.readNBytes(FileSystem.OFFSET_BYTES)).getInt();
        entry.offset = ByteBuffer.wrap(inputStream.readNBytes(FileSystem.OFFSET_BYTES)).getInt();
        entry.numScores = ByteBuffer.wrap(inputStream.readNBytes(FileSystem.TERM_LENGTH_BYTES)).getInt();
        return entry;
    }

    public TermIndex readIndex() throws IOException {
        TermIndex termIndex = new TermIndex(documentCount);
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
