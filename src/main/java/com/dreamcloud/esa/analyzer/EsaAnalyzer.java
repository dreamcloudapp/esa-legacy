package com.dreamcloud.esa.analyzer;

import com.dreamcloud.esa.DictionaryFilter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.TypeTokenFilter;
import org.apache.lucene.analysis.en.EnglishMinimalStemFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.ClassicFilter;

import java.io.IOException;

public class EsaAnalyzer extends Analyzer {
    AnalyzerOptions options;

    public EsaAnalyzer(AnalyzerOptions options) {
        this.options = options;
    }

    protected Analyzer.TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = options.tokenizerFactory.getTokenizer();
        TokenStream result = source;

        if (options.stopTokenTypes != null) {
            result = new TypeTokenFilter(source, options.stopTokenTypes);
        }
        if (options.asciiFoldingFilter) {
            result = new ASCIIFoldingFilter(result, false);
        }
        if (options.lowerCaseFilter) {
            result = new LowerCaseFilter(result);
        }
        if (options.classicFilter) {
            result = new ClassicFilter(result);
        }
        if (options.singularCaseFilter) {
            result = new EnglishMinimalStemFilter(result);
        }
        if (options.dictionaryRepository != null) {
            result = new DictionaryFilter(result, options.dictionaryRepository);
        }

        if (options.stopWordsRepository != null) {
            try {
                result = new StopFilter(result, options.stopWordsRepository.getStopWords());
            } catch (IOException e) {
                System.out.println("ESA warning: failed to load stop word dictionary; " + e.getMessage());
            }
        }

        if (options.porterStemmerFilter) {
            for(int i=0; i<options.porterStemmerFilterDepth; i++) {
                result = new PorterStemFilter(result);
            }
        }

        return new Analyzer.TokenStreamComponents(source, result);
    }
}
