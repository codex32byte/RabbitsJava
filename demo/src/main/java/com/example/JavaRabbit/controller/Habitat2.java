        package com.example.JavaRabbit.controller;

        import com.example.JavaRabbit.Model.*;
        import javafx.animation.AnimationTimer;
        import javafx.application.Application;
        import javafx.application.Platform;
        import javafx.fxml.FXMLLoader;
        import javafx.scene.Parent;
        import javafx.scene.Scene;
        import javafx.scene.control.Alert;
        import javafx.scene.control.TextArea;
        import javafx.scene.image.Image;
        import javafx.scene.layout.*;
        import javafx.stage.Stage;

        import java.io.*;
        import java.sql.Connection;
        import java.sql.PreparedStatement;
        import java.sql.ResultSet;
        import java.sql.SQLException;
        import java.text.SimpleDateFormat;
        import java.util.*;
        import java.util.logging.Level;
        import java.util.logging.Logger;

        public class Habitat2 extends Application {

            private static final Logger logger = Logger.getLogger(Habitat2.class.getName());

            private static volatile Habitat2 instance;
            private mainController mainController = null;
            private Timer timer;
            public static boolean timeFlag;
            private long startTime;
            private boolean resultWindowFlag;
            private String statStr;
            private static int P1;
            private static int N1;
            private int P2Percentage;
            public static boolean startFlag;
            private Vector<Rabbit> listRabbits;

            private final TreeSet<Integer> uniqueIds;
            private final HashMap<Integer, Long> birthTimes;
            private long commonLifetime;
            private long albinoLifetime;
            public boolean albinoPaused;
            public boolean commonPaused;
            private static int commonPriority = Thread.NORM_PRIORITY;// Default priority
            private static int albinoPriority = Thread.NORM_PRIORITY;// Default priority
            private CommonRabbitAI commonRabbitAIThread;
            private AlbinoRabbitAI albinoRabbitAIThread;
            private AnimationTimer animationTimer;


            public Habitat2() {
                initialize();
                listRabbits = new Vector<>();
                uniqueIds = new TreeSet<>();
                birthTimes = new HashMap<>();
            }

            public static Habitat2 getInstance() {
                Habitat2 localInstance = instance;
                if (localInstance == null) {
                    synchronized (Habitat2.class) {
                        localInstance = instance;
                        if (localInstance == null) {
                            instance = localInstance = new Habitat2();
                        }
                    }
                }
                return localInstance;
            }


            public static void main(String[] args) {
                launch(args);
            }

            public static void setResultWindowFlag(boolean flag) {
                getInstance().resultWindowFlag = flag;
            }

            public static void setContinueFlag() {
                startFlag = true;
            }

            public static void setStopFlag() {
                startFlag = false;
            }
            public static void setCommonPriority(int priority) {
                commonPriority = priority;
            }

            public static void setAlbinoPriority(int priority) {
                albinoPriority = priority;
            }
            public void stopAlbinoAI() {
                synchronized (this) {
                    albinoPaused = true; // Update flag to pause albino AI thread
                    notifyAll(); // Notify waiting threads to check for changes
                }
            }

            public void resumeAlbinoAI() {
                synchronized (this) {
                    albinoPaused = false; // Update flag to resume albino AI thread
                    notifyAll(); // Notify waiting threads to resume
                }
            }

            public void stopCommonAI() {
                synchronized (this) {
                    commonPaused = true; // Update flag to pause common AI thread
                    notifyAll(); // Notify waiting threads to check for changes
                }
            }

            public void resumeCommonAI() {
                synchronized (this) {
                    commonPaused = false; // Update flag to resume common AI thread
                    notifyAll(); // Notify waiting threads to resume
                }
            }

            @Override
            public void start(Stage primaryStage) {
                try {
                    FXMLLoader mainMenuLoader = new FXMLLoader(getClass().getResource("/com/example/JavaRabbit/view/mainWindow.fxml"));
                    Parent mainMenuRoot = mainMenuLoader.load();

                    mainController = mainMenuLoader.getController();
                    setController(mainController); // Setting the controller here
                    if (configFileExists()) {
                        loadConfigFromFile();
                    } else {
                        logger.warning("Configuration file does not exist.");
                    }
                    Scene mainMenuScene = new Scene(mainMenuRoot, 900, 750);
                    primaryStage.setScene(mainMenuScene);
                    primaryStage.setTitle("Rabbit Habitat");

                    // Load the background image // doesn't work till now !!
                    Image img = new Image(new FileInputStream("src/image/background.jpg"));
                    BackgroundImage bImg = new BackgroundImage(img,
                            BackgroundRepeat.NO_REPEAT,
                            BackgroundRepeat.NO_REPEAT,
                            BackgroundPosition.DEFAULT,
                            BackgroundSize.DEFAULT);

                    Background bGround = new Background(bImg);
                    mainController.mainPane.setBackground(bGround);

                    // Add event handler for key presses
                    mainMenuScene.setOnKeyPressed(event -> mainController.keyPressed(event));

                    // Add event handler for close request
                    primaryStage.setOnCloseRequest(event -> {
                        saveConfigToFile();
                        // Call method to stop the simulation
                        Platform.exit();
                        System.exit(0);
                    });
                    //TCPClient.connectToServer();
                    primaryStage.show();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error during initialization", e);
                }
            }

            // Method to set the mainController instance
            public void setController(mainController controller) {
                this.mainController = controller;
            }

            public void setP2Percentage(int value) {
                this.P2Percentage = value;
            }

            public void startAction() {
                startFlag = true;
                startTime = System.currentTimeMillis();
                if (mainController != null) {
                    mainController.getStatistic().setVisible(false);
                    mainController.getTimeLabel().setVisible(false);
                }
                startCycle();
            }


            public void stopAction() {
                startFlag = false;
                if (resultWindowFlag) {
                    // Show the latest updated statistics
                    showResultWindow();
                }
                try {
                    updateRabbits(System.currentTimeMillis() - startTime);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error during stopping action", e);
                }
                if (!startFlag) {
                    timer.cancel();
                    timer = new Timer();
                    startTime = System.currentTimeMillis();
                    clearListRabbits();
                }
            }

            private void clearListRabbits() {

                synchronized (listRabbits) {
                    listRabbits.forEach(rabbit -> mainController.getRabbitsPane().getChildren().remove(rabbit.getImageView()));
                    listRabbits.clear();
                }

            }
            private boolean configFileExists() {
                File configFile = new File(CONFIG_FILE_PATH); // Assuming CONFIG_FILE_PATH is the path to your configuration file
                return configFile.exists() && configFile.isFile();
            }

            private int generateUniqueId() {
                synchronized (uniqueIds) {
                    int id = new Random().nextInt(Integer.MAX_VALUE);
                    while (uniqueIds.contains(id)) {
                        id = new Random().nextInt(Integer.MAX_VALUE);
                    }
                    uniqueIds.add(id);
                    return id;
                }
            }

            private void bornRabbits(long currentTime, int P, int N) {
                Random random = new Random();
                Habitat2 habitatInstance = Habitat2.getInstance(); // Get the instance of Habitat
                int randP;
                int totalRabbits = listRabbits.size();
                int albinoCount = (int) listRabbits.stream().filter(r -> r instanceof AlbinoRabbit).count();
                int maxAlbinoCount = (int) (totalRabbits * (habitatInstance.P2Percentage / 100.0));
                if ((currentTime / 1000) % N == 0) {
                    randP = random.nextInt(100);
                    if (P <= randP && albinoCount < maxAlbinoCount) {
                        try {
                            bornAlbinoRabbit();
                        } catch (IOException e) {
                            logger.log(Level.SEVERE, "Error during albino rabbit birth", e);
                        }
                    } else {
                        try {
                            bornCommonRabbit();
                        } catch (IOException e) {
                            logger.log(Level.SEVERE, "Error during common rabbit birth", e);
                        }
                    }
                }
            }

            private void bornCommonRabbit() throws IOException {
                Random random = new Random();
                int x;
                int y;
                synchronized (mainController.getRabbitsPane()) {
                    x = (int) (random.nextDouble() * (mainController.getRabbitsPane().getWidth() - 1));
                    y = (int) (random.nextDouble() * (mainController.getRabbitsPane().getHeight() - 10));
                }

                // Provide currentTime and lifetime values
                long currentTime = System.currentTimeMillis();
                long lifetime = calculateLifetime("Common");
                // Create AI instance for CommonRabbit
                commonRabbitAIThread = new CommonRabbitAI(x, y, 1);
                commonRabbitAIThread.setPriority(commonPriority);
                CommonRabbit rabbit = new CommonRabbit(x, y, currentTime, lifetime, generateUniqueId(), commonRabbitAIThread);
                Platform.runLater(() -> {
                    synchronized (listRabbits) {
                        mainController.getRabbitsPane().getChildren().add(rabbit.getImageView());
                        listRabbits.add(rabbit);
                        birthTimes.put(rabbit.getId(), currentTime);
                    }
                });
            }

            private void bornAlbinoRabbit() throws IOException {
                Random random = new Random();
                int x;
                int y;
                synchronized (mainController.getRabbitsPane()) {
                    x = (int) (random.nextDouble() * (mainController.getRabbitsPane().getWidth() + 1));
                    y = (int) (random.nextDouble() * (mainController.getRabbitsPane().getHeight() - 2));
                }

                // Provide currentTime and lifetime values
                long currentTime = System.currentTimeMillis();
                long lifetime = calculateLifetime("Albino"); // Calculate lifetime for Albino Rabbit
                // Create AI instance for AlbinoRabbit
                albinoRabbitAIThread = new AlbinoRabbitAI(x, y, 1);
                AlbinoRabbit rabbit = new AlbinoRabbit(x, y, currentTime, lifetime, generateUniqueId(), albinoRabbitAIThread);
                albinoRabbitAIThread.setPriority(albinoPriority);
                Platform.runLater(() -> {
                    synchronized (listRabbits) {
                        mainController.getRabbitsPane().getChildren().add(rabbit.getImageView());
                        listRabbits.add(rabbit);
                        birthTimes.put(rabbit.getId(), currentTime);
                    }
                });
            }
            private void updateRabbits(long currentTime) throws IOException {
                if (startFlag) {
                    showTimeLabel();
                    bornRabbits(currentTime, P1, N1);
                    // Update the position of each rabbit
                    synchronized (listRabbits) {
                        for (Rabbit rabbit : listRabbits) {
                            rabbit.updatePosition(currentTime);
                        }
                    }
                }
            }

            private void removeExpiredRabbits() {
                if (!startFlag) {
                    return; // If simulation is stopped, do not execute
                }
                long currentTime = System.currentTimeMillis();
                synchronized (listRabbits) {
                    Iterator<Rabbit> iterator = listRabbits.iterator();
                    while (iterator.hasNext()) {
                        Rabbit rabbit = iterator.next();
                        if (!rabbit.isAlive(currentTime)) {
                            Platform.runLater(() -> {
                                mainController.getRabbitsPane().getChildren().remove(rabbit.getImageView());
                            });
                            iterator.remove(); // Remove dead rabbit from the list
                        }
                    }
                }
            }
            void showTimeLabel() {
                if (timeFlag) {
                    mainController.getTimeLabel().setVisible(true);
                    if (startTime != 0) {
                        long time = System.currentTimeMillis() - startTime;
                        long seconds = (time / 1000) % 60;
                        long minutes = (time / (1000 * 60)) % 60;
                        String timeStr = String.format("Time: %02d:%02d", minutes, seconds);
                        mainController.printLabel(timeStr);

                        // Customize font and color for the time label
                        String font = "Arial";
                        String textColor = "#FF0000"; // Red color code, adjust as needed
                        int fontSize = 14;

                        String styledText = String.format("-fx-font-family: '%s'; -fx-font-size: %d; -fx-text-fill: %s;", font, fontSize, textColor);
                        mainController.getTimeLabel().setStyle(styledText);
                    }
                } else {
                    mainController.getTimeLabel().setVisible(false);
                }
            }

            private long calculateLifetime(String rabbitType) {
                long lifetime;
                if (rabbitType.equals("Common")) {
                    lifetime = commonLifetime;// Get the user-defined lifetime for common rabbits
                } else {
                    lifetime = albinoLifetime; // Get the user-defined lifetime for albino rabbits
                }
                return lifetime;
            }

            void showCurrentObjects() {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Current Objects");
                alert.setHeaderText(null);

                TextArea textArea = new TextArea();
                textArea.setEditable(false);

                // Populate text area with current objects, their time of birth, and lifetime
                long currentTime = System.currentTimeMillis();
                SimpleDateFormat sdf = new SimpleDateFormat("mm:ss"); // Format for date and time
                for (Rabbit rabbit : listRabbits) {
                    if (rabbit.isAlive(currentTime)) {
                        String birthTimeFormatted = sdf.format(new Date(rabbit.getTimeOfBirth()));
                        long lifetimeInSeconds = rabbit.getLifetime() / 1000; // Convert lifetime to seconds

                        textArea.appendText(String.format("ID: %d, Type: %s, Time of Birth: %s, Lifetime: %d seconds\n",
                                rabbit.getId(), rabbit instanceof CommonRabbit ? "Common" : "Albino",
                                birthTimeFormatted, lifetimeInSeconds));
                    }
                }

                alert.getDialogPane().setContent(textArea);
                alert.showAndWait();
            }


            void hideTimeLabel() {
                mainController.getTimeLabel().setVisible(false);
            }

            public void showResultWindow() {
                try {
                    ResultWindow.getInstance().showMenu(); // Remove the argument passed here
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error opening ResultWindow", e);
                }
            }

            public String getStatistics() {
                String statStr;
                long finalTime = System.currentTimeMillis() - startTime;
                long countCommonRabbit = listRabbits.stream().filter(r -> r instanceof CommonRabbit).count();
                long countAlbinoRabbit = listRabbits.stream().filter(r -> r instanceof AlbinoRabbit).count();

                statStr = String.format("Common rabbits: %d\n", countCommonRabbit);
                statStr += String.format("Albino rabbits: %d\n", countAlbinoRabbit);
                statStr += String.format("Time: %d seconds", finalTime / 1000);

                return statStr;
            }
            private void initialize() {
                timeFlag = false;
                startFlag = false;
                timer = new Timer();
                startTime = System.currentTimeMillis();
            }

            private void startCycle() {
                animationTimer = new AnimationTimer() {
                    private long lastUpdate = 0;

                    @Override
                    public void handle(long now) {
                        if (now - lastUpdate >= 60_000_000) { //
                            lastUpdate = now;
                            try {
                                updateRabbits(System.currentTimeMillis() - startTime);
                                removeExpiredRabbits(); // Check rabbit images
                            } catch (IOException e) {
                                logger.log(Level.SEVERE, "Error during rabbit update", e);
                            }
                        }
                    }
                };
                animationTimer.start();
            }


            public static void setP1(Integer value) {
                getInstance().P1 = value;
            }

            public static void setN1(Integer value) {
                getInstance().N1 = value;
            }

            public static void setTimeFlag(boolean flag) {
                timeFlag = flag;
            }

            public void setLifetimeValues(long commonLifetime, long albinoLifetime) {
                this.commonLifetime = commonLifetime;
                this.albinoLifetime = albinoLifetime;
            }

            public static void pauseAI() {
                getInstance().stopAlbinoAI();
                getInstance().stopCommonAI();
            }

            public static void resumeAI() {
                getInstance().resumeAlbinoAI();
                getInstance().resumeCommonAI();
            }

            private static final String CONFIG_FILE_PATH = "config.txt";

            public void saveConfigToFile() {
                try (PrintWriter writer = new PrintWriter(new FileWriter(CONFIG_FILE_PATH))) {
                    // Get instance of Habitat to access instance variables
                    Habitat2 habitat = getInstance();

                    // Convert lifetime values from seconds to milliseconds
                    long commonLifetimeMillis = habitat.commonLifetime / 1000;
                    long albinoLifetimeMillis = habitat.albinoLifetime / 1000;

                    // Write simulation settings to the configuration file
                    writer.println("P1=" + habitat.P1);
                    writer.println("N1=" + habitat.N1);
                    writer.println("P2Percentage=" + habitat.P2Percentage);
                    writer.println("CommonLifetime=" + commonLifetimeMillis);
                    writer.println("AlbinoLifetime=" + albinoLifetimeMillis);
                    writer.println("CommonPriority=" + mainController.commonPriorityDropdown.getValue());
                    writer.println("AlbinoPriority=" + mainController.albinoPriorityDropdown.getValue());


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }



            public void loadConfigFromFile() {
                try (Scanner scanner = new Scanner(new File(CONFIG_FILE_PATH))) {
                    while (scanner.hasNextLine()) {
                        String line = scanner.nextLine();
                        String[] parts = line.split("=");
                        if (parts.length == 2) {
                            String key = parts[0];
                            String value = parts[1];
                            // Update simulation settings based on the values read from the configuration file
                            switch (key) {
                                case "P1":
                                    double p1Value = Double.parseDouble(value);
                                    Platform.runLater(() -> mainController.setP1SliderValue(p1Value));
                                    break;
                                case "N1":
                                    Platform.runLater(() -> mainController.setN1TextFieldValue(value));
                                    break;
                                case "P2Percentage":
                                    double p2PercentageValue = Double.parseDouble(value);
                                    Platform.runLater(() -> mainController.setP2PercentageSliderValue(p2PercentageValue));
                                    break;
                                case "CommonLifetime":
                                    Platform.runLater(() -> mainController.setCommonLifetimeTextFieldValue(value));
                                    break;
                                case "AlbinoLifetime":
                                    Platform.runLater(() -> mainController.setAlbinoLifetimeTextFieldValue(value));
                                    break;
                                case "CommonPriority":
                                    int commonPriority = Integer.parseInt(value);
                                    Platform.runLater(() -> mainController.setCommonPriority(commonPriority));
                                    break;
                                case "AlbinoPriority":
                                    int albinoPriority = Integer.parseInt(value);
                                    Platform.runLater(() -> mainController.setAlbinoPriority(albinoPriority));
                                    break;

                            }
                        }
                    }
                } catch (FileNotFoundException e) {

                    e.printStackTrace();
                }
            }


            public void saveObjectsToFile(File file) {
                try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file))) {
                    // Serialize each live rabbit object
                    for (Rabbit rabbit : listRabbits) {
                        // Write x, y coordinates and remaining lifetime along with the rabbit object
                        outputStream.writeObject(rabbit);
                        outputStream.writeInt((int) rabbit.getX());
                        outputStream.writeInt((int) rabbit.getY());
                        outputStream.writeLong(rabbit.getLifetime() - (System.currentTimeMillis() - rabbit.getTimeOfBirth()));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            public void loadObjectsFromFile(File file, long currentTime) {
                // Clear existing live objects
                clearListRabbits();

                // Get the controller's RabbitsPane
                Pane rabbitsPane = mainController.getRabbitsPane();

                try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file))) {
                    // Deserialize and add each rabbit object
                    while (true) {
                        try {
                            Rabbit rabbit = (Rabbit) inputStream.readObject();
                            // Read x, y coordinates and remaining lifetime
                            int x = inputStream.readInt();
                            int y = inputStream.readInt();
                            long remainingLifetime = inputStream.readLong();
                            // Instantiate a new Rabbit object with the provided information
                            Rabbit newRabbit = instantiateRabbitWithTime(rabbit, currentTime, x, y, remainingLifetime);
                            // Add the new rabbit to the list
                            listRabbits.add(newRabbit);
                            // Add the rabbit's image view to the scene
                            Platform.runLater(() -> rabbitsPane.getChildren().add(newRabbit.getImageView()));
                            //
                            //startTime = System.currentTimeMillis();
                        } catch (EOFException e) {
                            // End of file reached
                            break;
                        }
                    }

                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            // Instantiate a new Rabbit object with the provided information
            private Rabbit instantiateRabbitWithTime(Rabbit rabbit, long currentTime, int x, int y, long remainingLifetime) {
                Rabbit newRabbit = null;
                BaseAI ai = null;

                if (rabbit instanceof CommonRabbit) {
                    ai = new CommonRabbitAI(x, y, 1);
                    ((CommonRabbitAI) ai).start(); // Start the AI thread
                    newRabbit = new CommonRabbit(x, y, currentTime, remainingLifetime, rabbit.getId(), ai);
                } else if (rabbit instanceof AlbinoRabbit) {
                    ai = new AlbinoRabbitAI(x, y, 1);
                    ((AlbinoRabbitAI) ai).start(); // Start the AI thread
                    newRabbit = new AlbinoRabbit(x, y, currentTime, remainingLifetime, rabbit.getId(), ai);
                }

                return newRabbit;
            }

            public void saveRabbitsToDatabase() {
                String insertRabbitSQL = "INSERT INTO rabbits (type, x, y, birth_time, lifetime) VALUES (?, ?, ?, ?, ?)";

                try (Connection conn = DBConfig.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(insertRabbitSQL)) {

                    for (Rabbit rabbit : listRabbits) {
                        pstmt.setString(1, rabbit instanceof CommonRabbit ? "Common" : "Albino");
                        pstmt.setInt(2, (int) rabbit.getX());
                        pstmt.setInt(3, (int) rabbit.getY());
                        pstmt.setLong(4, rabbit.getTimeOfBirth());
                        pstmt.setLong(5, rabbit.getLifetime());
                        pstmt.addBatch();
                    }
                    pstmt.executeBatch();

                    System.out.println("Rabbits saved to the database successfully.");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            public void loadRabbitsFromDatabase(String rabbitType) {
                String selectRabbitSQL = "SELECT id, x, y, birth_time, lifetime FROM rabbits WHERE type=?";

                try (Connection conn = DBConfig.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(selectRabbitSQL)) {

                    // Set the rabbit type parameter
                    pstmt.setString(1, rabbitType);

                    // Execute the query
                    try (ResultSet rs = pstmt.executeQuery()) {
                        // Clear existing rabbits and their UI representations
                        clearListRabbits();
                        Pane rabbitsPane = mainController.getRabbitsPane();

                        while (rs.next()) {
                            int id = rs.getInt("id");
                            int x = rs.getInt("x");
                            int y = rs.getInt("y");
                            long birthTimeMillis = rs.getLong("birth_time");
                            long lifetimeMillis = rs.getLong("lifetime");

                            // Convert birthTime and lifetime from seconds to milliseconds if necessary
                            long birthTime = birthTimeMillis * 1000; // Convert seconds to milliseconds
                            long lifetime = lifetimeMillis * 1000; // Convert seconds to milliseconds

                            // Calculate current time
                            long currentTime = System.currentTimeMillis();

                            // Instantiate appropriate RabbitAI based on the rabbit type
                            BaseAI ai;
                            Rabbit rabbit;
                            if (rabbitType.equals("Albino")) {
                                ai = new AlbinoRabbitAI(x, y, 1);
                                ai.setPriority(albinoPriority);
                                rabbit = new AlbinoRabbit(x, y, birthTime, lifetime, id, ai);
                            } else {
                                ai = new CommonRabbitAI(x, y, 1);
                                ai.setPriority(commonPriority);
                                rabbit = new CommonRabbit(x, y, birthTime, lifetime, id, ai);
                            }
                            ai.start(); // Start the AI thread

                            // Calculate remaining lifetime in milliseconds
                            long remainingLifetimeMillis = lifetime - (currentTime - birthTime);

                            // Pass the Rabbit object to instantiateRabbitWithTime method
                            Rabbit instantiatedRabbit = instantiateRabbitWithTime(rabbit, currentTime, x, y, remainingLifetimeMillis);

                            // Add the instantiated rabbit to the list and UI
                            Platform.runLater(() -> {
                                rabbitsPane.getChildren().add(instantiatedRabbit.getImageView());
                                listRabbits.add(instantiatedRabbit);
                            });
                        }

                        System.out.println(rabbitType + " rabbits loaded from the database successfully.");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }



            public Vector<Rabbit> getListRabbits() {
                return listRabbits;
            }


        }

