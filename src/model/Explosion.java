package model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una explosión animada (partículas que se expanden).
 */
public class Explosion {
    private final List<Particle> particles;
    private long startTime;
    private static final long DURATION_MS = 800; // duración de la explosión
    
    public Explosion(double x, double y, Color color, int ballDiameter) {
        this.startTime = System.currentTimeMillis();
        this.particles = new ArrayList<>();
        
        // crear partículas en todas direcciones
        int numParticles = Math.min(20, ballDiameter / 2);
        for (int i = 0; i < numParticles; i++) {
            double angle = (2 * Math.PI * i) / numParticles;
            double speed = 0.1 + Math.random() * 0.2; // px/ms
            particles.add(new Particle(x, y, angle, speed, color));
        }
    }
    
    public void update(double deltaMs) {
        for (Particle p : particles) {
            p.update(deltaMs);
        }
    }
    
    public boolean isFinished() {
        return System.currentTimeMillis() - startTime > DURATION_MS;
    }
    
    public List<Particle> getParticles() {
        return particles;
    }
    
    public double getProgress() {
        long elapsed = System.currentTimeMillis() - startTime;
        return Math.min(1.0, elapsed / (double) DURATION_MS);
    }
    
    /**
     * Partícula individual de la explosión.
     */
    public static class Particle {
        private double x, y;
        private final double velX, velY;
        private final Color color;
        
        public Particle(double startX, double startY, double angle, double speed, Color color) {
            this.x = startX;
            this.y = startY;
            this.velX = Math.cos(angle) * speed;
            this.velY = Math.sin(angle) * speed;
            this.color = color;
        }
        
        public void update(double deltaMs) {
            x += velX * deltaMs;
            y += velY * deltaMs;
        }
        
        public double getX() { return x; }
        public double getY() { return y; }
        public Color getColor() { return color; }
    }
}
