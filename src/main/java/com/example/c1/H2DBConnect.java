package com.example.c1;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetProvider;
import java.sql.*;

public class H2DBConnect {
    private static final String FILE_JDBC_URL = "jdbc:h2:~/testdb";
    private static final String MEMORY_JDBC_URL = "jdbc:h2:mem:testdb";
    private static final String USER = "sa";
    private static final String PASSWORD = "";
    private Connection connection;

    public void connect(boolean inMemory) throws SQLException {
        try {
            Class.forName("org.h2.Driver");
            String url = inMemory ? MEMORY_JDBC_URL : FILE_JDBC_URL;
            connection = DriverManager.getConnection(url, USER, PASSWORD);
            initializeDatabase();
        } catch (ClassNotFoundException e) {
            throw new SQLException("H2 driver not found", e);
        }
    }

    private void initializeDatabase() throws SQLException {
        // Create genres table if not exists
        executeUpdate("CREATE TABLE IF NOT EXISTS genres (" +
                "id INT PRIMARY KEY, " +
                "name VARCHAR(255) NOT NULL)");

        // Check if genres table is empty
        try (ResultSet rs = executeQuery("SELECT COUNT(*) FROM genres")) {
            if (rs.next() && rs.getInt(1) == 0) {
                // Insert default genres
                executeUpdate("INSERT INTO genres VALUES (1, 'Action')");
                executeUpdate("INSERT INTO genres VALUES (2, 'Comedy')");
                executeUpdate("INSERT INTO genres VALUES (3, 'Drama')");
                executeUpdate("INSERT INTO genres VALUES (4, 'Sci-Fi')");
                executeUpdate("INSERT INTO genres VALUES (5, 'Thriller')");
            }
        }

        // Create directors table if not exists
        executeUpdate("CREATE TABLE IF NOT EXISTS directors (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(255) NOT NULL)");

        // Check if directors table is empty
        try (ResultSet rs = executeQuery("SELECT COUNT(*) FROM directors")) {
            if (rs.next() && rs.getInt(1) == 0) {
                // Insert default directors
                executeUpdate("INSERT INTO directors (name) VALUES ('Christopher Nolan')");
                executeUpdate("INSERT INTO directors (name) VALUES ('Quentin Tarantino')");
                executeUpdate("INSERT INTO directors (name) VALUES ('Steven Spielberg')");
                executeUpdate("INSERT INTO directors (name) VALUES ('James Cameron')");
                executeUpdate("INSERT INTO directors (name) VALUES ('Martin Scorsese')");
            }
        }

        // Create movies table if not exists
        executeUpdate("CREATE TABLE IF NOT EXISTS movies (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "title VARCHAR(255) NOT NULL, " +
                "original_title VARCHAR(255) NOT NULL, " +
                "release_year INT NOT NULL, " +
                "imdb_rating DECIMAL(3,1), " +
                "views INT DEFAULT 0, " +
                "director_id INT, " +
                "genre_id INT, " +
                "FOREIGN KEY (director_id) REFERENCES directors(id), " +
                "FOREIGN KEY (genre_id) REFERENCES genres(id))");

        // Check if movies table is empty
        try (ResultSet rs = executeQuery("SELECT COUNT(*) FROM movies")) {
            if (rs.next() && rs.getInt(1) == 0) {
                // Insert sample movies
                executeUpdate("INSERT INTO movies (title, original_title, release_year, imdb_rating, views, director_id, genre_id) " +
                        "VALUES ('Inception', 'Inception', 2010, 8.8, 100, 1, 4)");
                executeUpdate("INSERT INTO movies (title, original_title, release_year, imdb_rating, views, director_id, genre_id) " +
                        "VALUES ('The Dark Knight', 'The Dark Knight', 2008, 9.0, 150, 1, 1)");
                executeUpdate("INSERT INTO movies (title, original_title, release_year, imdb_rating, views, director_id, genre_id) " +
                        "VALUES ('Pulp Fiction', 'Pulp Fiction', 1994, 8.9, 120, 2, 3)");
                executeUpdate("INSERT INTO movies (title, original_title, release_year, imdb_rating, views, director_id, genre_id) " +
                        "VALUES ('Interstellar', 'Interstellar', 2014, 8.6, 90, 1, 4)");
            }
        }
    }

    public CachedRowSet executeQuery(String query) throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            CachedRowSet crs = RowSetProvider.newFactory().createCachedRowSet();
            crs.populate(rs);
            return crs;
        }
    }

    public int executeUpdate(String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            return stmt.executeUpdate(sql);
        }
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return connection.prepareStatement(sql);
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return connection.prepareStatement(sql, autoGeneratedKeys);
    }

    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Connection close error: " + e.getMessage());
            }
        }
    }

    public Connection getConnection() {
        return connection;
    }
}