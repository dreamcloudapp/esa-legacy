package com.dreamcloud.esa.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

public class MySQLConnection {
    static String ODBC_ENVIRONMENT_VARIABLE = "DC_ESA_ODBC_CONNECTION_STRING";

    public static Connection getConnection() throws SQLException {
        Map<String, String> env = System.getenv();
        if (!env.containsKey(ODBC_ENVIRONMENT_VARIABLE)) {
            throw new SQLException("The ODBC connection string was empty: set the " + ODBC_ENVIRONMENT_VARIABLE + " and try again.");
        }
        return DriverManager.getConnection("jdbc:" + env.get(ODBC_ENVIRONMENT_VARIABLE));
    }
}
