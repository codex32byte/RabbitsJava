//Rabbit
package com.example.JavaRabbit.Model;

import javafx.scene.image.ImageView;

import java.io.Serializable;

public abstract class Rabbit implements Serializable {
    private static final long serialVersionUID = 1L; // Serialization version ID
    protected double x;
    protected double y;
    protected long timeOfBirth; // Updated to include time of birth
    protected long lifetime;
    protected int id;
    protected transient ImageView imageView; // Marking as transient since ImageView is not serializable
    protected transient BaseAI ai; // Marking as transient since BaseAI might need custom serialization logic

    public Rabbit(double x, double y, long timeOfBirth, long lifetime, int id, BaseAI ai) {
        this.x = x;
        this.y = y;
        this.timeOfBirth = timeOfBirth; //
        this.lifetime = lifetime;
        this.id = id;
        this.ai = ai;
    }

    public abstract void updatePosition(long currentTime);

    public boolean isAlive(long currentTime) {
        return (currentTime - timeOfBirth) < lifetime;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public long getTimeOfBirth() {
        return timeOfBirth;
    }

    public long getLifetime() {
        return lifetime;
    }

    public int getId() {
        return id;
    }

    public ImageView getImageView() {
        return imageView;
    }

    public void setImageView(ImageView imageView) {
        this.imageView = imageView;
    }

    public Object getAI() {
        return null;
    }

}
