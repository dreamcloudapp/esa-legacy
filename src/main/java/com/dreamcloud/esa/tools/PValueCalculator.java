package com.dreamcloud.esa.tools;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.commons.math.stat.correlation.SpearmansCorrelation;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

class WordSimilarity {
    public String word1;
    public String word2;
    double similarity;
}

public class PValueCalculator {
    String spearmanFile;

    public PValueCalculator(String spearmanFile) {
        this.spearmanFile = spearmanFile;
    }

    public double getSpearmanCorrelation(SemanticSimilarityTool similarity) throws Exception {
        ArrayList<WordSimilarity> humanSimilarityList = this.readHumanScores();
        double[] humanScores = new double[humanSimilarityList.size()];
        double[] esaScores = new double[humanSimilarityList.size()];

        for(int i=0; i<humanSimilarityList.size(); i++) {
            WordSimilarity wordSim = humanSimilarityList.get(i);
            humanScores[i] = wordSim.similarity;
            esaScores[i] = similarity.findSemanticSimilarity(wordSim.word1, wordSim.word2);
        }

        SpearmansCorrelation spearmansCorrelation = new SpearmansCorrelation();
        return spearmansCorrelation.correlation(humanScores, esaScores);
    }

    private ArrayList<WordSimilarity> readHumanScores() throws IOException, CsvValidationException {
        ArrayList<WordSimilarity> humanSimilarityList = new ArrayList<>();
        CSVReader csvReader = new CSVReader(new FileReader(spearmanFile));
        String[] values;
        while ((values = csvReader.readNext()) != null) {
            if (values.length < 3 ) {
                throw new CsvValidationException("Word sim file improperly formatted.");
            }
            WordSimilarity wordSim = new WordSimilarity();
            wordSim.word1 = values[0];
            wordSim.word2 = values[1];
            wordSim.similarity = Double.parseDouble(values[2]) / 10.0;
            humanSimilarityList.add(wordSim);
        }
        return humanSimilarityList;
    }
}
