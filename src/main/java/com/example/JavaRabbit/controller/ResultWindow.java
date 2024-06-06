package com.example.JavaRabbit.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;

public class ResultWindow {

    private static volatile ResultWindow instance;
    private Stage stage;

    private ResultWindow() {
    }

    public static ResultWindow getInstance() {
        if (instance == null) {
            synchronized (ResultWindow.class) {
                if (instance == null) {
                    instance = new ResultWindow();
                }
            }
        }
        return instance;
    }

    public void showMenu() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/JavaRabbit/view/resultWindow.fxml"));
        Scene root = new Scene(loader.load());
        ResultWindowController controller = loader.getController();
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(root);
        stage.setTitle("Result Window");
        stage.showAndWait();
    }

    public void closeWindow() {
        if (stage != null) {
            stage.close();

        }
    }
}