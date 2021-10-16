package com.dreamcloud.esa.documentPreprocessor;

import com.dreamcloud.esa.EsaOptions;

import java.io.IOException;

public interface DocumentPreprocessor {
    String process(String document) throws Exception;
    String getInfo();
}
