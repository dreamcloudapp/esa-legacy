package com.dreamcloud.esa.fs;

import java.io.*;

public class DocumentScoreFileReader {
    //RandomAccessFile scoreFileReader;
    FileInputStream scoreFileReader;

    public DocumentScoreFileReader(File scoreFile) throws FileNotFoundException {
        //scoreFileReader = new RandomAccessFile(scoreFile, "r");
        scoreFileReader = new FileInputStream(scoreFile);
    }

    public byte[] readScores(int offset, int numScores) throws IOException {
        scoreFileReader.getChannel().position(offset);
        //byte[] scores = new byte[numScores * FileSystem.DOCUMENT_SCORE_BYTES];
        byte[] scores = scoreFileReader.readNBytes(numScores * FileSystem.DOCUMENT_SCORE_BYTES);
        return scores;
    }
}
