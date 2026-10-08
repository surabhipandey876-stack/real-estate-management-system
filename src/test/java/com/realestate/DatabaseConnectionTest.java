package com.realestate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

class DatabaseConnectionTest {

    @Test
    void shouldConnectToMysqlDatabase() throws Exception {
        String url = System.getenv().getOrDefault(
                "DB_URL",
                "jdbc:mysql://localhost:3306/real_estate_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
        );

        String username = System.getenv().getOrDefault("DB_USERNAME", "root");
        String password = System.getenv("DB_PASSWORD");

        Assumptions.assumeTrue(password != null && !password.isBlank(),
                "Skipping MySQL integration check because DB_PASSWORD is not configured.");

        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            assertNotNull(connection);
            assertTrue(connection.isValid(5));
            assertEquals("real_estate_db", connection.getCatalog());

            try (Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery(
                            "SELECT id, first_name, last_name, email, password_hash, phone_number, role_id, active "
                                    + "FROM users WHERE 1 = 0"
                    )) {
                assertFalse(result.next(), "The registration user columns should exist.");
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM roles WHERE name = ?"
            )) {
                statement.setString(1, "CUSTOMER");
                try (ResultSet result = statement.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals(1, result.getInt(1), "The CUSTOMER role must be seeded before registration.");
                }
            }
        }
    }
}
