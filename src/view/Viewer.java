package view;

import model.Ball;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class Viewer extends JPanel implements Runnable {
    private Thread thread;
    private final View view;
    private Ball selectedBall;
    private volatile boolean running = false;
    private volatile double fps = 0.0;
    private volatile double lastPaintMs = 0.0;
    private volatile boolean rotatingLeft = false;
    private volatile boolean rotatingRight = false;
    private volatile boolean thrusting = false;
    private volatile boolean braking = false;
    // rotation speed in radians per second (adjustable)
    private static final double ROTATION_RAD_PER_SEC = Math.toRadians(180); // 180°/s
    // thrust magnitude in px/ms per frame (~60fps) - ajusta según necesites
    private static final double THRUST_MAGNITUDE = 0.008; // aceleración por frame
    private static final double BRAKE_FACTOR = 0.95; // factor de frenado

    public Viewer(View view) {
        this.view = view;
        setBackground(Color.WHITE);
        // preferred size can be managed by layout; leave as default
        thread = new Thread(this, "Viewer-Thread");
        selectedBall = null;

        // make sure we can receive key events in the window even if focus is elsewhere
        setFocusable(true);

        // Mouse listener to select a ball on click
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point p = e.getPoint();
                Ball found = view.getController().findBallAt(p);
                selectedBall = found;
                // repaint to show selection
                repaint();
                // request focus so key bindings work after selecting
                Viewer.this.requestFocusInWindow();
            }
        });

        // Key bindings for WASD to control the selected ball (Asteroids style)
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();
        // rotate left (A) - pressed / released for smooth continuous rotation
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, false), "rotateLeftPressed");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, true), "rotateLeftReleased");
        am.put("rotateLeftPressed", new AbstractAction() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { rotatingLeft = true; } });
        am.put("rotateLeftReleased", new AbstractAction() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { rotatingLeft = false; } });
        // rotate right (D)
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0, false), "rotateRightPressed");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0, true), "rotateRightReleased");
        am.put("rotateRightPressed", new AbstractAction() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { rotatingRight = true; } });
        am.put("rotateRightReleased", new AbstractAction() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { rotatingRight = false; } });
        // thrust (W) -> apply thrust in direction of orientation (Asteroids style)
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, false), "thrustPressed");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, true), "thrustReleased");
        am.put("thrustPressed", new AbstractAction() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { thrusting = true; } });
        am.put("thrustReleased", new AbstractAction() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { thrusting = false; } });
        // brake (S) -> reduce speed (friction)
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0, false), "brakePressed");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0, true), "brakeReleased");
        am.put("brakePressed", new AbstractAction() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { braking = true; } });
        am.put("brakeReleased", new AbstractAction() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { braking = false; } });
        // fire (SPACE) -> disparar proyectil
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false), "fire");
        am.put("fire", new AbstractAction() { 
            @Override 
            public void actionPerformed(java.awt.event.ActionEvent e) { 
                if (selectedBall != null && view.getController() != null) {
                    view.getController().fireBullet(selectedBall);
                }
            } 
        });
    // Deselect on Escape
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "deselect");
    am.put("deselect", new AbstractAction() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { selectedBall = null; Viewer.this.repaint(); } });
        // Navigate selection with arrow keys: right = next, left = previous
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false), "selectNext");
        am.put("selectNext", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                selectedBall = view.getController().getNextBall(selectedBall);
                Viewer.this.requestFocusInWindow();
                Viewer.this.repaint();
            }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false), "selectPrev");
        am.put("selectPrev", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                selectedBall = view.getController().getPreviousBall(selectedBall);
                Viewer.this.requestFocusInWindow();
                Viewer.this.repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        long paintStart = System.nanoTime();

        // Verificar que el controller esté disponible
        if (view.getController() == null) {
            return;
        }

        // Dibujar todas las bolas (snapshot)
        List<Ball> balls = view.getController().getAllBalls();
        if (balls != null) {
            for (Ball ball : balls) {
                if (ball == null) continue; // Saltar bolas nulas
                // dibujar estela primero (debajo de la bola)
                if (ball == selectedBall) {
                    paintTrail(ball, g2);
                }
                paintBall(ball, g2);
            }
        }
        
        // Dibujar proyectiles
        List<model.Projectile> projectiles = view.getController().getAllProjectiles();
        if (projectiles != null) {
            for (model.Projectile proj : projectiles) {
                if (proj != null && proj.isActive()) {
                    paintProjectile(proj, g2);
                }
            }
        }
        
        // Dibujar explosiones
        List<model.Explosion> explosions = view.getController().getAllExplosions();
        if (explosions != null) {
            for (model.Explosion exp : explosions) {
                if (exp != null && !exp.isFinished()) {
                    paintExplosion(exp, g2);
                }
            }
        }
        
        // Dibujar la habitación (si existe)
        model.Habitacion hab = view.getController().getHabitacion();
        if (hab != null) {
            java.awt.Rectangle area = hab.getArea();
            // relleno semitransparente
            Color fill = new Color(200, 200, 200, 80);
            g2.setColor(fill);
            g2.fillRect(area.x, area.y, area.width, area.height);
            // borde
            g2.setColor(Color.DARK_GRAY);
            g2.setStroke(new BasicStroke(2));
            g2.drawRect(area.x, area.y, area.width, area.height);
        }
        long paintEnd = System.nanoTime();
        lastPaintMs = (paintEnd - paintStart) / 1_000_000.0;
    }

    @Override
    public void run() {
        running = true;
        long last = System.nanoTime();
        final double alpha = 0.1; // smoothing
        while (running) {
            long now = System.nanoTime();
            double dtSeconds = (now - last) / 1_000_000_000.0; // seconds since last loop
            double instantaneousFps = 1_000_000_000.0 / Math.max(1, (now - last));
            fps = (1 - alpha) * fps + alpha * instantaneousFps;
            last = now;
            
            // actualizar proyectiles y explosiones
            double dtMs = dtSeconds * 1000.0;
            if (view.getController() != null) {
                view.getController().updateProjectilesAndExplosions(dtMs);
            }
            
            // apply continuous rotation of orientation and thrust (Asteroids style)
            Ball sel = selectedBall;
            if (sel != null && view.getController() != null) {
                // rotar orientación (no velocidad)
                if (rotatingLeft) view.getController().rotateBallOrientation(sel, -ROTATION_RAD_PER_SEC * dtSeconds);
                if (rotatingRight) view.getController().rotateBallOrientation(sel, ROTATION_RAD_PER_SEC * dtSeconds);
                // aplicar empuje en dirección de orientación
                if (thrusting) view.getController().applyThrustToBall(sel, THRUST_MAGNITUDE);
                // frenar (reducir velocidad gradualmente)
                if (braking) view.getController().scaleSpeed(sel, BRAKE_FACTOR);
            }
            // request a repaint on EDT
            SwingUtilities.invokeLater(this::repaint);
            try {
                Thread.sleep(16); // ~60 FPS
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void startViewer() {
        if (thread == null || !thread.isAlive()) {
            thread = new Thread(this, "Viewer-Thread");
            thread.setDaemon(true);
            thread.start();
        }
    }

    public double getFps() {
        return fps;
    }

    public double getLastPaintMs() { return lastPaintMs; }

    /** Return currently selected ball or null. */
    public Ball getSelectedBall() { return selectedBall; }

    public void stopViewer() {
        running = false;
        if (thread != null) thread.interrupt();
    }

    public Thread getThread() {
        return this.thread;
    }

    public void paintBall(Ball ball, Graphics2D g) {
        int diameter = ball.getDIAMETER();
        int x = ball.getX();
        int y = ball.getY();
        Color color = ball.getCOLOR();
        // if this ball is currently selected, draw a triangle pointing in orientation direction (Asteroids)
        if (ball == selectedBall) {
            // compute center
            double cx = x + diameter / 2.0;
            double cy = y + diameter / 2.0;
            // usar ángulo de orientación en vez de dirección de velocidad
            double angle = ball.getAngle();
            // tip length and base distance (make tip longer than radius)
            double radius = diameter / 2.0;
            // make the triangle stubbier/wider so it's not too thin
            double tipLen = Math.max(radius * 1.2, radius * 1.4);
            double baseDist = radius * 0.6; // distance from center to base midpoint
            double baseHalf = radius * 1.1; // half-width of base (wider)

            // direction unit vector
            double ux = Math.cos(angle);
            double uy = Math.sin(angle);
            // perpendicular unit
            double px = -uy;
            double py = ux;

            // compute triangle points
            int tx = (int) Math.round(cx + ux * tipLen);
            int ty = (int) Math.round(cy + uy * tipLen);
            int bx1 = (int) Math.round(cx - ux * baseDist + px * baseHalf);
            int by1 = (int) Math.round(cy - uy * baseDist + py * baseHalf);
            int bx2 = (int) Math.round(cx - ux * baseDist - px * baseHalf);
            int by2 = (int) Math.round(cy - uy * baseDist - py * baseHalf);

            Polygon poly = new Polygon(new int[]{tx, bx1, bx2}, new int[]{ty, by1, by2}, 3);
            
            // Dibujar triángulo con borde grueso
            g.setColor(color);
            g.fill(poly);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(3f));
            g.draw(poly);
            
            // Dibujar círculo indicador en la punta para mostrar claramente hacia dónde apunta
            int tipSize = 6;
            g.setColor(new Color(255, 100, 0)); // Naranja brillante
            g.fillOval(tx - tipSize/2, ty - tipSize/2, tipSize, tipSize);
            // Borde del círculo de la punta
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2f));
            g.drawOval(tx - tipSize/2, ty - tipSize/2, tipSize, tipSize);
        } else {
            g.setColor(color);
            g.fillOval(x, y, diameter, diameter);
        }
    }
    
    private void paintTrail(Ball ball, Graphics2D g) {
        if (ball == null) return;
        
        List<Ball.TrailPoint> trail = ball.getTrail();
        if (trail == null || trail.isEmpty()) return;
        
        Color baseColor = ball.getCOLOR();
        if (baseColor == null) return;
        
        int size = trail.size();
        
        for (int i = 0; i < size; i++) {
            Ball.TrailPoint tp = trail.get(i);
            if (tp == null) continue; // Saltar puntos nulos
            
            // alfa decrece hacia atrás
            float alpha = (1.0f - (i / (float)size)) * 0.6f;
            int sizePoint = Math.max(2, (int)((1.0f - (i / (float)size)) * 5));
            
            Color trailColor = new Color(
                baseColor.getRed() / 255f,
                baseColor.getGreen() / 255f,
                baseColor.getBlue() / 255f,
                alpha
            );
            
            g.setColor(trailColor);
            g.fillOval((int)(tp.x - sizePoint/2), (int)(tp.y - sizePoint/2), sizePoint, sizePoint);
        }
    }
    
    private void paintProjectile(model.Projectile proj, Graphics2D g) {
        if (!proj.isActive()) return;
        
        int size = proj.getSize();
        int x = (int)(proj.getX() - size/2);
        int y = (int)(proj.getY() - size/2);
        
        // Borde oscuro para contraste
        g.setColor(new Color(100, 0, 0));
        g.fillOval(x - 1, y - 1, size + 2, size + 2);
        
        // Color principal del proyectil
        g.setColor(proj.getColor());
        g.fillOval(x, y, size, size);
        
        // Brillo naranja/amarillo en el centro
        g.setColor(new Color(255, 200, 50));
        g.fillOval((int)(proj.getX() - size/4), (int)(proj.getY() - size/4), size/2, size/2);
    }
    
    private void paintExplosion(model.Explosion exp, Graphics2D g) {
        if (exp == null) return;
        
        List<model.Explosion.Particle> particles = exp.getParticles();
        if (particles == null) return;
        
        double progress = exp.getProgress();
        
        for (model.Explosion.Particle p : particles) {
            if (p == null) continue; // Saltar partículas nulas
            
            // alfa decrece con el tiempo
            float alpha = (float)(1.0 - progress) * 0.8f;
            Color c = p.getColor();
            Color particleColor = new Color(
                c.getRed() / 255f,
                c.getGreen() / 255f,
                c.getBlue() / 255f,
                alpha
            );
            
            g.setColor(particleColor);
            int size = (int)(4 * (1.0 - progress * 0.5)); // se encogen
            g.fillOval((int)(p.getX() - size/2), (int)(p.getY() - size/2), size, size);
        }
    }
}
