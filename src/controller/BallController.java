package controller;

import model.Ball;
import model.Model;

/**
 * Controller for ball physical parameters.
 * Velocities are expected in px/ms and accelerations in px/ms^2.
 */
public class BallController {
    // default acceleration in internal units (px/ms^2)
    private volatile double defaultAccX = 0.0;
    private volatile double defaultAccY = 0.0;

    public BallController() {}

    /** Set default acceleration (internal units: px/ms^2) */
    public void setDefaultAcceleration(double axMs2, double ayMs2) {
        this.defaultAccX = axMs2;
        this.defaultAccY = ayMs2;
    }

    public double getDefaultAccX() { return defaultAccX; }
    public double getDefaultAccY() { return defaultAccY; }

    /** Apply defaults to a newly created ball (acceleration only). */
    public void applyDefaultsTo(Ball b) {
        if (b == null) return;
        b.setAcceleration(defaultAccX, defaultAccY);
    }

    /**
     * Set acceleration on a specific Ball (units: px/ms^2).
     */
    public void setAccelerationOnBall(Ball b, double axMs2, double ayMs2) {
        if (b == null) return;
        b.setAcceleration(axMs2, ayMs2);
    }

    /**
     * Apply given acceleration to all balls in the provided model (units: px/ms^2).
     */
    public void applyAccelerationToAll(Model model, double axMs2, double ayMs2) {
        if (model == null) return;
        for (Ball bb : model.getAllBalls()) {
            setAccelerationOnBall(bb, axMs2, ayMs2);
        }
    }

    /**
     * Convenience: set the default acceleration using UI units px/s^2. Converts to px/ms^2.
     */
    public void setDefaultAccelerationFromUi(double ax_px_s2, double ay_px_s2) {
        // convert px/s^2 -> px/ms^2: divide by 1_000_000
        setDefaultAcceleration(ax_px_s2 / 1_000_000.0, ay_px_s2 / 1_000_000.0);
    }

    /** Apply defaults to all balls in a model. */
    public void applyDefaultsToAll(Model model) {
        for (Ball b : model.getAllBalls()) {
            applyDefaultsTo(b);
        }
    }
}
