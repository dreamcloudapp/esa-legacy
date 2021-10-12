package com.dreamcloud.esa;

import org.apache.lucene.store.Directory;

import java.io.File;
import java.io.IOException;

public interface Indexer {
    public void analyze(File file);
    public void index(File file) throws IOException;
}
