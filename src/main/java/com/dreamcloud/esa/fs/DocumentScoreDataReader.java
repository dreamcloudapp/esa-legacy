package com.dreamcloud.esa.fs;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface DocumentScoreDataReader {
    public ByteBuffer readScores(int offset, int numScores) throws IOException;
}
