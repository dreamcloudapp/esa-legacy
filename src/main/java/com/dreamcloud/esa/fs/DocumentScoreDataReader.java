package com.dreamcloud.esa.fs;

import java.io.IOException;

public interface DocumentScoreDataReader {
    public byte[] readScores(int offset, int numScores) throws IOException;
}
