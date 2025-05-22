package com.example.c1;

import java.util.ArrayList;
import java.util.List;

public class DirectorListImpl implements DirectorDAO {
    private final List<Director> directors;

    public DirectorListImpl() {
        directors = new ArrayList<>();
        directors.add(new Director(1, "Christopher Nolan"));
        directors.add(new Director(2, "Quentin Tarantino"));
        directors.add(new Director(3, "Steven Spielberg"));
        directors.add(new Director(4, "James Cameron"));
        directors.add(new Director(5, "Martin Scorsese"));
    }

    @Override
    public List<Director> getAllDirectors() {
        return new ArrayList<>(directors);
    }

    @Override
    public Director getDirectorById(int id) {
        return directors.stream()
                .filter(d -> d.getId() == id)
                .findFirst()
                .orElse(null);
    }

    @Override
    public double getViewPercentage(Director director) {
        return 0; // Реализация в MovieDAO
    }
}