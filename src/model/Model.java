package model;

import java.util.ArrayList;
import java.util.List;

public class Model {
    private final List<Ball> ballList;
    private final List<Projectile> projectileList;
    private final List<Explosion> explosionList;
    private final Habitacion habitacion;
    private volatile boolean paused = false;
    private int viewerWidth;
    private int viewerHeight;

    public Model(int viewerWidth, int viewerHeight) {
        this.viewerWidth = Math.max(200, viewerWidth);
        this.viewerHeight = Math.max(200, viewerHeight);
        this.ballList = new ArrayList<>();
        this.projectileList = new ArrayList<>();
        this.explosionList = new ArrayList<>();
        // create a central room
        int rw = Math.max(100, this.viewerWidth / 4);
        int rh = Math.max(100, this.viewerHeight / 4);
        int rx = (this.viewerWidth - rw) / 2;
        int ry = (this.viewerHeight - rh) / 2;
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
        return viewerWidth;
    }
    
    public int getViewerHeight() {
        return viewerHeight;
    }
    
    public void updateViewerDimensions(int width, int height) {
        this.viewerWidth = Math.max(1, width);
        this.viewerHeight = Math.max(1, height);
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
    
    public synchronized void fireBullet(Ball shooter) {
        if (shooter == null) return;
        double cx = shooter.getX() + shooter.getDIAMETER() / 2.0;
        double cy = shooter.getY() + shooter.getDIAMETER() / 2.0;
        double angle = shooter.getAngle();
        // disparar desde la punta de la nave
        double tipDistance = shooter.getDIAMETER() * 0.7;
        double startX = cx + Math.cos(angle) * tipDistance;
        double startY = cy + Math.sin(angle) * tipDistance;
        projectileList.add(new Projectile(startX, startY, angle, shooter.getCOLOR()));
    }
    
    public synchronized List<Projectile> getAllProjectiles() {
        return new ArrayList<>(projectileList);
    }
    
    public synchronized List<Explosion> getAllExplosions() {
        return new ArrayList<>(explosionList);
    }
    
    public synchronized void updateProjectiles(double deltaMs) {
        // Lista de proyectiles a eliminar
        List<Projectile> projectilesToRemove = new ArrayList<>();
        List<Ball> ballsToRemove = new ArrayList<>();
        List<Explosion> explosionsToAdd = new ArrayList<>();
        
        // Actualizar posición de proyectiles
        for (Projectile p : projectileList) {
            if (!p.isActive()) continue;
            p.update(deltaMs);
            p.checkBounds(viewerWidth, viewerHeight);
            if (!p.isActive()) {
                projectilesToRemove.add(p);
            }
        }
        
        // Detectar colisiones (solo proyectiles activos)
        for (Projectile p : projectileList) {
            if (!p.isActive() || projectilesToRemove.contains(p)) continue;
            
            for (Ball b : ballList) {
                if (ballsToRemove.contains(b)) continue;
                
                if (p.collidesWith(b)) {
                    // Crear explosión
                    explosionsToAdd.add(new Explosion(
                        b.getX() + b.getDIAMETER() / 2.0,
                        b.getY() + b.getDIAMETER() / 2.0,
                        b.getCOLOR(),
                        b.getDIAMETER()
                    ));
                    
                    // Marcar para eliminar
                    ballsToRemove.add(b);
                    projectilesToRemove.add(p);
                    p.deactivate();
                    break; // Un proyectil solo puede golpear una bola
                }
            }
        }
        
        // Eliminar bolas golpeadas
        for (Ball b : ballsToRemove) {
            b.stop();
            ballList.remove(b);
        }
        
        // Eliminar proyectiles
        projectileList.removeAll(projectilesToRemove);
        
        // Añadir explosiones
        explosionList.addAll(explosionsToAdd);
    }
    
    public synchronized void updateExplosions(double deltaMs) {
        List<Explosion> explosionsToRemove = new ArrayList<>();
        
        for (Explosion e : explosionList) {
            e.update(deltaMs);
            if (e.isFinished()) {
                explosionsToRemove.add(e);
            }
        }
        
        explosionList.removeAll(explosionsToRemove);
    }
}
