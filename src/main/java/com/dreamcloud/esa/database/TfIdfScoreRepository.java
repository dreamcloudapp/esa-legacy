package com.dreamcloud.esa.database;

import com.dreamcloud.esa.tfidf.TfIdfScore;

import java.sql.*;
import java.util.UUID;

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

    protected static byte[] generateUuid() {
        UUID id = UUID.randomUUID();
        return decodeHexString(id.toString().replace("-", ""));
    }

    public void saveTfIdfScores(String document, TfIdfScore[] scores) {
        try {
            if (this.con == null) {
                this.con = MySQLConnection.getConnection();
            }

            //create document
            PreparedStatement docStatement = con.prepareStatement("insert into dc.document(`id`, `title`) values(?, ?)");
            byte[] docId = generateUuid();
            docStatement.setBytes(1, docId);
            docStatement.setString(2, document);
            docStatement.executeUpdate();

            for (TfIdfScore score: scores) {
                PreparedStatement termStatement = con.prepareStatement("insert into dc.term(`id`, `term`) values(?, ?) on duplicate key update term = term");
                byte[] termId = generateUuid();
                termStatement.setBytes(1, termId);
                termStatement.setString(2, score.getTerm());
                termStatement.executeUpdate();

                PreparedStatement scoreStatement = con.prepareStatement("insert into dc.score(`document_id`, `term_id`, `score`) values(?, ?, ?)");
                scoreStatement.setBytes(1, docId);
                scoreStatement.setBytes(2, termId);
                scoreStatement.setDouble(3, score.getScore());
                scoreStatement.executeUpdate();
            }
        }
        catch (Exception e) {
            System.out.println("MySQL is unhappy about something:");
            e.printStackTrace();
            System.exit(-1);
        }
    }
}

