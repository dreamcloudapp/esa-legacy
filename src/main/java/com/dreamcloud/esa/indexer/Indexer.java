package com.dreamcloud.esa.indexer;

import org.apache.lucene.store.Directory;

import java.io.File;
import java.io.IOException;

public interface Indexer {
    public void index(File file) throws IOException;
}
