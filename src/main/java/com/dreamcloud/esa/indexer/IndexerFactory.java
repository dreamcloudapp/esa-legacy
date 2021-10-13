package com.dreamcloud.esa.indexer;

import com.dreamcloud.esa.DocumentType;
import com.dreamcloud.esa.EsaOptions;
import org.apache.commons.cli.CommandLine;

public class IndexerFactory {
    public Indexer getIndexer(DocumentType documentType, WikiIndexerOptions options) {
        switch(documentType) {
            case WIKI:
                return new WikiIndexer(options);
            case DREAM:
                return new DreamIndexer(options);
            default:
                throw new IllegalArgumentException("No indexer for unrecognized document type " + documentType);
        }
    }
}
