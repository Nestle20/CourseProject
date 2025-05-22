package com.example.c1;

import java.util.List;

public interface GenreDAO {
    List<Genre> getAllGenres();
    Genre getGenreById(int id);
}
