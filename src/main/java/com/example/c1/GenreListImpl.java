package com.example.c1;

import java.util.ArrayList;
import java.util.List;

public class GenreListImpl implements GenreDAO {
    private final List<Genre> genres;

    public GenreListImpl() {
        genres = new ArrayList<>();
        genres.add(new Genre(1, "Action"));
        genres.add(new Genre(2, "Comedy"));
        genres.add(new Genre(3, "Drama"));
        genres.add(new Genre(4, "Sci-Fi"));
        genres.add(new Genre(5, "Thriller"));
    }

    @Override
    public List<Genre> getAllGenres() {
        return new ArrayList<>(genres);
    }

    @Override
    public Genre getGenreById(int id) {
        return genres.stream()
                .filter(g -> g.getId() == id)
                .findFirst()
                .orElse(null);
    }
}