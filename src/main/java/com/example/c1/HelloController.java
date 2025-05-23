package com.example.c1;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import java.util.List;
import java.util.Optional;

/**
 * Контроллер для главного окна приложения.
 * Управляет отображением данных о фильмах и обработкой пользовательских действий.
 */
public class HelloController {
    // Элементы интерфейса, аннотированные @FXML для связи с FXML

    @FXML private TableView<Movie> movieTable; // Таблица для отображения фильмов
    @FXML private TableColumn<Movie, Integer> idColumn; // Колонка с ID фильма
    @FXML private TableColumn<Movie, String> titleColumn; // Колонка с названием фильма
    @FXML private TableColumn<Movie, Integer> yearColumn; // Колонка с годом выпуска
    @FXML private TableColumn<Movie, Double> ratingColumn; // Колонка с рейтингом
    @FXML private TableColumn<Movie, Director> directorColumn; // Колонка с режиссером
    @FXML private TableColumn<Movie, Genre> genreColumn; // Колонка с жанром

    // Элементы управления для фильтрации и выбора
    @FXML private ComboBox<String> dataSourceComboBox; // Выбор источника данных
    @FXML private ComboBox<Genre> genreComboBox; // Выбор жанра для поиска
    @FXML private ComboBox<Director> directorComboBox; // Выбор режиссера для статистики
    @FXML private TextField minRatingField; // Поле для минимального рейтинга
    @FXML private TextField minYearField; // Поле для минимального года

    // Кнопки управления
    @FXML private Button searchButton; // Кнопка поиска
    @FXML private Button statsButton; // Кнопка статистики
    @FXML private Button duplicatesButton; // Кнопка поиска дубликатов
    @FXML private Button addButton; // Кнопка добавления
    @FXML private Button editButton; // Кнопка редактирования
    @FXML private Button deleteButton; // Кнопка удаления

    // Данные приложения
    private MovieDAO movieDAO; // DAO для работы с фильмами
    private final ObservableList<Movie> movies = FXCollections.observableArrayList(); // Список фильмов
    private final ObservableList<Genre> genres = FXCollections.observableArrayList(DAOFactory.getGenreDAO().getAllGenres()); // Список жанров
    private final ObservableList<Director> directors = FXCollections.observableArrayList(DAOFactory.getDirectorDAO().getAllDirectors()); // Список режиссеров

    /**
     * Инициализация контроллера.
     * Вызывается автоматически после загрузки FXML.
     */
    @FXML
    public void initialize() {
        setupTableColumns(); // Настройка колонок таблицы
        setupComboBoxes(); // Настройка выпадающих списков

        // Инициализация DAO с H2 базой данных по умолчанию
        movieDAO = DAOFactory.createMovieDAO(DAOFactory.DataSourceType.H2);
        refreshData(); // Загрузка данных

        // Настройка обработчиков событий для кнопок
        searchButton.setOnAction(e -> handleSmartSearch());
        statsButton.setOnAction(e -> handleViewStatistics());
        duplicatesButton.setOnAction(e -> handleFindDuplicates());
        addButton.setOnAction(e -> handleAddMovie());
        editButton.setOnAction(e -> handleEditMovie());
        deleteButton.setOnAction(e -> handleDeleteMovie());
    }

