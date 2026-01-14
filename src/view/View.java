package view;

import javax.swing.*;
import controller.Controller;

import java.awt.*;

/**
 * This class represents the window that the final user will see and interact with
 */
public class View extends JFrame {
    private final Controller controller;
    private final ControlPanel controlPanel;
    private final Viewer viewer;

    public View(Controller controller) {
        this.controller = controller;
        this.controlPanel = new ControlPanel();
        this.viewer = new Viewer(this);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800,600);

        JPanel content = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

    // don't call getGraphics() or start the viewer thread before the UI is visible

        // --- Panel de control (left) ---
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.VERTICAL;
    gbc.weightx = 0;
    gbc.weighty = 1.0;
        content.add(controlPanel, gbc);

        // --- Viewer (right) ---
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        content.add(viewer, gbc);

        add(content);
        setLocationRelativeTo(null);
        setVisible(true);
        // start the viewer's thread after the frame is visible
        viewer.startViewer();
    }
    
    public int getViewerWidth() {
        return viewer.getWidth();
    }
    
    public int getViewerHeight() {
        return viewer.getHeight();
    }
    
    public Controller getController() {
        return controller;
    }
    public ControlPanel getControlPanel() { return controlPanel; }
    public Viewer getViewer() { return viewer; }
}
