package com.dreamcloud.esa.tools;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.commons.math.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math.stat.correlation.SpearmansCorrelation;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class PValueCalculator {
    File file;
    File documentFile;
    ArrayList<DocumentSimilarity> humanSimilarityList;

    public static void resolveDocuments(ArrayList<DocumentSimilarity> docSims, File documentFile) throws IOException, CsvValidationException {
        CSVReader csvReader = new CSVReader(new FileReader(documentFile));
        ArrayList<String> documents = new ArrayList<>();
        String[] values;
        while ((values = csvReader.readNext()) != null) {
            if (values.length < 1 ) {
                throw new CsvValidationException("LP50 document file improperly formatted.");
            }
            documents.add(values[0]);
        }

        for (DocumentSimilarity docSim: docSims) {
            int doc1 = Integer.parseInt(docSim.doc1);
            int doc2 = Integer.parseInt(docSim.doc2);
            if (documents.size() <= doc1) {
                throw new CsvValidationException("LP50 document " + doc1 + " could not be found.");
            }
            if (documents.size() <= doc2) {
                throw new CsvValidationException("LP50 document " + doc1 + " could not be found.");
            }
            docSim.doc1 = documents.get(doc1);
            docSim.doc2 = documents.get(doc2);
        }
    }

    public PValueCalculator(File file) {
        this.file = file;
    }

    public PValueCalculator(File file, File documentFile) {
        this.file = file;
        this.documentFile = documentFile;
    }

    public double getPearsonCorrelation(SemanticSimilarityTool similarity) throws Exception {
        ArrayList<DocumentSimilarity> humanSimilarityList = this.readHumanScores();
        double[] humanScores = this.getHumanScores(humanSimilarityList);
        double[] esaScores = this.getEsaScores(humanSimilarityList, similarity);
        PearsonsCorrelation pearsonsCorrelation = new PearsonsCorrelation();
        return pearsonsCorrelation.correlation(humanScores, esaScores);
    }

    public double getSpearmanCorrelation(SemanticSimilarityTool similarity) throws Exception {
        ArrayList<DocumentSimilarity> humanSimilarityList = this.readHumanScores();
        double[] humanScores = this.getHumanScores(humanSimilarityList);
        double[] esaScores = this.getEsaScores(humanSimilarityList, similarity);
        SpearmansCorrelation spearmansCorrelation = new SpearmansCorrelation();
        return spearmansCorrelation.correlation(humanScores, esaScores);
    }

    private double[] getEsaScores(ArrayList<DocumentSimilarity> humanSimilarityList, SemanticSimilarityTool similarity) throws Exception {
        double[] esaScores = new double[humanSimilarityList.size()];
        for(int i=0; i<humanSimilarityList.size(); i++) {
            DocumentSimilarity docSim = humanSimilarityList.get(i);
            esaScores[i] = similarity.findSemanticSimilarity(docSim.doc1, docSim.doc2);

            String sourceDesc = docSim.doc1.substring(0, Math.min(16, docSim.doc1.length()));
            String compareDesc = docSim.doc2.substring(0, Math.min(16, docSim.doc2.length()));
            System.out.println("doc " + i + "\t ('" + sourceDesc + "', '" + compareDesc + "'):\t" + esaScores[i]);
        }
        return esaScores;
    }

    private double[] getHumanScores(ArrayList<DocumentSimilarity> humanSimilarityList) {
        double[] humanScores = new double[humanSimilarityList.size()];
        for(int i=0; i<humanSimilarityList.size(); i++) {
            DocumentSimilarity docSim = humanSimilarityList.get(i);
            humanScores[i] = docSim.similarity;
        }
        return humanScores;
    }

    public ArrayList<DocumentSimilarity> readHumanScores() throws IOException, CsvValidationException {
        if (humanSimilarityList == null) {
            humanSimilarityList = new ArrayList<>();
            CSVReader csvReader = new CSVReader(new FileReader(file));
            String[] values;
            while ((values = csvReader.readNext()) != null) {
                if (values.length < 3 ) {
                    throw new CsvValidationException("Word sim file improperly formatted.");
                }
                DocumentSimilarity docSim = new DocumentSimilarity();
                docSim.doc1 = values[0];
                docSim.doc2 = values[1];
                docSim.similarity = Double.parseDouble(values[2]) / 10.0;
                humanSimilarityList.add(docSim);
            }
            if (documentFile != null) {
                resolveDocuments(humanSimilarityList, documentFile);
            }
        }
        return humanSimilarityList;
    }
}
