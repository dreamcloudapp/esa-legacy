package com.dreamcloud.esa.documentPreprocessor;

public interface DocumentPreprocessor {
    String process(String document) throws Exception;
    String getInfo();
}
