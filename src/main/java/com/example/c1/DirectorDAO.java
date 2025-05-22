package com.example.c1;

import java.util.List;

public interface DirectorDAO {
    List<Director> getAllDirectors();
    Director getDirectorById(int id);
    double getViewPercentage(Director director);
}