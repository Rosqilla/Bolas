package model;

import java.awt.Color;
import java.awt.Rectangle;

public class Ball implements Runnable {
    private final Model model;
    private double posX, posY;
    private double velX, velY;
    // accelerations in px per millisecond squared
    private double accX = 0.0, accY = 0.0;
    // orientación de la bola en radianes (estilo Asteroids)
    private double angle = -Math.PI / 2; // apunta hacia arriba inicialmente
    // estela de partículas (trail)
    private final java.util.LinkedList<TrailPoint> trail = new java.util.LinkedList<>();
    private static final int MAX_TRAIL_LENGTH = 15;
    private long lastTrailUpdate = System.currentTimeMillis();
    private final int DIAMETER;
    private final Color COLOR;
    private volatile boolean running = true;
    private volatile boolean hasLock = false;
    private Thread myThread;
    private final Object posLock = new Object();

    public Ball(Model model) {
        this(model, 15);
    }

    public Ball(Model model, int diameter) {
        this.model = model;
        int vw = Math.max(1, model.getViewerWidth());
        int vh = Math.max(1, model.getViewerHeight());
        // colocar en posición aleatoria dentro del viewer
        posX = Math.random() * Math.max(1, vw - diameter);
        posY = Math.random() * Math.max(1, vh - diameter);
    // velocidades iniciales en px/ms (aleatorias). We pick speeds near previous px/s values but converted to px/ms
    // previous range was roughly [-150,150] px/s -> convert to px/ms: divide by 1000
    velX = (-150 + Math.random() * 300) / 1000.0; // [-0.15,0.15] px/ms
    velY = (-150 + Math.random() * 300) / 1000.0; // [-0.15,0.15] px/ms
        DIAMETER = Math.max(2, diameter);
        COLOR = new Color((float)Math.random(), (float)Math.random(), (float)Math.random());
    Thread thread = new Thread(this, "Ball-Thread");
    thread.setDaemon(true);
    this.myThread = thread;
    thread.start();
    }

    public Color getCOLOR() {
        return this.COLOR;
    }

    public int getX() {
        synchronized (posLock) { return (int) Math.round(posX); }
    }

    public int getY() {
        synchronized (posLock) { return (int) Math.round(posY); }
    }

    public int getDIAMETER() {
        return this.DIAMETER;
    }

