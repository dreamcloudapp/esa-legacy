package com.dreamcloud.esa.vectorizer;

import java.io.IOException;

public interface VectorizerFactory {
    public TextVectorizer getVectorizer() throws IOException;
}
