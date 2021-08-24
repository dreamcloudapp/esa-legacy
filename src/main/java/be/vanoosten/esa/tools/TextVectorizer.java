package be.vanoosten.esa.tools;

public interface TextVectorizer {
    ConceptVector vectorize(String text) throws Exception;
}
