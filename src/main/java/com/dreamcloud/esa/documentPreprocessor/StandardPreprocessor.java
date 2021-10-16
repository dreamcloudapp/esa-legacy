package com.dreamcloud.esa.documentPreprocessor;

import java.util.ArrayList;

public class StandardPreprocessor implements DocumentPreprocessor {
    public String process(String document) {
        return document.replaceAll("[+\\-&|!(){}\\[\\]^\"~*?:/\\\\]+", " ");
    }

    public String getInfo() {
        return this.getClass().getSimpleName();
    }
}
