package com.example.c1;

import java.util.List;

public interface MovieDAO {
    void addMovie(Movie movie);
    void updateMovie(Movie movie);
    void deleteMovie(int id);
    List<Movie> getAllMovies();
    List<Movie> smartSearch(Genre genre, double minRating, int minYear);
    double getDirectorViewPercentage(Director director);
    List<Movie> findDuplicatesByTmdb();
    void importFromCSV(String filePath);
    void exportToCSV(String filePath);
    String getCurrentFilePath();
}