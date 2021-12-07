package com.dreamcloud.esa.database;

import java.sql.*;
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
