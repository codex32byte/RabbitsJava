package com.example.JavaRabbit.Model;

import com.example.JavaRabbit.controller.Habitat;

import java.io.Serializable;

public class AlbinoRabbitAI extends BaseAI implements Serializable {
    public AlbinoRabbitAI(double x, double y, double speed) {
        super(x, y, speed);
        // Set constant movement along the X-axis
        dx = speed; // Constant speed along the X-axis
        dy = 0;     // No movement along the Y-axis
    }

    @Override
    public void move() {
        synchronized (lock) { // Use the lock object from BaseAI for synchronization
            while (Habitat.getInstance().albinoPaused) {
                try {
                    lock.wait(); // Wait on the lock object
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        // Move the rabbit
        double newX = x + dx;
        double newY = y + dy;
        x = newX;
        y = newY;
    }
}
