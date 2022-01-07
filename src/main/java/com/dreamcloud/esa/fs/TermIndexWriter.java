package com.dreamcloud.esa.fs;

import com.dreamcloud.esa.tfidf.TfIdfScore;

import java.io.*;
import java.nio.ByteBuffer;

public class TermIndexWriter {
    OutputStream outputStream;
    int offset = 0;
    int documentCount;

    public TermIndexWriter(int documentCount) {
        this.documentCount = documentCount;
    }

    public void open(File termIndex) throws IOException {
        outputStream = new FileOutputStream(termIndex);
        outputStream = new BufferedOutputStream(outputStream);
        offset = 0;
        ByteBuffer buffer = ByteBuffer.allocate(FileSystem.OFFSET_BYTES);
        buffer.putInt(documentCount);
        outputStream.write(buffer.array());
    }

    public void writeTerm(String term, int numScores) throws IOException {
        int termLength = term.getBytes().length;
        int termOffset = offset;
        offset += numScores * FileSystem.DOCUMENT_SCORE_BYTES;

        /*
            Write the following bytes:
            term length (4)
            term (term bytes length)
            doc freq (4)
            offset (4) (the starting offset in the score file)
            numScores (4)
         */
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + termLength + FileSystem.OFFSET_BYTES * 3);
        buffer.putInt(termLength);
        buffer.put(term.getBytes());
        buffer.putInt(numScores);
        buffer.putInt(termOffset);
        buffer.putInt(numScores);
        outputStream.write(buffer.array());
    }

    public void close() throws IOException {
        outputStream.close();
    }
}
