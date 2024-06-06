package com.example.JavaRabbit.Model;

import com.example.JavaRabbit.controller.Habitat;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;

public class AlbinoRabbit extends Rabbit implements Serializable {
    private static final long serialVersionUID = 1L; // Serialization version ID
    private static final String IMAGE_PATH = "src/image/albinoRabbit.png";

    public AlbinoRabbit() {
        this(0, 0, 0, 0, 0, null);
    }

    public AlbinoRabbit(int x, int y, long timeOfBirth, long lifetime, int id, BaseAI ai) {
        super(x, y, timeOfBirth, lifetime, id, ai);
        this.imageView = createImageView();
    }

    @Override
    public void updatePosition(long currentTime) {
        synchronized (ai.lock) {
            if (!Habitat.getInstance().albinoPaused) {
                ai.move();
                x = ai.getX();
                y = ai.getY();
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
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject(); // Serialize non-transient fields by default
        // Add custom serialization logic for transient or non-serializable fields if needed
    }

    // Custom deserialization logic
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject(); // Deserialize non-transient fields by default
        // Add custom deserialization logic for transient or non-serializable fields if needed
    }

    public double getSpeed() {
        return 0;
    }
}
