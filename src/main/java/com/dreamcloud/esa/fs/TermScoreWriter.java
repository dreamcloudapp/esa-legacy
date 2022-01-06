package com.dreamcloud.esa.fs;

import com.dreamcloud.esa.tfidf.TfIdfScore;

import java.io.*;
import java.nio.ByteBuffer;

public class TermScoreWriter {
    OutputStream outputStream;

    public TermScoreWriter() {

    }

    public void open(File termIndex) throws IOException {
        outputStream = new FileOutputStream(termIndex);
        outputStream = new BufferedOutputStream(outputStream);
    }

    public void writeTermScores(TermScore[] termScores) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(FileSystem.DOCUMENT_SCORE_BYTES);
        for (TermScore termScore: termScores) {
            buffer.putInt(termScore.document);
            buffer.putFloat(termScore.score);
            outputStream.write(buffer.array());
            buffer.clear();
        }
    }

    public void close() throws IOException {
        this.outputStream.close();
    }
}
