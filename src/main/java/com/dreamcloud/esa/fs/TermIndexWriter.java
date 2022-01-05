package com.dreamcloud.esa.fs;

import com.dreamcloud.esa.tfidf.TfIdfScore;

import java.io.*;
import java.nio.ByteBuffer;

public class TermIndexWriter {
    OutputStream outputStream;
    int offset = 0;

    public TermIndexWriter() {

    }

    public void open(File termIndex) throws IOException {
        outputStream = new FileOutputStream(termIndex);
        offset = 0;
    }

    public void writeTerm(String term, int numScores) throws IOException {
        int termLength = term.length();
        int termOffset = offset;
        offset += numScores * FileSystem.DOCUMENT_SCORE_BYTES;

        /*
            Write the following bytes:
            term length (4)
            term (term.length())
            offset (4) (the starting offset in the score file)
            numScores (4)
         */
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + term.length() + Integer.BYTES + Integer.BYTES);
        buffer.putInt(termLength);
        buffer.put(term.getBytes());
        buffer.putInt(termOffset);
        buffer.putInt(numScores);
        outputStream.write(buffer.array());
    }
}
