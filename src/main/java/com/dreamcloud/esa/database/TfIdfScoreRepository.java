package com.dreamcloud.esa.database;

import com.dreamcloud.esa.tfidf.TfIdfAnalyzer;
import com.dreamcloud.esa.tfidf.TfIdfScore;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;

import java.sql.*;
import java.util.ArrayList;

public class TfIdfScoreRepository {
    Connection con;

    public TfIdfScoreRepository() {

    }

    public void getTermDocumentFrequencies(TfIdfAnalyzer tfIdfAnalyzer) {
        try {
            if (this.con == null) {
                this.con = MySQLConnection.getConnection();
            }

            MutableObjectIntMap<String> termFrequencies = ObjectIntMaps.mutable.empty();

            PreparedStatement freqStatement = con.prepareStatement("select * from esa.df");
            ResultSet resultSet = freqStatement.executeQuery();
            int resultCount = 0;
            while (resultSet.next()) {
                String term = resultSet.getString(1);
                int count = resultSet.getInt(2);
                termFrequencies.put(term, count);
                resultCount++;
            }
            tfIdfAnalyzer.setDocumentCount(resultCount);
            tfIdfAnalyzer.setDocumentFrequencies(termFrequencies);
        }
        catch (Exception e) {
            System.out.println("Postgres is unhappy about something:");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void saveTermDocumentFrequencies(MutableObjectIntMap<String> termDocumentFrequencies) {
        try {
            if (this.con == null) {
                this.con = MySQLConnection.getConnection();
            }

            PreparedStatement freqStatement = con.prepareStatement("insert into esa.df(term, frequency) values(?, ?)");
            int i = 0;
            for (String term: termDocumentFrequencies.keySet()) {
                int count = termDocumentFrequencies.get(term);
                freqStatement.setString(1, term);
                freqStatement.setInt(2, count);
                freqStatement.executeUpdate();
                if(i++ % 1000 == 0) {
                    System.out.println("Saved document frequency: [" + term + "\t" + count + "]");
                }
            }
        }
        catch (Exception e) {
            System.out.println("Postgres is unhappy about something:");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public TfIdfScore[] getTfIdfScores(String term) {
        try {
            if (this.con == null) {
                this.con = MySQLConnection.getConnection();
            }

            PreparedStatement scoreStatement = con.prepareStatement("select document, score from esa.score where term = ?");
            scoreStatement.setString(1, term);
            ResultSet resultSet = scoreStatement.executeQuery();
            ArrayList<TfIdfScore> scores = new ArrayList<>();
            while (resultSet.next()) {
                String document = resultSet.getString(1);
                double score = resultSet.getDouble(2);
                scores.add(new TfIdfScore(document, term, score));
            }
            return scores.toArray(TfIdfScore[]::new);
        }
        catch (Exception e) {
            System.out.println("Postgres is unhappy about something:");
            e.printStackTrace();
            System.exit(-1);
        }
        return null;
    }

    public void saveTfIdfScores(String document, TfIdfScore[] scores) {
        try {
            if (this.con == null) {
                this.con = MySQLConnection.getConnection();
            }

            PreparedStatement scoreStatement = con.prepareStatement("insert into esa.score(document, term, score) values(?, ?, ?)");
            for (TfIdfScore score: scores) {
                scoreStatement.setString(1, document.substring(0, Math.min(128, document.length())));
                scoreStatement.setString(2, score.getTerm());
                if (Double.isNaN(score.getScore())) {
                    System.out.println("Found a NaN: " + score.getTerm() + ", " + document);
                }
                scoreStatement.setDouble(3, score.getScore());
                scoreStatement.executeUpdate();
            }
        }
        catch (Exception e) {
            System.out.println("Postgres is unhappy about something:");
            e.printStackTrace();
            System.exit(-1);
        }
    }
}

