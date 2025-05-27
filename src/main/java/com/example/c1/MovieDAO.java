package com.example.c1;

import java.time.LocalDate;
import java.util.List;

public interface MovieDAO {
    // Основные методы работы с фильмами
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

    // Методы для управления временными параметрами
    void setMovieSchedule(int movieId, LocalDate plannedDate);
    void updateMovieSchedule(int movieId, LocalDate newDate, String reason);
    MovieSchedule getMovieSchedule(int movieId);
    List<Movie> getMoviesWithUpcomingDeadlines(int daysBefore);
    void markMovieAsWatched(int movieId);
    List<ScheduleChange> getScheduleHistory(int movieId);
}