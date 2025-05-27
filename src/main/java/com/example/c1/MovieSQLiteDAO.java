package com.example.c1;

import javax.sql.rowset.CachedRowSet;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MovieSQLiteDAO implements MovieDAO {
    private final SQLiteDBConnect dbConnect;
    private final GenreDAO genreDAO;
    private final DirectorDAO directorDAO;

    public MovieSQLiteDAO(GenreDAO genreDAO, DirectorDAO directorDAO) {
        this.genreDAO = genreDAO;
        this.directorDAO = directorDAO;
        this.dbConnect = new SQLiteDBConnect("media_library.db");
        try {
            dbConnect.connect();
            initializeDatabase();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to SQLite database", e);
        }
    }

    private void initializeDatabase() {
        try {
            // Create tables if they don't exist
            dbConnect.executeUpdate("CREATE TABLE IF NOT EXISTS movies (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "title TEXT NOT NULL, " +
                    "original_title TEXT NOT NULL, " +
                    "year INTEGER NOT NULL, " +
                    "imdb_rating REAL, " +
                    "views INTEGER DEFAULT 0, " +
                    "director_id INTEGER, " +
                    "genre_id INTEGER, " +
                    "FOREIGN KEY (director_id) REFERENCES directors(id), " +
                    "FOREIGN KEY (genre_id) REFERENCES genres(id))");

            dbConnect.executeUpdate("CREATE TABLE IF NOT EXISTS movie_schedules (" +
                    "movie_id INTEGER PRIMARY KEY, " +
                    "planned_date DATE NOT NULL, " +
                    "completion_date DATE, " +
                    "reminder_sent BOOLEAN DEFAULT FALSE, " +
                    "FOREIGN KEY (movie_id) REFERENCES movies(id))");

            dbConnect.executeUpdate("CREATE TABLE IF NOT EXISTS schedule_changes (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "movie_id INTEGER NOT NULL, " +
                    "old_date DATE NOT NULL, " +
                    "new_date DATE NOT NULL, " +
                    "reason TEXT NOT NULL, " +
                    "change_date DATE NOT NULL, " +
                    "FOREIGN KEY (movie_id) REFERENCES movies(id))");

            // Insert initial data if tables are empty
            if (isTableEmpty("movies")) {
                insertInitialData();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error initializing database", e);
        }
    }

    private boolean isTableEmpty(String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (ResultSet rs = dbConnect.executeQuery(sql)) {
            return rs.next() && rs.getInt(1) == 0;
        }
    }

    private void insertInitialData() throws SQLException {
        // Insert sample directors
        dbConnect.executeUpdate("INSERT INTO directors (name) VALUES ('Christopher Nolan')");
        dbConnect.executeUpdate("INSERT INTO directors (name) VALUES ('Quentin Tarantino')");

        // Insert sample genres
        dbConnect.executeUpdate("INSERT INTO genres (id, name) VALUES (1, 'Action')");
        dbConnect.executeUpdate("INSERT INTO genres (id, name) VALUES (2, 'Drama')");

        // Insert sample movies
        dbConnect.executeUpdate("INSERT INTO movies (title, original_title, year, imdb_rating, views, director_id, genre_id) " +
                "VALUES ('Inception', 'Inception', 2010, 8.8, 100, 1, 1)");
        dbConnect.executeUpdate("INSERT INTO movies (title, original_title, year, imdb_rating, views, director_id, genre_id) " +
                "VALUES ('Pulp Fiction', 'Pulp Fiction', 1994, 8.9, 150, 2, 2)");
    }

    @Override
    public void addMovie(Movie movie) {
        String sql = "INSERT INTO movies (title, original_title, year, imdb_rating, views, director_id, genre_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = dbConnect.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, movie.getTitle());
            pstmt.setString(2, movie.getOriginalTitle());
            pstmt.setInt(3, movie.getYear());
            pstmt.setDouble(4, movie.getImdbRating());
            pstmt.setInt(5, movie.getViews());
            pstmt.setInt(6, movie.getDirector().getId());
            pstmt.setInt(7, movie.getGenre().getId());
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    movie.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error adding movie", e);
        }
    }

    @Override
    public void updateMovie(Movie movie) {
        String sql = "UPDATE movies SET title = ?, original_title = ?, year = ?, " +
                "imdb_rating = ?, views = ?, director_id = ?, genre_id = ? WHERE id = ?";
        try (PreparedStatement pstmt = dbConnect.prepareStatement(sql)) {
            pstmt.setString(1, movie.getTitle());
            pstmt.setString(2, movie.getOriginalTitle());
            pstmt.setInt(3, movie.getYear());
            pstmt.setDouble(4, movie.getImdbRating());
            pstmt.setInt(5, movie.getViews());
            pstmt.setInt(6, movie.getDirector().getId());
            pstmt.setInt(7, movie.getGenre().getId());
            pstmt.setInt(8, movie.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating movie", e);
        }
    }

    @Override
    public void deleteMovie(int id) {
        try {
            // First delete schedule-related data
            deleteScheduleData(id);

            // Then delete the movie
            String sql = "DELETE FROM movies WHERE id = ?";
            try (PreparedStatement pstmt = dbConnect.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting movie", e);
        }
    }

    private void deleteScheduleData(int movieId) throws SQLException {
        String deleteChangesSql = "DELETE FROM schedule_changes WHERE movie_id = ?";
        try (PreparedStatement pstmt = dbConnect.prepareStatement(deleteChangesSql)) {
            pstmt.setInt(1, movieId);
            pstmt.executeUpdate();
        }

        String deleteScheduleSql = "DELETE FROM movie_schedules WHERE movie_id = ?";
        try (PreparedStatement pstmt = dbConnect.prepareStatement(deleteScheduleSql)) {
            pstmt.setInt(1, movieId);
            pstmt.executeUpdate();
        }
    }

    @Override
    public List<Movie> getAllMovies() {
        List<Movie> movies = new ArrayList<>();
        String sql = "SELECT m.id, m.title, m.original_title, m.year, m.imdb_rating, m.views, " +
                "m.director_id, m.genre_id, d.name AS director_name, g.name AS genre_name " +
                "FROM movies m " +
                "LEFT JOIN directors d ON m.director_id = d.id " +
                "LEFT JOIN genres g ON m.genre_id = g.id";

        try (CachedRowSet crs = dbConnect.executeQuery(sql)) {
            while (crs.next()) {
                Director director = new Director(crs.getInt("director_id"), crs.getString("director_name"));
                Genre genre = new Genre(crs.getInt("genre_id"), crs.getString("genre_name"));
                movies.add(new Movie(
                        crs.getInt("id"),
                        crs.getString("title"),
                        crs.getString("original_title"),
                        crs.getInt("year"),
                        crs.getDouble("imdb_rating"),
                        crs.getInt("views"),
                        director,
                        genre
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting movies", e);
        }
        return movies;
    }

    @Override
    public void setMovieSchedule(int movieId, LocalDate plannedDate) {
        String sql = "INSERT OR REPLACE INTO movie_schedules (movie_id, planned_date, completion_date, reminder_sent) " +
                "VALUES (?, ?, NULL, FALSE)";
        try (PreparedStatement pstmt = dbConnect.prepareStatement(sql)) {
            pstmt.setInt(1, movieId);
            pstmt.setDate(2, Date.valueOf(plannedDate));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error setting movie schedule", e);
        }
    }

    @Override
    public void updateMovieSchedule(int movieId, LocalDate newDate, String reason) {
        try {
            // First get current date to save in history
            Optional<LocalDate> currentDate = getCurrentPlannedDate(movieId);
            if (!currentDate.isPresent()) {
                throw new RuntimeException("No existing schedule found for movie ID: " + movieId);
            }

            // Save change to history
            String historySql = "INSERT INTO schedule_changes (movie_id, old_date, new_date, reason, change_date) " +
                    "VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = dbConnect.prepareStatement(historySql)) {
                pstmt.setInt(1, movieId);
                pstmt.setDate(2, Date.valueOf(currentDate.get()));
                pstmt.setDate(3, Date.valueOf(newDate));
                pstmt.setString(4, reason);
                pstmt.setDate(5, Date.valueOf(LocalDate.now()));
                pstmt.executeUpdate();
            }

            // Update the schedule
            String updateSql = "UPDATE movie_schedules SET planned_date = ?, reminder_sent = FALSE WHERE movie_id = ?";
            try (PreparedStatement pstmt = dbConnect.prepareStatement(updateSql)) {
                pstmt.setDate(1, Date.valueOf(newDate));
                pstmt.setInt(2, movieId);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error updating movie schedule", e);
        }
    }

    private Optional<LocalDate> getCurrentPlannedDate(int movieId) throws SQLException {
        String sql = "SELECT planned_date FROM movie_schedules WHERE movie_id = ?";
        try (PreparedStatement pstmt = dbConnect.prepareStatement(sql)) {
            pstmt.setInt(1, movieId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getDate("planned_date").toLocalDate());
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public MovieSchedule getMovieSchedule(int movieId) {
        String sql = "SELECT planned_date, completion_date, reminder_sent FROM movie_schedules WHERE movie_id = ?";
        try (PreparedStatement pstmt = dbConnect.prepareStatement(sql)) {
            pstmt.setInt(1, movieId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    MovieSchedule schedule = new MovieSchedule(movieId, rs.getDate("planned_date").toLocalDate());
                    if (rs.getDate("completion_date") != null) {
                        schedule.markAsCompleted(rs.getDate("completion_date").toLocalDate());
                    }
                    schedule.setReminderSent(rs.getBoolean("reminder_sent"));
                    return schedule;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting movie schedule", e);
        }
        return null;
    }

    @Override
    public List<Movie> getMoviesWithUpcomingDeadlines(int daysBefore) {
        LocalDate now = LocalDate.now();
        LocalDate deadline = now.plusDays(daysBefore);

        String sql = "SELECT m.id, m.title, m.original_title, m.year, m.imdb_rating, m.views, " +
                "m.director_id, m.genre_id, d.name AS director_name, g.name AS genre_name " +
                "FROM movies m " +
                "JOIN movie_schedules ms ON m.id = ms.movie_id " +
                "JOIN directors d ON m.director_id = d.id " +
                "JOIN genres g ON m.genre_id = g.id " +
                "WHERE ms.planned_date BETWEEN ? AND ? " +
                "AND ms.completion_date IS NULL " +
                "AND (ms.reminder_sent = FALSE OR ms.reminder_sent IS NULL)";

        List<Movie> movies = new ArrayList<>();
        try (PreparedStatement pstmt = dbConnect.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(now));
            pstmt.setDate(2, Date.valueOf(deadline));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Director director = new Director(rs.getInt("director_id"), rs.getString("director_name"));
                    Genre genre = new Genre(rs.getInt("genre_id"), rs.getString("genre_name"));
                    movies.add(new Movie(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("original_title"),
                            rs.getInt("year"),
                            rs.getDouble("imdb_rating"),
                            rs.getInt("views"),
                            director,
                            genre
                    ));
                }
            }

            // Mark reminders as sent
            if (!movies.isEmpty()) {
                String updateSql = "UPDATE movie_schedules SET reminder_sent = TRUE " +
                        "WHERE planned_date BETWEEN ? AND ?";
                try (PreparedStatement updatePstmt = dbConnect.prepareStatement(updateSql)) {
                    updatePstmt.setDate(1, Date.valueOf(now));
                    updatePstmt.setDate(2, Date.valueOf(deadline));
                    updatePstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting movies with upcoming deadlines", e);
        }
        return movies;
    }

    @Override
    public void markMovieAsWatched(int movieId) {
        String sql = "UPDATE movie_schedules SET completion_date = ? WHERE movie_id = ?";
        try (PreparedStatement pstmt = dbConnect.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(LocalDate.now()));
            pstmt.setInt(2, movieId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error marking movie as watched", e);
        }
    }

    @Override
    public List<ScheduleChange> getScheduleHistory(int movieId) {
        String sql = "SELECT old_date, new_date, reason, change_date FROM schedule_changes " +
                "WHERE movie_id = ? ORDER BY change_date DESC";
        List<ScheduleChange> history = new ArrayList<>();

        try (PreparedStatement pstmt = dbConnect.prepareStatement(sql)) {
            pstmt.setInt(1, movieId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    history.add(new ScheduleChange(
                            rs.getDate("old_date").toLocalDate(),
                            rs.getDate("new_date").toLocalDate(),
                            rs.getString("reason"),
                            rs.getDate("change_date").toLocalDate()
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting schedule history", e);
        }
        return history;
    }

    @Override
    public List<Movie> smartSearch(Genre genre, double minRating, int minYear) {
        List<Movie> result = new ArrayList<>();
        String sql = "SELECT m.id, m.title, m.original_title, m.year, m.imdb_rating, m.views, " +
                "m.director_id, d.name AS director_name " +
                "FROM movies m " +
                "JOIN directors d ON m.director_id = d.id " +
                "WHERE m.genre_id = ? AND m.imdb_rating >= ? AND m.year >= ?";

        try (PreparedStatement pstmt = dbConnect.prepareStatement(sql)) {
            pstmt.setInt(1, genre.getId());
            pstmt.setDouble(2, minRating);
            pstmt.setInt(3, minYear);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Director director = new Director(rs.getInt("director_id"), rs.getString("director_name"));
                    result.add(new Movie(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("original_title"),
                            rs.getInt("year"),
                            rs.getDouble("imdb_rating"),
                            rs.getInt("views"),
                            director,
                            genre
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error in smart search", e);
        }
        return result;
    }

    @Override
    public double getDirectorViewPercentage(Director director) {
        try {
            String totalSql = "SELECT SUM(views) AS total FROM movies WHERE director_id = ?";
            String allSql = "SELECT SUM(views) AS all_total FROM movies";

            int directorViews = 0;
            int allViews = 0;

            try (PreparedStatement pstmt = dbConnect.prepareStatement(totalSql)) {
                pstmt.setInt(1, director.getId());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        directorViews = rs.getInt("total");
                    }
                }
            }

            try (ResultSet rs = dbConnect.executeQuery(allSql)) {
                if (rs.next()) {
                    allViews = rs.getInt("all_total");
                }
            }

            if (allViews == 0) return 0;
            return (double) directorViews / allViews * 100;
        } catch (SQLException e) {
            throw new RuntimeException("Error calculating view percentage", e);
        }
    }

    @Override
    public List<Movie> findDuplicatesByTmdb() {
        List<Movie> duplicates = new ArrayList<>();
        String sql = "SELECT m1.id, m1.title, m1.original_title, m1.year, m1.imdb_rating, " +
                "m1.views, m1.director_id, m1.genre_id, d.name AS director_name, g.name AS genre_name " +
                "FROM movies m1 " +
                "JOIN movies m2 ON m1.original_title = m2.original_title AND m1.year = m2.year AND m1.id != m2.id " +
                "JOIN directors d ON m1.director_id = d.id " +
                "JOIN genres g ON m1.genre_id = g.id";

        try (CachedRowSet crs = dbConnect.executeQuery(sql)) {
            while (crs.next()) {
                Director director = new Director(crs.getInt("director_id"), crs.getString("director_name"));
                Genre genre = new Genre(crs.getInt("genre_id"), crs.getString("genre_name"));
                duplicates.add(new Movie(
                        crs.getInt("id"),
                        crs.getString("title"),
                        crs.getString("original_title"),
                        crs.getInt("year"),
                        crs.getDouble("imdb_rating"),
                        crs.getInt("views"),
                        director,
                        genre
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding duplicates", e);
        }
        return duplicates;
    }

    @Override
    public void importFromCSV(String filePath) {
        throw new UnsupportedOperationException("Import from CSV not supported for SQLite");
    }

    @Override
    public void exportToCSV(String filePath) {
        throw new UnsupportedOperationException("Export to CSV not supported for SQLite");
    }

    @Override
    public String getCurrentFilePath() {
        return dbConnect.getDbPath();
    }

    public void disconnect() {
        dbConnect.disconnect();
    }
}