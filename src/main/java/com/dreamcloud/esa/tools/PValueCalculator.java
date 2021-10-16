package com.dreamcloud.esa.tools;

import org.apache.commons.math.stat.correlation.SpearmansCorrelation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

class WordSimilarity {
    public String word1;
    public String word2;
    double similarity;
}

public class PValueCalculator {
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

    private ArrayList<WordSimilarity> readHumanScores() throws IOException {
        ArrayList<WordSimilarity> humanSimilarityList = new ArrayList<>();
        BufferedReader reader;
        reader = new BufferedReader(new FileReader("./src/data/en-wordsim353.txt"));
        String line = reader.readLine();
        while (line != null) {
            if (!"".equals(line)) {
                String[] wordSimParts = line.split("\t");
                if (wordSimParts.length < 3 ) {
                    throw new IOException("Word sim file improperly formatted.");
                }
                WordSimilarity wordSim = new WordSimilarity();
                wordSim.word1 = wordSimParts[0];
                wordSim.word2 = wordSimParts[1];
                wordSim.similarity = Double.parseDouble(wordSimParts[2]);
                humanSimilarityList.add(wordSim);
            }
            line = reader.readLine();
        }
        reader.close();
        return humanSimilarityList;
    }
}
