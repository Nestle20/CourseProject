package com.example.c1;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MovieCSVDAO implements MovieDAO {
    private static final String CSV_HEADER = "id;title;original_title;release_year;imdb_rating;views;director_id;genre_id";
    private List<Movie> movies = new ArrayList<>();
    private final GenreDAO genreDAO;
    private final DirectorDAO directorDAO;
    private String currentFilePath = "movies.csv";
    private AtomicInteger idGenerator = new AtomicInteger(1);

    public MovieCSVDAO(GenreDAO genreDAO, DirectorDAO directorDAO) {
        this.genreDAO = genreDAO;
        this.directorDAO = directorDAO;
        ensureFileExists();
        loadFromCSV();
    }

    private void ensureFileExists() {
        File file = new File(currentFilePath);
        if (!file.exists()) {
            try {
                file.createNewFile();
                try (PrintWriter pw = new PrintWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
                    pw.println(CSV_HEADER);
                }
            } catch (IOException e) {
                System.err.println("Error creating CSV file: " + e.getMessage());
            }
        }
    }

    private void loadFromCSV() {
        File file = new File(currentFilePath);
        if (file.exists() && file.length() > 0) {
            try (BufferedReader br = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
                String line;
                boolean firstLine = true;
                while ((line = br.readLine()) != null) {
                    if (firstLine) {
                        firstLine = false;
                        continue;
                    }
                    String[] values = line.split(";");
                    if (values.length >= 8) {
                        try {
                            int id = Integer.parseInt(values[0].trim());
                            String title = values[1].trim();
                            String originalTitle = values[2].trim();
                            int year = Integer.parseInt(values[3].trim());
                            double rating = Double.parseDouble(values[4].trim());
                            int views = Integer.parseInt(values[5].trim());
                            int directorId = Integer.parseInt(values[6].trim());
                            int genreId = Integer.parseInt(values[7].trim());

                            Director director = directorDAO.getDirectorById(directorId);
                            Genre genre = genreDAO.getGenreById(genreId);

                            if (director != null && genre != null) {
                                movies.add(new Movie(id, title, originalTitle, year, rating, views, director, genre));
                                if (id >= idGenerator.get()) {
                                    idGenerator.set(id + 1);
                                }
                            }
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid data format in CSV line: " + line);
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Error reading CSV file: " + e.getMessage());
            }
        }
    }

    private void saveToCSV() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(currentFilePath, StandardCharsets.UTF_8))) {
            pw.println(CSV_HEADER);
            for (Movie movie : movies) {
                pw.println(String.format("%d;%s;%s;%d;%.1f;%d;%d;%d",
                        movie.getId(),
                        movie.getTitle(),
                        movie.getOriginalTitle(),
                        movie.getYear(),
                        movie.getImdbRating(),
                        movie.getViews(),
                        movie.getDirector().getId(),
                        movie.getGenre().getId()));
            }
        } catch (IOException e) {
            System.err.println("Error writing to CSV file: " + e.getMessage());
        }
    }

    @Override
    public void addMovie(Movie movie) {
        if (movie.getId() == 0) {
            movie.setId(idGenerator.getAndIncrement());
        }
        movies.add(movie);
        saveToCSV();
    }

    @Override
    public void updateMovie(Movie movie) {
        for (int i = 0; i < movies.size(); i++) {
            if (movies.get(i).getId() == movie.getId()) {
                movies.set(i, movie);
                break;
            }
        }
        saveToCSV();
    }

    @Override
    public void deleteMovie(int id) {
        movies.removeIf(m -> m.getId() == id);
        saveToCSV();
    }

    @Override
    public List<Movie> getAllMovies() {
        return new ArrayList<>(movies);
    }

    @Override
    public List<Movie> smartSearch(Genre genre, double minRating, int minYear) {
        List<Movie> result = new ArrayList<>();
        for (Movie movie : movies) {
            if (movie.getGenre().equals(genre) &&
                    movie.getImdbRating() >= minRating &&
                    movie.getYear() >= minYear) {
                result.add(movie);
            }
        }
        return result;
    }

    @Override
    public double getDirectorViewPercentage(Director director) {
        int directorViews = 0;
        int totalViews = 0;

        for (Movie movie : movies) {
            totalViews += movie.getViews();
            if (movie.getDirector().equals(director)) {
                directorViews += movie.getViews();
            }
        }

        if (totalViews == 0) return 0;
        return (double) directorViews / totalViews * 100;
    }

    @Override
    public List<Movie> findDuplicatesByTmdb() {
        List<Movie> duplicates = new ArrayList<>();
        Map<String, List<Movie>> titleYearMap = new HashMap<>();

        for (Movie movie : movies) {
            String key = movie.getOriginalTitle() + "|" + movie.getYear();
            titleYearMap.computeIfAbsent(key, k -> new ArrayList<>()).add(movie);
        }

        for (List<Movie> movieList : titleYearMap.values()) {
            if (movieList.size() > 1) {
                duplicates.addAll(movieList);
            }
        }

        return duplicates;
    }

    @Override
    public void importFromCSV(String filePath) {
        this.currentFilePath = filePath;
        movies.clear();
        loadFromCSV();
    }

    @Override
    public void exportToCSV(String filePath) {
        this.currentFilePath = filePath;
        saveToCSV();
    }

    @Override
    public String getCurrentFilePath() {
        return currentFilePath;
    }
}