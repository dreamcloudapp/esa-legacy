package com.dreamcloud.esa.database;

import com.dreamcloud.esa.tfidf.TfIdfScore;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;

import java.sql.*;

public class TfIdfScoreRepository {
    PreparedStatement statement;
    Connection con;

    public TfIdfScoreRepository() {

    }

    protected static byte hexToByte(String hexString) {
        int firstDigit = toDigit(hexString.charAt(0));
        int secondDigit = toDigit(hexString.charAt(1));
        return (byte) ((firstDigit << 4) + secondDigit);
    }

    protected static int toDigit(char hexChar) {
        int digit = Character.digit(hexChar, 16);
        if(digit == -1) {
            throw new IllegalArgumentException(
                    "Invalid Hexadecimal Character: "+ hexChar);
        }
        return digit;
    }

    protected static byte[] decodeHexString(String hexString) {
        if (hexString.length() % 2 == 1) {
            throw new IllegalArgumentException(
                    "Invalid hexadecimal String supplied.");
        }

        byte[] bytes = new byte[hexString.length() / 2];
        for (int i = 0; i < hexString.length(); i += 2) {
            bytes[i / 2] = hexToByte(hexString.substring(i, i + 2));
        }
        return bytes;
    }

    protected PreparedStatement getStatement() throws SQLException {
        if (statement == null) {
            statement = con.prepareStatement("insert into dc.term_map(`term_id`, `concept_id`, `score`) values(?, ?, ?)");
        }
        return statement;
    }

    public void saveTermDocumentFrequencies(MutableObjectIntMap<String> termDocumentFrequencies) {
        try {
            if (this.con == null) {
                this.con = MySQLConnection.getConnection();
            }

            PreparedStatement freqStatement = con.prepareStatement("insert into esa.df(term, count, ) values(?, ?)");
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

