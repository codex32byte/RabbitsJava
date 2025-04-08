    package com.example.JavaRabbit.Model;

    import java.io.Serializable;

    public abstract class BaseAI extends Thread implements Serializable {
        protected double x;
        protected double y;
        protected double dx;
        protected double dy;
        protected double speed;
        protected boolean isAlive;
        protected final Object lock = new Object(); // Object for synchronization

        public BaseAI(double x, double y, double speed) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.isAlive = true;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public abstract void move();

    }
