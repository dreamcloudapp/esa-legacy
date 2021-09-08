package be.vanoosten.esa.database;

import java.util.Vector;

public class DocumentVector {
    public String id;
    public Vector<ConceptWeight> conceptWeights;

    public DocumentVector(String id) {
        this.id = id;
        conceptWeights = new Vector<>();
    }

    public DocumentVector(String id, Vector<ConceptWeight> conceptWeights) {
        this.id = id;
        this.conceptWeights = conceptWeights;
    }

    public void addConceptWeight(ConceptWeight conceptWeight) {
        this.conceptWeights.add(conceptWeight);
    }
}
