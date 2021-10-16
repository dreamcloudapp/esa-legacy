package com.dreamcloud.esa.analyzer;

import org.apache.lucene.analysis.Tokenizer;

public abstract class TokenizerFactory {
    abstract public Tokenizer getTokenizer();
}
