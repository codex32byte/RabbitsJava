package com.example.JavaRabbit.Model;

import com.example.JavaRabbit.controller.Habitat;

import java.io.Serializable;
import java.util.Random;

public class CommonRabbitAI extends BaseAI implements Serializable {
    private static final int CHANGE_DIRECTION_INTERVAL = 5000; // Change direction every 5 seconds
    private long lastDirectionChangeTime;
    private Random random;

    public CommonRabbitAI(double x, double y, double speed) {
        super(x, y, speed);
        random = new Random();
        lastDirectionChangeTime = System.currentTimeMillis();
        // Initialize initial direction randomly
        dx = random.nextDouble() * 2 - 1;
        dy = random.nextDouble() * 2 - 1;
    }

    @Override
    public void move() {
        synchronized (lock) {
            while (Habitat.getInstance().commonPaused) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    // Thread was interrupted, exit the method
                    System.out.println("Thread was interrupted. Exiting...");
                    return;
                }
            }
        }

        // Check if it's time to change direction
        if (System.currentTimeMillis() - lastDirectionChangeTime >= CHANGE_DIRECTION_INTERVAL) {
            // Change direction randomly
            dx = random.nextDouble() * 2 - 1; // Random value between -1 and 1
            dy = random.nextDouble() * 2 - 1;
            lastDirectionChangeTime = System.currentTimeMillis();
        }

        // Move the rabbit
        x += dx * speed;
        y += dy * speed;
    }
}
