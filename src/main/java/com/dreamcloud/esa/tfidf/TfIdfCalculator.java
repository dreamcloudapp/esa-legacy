package com.dreamcloud.esa.tfidf;

import com.dreamcloud.esa.tfidf.strategy.*;
import com.dreamcloud.esa.tfidf.strategy.idf.LogarithmicInverseDocumentFrequency;
import com.dreamcloud.esa.tfidf.strategy.idf.NoInverseDocumentFrequency;
import com.dreamcloud.esa.tfidf.strategy.idf.ProbabilisticInverseDocumentFrequency;
import com.dreamcloud.esa.tfidf.strategy.norm.ByteLengthInverseDocumentFrequency;
import com.dreamcloud.esa.tfidf.strategy.norm.CosineNormalization;
import com.dreamcloud.esa.tfidf.strategy.norm.NoNormalization;
import com.dreamcloud.esa.tfidf.strategy.tf.*;

import java.util.Objects;

public class TfIdfCalculator implements TfIdfStrategy {
    protected final TermFrequencyStrategy tfStrategy;
    protected final InverseDocumentFrequencyStrategy idfStrategy;
    protected final NormalizationStrategy normStrategy;

    public TfIdfCalculator(TermFrequencyStrategy tfStrategy, InverseDocumentFrequencyStrategy idfStrategy, NormalizationStrategy normStrategy) {
        this.tfStrategy = tfStrategy;
        this.idfStrategy = idfStrategy;
        this.normStrategy = normStrategy;
    }

    public TfIdfCalculator(String mode) {
        Objects.requireNonNull(mode);
        if (mode.length() != 3) {
            throw new IllegalArgumentException("Mode must be 3 characters long.");
        }

        //@see https://nlp.stanford.edu/IR-book/html/htmledition/document-and-query-weighting-schemes-1.html

        //Term frequency strategy
        switch(mode.charAt(0)) {
            case 'n':
                tfStrategy = new NaturalTermFrequency();
                break;
            case 'l':
                tfStrategy = new LogarithmicTermFrequency();
                break;
            case 'a':
                tfStrategy = new AugmentedTermFrequency();
                break;
            case 'b':
                tfStrategy = new BooleanTermFrequency();
                break;
            case 'L':
                tfStrategy = new LogarithmicAverageTermFrequency();
                break;
            default:
                throw new IllegalArgumentException("Term frequency mode character must be in string: n1abL");
        }

        //Inverse document frequency strategy
        switch(mode.charAt(1)) {
            case 'n':
                idfStrategy = new NoInverseDocumentFrequency();
                break;
            case 't':
                idfStrategy = new LogarithmicInverseDocumentFrequency();
                break;
            case 'p':
                idfStrategy = new ProbabilisticInverseDocumentFrequency();
                break;
            default:
                throw new IllegalArgumentException("Inverse document frequency mode character must be in string: ntp");
        }

        //Normalization strategy
        switch(mode.charAt(2)) {
            case 'n':
                normStrategy = new NoNormalization();
                break;
            case 'c':
                normStrategy = new CosineNormalization();
                break;
                //todo: implement this (more complex, need to learn more theory!)
            /*case 'u':
                break;*/
            case 'b':
                normStrategy = new ByteLengthInverseDocumentFrequency();
                break;
            default:
                throw new IllegalArgumentException("Normalization mode character must be in string: nc");
        }
    }

    public double tf(double tf, TermInfo termInfo) {
        return tfStrategy.tf(tf, termInfo);
    }

    public double idf(int totalDocs, int termDocs) {
        return idfStrategy.idf(totalDocs, termDocs);
    }

    public double norm(TfIdfScore[] scores) {
        return normStrategy.norm(scores);
    }
}
