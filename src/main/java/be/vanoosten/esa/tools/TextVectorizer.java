package be.vanoosten.esa.tools;

import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;

public interface TextVectorizer {
    public ConceptVector vectorize(String text) throws Exception;
}
