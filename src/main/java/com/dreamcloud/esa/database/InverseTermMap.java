package com.dreamcloud.esa.database;

import java.sql.*;

public class InverseTermMap {
    PreparedStatement statement;
    Connection con;

    public InverseTermMap(Connection con) {
        this.con = con;
    }

    protected PreparedStatement getStatement() throws SQLException {
        if (statement == null) {
            statement = con.prepareStatement("insert into dc.term_map(`term_id`, `concept_id`, `score`) values(?, ?, ?)");
        }
        return statement;
    }

    public void saveTermScores(TermScores termScores) throws SQLException {
        //Create term
        PreparedStatement termStatement = con.prepareStatement("insert into dc.term(`term`) values(?)", Statement.NO_GENERATED_KEYS);
        termStatement.setString(1, termScores.term.utf8ToString());
        long termId = termStatement.executeUpdate();

        PreparedStatement  statement = getStatement();
        for(TermScore score: termScores.scores) {
            statement.setLong(1, termId);
            statement.setLong(2, score.docId);
            statement.setFloat(3, score.score);
            statement.executeUpdate();
        }
    }
}