    /**
     * Настраивает колонки таблицы фильмов.
     */
    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("year"));
        ratingColumn.setCellValueFactory(new PropertyValueFactory<>("imdbRating"));
        directorColumn.setCellValueFactory(new PropertyValueFactory<>("director"));
        genreColumn.setCellValueFactory(new PropertyValueFactory<>("genre"));
        movieTable.setItems(movies); // Устанавливаем данные для таблицы
    }

    /**
     * Настраивает выпадающие списки.
     */
    private void setupComboBoxes() {
        // Настройка выбора источника данных
        dataSourceComboBox.getItems().setAll(
                "H2 Database",
                "CSV File",
                "SQLite Database"
        );
        dataSourceComboBox.getSelectionModel().selectFirst();
        dataSourceComboBox.setOnAction(e -> switchDataSource());

        // Настройка выбора жанра
        genreComboBox.setItems(genres);
        if (!genres.isEmpty()) {
            genreComboBox.getSelectionModel().selectFirst();
        }

        // Настройка выбора режиссера
        directorComboBox.setItems(directors);
        if (!directors.isEmpty()) {
            directorComboBox.getSelectionModel().selectFirst();
        }
    }

    /**
     * Переключает источник данных в зависимости от выбора пользователя.
     */
    private void switchDataSource() {
        String selected = dataSourceComboBox.getSelectionModel().getSelectedItem();
        try {
            if ("H2 Database".equals(selected)) {
                movieDAO = DAOFactory.createMovieDAO(DAOFactory.DataSourceType.H2);
            } else if ("CSV File".equals(selected)) {
                movieDAO = DAOFactory.createMovieDAO(DAOFactory.DataSourceType.CSV);
            } else if ("SQLite Database".equals(selected)) {
                movieDAO = DAOFactory.createMovieDAO(DAOFactory.DataSourceType.SQLITE);
            }
            refreshData(); // Обновляем данные после смены источника
        } catch (Exception e) {
            showAlert("Error", "Failed to switch data source", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Обрабатывает добавление нового фильма.
     */
    private void handleAddMovie() {
        Dialog<Movie> dialog = createMovieDialog("Add Movie", null);
        Optional<Movie> result = dialog.showAndWait();
        result.ifPresent(movie -> {
            try {
                movieDAO.addMovie(movie); // Добавляем фильм через DAO
                refreshData(); // Обновляем таблицу
            } catch (Exception e) {
                showAlert("Error", "Failed to add movie", e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Обрабатывает редактирование фильма.
     */
    private void handleEditMovie() {
        Movie selectedMovie = movieTable.getSelectionModel().getSelectedItem();
        if (selectedMovie == null) {
            showAlert("Error", "No Selection", "Please select a movie to edit");
            return;
        }

        Dialog<Movie> dialog = createMovieDialog("Edit Movie", selectedMovie);
        Optional<Movie> result = dialog.showAndWait();
        result.ifPresent(movie -> {
            try {
                movieDAO.updateMovie(movie); // Обновляем фильм через DAO
                refreshData(); // Обновляем таблицу
            } catch (Exception e) {
                showAlert("Error", "Failed to update movie", e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Обрабатывает удаление фильма.
     */
    private void handleDeleteMovie() {
        Movie selectedMovie = movieTable.getSelectionModel().getSelectedItem();
        if (selectedMovie == null) {
            showAlert("Error", "No Selection", "Please select a movie to delete");
            return;
        }

        // Подтверждение удаления
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete Movie");
        alert.setContentText("Are you sure you want to delete: " + selectedMovie.getTitle() + "?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                movieDAO.deleteMovie(selectedMovie.getId()); // Удаляем фильм через DAO
                refreshData(); // Обновляем таблицу
            } catch (Exception e) {
                showAlert("Error", "Failed to delete movie", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Создает диалоговое окно для ввода данных о фильме.
     * @param title заголовок окна
     * @param movie фильм для редактирования (null для нового фильма)
     * @return диалоговое окно
     */
    private Dialog<Movie> createMovieDialog(String title, Movie movie) {
        Dialog<Movie> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText("Enter movie details");

        // Создаем поля формы
        TextField titleField = new TextField(movie != null ? movie.getTitle() : "");
        TextField originalTitleField = new TextField(movie != null ? movie.getOriginalTitle() : "");
        TextField yearField = new TextField(movie != null ? String.valueOf(movie.getYear()) : "");
        TextField ratingField = new TextField(movie != null ? String.valueOf(movie.getImdbRating()) : "");
        TextField viewsField = new TextField(movie != null ? String.valueOf(movie.getViews()) : "0");
        ComboBox<Director> directorCombo = new ComboBox<>(directors);
        ComboBox<Genre> genreCombo = new ComboBox<>(genres);

        // Устанавливаем текущие значения для редактирования
        if (movie != null) {
            directorCombo.getSelectionModel().select(movie.getDirector());
            genreCombo.getSelectionModel().select(movie.getGenre());
        } else {
            directorCombo.getSelectionModel().selectFirst();
            genreCombo.getSelectionModel().selectFirst();
        }

        // Создаем layout для диалога
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Original Title:"), 0, 1);
        grid.add(originalTitleField, 1, 1);
        grid.add(new Label("Year:"), 0, 2);
        grid.add(yearField, 1, 2);
        grid.add(new Label("IMDB Rating:"), 0, 3);
        grid.add(ratingField, 1, 3);
        grid.add(new Label("Views:"), 0, 4);
        grid.add(viewsField, 1, 4);
        grid.add(new Label("Director:"), 0, 5);
        grid.add(directorCombo, 1, 5);
        grid.add(new Label("Genre:"), 0, 6);
        grid.add(genreCombo, 1, 6);

        dialog.getDialogPane().setContent(grid);

        // Добавляем кнопки OK и Cancel
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Преобразуем результат в объект Movie
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {
                    int id = movie != null ? movie.getId() : 0;
                    String titleText = titleField.getText();
                    String originalTitle = originalTitleField.getText();
                    int year = Integer.parseInt(yearField.getText());
                    double rating = Double.parseDouble(ratingField.getText());
                    int views = Integer.parseInt(viewsField.getText());
                    Director director = directorCombo.getValue();
                    Genre genre = genreCombo.getValue();

                    return new Movie(id, titleText, originalTitle, year, rating, views, director, genre);
                } catch (NumberFormatException e) {
                    showAlert("Input Error", "Invalid Data", "Please enter valid numeric values");
                    return null;
                }
            }
            return null;
        });

        return dialog;
    }

    /**
     * Обрабатывает "умный" поиск фильмов.
     */
    private void handleSmartSearch() {
        try {
            Genre selectedGenre = genreComboBox.getSelectionModel().getSelectedItem();
            double minRating = minRatingField.getText().isEmpty() ? 0 : Double.parseDouble(minRatingField.getText());
            int minYear = minYearField.getText().isEmpty() ? 0 : Integer.parseInt(minYearField.getText());

            List<Movie> searchResults = movieDAO.smartSearch(selectedGenre, minRating, minYear);
            movies.setAll(searchResults); // Обновляем таблицу результатами поиска

            if (searchResults.isEmpty()) {
                showAlert("Information", "No Results", "No movies found matching the criteria");
            }
        } catch (NumberFormatException e) {
            showAlert("Input Error", "Invalid Data", "Please enter valid numeric values for rating and year");
        } catch (Exception e) {
            showAlert("Error", "Search Failed", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Обрабатывает просмотр статистики по режиссеру.
     */
    private void handleViewStatistics() {
        try {
            Director selectedDirector = directorComboBox.getSelectionModel().getSelectedItem();
            if (selectedDirector != null) {
                double percentage = movieDAO.getDirectorViewPercentage(selectedDirector);
                showAlert("View Statistics",
                        "View percentage for " + selectedDirector.getName(),
                        String.format("%.2f%% of all views", percentage));
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to get statistics", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Обрабатывает поиск дубликатов фильмов.
     */
    private void handleFindDuplicates() {
        try {
            List<Movie> duplicates = movieDAO.findDuplicatesByTmdb();
            movies.setAll(duplicates); // Отображаем дубликаты в таблице

            if (duplicates.isEmpty()) {
                showAlert("Information", "No Duplicates", "No duplicate movies found");
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to find duplicates", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Обновляет данные в таблице.
     */
    private void refreshData() {
        try {
            List<Movie> movieList = movieDAO.getAllMovies();
            movies.setAll(movieList); // Обновляем данные таблицы

            if (movieList.isEmpty()) {
                showAlert("Information", "No Data", "The movie table is empty. Add movies using the Add button.");
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to load data", "Error loading data from database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Показывает информационное окно.
     * @param title заголовок окна
     * @param header заголовок сообщения
     * @param content текст сообщения
     */
    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}