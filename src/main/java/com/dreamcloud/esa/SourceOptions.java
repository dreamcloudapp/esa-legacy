package com.dreamcloud.esa;

import com.dreamcloud.esa_score.analysis.CollectionInfo;
import com.dreamcloud.esa_score.fs.CollectionWriter;
import com.dreamcloud.esa_score.score.DocumentScoreReader;

/**
 * The data source for TF-IDF analysis.
 */
public class SourceOptions {
    public CollectionInfo collectionInfo;
    public DocumentScoreReader scoreReader;
    public CollectionWriter collectionWriter;
}
