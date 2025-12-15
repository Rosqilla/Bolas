package controller;

import model.Ball;
import model.Habitacion;
import model.Projectile;
import model.Explosion;
import view.View;
import model.Model;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.swing.*;

public class Controller {
    private final Model model;
    private final View view;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> autoTask;
    private final BallController ballController;

    public Controller() {
        // create view first so its dimensions can be queried by the model if needed
        this.view = new View(this);
        // create model with initial viewer dimensions
        this.model = new Model(view.getViewerWidth(), view.getViewerHeight());
        this.ballController = new BallController();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AutoBallScheduler");
            t.setDaemon(true);
            return t;
        });
        wireControls();
        // start a timer to periodically update model dimensions
        Timer dimensionUpdater = new Timer(100, e -> {
            model.updateViewerDimensions(view.getViewerWidth(), view.getViewerHeight());
        });
        dimensionUpdater.start();
    }
    
    
    public void addBall() {
        model.addBall();
    }
    public java.util.List<Ball> getAllBalls() {
        return model.getAllBalls();
    }
    public int getViewerWidth() {
        return view.getViewerWidth();
    }
    public int getViewerHeight() {
        return view.getViewerHeight();
    }

    public Habitacion getHabitacion() {
        return model.getHabitacion();
    }

    private void wireControls() {
        // manual add: remove any previous listeners then add
        JButton fire = view.getControlPanel().getFireButton();
    for (java.awt.event.ActionListener al : fire.getActionListeners()) fire.removeActionListener(al);
        fire.addActionListener(e -> addBallWithControlSettings());

        // auto toggle
        JButton autoBtn = view.getControlPanel().getAutoToggleButton();
    for (java.awt.event.ActionListener al : autoBtn.getActionListeners()) autoBtn.removeActionListener(al);
        autoBtn.addActionListener(e -> {
            JButton b = view.getControlPanel().getAutoToggleButton();
            if (autoTask == null || autoTask.isCancelled() || autoTask.isDone()) {
                int interval = (int) view.getControlPanel().getIntervalSpinner().getValue();
                // clamp interval to minimum 10ms to avoid scheduling issues
                if (interval < 10) interval = 10;
                // schedule first run immediately (initialDelay = 0) and then every 'interval' ms
                autoTask = scheduler.scheduleAtFixedRate(() -> addBallWithControlSettings(), 0, interval, TimeUnit.MILLISECONDS);
                SwingUtilities.invokeLater(() -> b.setText("Auto: ON"));
            } else {
                autoTask.cancel(false);
                SwingUtilities.invokeLater(() -> b.setText("Auto: OFF"));
            }
        });

        // react to interval changes: if auto is running, reschedule with new interval
        view.getControlPanel().getIntervalSpinner().addChangeListener(ev -> {
            int interval = (int) view.getControlPanel().getIntervalSpinner().getValue();
            if (interval < 10) interval = 10;
            // reschedule only if currently active
            if (autoTask != null && !autoTask.isCancelled() && !autoTask.isDone()) {
                // cancel and reschedule
                autoTask.cancel(false);
                autoTask = scheduler.scheduleAtFixedRate(() -> addBallWithControlSettings(), 0, interval, TimeUnit.MILLISECONDS);
            }
        });

        // FPS updater: Swing Timer on EDT updating label every 250ms
        Timer fpsTimer = new Timer(250, e -> {
            double fps = view.getViewer().getFps();
            double paint = view.getViewer().getLastPaintMs();
            view.getControlPanel().setFpsLabel(String.format("FPS: %.1f | Paint: %.2f ms", fps, paint));
        });
        fpsTimer.start();
        // clear button
        JButton clear = view.getControlPanel().getClearButton();
        for (java.awt.event.ActionListener al : clear.getActionListeners()) clear.removeActionListener(al);
        clear.addActionListener(e -> model.clearBalls());

        // pause button
        JButton pause = view.getControlPanel().getPauseButton();
    for (java.awt.event.ActionListener al : pause.getActionListeners()) pause.removeActionListener(al);
        pause.addActionListener(e -> {
            boolean now = !model.isPaused();
            model.setPaused(now);
            SwingUtilities.invokeLater(() -> pause.setText(now ? "Pausa: ON" : "Pausa: OFF"));
        });
    }

    private void addBallWithControlSettings() {
        Ball b;
        if (view.getControlPanel().isSizeRandom()) {
            int min = view.getControlPanel().getSizeMin();
            int max = view.getControlPanel().getSizeMax();
            b = model.createBallWithRandomSize(min, max);
        } else {
            int fixed = view.getControlPanel().getSizeMin();
            b = model.createBallWithSize(fixed);
        }
        // assign a random velocity in px/s and convert to internal px/ms
        double vx_px_s = -150 + Math.random() * 300; // [-150,150] px/s
        double vy_px_s = -150 + Math.random() * 300;
        double vx_px_ms = vx_px_s / 1000.0;
        double vy_px_ms = vy_px_s / 1000.0;
        b.setVelocity(vx_px_ms, vy_px_ms);
        // apply controller default acceleration (already in px/ms^2)
        ballController.applyDefaultsTo(b);
    }

    public BallController getBallController() { return ballController; }
    
    // Métodos para delegar operaciones de la vista al modelo
    public void rotateBall(Ball ball, double radians) {
        if (ball != null) {
            ball.rotate(radians);
        }
    }
    
    public void rotateBallOrientation(Ball ball, double radians) {
        if (ball != null) {
            ball.rotateOrientation(radians);
        }
    }
    
    public void applyThrustToBall(Ball ball, double thrustMagnitude) {
        if (ball != null) {
            ball.applyThrust(thrustMagnitude);
        }
    }
    
    public void fireBullet(Ball shooter) {
        model.fireBullet(shooter);
    }
    
    public List<Projectile> getAllProjectiles() {
        return model.getAllProjectiles();
    }
    
    public List<Explosion> getAllExplosions() {
        return model.getAllExplosions();
    }
    
    public void updateProjectilesAndExplosions(double deltaMs) {
        if (model != null) {
            model.updateProjectiles(deltaMs);
            model.updateExplosions(deltaMs);
        }
    }
    
    public void scaleSpeed(Ball ball, double factor) {
        if (ball != null) {
            ball.scaleSpeed(factor);
        }
    }
    
    public Ball findBallAt(java.awt.Point p) {
        List<Ball> balls = model.getAllBalls();
        for (Ball b : balls) {
            double bx = b.getX();
            double by = b.getY();
            int d = b.getDIAMETER();
            java.awt.Rectangle r = new java.awt.Rectangle((int)bx, (int)by, d, d);
            if (r.contains(p)) {
                return b;
            }
        }
        return null;
    }
    
    public Ball getNextBall(Ball current) {
        List<Ball> balls = model.getAllBalls();
        if (balls == null || balls.isEmpty()) return null;
        if (current == null) {
            return balls.get(0);
        }
        int idx = balls.indexOf(current);
        if (idx < 0) {
            return balls.get(0);
        }
        int next = (idx + 1) % balls.size();
        return balls.get(next);
    }
    
    public Ball getPreviousBall(Ball current) {
        List<Ball> balls = model.getAllBalls();
        if (balls == null || balls.isEmpty()) return null;
        if (current == null) {
            return balls.get(balls.size() - 1);
        }
        int idx = balls.indexOf(current);
        if (idx < 0) {
            return balls.get(balls.size() - 1);
        }
        int prev = (idx - 1 + balls.size()) % balls.size();
        return balls.get(prev);
    }
}
