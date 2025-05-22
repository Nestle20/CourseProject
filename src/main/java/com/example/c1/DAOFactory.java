package com.example.c1;

public class DAOFactory {
    private static final GenreDAO genreDAO = new GenreListImpl();
    private static final DirectorDAO directorDAO = new DirectorListImpl();

    public enum DataSourceType {
        H2, CSV, SQLITE
    }

    public static MovieDAO createMovieDAO(DataSourceType type) {
        switch (type) {
            case H2:
                return new MovieH2DAO(genreDAO, directorDAO);
            case CSV:
                return new MovieCSVDAO(genreDAO, directorDAO);
            case SQLITE:
                return new MovieSQLiteDAO(genreDAO, directorDAO);
            default:
                throw new IllegalArgumentException("Unknown data source type: " + type);
        }
    }

    public static GenreDAO getGenreDAO() {
        return genreDAO;
    }

    public static DirectorDAO getDirectorDAO() {
        return directorDAO;
    }
}