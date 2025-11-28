package model;

import controller.Controller;

import java.util.ArrayList;
import java.util.List;

public class Model {
    private final Controller controller;
    private final List<Ball> ballList;
    private final Habitacion habitacion;
    private volatile boolean paused = false;

    public Model(Controller controller) {
        this.controller = controller;
        this.ballList = new ArrayList<>();
        // create a central room; if viewer not yet sized, use defaults
        int vw = Math.max(200, controller.getViewerWidth());
        int vh = Math.max(200, controller.getViewerHeight());
        int rw = Math.max(100, vw / 4);
        int rh = Math.max(100, vh / 4);
        int rx = (vw - rw) / 2;
        int ry = (vh - rh) / 2;
        this.habitacion = new Habitacion(rx, ry, rw, rh);
    }
    public synchronized void addBall() {
        Ball ball = new Ball(this);
        ballList.add(ball);
    }

    /**
     * Create a Ball and return it (adds to internal list). Caller may configure it.
     */
    public synchronized Ball createBall() {
        Ball ball = new Ball(this);
        ballList.add(ball);
        return ball;
    }

    public synchronized void addBallWithSize(int diameter) {
        Ball ball = new Ball(this, diameter);
        ballList.add(ball);
    }

    public synchronized Ball createBallWithSize(int diameter) {
        Ball ball = new Ball(this, diameter);
        ballList.add(ball);
        return ball;
    }

    public synchronized void addBallWithRandomSize(int minDiameter, int maxDiameter) {
        int d = minDiameter + (int)(Math.random() * (maxDiameter - minDiameter + 1));
        addBallWithSize(d);
    }

    public synchronized Ball createBallWithRandomSize(int minDiameter, int maxDiameter) {
        int d = minDiameter + (int)(Math.random() * (maxDiameter - minDiameter + 1));
        return createBallWithSize(d);
    }

    public synchronized List<Ball> getAllBalls() {
        return new ArrayList<>(ballList);
    }
    public int getViewerWidth() {
        return controller.getViewerWidth();
    }
    public int getViewerHeight() {
        return controller.getViewerHeight();
    }

    public Habitacion getHabitacion() {
        return habitacion;
    }

    public void setPaused(boolean p) {
        this.paused = p;
    }

    public boolean isPaused() {
        return this.paused;
    }

    public synchronized void clearBalls() {
        for (Ball b : new ArrayList<>(ballList)) {
            b.stop();
        }
        ballList.clear();
    }
}
