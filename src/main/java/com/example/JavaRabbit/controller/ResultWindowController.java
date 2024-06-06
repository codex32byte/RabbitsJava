package com.example.JavaRabbit.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextArea;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ResultWindowController {

    @FXML
    private ResourceBundle resources;
    @FXML
    private URL location;
    @FXML
    private Button CancelButton;
    @FXML
    private Button OKButton;
    @FXML
    private TextArea resultTextArea;
    @FXML
    private DialogPane statWindow;

    @FXML
    void CancelButtonClick(ActionEvent event) {
        Habitat.setContinueFlag();
        statWindow.setVisible(false);
        statWindow.setDisable(true);
        ResultWindow.getInstance().closeWindow();
    }

    @FXML
    void OKButtonClick(ActionEvent event) throws IOException {
        Habitat.setStopFlag();
        statWindow.setVisible(false);
        statWindow.setDisable(true);
        ResultWindow.getInstance().closeWindow();
    }

    @FXML
    void initialize() {
        resultTextArea.setText(Habitat.getInstance().getStatistics());
    }

}