package com.dreamcloud.esa.vectorizer;

import com.dreamcloud.esa.EsaOptions;

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
                return new SqlVectorizer(options.analyzer);
            case "lucene":
            default:
                return new Vectorizer(options);
        }
    }
}
