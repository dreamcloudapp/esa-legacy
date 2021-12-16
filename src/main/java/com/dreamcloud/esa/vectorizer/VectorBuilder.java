package com.dreamcloud.esa.vectorizer;

import java.io.IOException;

public interface VectorBuilder {
    ConceptVector build(String document) throws IOException;
}
