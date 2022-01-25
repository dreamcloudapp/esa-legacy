package com.dreamcloud.esa;

import com.dreamcloud.esa.analysis.CollectionInfo;
import com.dreamcloud.esa.fs.CollectionWriter;
import com.dreamcloud.esa.score.DocumentScoreReader;

/**
 * The data source for TF-IDF analysis.
 */
public class SourceOptions {
    public CollectionInfo collectionInfo;
    public DocumentScoreReader scoreReader;
    public CollectionWriter collectionWriter;
}
