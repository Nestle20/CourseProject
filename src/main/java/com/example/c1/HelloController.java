package com.example.c1;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

public class HelloController {
    // Константы для цветов подсветки
    private static final String COLOR_ZERO_VIEWS = "-fx-background-color: #ADD8E6;";
    private static final String COLOR_ONE_VIEW = "-fx-background-color: #D3D3D3;";
    private static final String COLOR_TWO_TO_FOUR_VIEWS = "-fx-background-color: #FFFACD;";
    private static final String COLOR_FIVE_PLUS_VIEWS = "-fx-background-color: #FFC0CB;";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final int REMINDER_DAYS_BEFORE = 3;

    // Элементы таблицы
    @FXML private TableView<Movie> movieTable;
    @FXML private TableColumn<Movie, Integer> idColumn;
    @FXML private TableColumn<Movie, String> titleColumn;
    @FXML private TableColumn<Movie, Integer> yearColumn;
    @FXML private TableColumn<Movie, Double> ratingColumn;
    @FXML private TableColumn<Movie, Director> directorColumn;
    @FXML private TableColumn<Movie, Genre> genreColumn;
    @FXML private TableColumn<Movie, String> scheduleColumn;

    // Элементы управления
    @FXML private ComboBox<String> dataSourceComboBox;
    @FXML private ComboBox<Genre> genreComboBox;
    @FXML private ComboBox<Director> directorComboBox;
    @FXML private TextField minRatingField;
    @FXML private TextField minYearField;

    // Кнопки
    @FXML private Button searchButton;
    @FXML private Button statsButton;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button setScheduleButton;
    @FXML private Button updateScheduleButton;
    @FXML private Button markWatchedButton;
    @FXML private Button showHistoryButton;

    // Данные
    private MovieDAO movieDAO;
    private final ObservableList<Movie> movies = FXCollections.observableArrayList();
    private final ObservableList<Genre> genres = FXCollections.observableArrayList(DAOFactory.getGenreDAO().getAllGenres());
    private final ObservableList<Director> directors = FXCollections.observableArrayList(DAOFactory.getDirectorDAO().getAllDirectors());

