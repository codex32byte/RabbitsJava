package com.example.JavaRabbit.controller;

import com.example.JavaRabbit.Model.AlbinoRabbit;
import com.example.JavaRabbit.Model.Rabbit;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.*;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class mainController {

    @FXML
    BorderPane mainPane;
    @FXML
    private Pane rabbitsPane;
    @FXML
    private Label timeLabel;
    @FXML
    private Label statisticLabel;
    @FXML
    private RadioButton showTimeButton;
    @FXML
    private RadioButton hideTimeButton;
    @FXML
    private Button startButton;
    @FXML
    private Button stopButton;
    @FXML
    private BorderPane controlPanel;
    @FXML
    private CheckBox resultWindowCheckBox;
    @FXML
    private Button OkButton;
    @FXML
    private TextField N1TextField;
    @FXML
    private Slider P1Slider;
    @FXML
    private Slider P2PercentageSlider;
    @FXML
    private Label P1ValueLabel; // Added label for P1Slider value
    @FXML
    private Label P2ValueLabel; // Added label for P2PercentageSlider value
    @FXML
    private TextField commonLifetimeTextField; // Newly added TextField for common rabbit lifetime
    @FXML
    private TextField albinoLifetimeTextField; // Newly added TextField for albino rabbit lifetime
    @FXML
    private Button showObjectsButton;
    @FXML
    private Button stopAlbinoAIButton;
    @FXML
    private Button resumeAlbinoAIButton;
    @FXML
    private Button stopCommonAIButton;
    @FXML
    private Button resumeCommonAIButton;
    @FXML
    ComboBox<String> commonPriorityDropdown;

    @FXML
    ComboBox<String> albinoPriorityDropdown;

    private Socket clientSocket;
    private List<SocketAddress> connectedClients = new ArrayList<>();
    @FXML
    private MenuItem connectedListMenuItem;
    private ObjectOutputStream outputStream;
    private Habitat habitat;
    private static final int PORT_RANGE_START = 5000;
    private static final int PORT_RANGE_END = 6000;
    @FXML
    public void initialize() {
        habitat = Habitat.getInstance(); // Obtain the instance using the Singleton pattern
        habitat.setController(this);
        showTimeButton.setSelected(true); // Default to show time

        startButton.setDisable(true);
        stopButton.setDisable(true);
        // Add event filter for key presses
        mainPane.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);

        // Initialize the sliders
        P1Slider.setValue(10);  // Set default value for P1
        P2PercentageSlider.setValue(10);  // Set default value for P2
        N1TextField.setText("1");  // Set default value f   or N1
        // Bind labels to slider values
        P1ValueLabel.textProperty().bind(Bindings.format("%.0f", P1Slider.valueProperty()));
        P2ValueLabel.textProperty().bind(Bindings.format("%.0f", P2PercentageSlider.valueProperty()));
        commonPriorityDropdown.setValue("1"); // Set normal priority as default
        albinoPriorityDropdown.setValue("1"); // Set normal priority as default
        connectedListMenuItem.setOnAction(event -> showConnectedClients());
        new Thread(this::listenForUpdates).start();
    }
    private void listenForUpdates() {
        String serverAddress = "127.0.0.1";
        int port = PORT_RANGE_START;

        while (clientSocket == null && port <= PORT_RANGE_END) {
            try {
                clientSocket = new Socket(serverAddress, port);
            } catch (IOException e) {
                // Port is busy, try the next one
                port++;
            }
        }

        if (clientSocket == null) {
            System.err.println("Could not find a free port within the specified range.");
            return;
        }

        try {
            ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());

            while (true) {
                try {
                    Object receivedData = inputStream.readObject();

                    if (receivedData instanceof List) {
                        List<?> data = (List<?>) receivedData;
                        if (data.get(0) instanceof Integer && data.get(1) instanceof Integer && data.get(2) instanceof Integer &&
                                data.get(3) instanceof String && data.get(4) instanceof String &&
                                data.get(5) instanceof Long && data.get(6) instanceof Long) {
                            Platform.runLater(() -> {
                                List<Object> settings = (List<Object>) receivedData;
                                receiveAndApplySettings(settings);
                            });
                        } else if (data.get(0) instanceof SocketAddress) {
                            Platform.runLater(() -> {
                                List<SocketAddress> updatedClients = (List<SocketAddress>) receivedData;
                                handleConnectedClients(updatedClients);
                            });
                        } else {
                            // Handle other types of data (if any)
                        }
                    }
                    Thread.sleep(1000); // Adjust as needed
                } catch (EOFException eofException) {
                    System.err.println("End of stream reached unexpectedly. Waiting for more data...");
                }
            }

        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to retrieve data from the server.");
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void handleConnectedClients(List<SocketAddress> clients) {
        connectedClients.clear();
        connectedClients.addAll(clients);
        // Update UI to display the list of connected clients
    }
    private void receiveAndApplySettings(List<Object> settings) {
        int p1 = (int) settings.get(0);
        int n1 = (int) settings.get(1);
        int p2Percentage = (int) settings.get(2);
        String commonPriority = (String) settings.get(3);
        String albinoPriority = (String) settings.get(4);
        long commonLifetime = (long) settings.get(5);
        long albinoLifetime = (long) settings.get(6);

        // Update UI components with received data
        setP1SliderValue(p1);
        setN1TextFieldValue(String.valueOf(n1));
        setP2PercentageSliderValue(p2Percentage);
        setCommonPriority(Integer.parseInt(commonPriority));
        setAlbinoPriority(Integer.parseInt(albinoPriority));
        setCommonLifetimeTextFieldValue(String.valueOf(commonLifetime / 1000));
        setAlbinoLifetimeTextFieldValue(String.valueOf(albinoLifetime / 1000));
    }

    private void showConnectedClients() {
        ChoiceDialog<SocketAddress> dialog = new ChoiceDialog<>(null, connectedClients);
        dialog.setTitle("Connected Clients");
        dialog.setHeaderText("Select a Client IP Address:");
        dialog.setContentText("IP Address:");

        Optional<SocketAddress> result = dialog.showAndWait();
        if (result.isPresent()) {
            // Call sendSettingsToServer() method to send settings data to the server
            sendSettingsToServer();
        }

    }


    private void sendSettingsToServer() {
        try {
            // Create ObjectOutputStream only once when initializing the client
            if (outputStream == null) {
                outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            }

            int p1 = (int) P1Slider.getValue();
            int n1 = Integer.parseInt(N1TextField.getText());
            int p2Percentage = (int) P2PercentageSlider.getValue();
            String commonPriority = commonPriorityDropdown.getValue();
            String albinoPriority = albinoPriorityDropdown.getValue();
            long commonLifetime = Long.parseLong(commonLifetimeTextField.getText()) * 1000;
            long albinoLifetime = Long.parseLong(albinoLifetimeTextField.getText()) * 1000;
            List<Object> settings = new ArrayList<>();
            settings.add(p1);
            settings.add(n1);
            settings.add(p2Percentage);
            settings.add(commonPriority);
            settings.add(albinoPriority);
            settings.add(commonLifetime);
            settings.add(albinoLifetime);
            outputStream.writeObject(settings);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private String formatConnectedClients(List<SocketAddress> connectedClients) {
        StringBuilder builder = new StringBuilder();
        for (SocketAddress address : connectedClients) {
            builder.append(address).append("\n");
        }
        return builder.toString();
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }





    @FXML
    public void handlePriorityChange() {
        String commonPriority = commonPriorityDropdown.getValue();
        String albinoPriority = albinoPriorityDropdown.getValue();

        // Pass the selected priorities to Habitat class
        Habitat.setCommonPriority(Integer.parseInt(commonPriority));
        Habitat.setAlbinoPriority(Integer.parseInt(albinoPriority));
    }
    @FXML
    public void stopAlbinoAI(ActionEvent event) {

        if (Habitat.startFlag) {
            synchronized (habitat) {
                habitat.stopAlbinoAI(); // Pause the albino AI thread
            }
            stopAlbinoAIButton.setDisable(true); // Disable the stop button
            resumeAlbinoAIButton.setDisable(false); // Enable the resume button
        }
    }

    @FXML
    public void resumeAlbinoAI(ActionEvent event) {
        if (Habitat.startFlag) {
            synchronized (habitat) {
                habitat.resumeAlbinoAI(); // Resume the albino AI thread
                habitat.notifyAll(); // Notify all waiting threads

                stopAlbinoAIButton.setDisable(false); // Enable the stop button
                resumeAlbinoAIButton.setDisable(true); // Disable the resume button
            }
        }
    }

    @FXML
    public void stopCommonAI(ActionEvent event) {
        if (Habitat.startFlag) {
            synchronized (habitat) {
                habitat.stopCommonAI(); // Pause the common AI thread

                stopCommonAIButton.setDisable(true); // Disable the stop button
                resumeCommonAIButton.setDisable(false); // Enable the resume button
            }
        }
    }

    @FXML
    public void resumeCommonAI(ActionEvent event) {
        if (Habitat.startFlag) {
            synchronized (habitat) {
                habitat.resumeCommonAI(); // Resume the common AI thread
                habitat.notifyAll(); // Notify all waiting threads

                stopCommonAIButton.setDisable(false); // Enable the stop button
                resumeCommonAIButton.setDisable(true); // Disable the resume button
            }
        }
    }


    @FXML
    public void startAction() {
        int P1 = (int) P1Slider.getValue();
        if (!Habitat.startFlag && P1 != 0 ) {
            habitat.startAction();
            if (showTimeButton.isSelected()) {
                Habitat.getInstance().showTimeLabel();
            }
            // startButton.setDisable(true); // Disable start button
            stopButton.setDisable(false);
        }
    }

    @FXML
    public void stopAction() {
        if (Habitat.startFlag) {
            habitat.stopAction();
            startButton.setDisable(false); // Enable start button
            stopButton.setDisable(true);
        }
    }

    @FXML
    void showTimeButtonClick(ActionEvent event) {
        if (Habitat.startFlag) {
            hideTimeButton.setSelected(false);
            habitat.showTimeLabel();
            habitat.timeFlag = true;
        }
    }

    @FXML
    void hideTimeButtonClick(ActionEvent event) {
        showTimeButton.setSelected(false);
        habitat.hideTimeLabel();
        habitat.timeFlag = false;
    }

    @FXML
    public void keyPressed(KeyEvent event) {
        try {
            if (event.getCode().equals(javafx.scene.input.KeyCode.T)) {
                if (Habitat.startFlag) {
                    toggleTimeLabel();
                }
            } else if (event.getCode().equals(javafx.scene.input.KeyCode.B)) {
                if (!Habitat.startFlag) {
                    habitat.startAction();
                }
            } else if (event.getCode().equals(javafx.scene.input.KeyCode.E)) {
                if (Habitat.startFlag) {
                    habitat.stopAction();
                }
            }
        } catch (Exception e) {
            Logger.getLogger(mainController.class.getName()).log(Level.SEVERE, "Error handling key event", e);
        }
    }

    @FXML
    void resultWindowCheckBoxClick(ActionEvent event) {
        Habitat.setResultWindowFlag(resultWindowCheckBox.isSelected()); // Pass the selected state of the checkbox
    }

    private void toggleTimeLabel() {
        if (showTimeButton.isSelected()) {
            hideTimeButton.setSelected(true);
            hideTimeButtonClick(null); // Call hideTimeButtonClick method to hide the time label
        } else {
            showTimeButton.setSelected(true);
            showTimeButtonClick(null); // Call showTimeButtonClick method to show the time label
        }
        habitat.timeFlag = !habitat.timeFlag;
    }

    private void handleKeyPress(KeyEvent event) {
        try {
            if (event.getCode().equals(javafx.scene.input.KeyCode.T)) {
                if (Habitat.startFlag) {
                    toggleTimeLabel();
                }
            } else if (event.getCode().equals(javafx.scene.input.KeyCode.B)) {
                if (!Habitat.startFlag) {
                    habitat.startAction();
                }
            } else if (event.getCode().equals(javafx.scene.input.KeyCode.E)) {
                if (Habitat.startFlag) {
                    habitat.stopAction();
                }
            }
        } catch (Exception e) {
            Logger.getLogger(mainController.class.getName()).log(Level.SEVERE, "Error handling key event", e);
        }
    }

    @FXML
    void OkButtonClick(ActionEvent event) {
        if (!validateInputs()) {
            // Do nothing
        }else{
            // Enable start and stop buttons after validation
            startButton.setDisable(false);
            stopButton.setDisable(false);
        }

    }

    private boolean validateInputs() {
        int selectedP1 = (int) P1Slider.getValue();
        int selectedN1;
        try {
            selectedN1 = Integer.parseInt(N1TextField.getText());
            if (selectedN1 <= 0 || selectedN1 > 100) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Confirm Input");
                alert.setHeaderText("Invalid Input");
                alert.setContentText("N1 must be greater than 0 and less than or equal to 100.\n" +
                        "Do you want to continue with default entry?");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    // Set default N1
                    selectedN1 = 1;
                } else {
                    // User wants to edit inputs, return without closing the window
                    return false;
                }
            }
        } catch (NumberFormatException e) {
            showInvalidInputAlert("N1 must be a valid integer.");
            return false;
        }
        // Set values in Habitat
        Habitat.setP1(selectedP1);
        Habitat.setN1(selectedN1);
        Habitat.getInstance().setP2Percentage((int) P2PercentageSlider.getValue());

        // Validate lifetimes
        if (!validateLifetimeInput(commonLifetimeTextField.getText(), "Common Rabbit"))
            return false;
        if (!validateLifetimeInput(albinoLifetimeTextField.getText(), "Albino Rabbit"))
            return false;

        return true;
    }

    private boolean validateLifetimeInput(String input, String type) {
        long commonLifetime, albinoLifetime;

        try {
            commonLifetime = Long.parseLong(commonLifetimeTextField.getText()) * 1000; // Convert seconds to milliseconds
            albinoLifetime = Long.parseLong(albinoLifetimeTextField.getText()) * 1000; // Convert seconds to milliseconds

            if (commonLifetime <= 0 || albinoLifetime <= 0) {
                showInvalidInputAlert("Lifetime values must be greater than 0.");
                return false;
            }
        } catch (NumberFormatException e) {
            showInvalidInputAlert("Invalid input. Please enter a valid number.");
            return false;
        }

        habitat.setLifetimeValues(commonLifetime, albinoLifetime); // Pass lifetime values to Habitat

        return true;
    }

    private void showInvalidInputAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Invalid Input");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public Pane getRabbitsPane() {
        return rabbitsPane;
    }

    public Label getTimeLabel() {
        return timeLabel;
    }

    public Label getStatistic() {
        return statisticLabel;
    }

    public void printLabel(String text) {
        timeLabel.setText(text);
    }

    @FXML
    void showCurrentObjects(ActionEvent event) {
        Habitat.getInstance().showCurrentObjects();
    }




    @FXML
    void saveObjects(ActionEvent event) {
        // Save live objects to the selected file
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Objects");
        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            // Save live objects
            Habitat.getInstance().saveObjectsToFile(file);
        }
    }

    @FXML
    void loadObjects(ActionEvent event) {
        // Stop current simulation
        Habitat.getInstance().stopAction();

        // Load objects from the selected file
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Objects");
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            // Get current time
            long currentTime = System.currentTimeMillis();
            // Load objects from file
            // Load objects from file and start simulation
            Habitat.getInstance().loadObjectsFromFile(file, currentTime);
        }
    }
    @FXML
    void openConsole(ActionEvent event) {
        // Open non-modal dialog for console command
        // Instantiate dialog
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Console");

        // Set the button types (only for the dialog to be closable)
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        // Get the close button and make it invisible
        Node closeButton = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeButton.setVisible(false);

        // Create a label for the introduction message
        Label introductionLabel = new Label();
        introductionLabel.setPrefWidth(400);
        introductionLabel.setStyle("-fx-background-color: black; -fx-text-fill: white;");
        introductionLabel.setText("Welcome to the Console!\nThis is a sample console application.\nYou can enter commands below and press Enter to execute them.\n\nC:\\CodeXProject\\Reduce albino by %");

        // Create and configure the text area for command input and output
        TextArea textArea = new TextArea();
        textArea.setPrefWidth(400);
        textArea.setPrefHeight(300);
        textArea.setStyle("-fx-control-inner-background: black; -fx-text-fill: white;");
        textArea.setEditable(true); // Allow user input

        // Set the content of the dialog pane
        VBox content = new VBox(introductionLabel, textArea);
        dialog.getDialogPane().setContent(content);

        // Request focus on the text area by default
        Platform.runLater(textArea::requestFocus);

        // Convert the result to a string when Enter is pressed
        dialog.getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER && !e.isShiftDown()) {
                e.consume(); // Consume the event to prevent newline character in the text area
                try (BufferedReader reader = new BufferedReader(new StringReader(textArea.getText()))) {
                    String[] lines = reader.lines().toArray(String[]::new);
                    String userInput = lines[lines.length - 1]; // Get the last line
                    String response = processCommand(userInput);
                    textArea.appendText("\n" + response + "\n"); // Add response and new prompt
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Show the dialog
        dialog.showAndWait();
    }



    public String processCommand(String command) {
        if (command.toLowerCase().startsWith("reduce albino by")) {
            try {
                int percentage = Integer.parseInt(command.replaceAll("[^0-9]", ""));

                // Implement logic to reduce albino rabbits by the specified percentage
                double percent = percentage / 100.0;
                reduceAlbinoRabbits(percent);

                return "Reduced " + percentage + "% of albino rabbits.";
            } catch (NumberFormatException e) {
                return "Invalid percentage format.";
            }
        } else {
            return "Unknown command.";
        }
    }

    private void reduceAlbinoRabbits(double percent) {
        List<Rabbit> albinoRabbitsToRemove = new ArrayList<>();

        for (Rabbit rabbit : Habitat.getInstance().getListRabbits()) {
            if (rabbit instanceof AlbinoRabbit) {
                albinoRabbitsToRemove.add(rabbit);
            }
        }

        int totalAlbinoRabbits = albinoRabbitsToRemove.size();
        int numToRemove = (int) (totalAlbinoRabbits * percent);

        for (int i = 0; i < numToRemove; i++) {
            if (!albinoRabbitsToRemove.isEmpty()) {
                Rabbit rabbitToRemove = albinoRabbitsToRemove.removeFirst();
                getRabbitsPane().getChildren().remove(rabbitToRemove.getImageView());
                Habitat.getInstance().getListRabbits().remove(rabbitToRemove);
            }
        }
    }


    public void setP1SliderValue(double value) {
        P1Slider.setValue(value);
    }

    public void setN1TextFieldValue(String value) {
        N1TextField.setText(value);
    }

    public void setP2PercentageSliderValue(double value) {
        P2PercentageSlider.setValue(value);
    }

    public void setCommonLifetimeTextFieldValue(String value) {
        commonLifetimeTextField.setText(value);
    }

    public void setAlbinoLifetimeTextFieldValue(String value) {
        albinoLifetimeTextField.setText(value);
    }
    public void setCommonPriority(int priority) {

        commonPriorityDropdown.setValue(String.valueOf(priority));
    }

    public void setAlbinoPriority(int priority) {
        albinoPriorityDropdown.setValue(String.valueOf(priority));
    }


    @FXML
    private void handleSaveToDatabase() {
        Habitat.getInstance().saveRabbitsToDatabase();
    }

    @FXML
    private void handleLoadFromDatabase() {
        // Create a dialog to prompt the user for their choice
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Load from Database");
        alert.setHeaderText("Choose Rabbit Type");
        alert.setContentText("Do you want to retrieve Albino or Common rabbits?");

        // Add buttons for the user to choose
        ButtonType albinoButton = new ButtonType("Albino");
        ButtonType commonButton = new ButtonType("Common");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(albinoButton, commonButton, cancelButton);

        // Show the dialog and wait for the user's response
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == albinoButton) {
                // User chose Albino rabbits
                Habitat.getInstance().loadRabbitsFromDatabase("Albino");
            } else if (result.get() == commonButton) {
                // User chose Common rabbits
                Habitat.getInstance().loadRabbitsFromDatabase("Common");
            }
        }
    }

}