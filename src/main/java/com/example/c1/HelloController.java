package com.example.c1;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.List;

public class HelloController {
    @FXML private TableView<Movie> movieTable;
    @FXML private TableColumn<Movie, Integer> idColumn;
    @FXML private TableColumn<Movie, String> titleColumn;
    @FXML private TableColumn<Movie, Integer> yearColumn;
    @FXML private TableColumn<Movie, Double> ratingColumn;
    @FXML private TableColumn<Movie, Director> directorColumn;
    @FXML private TableColumn<Movie, Genre> genreColumn;

    @FXML private ComboBox<String> dataSourceComboBox;
    @FXML private ComboBox<Genre> genreComboBox;
    @FXML private ComboBox<Director> directorComboBox;
    @FXML private TextField minRatingField;
    @FXML private TextField minYearField;
    @FXML private Button searchButton;
    @FXML private Button statsButton;
    @FXML private Button duplicatesButton;

    private MovieDAO movieDAO;
    private final ObservableList<Movie> movies = FXCollections.observableArrayList();
    private final ObservableList<Genre> genres = FXCollections.observableArrayList(DAOFactory.getGenreDAO().getAllGenres());
    private final ObservableList<Director> directors = FXCollections.observableArrayList(DAOFactory.getDirectorDAO().getAllDirectors());

    @FXML
    public void initialize() {
        setupTableColumns();
        setupComboBoxes();

        // Инициализация с H2 базой данных по умолчанию
        movieDAO = DAOFactory.createMovieDAO(DAOFactory.DataSourceType.H2);
        refreshData();

        // Настройка обработчиков событий
        searchButton.setOnAction(e -> handleSmartSearch());
        statsButton.setOnAction(e -> handleViewStatistics());
        duplicatesButton.setOnAction(e -> handleFindDuplicates());
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("year"));
        ratingColumn.setCellValueFactory(new PropertyValueFactory<>("imdbRating"));
        directorColumn.setCellValueFactory(new PropertyValueFactory<>("director"));
        genreColumn.setCellValueFactory(new PropertyValueFactory<>("genre"));
        movieTable.setItems(movies);
    }

    private void setupComboBoxes() {
        // Настройка ComboBox для выбора источника данных (без дублирования)
        dataSourceComboBox.getItems().setAll(
                "H2 База данных",
                "CSV файл",
                "SQLite База данных"
        );
        dataSourceComboBox.getSelectionModel().selectFirst();
        dataSourceComboBox.setOnAction(e -> switchDataSource());

        // Настройка ComboBox для жанров
        genreComboBox.setItems(genres);
        if (!genres.isEmpty()) {
            genreComboBox.getSelectionModel().selectFirst();
        }

        // Настройка ComboBox для режиссеров
        directorComboBox.setItems(directors);
        if (!directors.isEmpty()) {
            directorComboBox.getSelectionModel().selectFirst();
        }
    }

    private void switchDataSource() {
        String selected = dataSourceComboBox.getSelectionModel().getSelectedItem();
        try {
            if ("H2 База данных".equals(selected)) {
                movieDAO = DAOFactory.createMovieDAO(DAOFactory.DataSourceType.H2);
            } else if ("CSV файл".equals(selected)) {
                movieDAO = DAOFactory.createMovieDAO(DAOFactory.DataSourceType.CSV);
            } else if ("SQLite База данных".equals(selected)) {
                movieDAO = DAOFactory.createMovieDAO(DAOFactory.DataSourceType.SQLITE);
            }
            refreshData();
        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось изменить источник данных", e.getMessage());
        }
    }

    private void handleSmartSearch() {
        try {
            Genre selectedGenre = genreComboBox.getSelectionModel().getSelectedItem();
            double minRating = minRatingField.getText().isEmpty() ? 0 : Double.parseDouble(minRatingField.getText());
            int minYear = minYearField.getText().isEmpty() ? 0 : Integer.parseInt(minYearField.getText());

            List<Movie> searchResults = movieDAO.smartSearch(selectedGenre, minRating, minYear);
            movies.setAll(searchResults);
        } catch (NumberFormatException e) {
            showAlert("Ошибка ввода", "Некорректные данные", "Введите числовые значения для рейтинга и года");
        } catch (Exception e) {
            showAlert("Ошибка поиска", "Не удалось выполнить поиск", e.getMessage());
        }
    }

    private void handleViewStatistics() {
        try {
            Director selectedDirector = directorComboBox.getSelectionModel().getSelectedItem();
            if (selectedDirector != null) {
                double percentage = movieDAO.getDirectorViewPercentage(selectedDirector);
                showAlert("Статистика просмотров",
                        "Процент просмотров для " + selectedDirector.getName(),
                        String.format("%.2f%% от всех просмотров", percentage));
            }
        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось получить статистику", e.getMessage());
        }
    }

    private void handleFindDuplicates() {
        try {
            List<Movie> duplicates = movieDAO.findDuplicatesByTmdb();
            movies.setAll(duplicates);
            if (duplicates.isEmpty()) {
                showAlert("Поиск дубликатов", "Результат", "Дубликаты не найдены");
            }
        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось найти дубликаты", e.getMessage());
        }
    }

    private void refreshData() {
        try {
            List<Movie> movieList = movieDAO.getAllMovies();
            movies.setAll(movieList);
        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось загрузить данные", e.getMessage());
        }
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}