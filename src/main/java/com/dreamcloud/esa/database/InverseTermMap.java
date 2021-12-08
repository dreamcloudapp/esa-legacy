package com.dreamcloud.esa.database;

import com.dreamcloud.esa.vectorizer.ConceptVector;
import org.apache.commons.collections15.map.LazyMap;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InverseTermMap {
    PreparedStatement statement;
    Connection con;

    public InverseTermMap() {

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

    public ConceptVector getTermVector(String term) throws SQLException, IOException {
        if (this.con == null) {
            this.con = MySQLConnection.getConnection();
        }

        byte[] termId = null;
        PreparedStatement termIdStatement = con.prepareStatement("select id from dc.term where term = ?");
        termIdStatement.setString(1, term);
        ResultSet resultSet = termIdStatement.executeQuery();
        while(resultSet.next()) {
             termId = resultSet.getBytes(1);
            break;
        }

        Map<String, Float> conceptWeights = new HashMap<>();
        ConceptVector conceptVector = new ConceptVector(conceptWeights);
        if (termId != null) {
            PreparedStatement termScoreStatement = con.prepareStatement("select concept_id, score from dc.term_map where term_id = ?");
            termScoreStatement.setBytes(1, termId);

            resultSet = termScoreStatement.executeQuery();
            while(resultSet.next()) {
                String conceptId = String.valueOf(resultSet.getInt(1));
                float score = resultSet.getFloat(2);
                conceptWeights.put(conceptId, score);
            }
        }
        return conceptVector;
    }

    public void saveTermScores(TermScores termScores) {
        try {
            if (this.con == null) {
                this.con = MySQLConnection.getConnection();
            }

            //Create term
            PreparedStatement termStatement = con.prepareStatement("insert into dc.term(`id`, `term`) values(?, ?)");
            byte[] id = generateUuid();
            termStatement.setBytes(1, id);
            termStatement.setString(2, termScores.term.utf8ToString());

            termStatement.executeUpdate();

            PreparedStatement statement = getStatement();
            for (TermScore score : termScores.scores) {
                statement.setBytes(1, id);
                statement.setLong(2, score.docId);
                statement.setFloat(3, score.score);
                statement.executeUpdate();
            }
        }
        catch (Exception e) {
            System.out.println("MySQL is unhappy about something:");
            e.printStackTrace();
        }
    }
}
