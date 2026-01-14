package model;

import java.awt.Rectangle;

/**
 * Habitacion representa un área central (rectángulo) por donde sólo puede
 * cruzar una bola a la vez. Las otras bolas que lleguen esperarán en la pared
 * hasta que la habitación quede libre.
 */
public class Habitacion {
    private final Rectangle area; // coordenadas relativas al viewer
    // true cuando la habitación está ocupada
    private boolean occupied = false;
    // referencia opcional a la bola que actualmente ocupa la habitación
    private Ball occupant = null;

    public Habitacion(int x, int y, int width, int height) {
        this.area = new Rectangle(x, y, width, height);
    }

    public Rectangle getArea() {
        // devolver una copia para evitar exposición del objeto mutable
        synchronized (this) {
            return new Rectangle(area);
        }
    }

    /**
     * Intento no bloqueante de entrar registrando la bola que solicita acceso.
     * @param b la bola que intenta entrar
     * @return true si obtuvo permiso
     */
    public synchronized boolean tryGoIn(Ball b) {
        if (!occupied) {
            occupied = true;
            occupant = b;
            return true;
        }
        return false;
    }

    /**
     * Entrar bloqueando hasta que haya permiso.
     * @throws InterruptedException si el hilo es interrumpido
     */
    public synchronized void enter() throws InterruptedException {
        while (occupied) {
            wait();
        }
        occupied = true;
    }

    /**
     * Entrar bloqueando hasta que haya permiso, y registrar la bola que entra.
     * @param b la bola que entra
     * @throws InterruptedException si el hilo es interrumpido
     */
    public synchronized void goIn(Ball b) throws InterruptedException {
        while (occupied) {
            wait();
        }
        occupied = true;
        occupant = b;
    }

    /**
     * Salir y liberar la habitación.
     */
    public synchronized void exit() {
        if (occupied) {
            occupied = false;
            occupant = null;
            notifyAll();
        }
    }

    /**
     * Obtener la bola que actualmente ocupa la habitación, o null si está libre o no registrada.
     */
    public synchronized Ball getOccupant() {
        return occupant;
    }
}
