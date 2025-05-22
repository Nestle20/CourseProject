package com.example.c1;

import java.io.Serializable;

public class Movie implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String title;
    private String originalTitle;
    private int year;
    private double imdbRating;
    private int views;
    private Director director;
    private Genre genre;

    public Movie(int id, String title, String originalTitle, int year,
                 double imdbRating, int views, Director director, Genre genre) {
        this.id = id;
        this.title = title;
        this.originalTitle = originalTitle;
        this.year = year;
        this.imdbRating = imdbRating;
        this.views = views;
        this.director = director;
        this.genre = genre;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getOriginalTitle() { return originalTitle; }
    public void setOriginalTitle(String originalTitle) { this.originalTitle = originalTitle; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public double getImdbRating() { return imdbRating; }
    public void setImdbRating(double imdbRating) { this.imdbRating = imdbRating; }
    public int getViews() { return views; }
    public void setViews(int views) { this.views = views; }
    public Director getDirector() { return director; }
    public void setDirector(Director director) { this.director = director; }
    public Genre getGenre() { return genre; }
    public void setGenre(Genre genre) { this.genre = genre; }

    @Override
    public String toString() {
        return title + " (" + year + ")";
    }
}