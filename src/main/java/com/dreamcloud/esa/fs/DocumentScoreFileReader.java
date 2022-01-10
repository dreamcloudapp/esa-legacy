package com.dreamcloud.esa.fs;

import java.io.*;
import java.nio.ByteBuffer;

public class DocumentScoreFileReader implements DocumentScoreDataReader {
    //RandomAccessFile scoreFileReader;
    FileInputStream scoreFileReader;

    public DocumentScoreFileReader(File scoreFile) throws FileNotFoundException {
        //scoreFileReader = new RandomAccessFile(scoreFile, "r");
        scoreFileReader = new FileInputStream(scoreFile);
    }

    public ByteBuffer readScores(int offset, int numScores) throws IOException {
        scoreFileReader.getChannel().position(offset);
        //byte[] scores = new byte[numScores * FileSystem.DOCUMENT_SCORE_BYTES];
        byte[] scores = scoreFileReader.readNBytes(numScores * FileSystem.DOCUMENT_SCORE_BYTES);
        return ByteBuffer.wrap(scores);
    }
}
