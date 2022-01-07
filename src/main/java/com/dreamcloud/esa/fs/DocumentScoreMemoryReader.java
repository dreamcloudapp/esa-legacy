package com.dreamcloud.esa.fs;

import java.io.*;
import java.util.Arrays;

public class DocumentScoreMemoryReader implements DocumentScoreDataReader {
    byte[] scoreData = null;

    public DocumentScoreMemoryReader(File scoreFile) throws IOException {
        InputStream inputStream = new FileInputStream(scoreFile);
        inputStream = new BufferedInputStream(inputStream);
        scoreData = inputStream.readAllBytes();
    }

    public byte[] readScores(int offset, int numScores) throws IOException {
        return Arrays.copyOfRange(scoreData, offset, offset + numScores * FileSystem.DOCUMENT_SCORE_BYTES);
    }
}
