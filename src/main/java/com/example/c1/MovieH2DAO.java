package com.example.c1;

import javax.sql.rowset.CachedRowSet;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to H2 database", e);
        }
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
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting movie", e);
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