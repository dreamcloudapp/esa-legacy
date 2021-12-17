package com.dreamcloud.esa.indexer;

import com.dreamcloud.esa.database.TfIdfScoreRepository;
import com.dreamcloud.esa.tfidf.TfIdfScore;
import com.dreamcloud.esa.vectorizer.PruneOptions;

import java.io.IOException;

public class IndexPruner {
    protected PruneOptions options;

    public IndexPruner(PruneOptions options) {
        this.options = options;
    }

    public void prune(TfIdfScoreRepository tfIdfScoreRepository) throws IOException {
        String[] terms = tfIdfScoreRepository.getTerms();
        for (String term: terms) {
            this.pruneTerm(term, tfIdfScoreRepository);
        }
    }

    private void pruneTerm(String term, TfIdfScoreRepository tfIdfScoreRepository) {
        if (!"link".equals(term) && !"number".equals(term)) {
            return;
        }
        int w = options.windowSize;
        TfIdfScore[] docScores = tfIdfScoreRepository.getTfIdfScores(term);
        for (TfIdfScore score: docScores) {
            if (w < docScores.length) {
                double firstScore = score.getScore();
                double lastScore = docScores[w].getScore();
                if ((firstScore - lastScore) < (firstScore * options.dropOff)) {
                    //tfIdfScoreRepository.pruneTerm(term, firstScore);
                    System.out.println("pruning " + term + " to " + w + " scores (" + firstScore + "), from " + docScores.length);
                    break;
                }
            }
            w++;
        }
    }
}
