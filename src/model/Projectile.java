package model;

import java.awt.Color;

/**
 * Representa un proyectil disparado por una bola.
 */
public class Projectile {
    private double x, y;
    private double velX, velY;
    private final Color color;
    private volatile boolean active = true;
    private static final int SIZE = 5;
    private static final double SPEED = 0.5; // px/ms
    // Color fijo rojo brillante para buena visibilidad contra fondo blanco
    private static final Color PROJECTILE_COLOR = new Color(220, 20, 20); // Rojo brillante
    
    public Projectile(double startX, double startY, double angle, Color shipColor) {
        this.x = startX;
        this.y = startY;
        // velocidad del proyectil en la dirección del ángulo
        this.velX = Math.cos(angle) * SPEED;
        this.velY = Math.sin(angle) * SPEED;
        this.color = PROJECTILE_COLOR; // Color fijo visible
    }
    
    public void update(double deltaMs) {
        if (!active) return;
        x += velX * deltaMs;
        y += velY * deltaMs;
    }
    
    public void checkBounds(int width, int height) {
        if (x < 0 || x > width || y < 0 || y > height) {
            active = false;
        }
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void deactivate() {
        active = false;
    }
    
    public double getX() { return x; }
    public double getY() { return y; }
    public int getSize() { return SIZE; }
    public Color getColor() { return color; }
    
    /**
     * Verifica si este proyectil colisiona con una bola.
     * Usa detección de colisión círculo-círculo.
     */
    public boolean collidesWith(Ball ball) {
        if (!active || ball == null) return false;
        
        // Centro de la bola
        double bx = ball.getX() + ball.getDIAMETER() / 2.0;
        double by = ball.getY() + ball.getDIAMETER() / 2.0;
        double ballRadius = ball.getDIAMETER() / 2.0;
        
        // Radio del proyectil
        double projRadius = SIZE / 2.0;
        
        // Distancia entre centros
        double dist = Math.hypot(x - bx, y - by);
        
        // Colisión si la distancia es menor que la suma de radios
        return dist < (ballRadius + projRadius);
    }
}
