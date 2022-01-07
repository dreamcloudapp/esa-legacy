package com.dreamcloud.esa.fs;

import java.io.*;

public class TermIndexWriter {
    DataOutputStream outputStream;
    int offset = 0;
    int documentCount;

    public TermIndexWriter(int documentCount) {
        this.documentCount = documentCount;
    }

    public void open(File termIndex) throws IOException {
        OutputStream sourceOutputStream = new FileOutputStream(termIndex);
        sourceOutputStream = new BufferedOutputStream(sourceOutputStream);
        outputStream = new DataOutputStream(sourceOutputStream);
        offset = 0;
        outputStream.writeInt(documentCount);
    }

    public void writeTerm(String term, int numScores) throws IOException {
        int termLength = term.getBytes().length;
        int termOffset = offset;
        offset += numScores * FileSystem.DOCUMENT_SCORE_BYTES;

        outputStream.writeInt(termLength);
        outputStream.write(term.getBytes());
        outputStream.writeInt(numScores);
        outputStream.writeInt(termOffset);
        outputStream.writeInt(numScores);
    }

    public void close() throws IOException {
        outputStream.close();
    }
}
