package by.bsuir.vstdio.dao;

import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public enum ConnectionsHandler {
    INSTANCE;

    @NotNull
    public Connection getConnection() {
        try {
            return DriverManager.getConnection("jdbc:mysql://localhost:3306/web_cinema?serverTimezone=Europe/Moscow&useSSL=false&allowPublicKeyRetrieval=true",
                    "root",
                    "!Fib1235813");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
