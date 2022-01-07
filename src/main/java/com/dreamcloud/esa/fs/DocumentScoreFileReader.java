package com.dreamcloud.esa.fs;

import java.io.*;

public class DocumentScoreFileReader {
    RandomAccessFile scoreFileReader;

    public DocumentScoreFileReader(File scoreFile) throws FileNotFoundException {
        scoreFileReader = new RandomAccessFile(scoreFile, "r");
    }

    public byte[] readScores(int offset, int numScores) throws IOException {
        scoreFileReader.seek(offset);
        byte[] scores = new byte[numScores * FileSystem.DOCUMENT_SCORE_BYTES];
        scoreFileReader.read(scores, 0, numScores * FileSystem.DOCUMENT_SCORE_BYTES);
        return scores;
    }
}
