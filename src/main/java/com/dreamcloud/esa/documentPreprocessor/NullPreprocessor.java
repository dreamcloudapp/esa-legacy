package com.dreamcloud.esa.documentPreprocessor;

public class NullPreprocessor implements DocumentPreprocessor {
    public String process(String document) throws Exception {
        return document;
    }

    public String getInfo() {
        return this.getClass().getSimpleName();
    }
}
