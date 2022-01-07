package com.dreamcloud.esa;

import com.dreamcloud.esa.tfidf.CollectionInfo;
import com.dreamcloud.esa.tfidf.DocumentScoreReader;

/**
 * The data source for TF-IDF analysis.
 */
public class SourceOptions {
    public CollectionInfo collectionInfo;
    public DocumentScoreReader scoreReader;
}
