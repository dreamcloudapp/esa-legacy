package com.dreamcloud.esa.database;

import org.apache.lucene.util.BytesRef;

import java.util.ArrayList;

public class TermScores {
    public BytesRef term;
    public ArrayList<TermScore> scores;

    public TermScores(BytesRef term) {
        this.term = term;
        scores = new ArrayList<>();
    }
}
