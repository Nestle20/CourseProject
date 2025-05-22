module com.example.c1 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql.rowset;


    opens com.example.c1 to javafx.fxml;
    exports com.example.c1;
}