    @FXML
    public void initialize() {
        setupTableColumns();
        setupComboBoxes();
        setupRowFactory();

        // Инициализация с H2 базой данных по умолчанию
        movieDAO = DAOFactory.createMovieDAO(DAOFactory.DataSourceType.H2);
        refreshData();

        // Назначение обработчиков событий
        searchButton.setOnAction(e -> handleSmartSearch());
        statsButton.setOnAction(e -> handleViewStatistics());
        addButton.setOnAction(e -> handleAddMovie());
        editButton.setOnAction(e -> handleEditMovie());
        deleteButton.setOnAction(e -> handleDeleteMovie());
        setScheduleButton.setOnAction(e -> handleSetSchedule());
        updateScheduleButton.setOnAction(e -> handleUpdateSchedule());
        markWatchedButton.setOnAction(e -> handleMarkWatched());
        showHistoryButton.setOnAction(e -> handleShowHistory());

        // Проверка напоминаний при запуске
        checkReminders();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("year"));
        ratingColumn.setCellValueFactory(new PropertyValueFactory<>("imdbRating"));
        directorColumn.setCellValueFactory(new PropertyValueFactory<>("director"));
        genreColumn.setCellValueFactory(new PropertyValueFactory<>("genre"));
        scheduleColumn.setCellValueFactory(cellData -> {
            MovieSchedule schedule = movieDAO.getMovieSchedule(cellData.getValue().getId());
            return schedule != null ?
                    new SimpleStringProperty(formatSchedule(schedule)) :
                    new SimpleStringProperty("Не назначено");
        });
        movieTable.setItems(movies);
    }

    private String formatSchedule(MovieSchedule schedule) {
        String str = schedule.getPlannedDate().format(DATE_FORMATTER);
        if (schedule.getCompletionDate() != null) {
            str += " (просмотрено " + schedule.getCompletionDate().format(DATE_FORMATTER) + ")";
        }
        return str;
    }

    private void setupComboBoxes() {
        dataSourceComboBox.getItems().setAll(
                "H2 Database",
                "CSV File",
                "SQLite Database"
        );
        dataSourceComboBox.getSelectionModel().selectFirst();
        dataSourceComboBox.setOnAction(e -> switchDataSource());

        genreComboBox.setItems(genres);
        if (!genres.isEmpty()) {
            genreComboBox.getSelectionModel().selectFirst();
        }

        directorComboBox.setItems(directors);
        if (!directors.isEmpty()) {
            directorComboBox.getSelectionModel().selectFirst();
        }
    }

    private void setupRowFactory() {
        movieTable.setRowFactory(tv -> new TableRow<Movie>() {
            @Override
            protected void updateItem(Movie movie, boolean empty) {
                super.updateItem(movie, empty);

                if (movie == null || empty) {
                    setStyle("");
                    return;
                }

                // Устанавливаем цвет фона в зависимости от количества просмотров
                int views = movie.getViews();
                if (views == 0) {
                    setStyle(COLOR_ZERO_VIEWS);
                } else if (views == 1) {
                    setStyle(COLOR_ONE_VIEW);
                } else if (views > 1 && views < 5) {
                    setStyle(COLOR_TWO_TO_FOUR_VIEWS);
                } else {
                    setStyle(COLOR_FIVE_PLUS_VIEWS);
                }
            }
        });
    }

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
            refreshData();
        } catch (Exception e) {
            showAlert("Error", "Failed to switch data source", e.getMessage());
            e.printStackTrace();
        }
    }

    private void refreshData() {
        try {
            List<Movie> movieList = movieDAO.getAllMovies();
            movies.setAll(movieList);
            movieTable.refresh();

            if (movieList.isEmpty()) {
                showAlert("Information", "No Data", "The movie table is empty. Add movies using the Add button.");
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to load data", "Error loading data from database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void handleSetSchedule() {
        Movie selectedMovie = movieTable.getSelectionModel().getSelectedItem();
        if (selectedMovie == null) {
            showAlert("Ошибка", "Не выбрано", "Пожалуйста, выберите фильм");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(LocalDate.now().plusDays(7).format(DATE_FORMATTER));
        dialog.setTitle("Установка срока");
        dialog.setHeaderText("Установите срок просмотра для: " + selectedMovie.getTitle());
        dialog.setContentText("Дата (дд.мм.гггг):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(dateStr -> {
            try {
                LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
                movieDAO.setMovieSchedule(selectedMovie.getId(), date);
                refreshData();
                showAlert("Успех", "Срок установлен",
                        "Для фильма " + selectedMovie.getTitle() + " установлен срок: " + dateStr);
            } catch (DateTimeParseException e) {
                showAlert("Ошибка", "Неверная дата", "Пожалуйста, введите дату в формате дд.мм.гггг");
            } catch (Exception e) {
                showAlert("Ошибка", "Ошибка установки срока", e.getMessage());
            }
        });
    }

    private void handleUpdateSchedule() {
        Movie selectedMovie = movieTable.getSelectionModel().getSelectedItem();
        if (selectedMovie == null) {
            showAlert("Ошибка", "Не выбрано", "Пожалуйста, выберите фильм");
            return;
        }

        MovieSchedule schedule = movieDAO.getMovieSchedule(selectedMovie.getId());
        if (schedule == null) {
            showAlert("Ошибка", "Нет расписания", "Для этого фильма не установлен срок просмотра");
            return;
        }

        // Create a dialog with two input fields
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Перенос срока");
        dialog.setHeaderText("Перенести срок просмотра для: " + selectedMovie.getTitle());

        // Set the button types
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Create the date and reason input fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Новая дата (дд.мм.гггг):"), 0, 0);
        TextField dateField = new TextField(schedule.getPlannedDate().format(DATE_FORMATTER));
        grid.add(dateField, 1, 0);
        grid.add(new Label("Причина:"), 0, 1);
        TextField reasonField = new TextField();
        grid.add(reasonField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Convert the result to a pair of date and reason
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return new Pair<>(dateField.getText(), reasonField.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();
        result.ifPresent(pair -> {
            try {
                LocalDate newDate = LocalDate.parse(pair.getKey(), DATE_FORMATTER);
                String reason = pair.getValue();

                if (reason.isEmpty()) {
                    showAlert("Ошибка", "Не указана причина", "Пожалуйста, укажите причину переноса");
                    return;
                }

                movieDAO.updateMovieSchedule(selectedMovie.getId(), newDate, reason);
                refreshData();
                showAlert("Успех", "Срок перенесен",
                        "Срок просмотра для " + selectedMovie.getTitle() + " перенесен на " +
                                pair.getKey() + " по причине: " + reason);
            } catch (DateTimeParseException e) {
                showAlert("Ошибка", "Неверная дата", "Пожалуйста, введите дату в формате дд.мм.гггг");
            } catch (Exception e) {
                showAlert("Ошибка", "Ошибка переноса срока", e.getMessage());
            }
        });
    }

    private void handleMarkWatched() {
        Movie selectedMovie = movieTable.getSelectionModel().getSelectedItem();
        if (selectedMovie == null) {
            showAlert("Ошибка", "Не выбрано", "Пожалуйста, выберите фильм");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Подтверждение");
        confirmation.setHeaderText("Отметить как просмотренное");
        confirmation.setContentText("Вы уверены, что хотите отметить фильм '" +
                selectedMovie.getTitle() + "' как просмотренный?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                movieDAO.markMovieAsWatched(selectedMovie.getId());
                refreshData();
                showAlert("Успех", "Отмечено как просмотренное",
                        "Фильм " + selectedMovie.getTitle() + " отмечен как просмотренный");
            } catch (Exception e) {
                showAlert("Ошибка", "Ошибка отметки", e.getMessage());
            }
        }
    }

    private void handleShowHistory() {
        Movie selectedMovie = movieTable.getSelectionModel().getSelectedItem();
        if (selectedMovie == null) {
            showAlert("Ошибка", "Не выбрано", "Пожалуйста, выберите фильм");
            return;
        }

        List<ScheduleChange> history = movieDAO.getScheduleHistory(selectedMovie.getId());
        if (history.isEmpty()) {
            showAlert("Информация", "История изменений",
                    "Для фильма " + selectedMovie.getTitle() + " нет истории изменений сроков");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("История изменений сроков для: ").append(selectedMovie.getTitle()).append("\n\n");

        for (ScheduleChange change : history) {
            sb.append(change.getChangeDate().format(DATE_FORMATTER))
                    .append(": перенесено с ")
                    .append(change.getOldDate().format(DATE_FORMATTER))
                    .append(" на ")
                    .append(change.getNewDate().format(DATE_FORMATTER))
                    .append("\nПричина: ")
                    .append(change.getReason())
                    .append("\n\n");
        }

        TextArea textArea = new TextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefSize(500, 300);

        ScrollPane scrollPane = new ScrollPane(textArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        Stage stage = new Stage();
        stage.setTitle("История изменений сроков: " + selectedMovie.getTitle());
        stage.setScene(new Scene(scrollPane, 500, 400));
        stage.show();
    }

    private void checkReminders() {
        List<Movie> upcomingMovies = movieDAO.getMoviesWithUpcomingDeadlines(REMINDER_DAYS_BEFORE);
        if (!upcomingMovies.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Напоминание: у вас запланированы к просмотру:\n\n");

            for (Movie movie : upcomingMovies) {
                MovieSchedule schedule = movieDAO.getMovieSchedule(movie.getId());
                sb.append("- ").append(movie.getTitle())
                        .append(" (до ").append(schedule.getPlannedDate().format(DATE_FORMATTER)).append(")\n");
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Напоминание");
            alert.setHeaderText("Предстоящие просмотры");
            alert.setContentText(sb.toString());
            alert.showAndWait();
        }
    }

    private void handleSmartSearch() {
        try {
            Genre selectedGenre = genreComboBox.getSelectionModel().getSelectedItem();
            double minRating = minRatingField.getText().isEmpty() ? 0 : Double.parseDouble(minRatingField.getText());
            int minYear = minYearField.getText().isEmpty() ? 0 : Integer.parseInt(minYearField.getText());

            List<Movie> searchResults = movieDAO.smartSearch(selectedGenre, minRating, minYear);
            movies.setAll(searchResults);
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

    private void handleAddMovie() {
        Dialog<Movie> dialog = createMovieDialog("Add Movie", null);
        Optional<Movie> result = dialog.showAndWait();
        result.ifPresent(movie -> {
            try {
                movieDAO.addMovie(movie);
                refreshData();
            } catch (Exception e) {
                showAlert("Error", "Failed to add movie", e.getMessage());
                e.printStackTrace();
            }
        });
    }

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
                movieDAO.updateMovie(movie);
                refreshData();
            } catch (Exception e) {
                showAlert("Error", "Failed to update movie", e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void handleDeleteMovie() {
        Movie selectedMovie = movieTable.getSelectionModel().getSelectedItem();
        if (selectedMovie == null) {
            showAlert("Error", "No Selection", "Please select a movie to delete");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete Movie");
        alert.setContentText("Are you sure you want to delete: " + selectedMovie.getTitle() + "?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                movieDAO.deleteMovie(selectedMovie.getId());
                refreshData();
            } catch (Exception e) {
                showAlert("Error", "Failed to delete movie", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private Dialog<Movie> createMovieDialog(String title, Movie movie) {
        Dialog<Movie> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText("Enter movie details");

        // Поля формы
        TextField titleField = new TextField(movie != null ? movie.getTitle() : "");
        TextField originalTitleField = new TextField(movie != null ? movie.getOriginalTitle() : "");
        TextField yearField = new TextField(movie != null ? String.valueOf(movie.getYear()) : "");
        TextField ratingField = new TextField(movie != null ? String.valueOf(movie.getImdbRating()) : "");
        TextField viewsField = new TextField(movie != null ? String.valueOf(movie.getViews()) : "0");
        ComboBox<Director> directorCombo = new ComboBox<>(directors);
        ComboBox<Genre> genreCombo = new ComboBox<>(genres);

        if (movie != null) {
            directorCombo.getSelectionModel().select(movie.getDirector());
            genreCombo.getSelectionModel().select(movie.getGenre());
        } else {
            directorCombo.getSelectionModel().selectFirst();
            genreCombo.getSelectionModel().selectFirst();
        }

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
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

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
}