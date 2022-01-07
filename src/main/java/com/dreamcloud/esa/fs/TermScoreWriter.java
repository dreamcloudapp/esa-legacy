package com.dreamcloud.esa.fs;

import java.io.*;
import java.nio.ByteBuffer;

public class TermScoreWriter {
    DataOutputStream outputStream;

    public TermScoreWriter() {

    }

    public void open(File termIndex) throws IOException {
        outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(termIndex)));
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

    public void writeTermScores(byte[] termScores) throws IOException {
        outputStream.write(termScores);
    }

    public void close() throws IOException {
        this.outputStream.close();
    }
}
