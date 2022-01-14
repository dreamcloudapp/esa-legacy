package com.dreamcloud.esa.vectorizer;

import com.dreamcloud.esa.EsaOptions;
import com.dreamcloud.esa.database.TfIdfScoreRepository;
import com.dreamcloud.esa.tfidf.BM25Calculator;
import com.dreamcloud.esa.tfidf.TfIdfAnalyzer;
import com.dreamcloud.esa.tfidf.TfIdfCalculator;

import java.io.IOException;

public class CommandLineVectorizerFactory implements VectorizerFactory {
    EsaOptions options;

    public CommandLineVectorizerFactory(EsaOptions options) {
        this.options = options;
    }

    @Override
    public TextVectorizer getVectorizer() throws IOException {
        switch (this.options.vectorizerType) {
            case "sql":
                return new SqlVectorizer(new VectorBuilder(options.sourceOptions.scoreReader, options.sourceOptions.collectionInfo, new TfIdfAnalyzer(new TfIdfCalculator(options.tfIdfQueryMode), options.analyzer, options.sourceOptions.collectionInfo), options.preprocessor, options.pruneOptions));
            case "lucene":
            default:
                return new Vectorizer(options);
        }
    }
}
