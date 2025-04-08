module com.example.demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires java.logging;
    requires java.sql;

    exports com.example.JavaRabbit.Model;
    opens com.example.JavaRabbit.Model to javafx.fxml;
    exports com.example.JavaRabbit.controller;
    opens com.example.JavaRabbit.controller to javafx.fxml;

}