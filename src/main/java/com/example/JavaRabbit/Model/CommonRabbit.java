package com.example.JavaRabbit.Model;

import com.example.JavaRabbit.controller.Habitat;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.*;

public class CommonRabbit extends Rabbit implements Serializable {
    private static final long serialVersionUID = 1L; // Serialization version ID
    private static final String IMAGE_PATH = "src/image/commonRabbit.png";

    public CommonRabbit() {
        this(0, 0, 0, 0, 0, null);
    }

    public CommonRabbit(int x, int y, long timeOfBirth, long lifetime, int id, BaseAI ai) {
        super(x, y, timeOfBirth, lifetime, id, ai);
        this.imageView = createImageView();
    }

    @Override
    public void updatePosition(long currentTime) {
        synchronized (ai.lock) {
            if (!Habitat.getInstance().commonPaused) {
                ai.move();
                x = ai.getX();
                y = ai.getY();
                // Get the bounds of the pane
                double paneWidth = 1000;
                double paneHeight = 1000;

                // Constrain rabbit's movement within the pane bounds
                x = Math.max(0, Math.min(x, paneWidth - imageView.getBoundsInParent().getWidth()));
                y = Math.max(0, Math.min(y, paneHeight - imageView.getBoundsInParent().getHeight()));
                imageView.relocate(x, y);
            }
        }
    }

    private ImageView createImageView() {
        try {
            Image image = new Image(new FileInputStream(IMAGE_PATH));
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(50);
            imageView.setFitHeight(50);
            imageView.relocate(x, y);
            return imageView;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Custom serialization logic
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject(); // Serialize non-transient fields by default
        // Add custom serialization logic for transient or non-serializable fields if needed
    }

    // Custom deserialization logic
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject(); // Deserialize non-transient fields by default
        // Add custom deserialization logic for transient or non-serializable fields if needed
    }

    public double getSpeed() {
        return 0;
    }
}
