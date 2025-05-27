package com.example.c1;

import javax.sql.rowset.CachedRowSet;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MovieH2DAO implements MovieDAO {
    private final H2DBConnect dbConnect;
    private final GenreDAO genreDAO;
    private final DirectorDAO directorDAO;

    public MovieH2DAO(GenreDAO genreDAO, DirectorDAO directorDAO) {
        this.genreDAO = genreDAO;
        this.directorDAO = directorDAO;
        this.dbConnect = new H2DBConnect();
        try {
            dbConnect.connect(false);
            initializeScheduleTables();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to H2 database", e);
        }
    }

    private void initializeScheduleTables() throws SQLException {
        dbConnect.executeUpdate("CREATE TABLE IF NOT EXISTS movie_schedules (" +
                "movie_id INT PRIMARY KEY, " +
                "planned_date DATE NOT NULL, " +
                "completion_date DATE, " +
                "reminder_sent BOOLEAN DEFAULT FALSE, " +
                "FOREIGN KEY (movie_id) REFERENCES movies(id))");

        dbConnect.executeUpdate("CREATE TABLE IF NOT EXISTS schedule_changes (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "movie_id INT NOT NULL, " +
                "old_date DATE NOT NULL, " +
                "new_date DATE NOT NULL, " +
                "reason VARCHAR(255) NOT NULL, " +
                "change_date DATE NOT NULL, " +
                "FOREIGN KEY (movie_id) REFERENCES movies(id))");
    }

    @Override
    public void addMovie(Movie movie) {
        String sql = "INSERT INTO movies (title, original_title, release_year, imdb_rating, views, director_id, genre_id) " +
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
        String sql = "UPDATE movies SET title = ?, original_title = ?, release_year = ?, " +
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
        String sql = "DELETE FROM movies WHERE id = ?";
        try (PreparedStatement pstmt = dbConnect.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            deleteScheduleData(id);
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
        String sql = "SELECT id, title, original_title, release_year, imdb_rating, views, director_id, genre_id FROM movies";

        try (CachedRowSet crs = dbConnect.executeQuery(sql)) {
            while (crs.next()) {
                int directorId = crs.getInt("director_id");
                int genreId = crs.getInt("genre_id");

                Director director = directorDAO.getDirectorById(directorId);
                Genre genre = genreDAO.getGenreById(genreId);

                movies.add(new Movie(
                        crs.getInt("id"),
                        crs.getString("title"),
                        crs.getString("original_title"),
                        crs.getInt("release_year"),
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
        String sql = "MERGE INTO movie_schedules KEY(movie_id) VALUES (?, ?, NULL, FALSE)";
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
        Optional<LocalDate> currentDate = null;
        try {
            currentDate = getCurrentPlannedDate(movieId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (!currentDate.isPresent()) {
            throw new RuntimeException("No existing schedule found for movie ID: " + movieId);
        }

        String historySql = "INSERT INTO schedule_changes (movie_id, old_date, new_date, reason, change_date) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = dbConnect.prepareStatement(historySql)) {
            pstmt.setInt(1, movieId);
            pstmt.setDate(2, Date.valueOf(currentDate.get()));
            pstmt.setDate(3, Date.valueOf(newDate));
            pstmt.setString(4, reason);
            pstmt.setDate(5, Date.valueOf(LocalDate.now()));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving schedule change history", e);
        }

        String updateSql = "UPDATE movie_schedules SET planned_date = ?, reminder_sent = FALSE WHERE movie_id = ?";
        try (PreparedStatement pstmt = dbConnect.prepareStatement(updateSql)) {
            pstmt.setDate(1, Date.valueOf(newDate));
            pstmt.setInt(2, movieId);
            pstmt.executeUpdate();
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
            // If no schedule exists, return null
        }
        return null;
    }

    @Override
    public List<Movie> getMoviesWithUpcomingDeadlines(int daysBefore) {
        LocalDate now = LocalDate.now();
        LocalDate deadline = now.plusDays(daysBefore);

        String sql = "SELECT m.id, m.title, m.original_title, m.release_year, m.imdb_rating, m.views, " +
                "m.director_id, m.genre_id " +
                "FROM movies m JOIN movie_schedules ms ON m.id = ms.movie_id " +
                "WHERE ms.planned_date BETWEEN ? AND ? AND ms.completion_date IS NULL " +
                "AND (ms.reminder_sent = FALSE OR ms.reminder_sent IS NULL)";

        List<Movie> movies = new ArrayList<>();
        try (PreparedStatement pstmt = dbConnect.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(now));
            pstmt.setDate(2, Date.valueOf(deadline));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int directorId = rs.getInt("director_id");
                    int genreId = rs.getInt("genre_id");

                    Director director = directorDAO.getDirectorById(directorId);
                    Genre genre = genreDAO.getGenreById(genreId);

                    movies.add(new Movie(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("original_title"),
                            rs.getInt("release_year"),
                            rs.getDouble("imdb_rating"),
                            rs.getInt("views"),
                            director,
                            genre
                    ));
                }
            }

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
        String sql = "SELECT id, title, original_title, release_year, imdb_rating, views, director_id " +
                "FROM movies WHERE genre_id = ? AND imdb_rating >= ? AND release_year >= ?";

        try (PreparedStatement pstmt = dbConnect.prepareStatement(sql)) {
            pstmt.setInt(1, genre.getId());
            pstmt.setDouble(2, minRating);
            pstmt.setInt(3, minYear);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int directorId = rs.getInt("director_id");
                    Director director = directorDAO.getDirectorById(directorId);

                    result.add(new Movie(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("original_title"),
                            rs.getInt("release_year"),
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
        String sql = "SELECT m1.id, m1.title, m1.original_title, m1.release_year, m1.imdb_rating, " +
                "m1.views, m1.director_id, m1.genre_id " +
                "FROM movies m1 " +
                "INNER JOIN movies m2 ON m1.original_title = m2.original_title " +
                "AND m1.release_year = m2.release_year AND m1.id != m2.id";

        try (CachedRowSet crs = dbConnect.executeQuery(sql)) {
            while (crs.next()) {
                int directorId = crs.getInt("director_id");
                int genreId = crs.getInt("genre_id");

                Director director = directorDAO.getDirectorById(directorId);
                Genre genre = genreDAO.getGenreById(genreId);

                duplicates.add(new Movie(
                        crs.getInt("id"),
                        crs.getString("title"),
                        crs.getString("original_title"),
                        crs.getInt("release_year"),
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
        throw new UnsupportedOperationException("Import from CSV not supported for H2");
    }

    @Override
    public void exportToCSV(String filePath) {
        throw new UnsupportedOperationException("Export to CSV not supported for H2");
    }

    @Override
    public String getCurrentFilePath() {
        return "testdb.mv.db";
    }
}