    @Override
    public void run() {
        Habitacion hab = model.getHabitacion();
        boolean inside = false;
    // physics parameters
    // To preserve the classic bouncing trajectory, gravity is 0 and damping is 1.0 by default.
    // These can be tuned later to get more 'physical' behaviour.
    final double damping = 1.0; // no air damping so speed magnitude preserved
    final double tickMs = 10.0; // milliseconds per tick (10 ms)
        while (running) {
            // Respect pause flag from model: if paused, sleep in this thread until resumed
            if (model.isPaused()) {
                try {
                    Thread.sleep(50);
                    continue;
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            double nextX, nextY, nextVelX, nextVelY;
            // física básica: integrate acceleration -> velocity -> position
            double dt = tickMs; // milliseconds per tick (10 ms)
            // accX/accY are in px/ms^2, vel in px/ms, dt in ms
            nextVelX = velX + accX * dt;
            nextVelY = velY + accY * dt;
            // integrate velocity -> position (vel in px/ms, dt in ms => delta pos in px)
            nextX = posX + nextVelX * dt;
            nextY = posY + nextVelY * dt;

            Rectangle area = hab.getArea();
            // rectángulo de la bola (posible solapamiento)
            Rectangle bolaRect = new Rectangle((int)Math.round(nextX), (int)Math.round(nextY), DIAMETER, DIAMETER);

            if (!inside && area.intersects(bolaRect)) {
                // Nos colocamos justo fuera del área según la dirección y esperamos a entrar (bloqueante)
                // Ajustar posición fuera de la habitación con lock para evitar race con el viewer
                synchronized (posLock) {
                    if (nextX + DIAMETER > area.x && posX + DIAMETER <= area.x) {
                        posX = area.x - DIAMETER; // pared izquierda
                    } else if (nextX < area.x + area.width && posX >= area.x + area.width) {
                        posX = area.x + area.width; // pared derecha
                    }
                    if (nextY + DIAMETER > area.y && posY + DIAMETER <= area.y) {
                        posY = area.y - DIAMETER; // pared superior
                    } else if (nextY < area.y + area.height && posY >= area.y + area.height) {
                        posY = area.y + area.height; // pared inferior
                    }
                }
                // Intento no bloqueante de entrar; si la habitación está ocupada, rebotar contra la pared
            if (hab.tryGoIn(this)) {
                    hasLock = true;
                    inside = true;
                    // ya podemos avanzar dentro
                    synchronized (posLock) {
                        posX = nextX;
                        posY = nextY;
                        velX = nextVelX; velY = nextVelY;
                    }
                } else {
                    // habitación ocupada: simular rebote contra la pared más próxima
                        synchronized (posLock) {
                        boolean bounced = false;
                        // si venimos por la izquierda
                        if (nextX + DIAMETER > area.x && posX + DIAMETER <= area.x) {
                            posX = area.x - DIAMETER;
                            velX = -Math.abs(velX);
                            bounced = true;
                        }
                        // desde la derecha
                        if (nextX < area.x + area.width && posX >= area.x + area.width) {
                            posX = area.x + area.width;
                            velX = Math.abs(velX);
                            bounced = true;
                        }
                        // desde arriba
                        if (nextY + DIAMETER > area.y && posY + DIAMETER <= area.y) {
                            posY = area.y - DIAMETER;
                            velY = -Math.abs(velY);
                            bounced = true;
                        }
                        // desde abajo
                        if (nextY < area.y + area.height && posY >= area.y + area.height) {
                            posY = area.y + area.height;
                            velY = Math.abs(velY);
                            bounced = true;
                        }
                        // si por alguna razón no detectamos una cara (caso corner), invertir ambas componentes
                        if (!bounced) {
                            velX = -velX; velY = -velY;
                        }
                    }
                }
            } else if (inside) {
                // estamos dentro, avanzar
                synchronized (posLock) { posX = nextX; posY = nextY; velX = nextVelX; velY = nextVelY; }
                Rectangle current = new Rectangle((int)Math.round(posX), (int)Math.round(posY), DIAMETER, DIAMETER);
                // si ya hemos salido completamente, liberamos la habitación
                if (!area.intersects(current)) {
                    if (hasLock) {
                        hab.exit();
                        hasLock = false;
                    }
                    inside = false;
                }
            } else {
                // no interactúa con la habitación, moverse normalmente
                synchronized (posLock) { posX = nextX; posY = nextY; velX = nextVelX; velY = nextVelY; }
            }

            int w = model.getViewerWidth();
            int h = model.getViewerHeight();
            // fronteras: sincronizar para actualizar posiciones atómicas y reflejar velocidades
            synchronized (posLock) {
                // actualizar estela
                long now = System.currentTimeMillis();
                if (now - lastTrailUpdate > 30) { // actualizar cada 30ms
                    trail.addFirst(new TrailPoint(posX + DIAMETER/2.0, posY + DIAMETER/2.0));
                    if (trail.size() > MAX_TRAIL_LENGTH) {
                        trail.removeLast();
                    }
                    lastTrailUpdate = now;
                }
                
                if (w > 0) {
                    if (posX <= 0) {
                        posX = 0;
                        // reflect X keeping the magnitude (classic bounce)
                        velX = Math.abs(velX);
                    }
                    if (posX + DIAMETER >= w) {
                        posX = Math.max(0, w - DIAMETER);
                        velX = -Math.abs(velX);
                    }
                }
                if (h > 0) {
                    if (posY <= 0) {
                        posY = 0;
                        velY = Math.abs(velY);
                    }
                    if (posY + DIAMETER >= h) {
                        posY = Math.max(0, h - DIAMETER);
                        velY = -Math.abs(velY);
                    }
                }
                // apply damping (currently 1.0 so no change)
                velX *= damping;
                velY *= damping;
            }

            try {
                Thread.sleep((long)(tickMs));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void stop() {
        // if currently inside habitacion, release it
        try {
            Habitacion hab = model.getHabitacion();
            if (hasLock && hab != null) {
                hab.exit();
                hasLock = false;
            }
        } catch (Exception ignored) {}
        // interrupt if waiting on enter()
        if (myThread != null && myThread.isAlive()) myThread.interrupt();
        running = false;
    }

    /** Set acceleration (px per ms^2) for this ball. */
    public void setAcceleration(double ax, double ay) {
        synchronized (posLock) {
            this.accX = ax; this.accY = ay;
        }
    }

    /** Getters for acceleration (px/ms^2) */
    public double getAccX() { synchronized (posLock) { return accX; } }
    public double getAccY() { synchronized (posLock) { return accY; } }

    /**
     * Escala la componente vertical de la velocidad por un factor (ej: 0.9 reduce el 10%).
     */
    public void scaleVertical(double factor) {
        synchronized (posLock) {
            velY *= factor;
        }
    }

    /**
     * Aplicar un impulso a la velocidad (en px/s).
     */
    public void applyImpulse(double ix, double iy) {
        synchronized (posLock) {
            // ix,iy are interpreted as delta velocity in px/ms
            velX += ix; velY += iy;
        }
    }

    /** Getters for velocity (px/ms) */
    public double getVelX() {
        synchronized (posLock) { return velX; }
    }

    public double getVelY() {
        synchronized (posLock) { return velY; }
    }

    /** Set absolute velocity (px/ms) */
    public void setVelocity(double vx, double vy) {
        synchronized (posLock) {
            this.velX = vx;
            this.velY = vy;
        }
    }

    /** Rotate velocity vector by given radians (positive = counter-clockwise). */
    public void rotate(double radians) {
        synchronized (posLock) {
            double vx = this.velX;
            double vy = this.velY;
            double r = Math.hypot(vx, vy);
            if (r == 0.0) return; // nothing to rotate
            double theta = Math.atan2(vy, vx);
            theta += radians;
            this.velX = r * Math.cos(theta);
            this.velY = r * Math.sin(theta);
        }
    }
    
    /** Rotar la orientación de la bola (estilo Asteroids) */
    public void rotateOrientation(double radians) {
        synchronized (posLock) {
            this.angle += radians;
            // normalizar entre -PI y PI
            while (this.angle > Math.PI) this.angle -= 2 * Math.PI;
            while (this.angle < -Math.PI) this.angle += 2 * Math.PI;
        }
    }
    
    /** Aplicar empuje en la dirección de orientación (estilo Asteroids) */
    public void applyThrust(double thrustMagnitude) {
        synchronized (posLock) {
            // thrust en px/ms (magnitud del empuje)
            double thrustX = Math.cos(this.angle) * thrustMagnitude;
            double thrustY = Math.sin(this.angle) * thrustMagnitude;
            this.velX += thrustX;
            this.velY += thrustY;
        }
    }
    
    /** Obtener ángulo de orientación actual */
    public double getAngle() {
        synchronized (posLock) {
            return this.angle;
        }
    }

    /** Multiply the current speed by a factor (e.g., 1.1 to increase by 10%). */
    public void scaleSpeed(double factor) {
        synchronized (posLock) {
            this.velX *= factor;
            this.velY *= factor;
        }
    }

    /**
     * Revertir la componente vertical con un coeficiente de restitución.
     */
    public void bounceVertical(double restitution) {
        synchronized (posLock) {
            velY = -Math.abs(velY) * restitution;
        }
    }
    
    /**
     * Obtener la estela actual.
     */
    public java.util.List<TrailPoint> getTrail() {
        synchronized (posLock) {
            return new java.util.ArrayList<>(trail);
        }
    }
    
    /**
     * Punto en la estela.
     */
    public static class TrailPoint {
        public final double x, y;
        
        public TrailPoint(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}
