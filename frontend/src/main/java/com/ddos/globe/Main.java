package com.ddos.globe;

import javafx.application.Application;
import javafx.stage.Stage;
import java.sql.*;

public class Main extends Application {
    
    private static final String DB_URL = "jdbc:sqlite:users.db";
    
    @Override
    public void start(Stage primaryStage) {
        setupDatabase();
        
        if (checkLoggedIn()) {
            // User is logged in, go directly to main app
            launchDDoSGlobe(primaryStage);
        } else {
            // User not logged in, show login page
            launchLoginPage(primaryStage);
        }
    }
    
    private void setupDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            
            // Create simple sessions table
            String sql = """
                CREATE TABLE IF NOT EXISTS sessions (
                    username TEXT PRIMARY KEY,
                    logged_in BOOLEAN DEFAULT 0
                )
            """;
            stmt.execute(sql);
            
        } catch (SQLException e) {
            System.out.println("Database setup: " + e.getMessage());
        }
    }
    
    private boolean checkLoggedIn() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM sessions WHERE logged_in = 1");
            return rs.getInt(1) > 0;
            
        } catch (SQLException e) {
            System.out.println("Check login: " + e.getMessage());
            return false;
        }
    }
    
    public static void setUserLoggedIn(String username, boolean loggedIn) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                 "INSERT OR REPLACE INTO sessions (username, logged_in) VALUES (?, ?)")) {
            
            pstmt.setString(1, username);
            pstmt.setBoolean(2, loggedIn);
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            System.out.println("Set login: " + e.getMessage());
        }
    }
    
    private void launchLoginPage(Stage primaryStage) {
        try {
            LoginPage loginPage = new LoginPage();
            loginPage.start(primaryStage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void launchDDoSGlobe(Stage primaryStage) {
        try {
            DDosGlobe globeApp = new DDosGlobe();
            globeApp.start(primaryStage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.out.println("SQLite JDBC driver not found");
            return;
        }
        launch(args);
    }
}