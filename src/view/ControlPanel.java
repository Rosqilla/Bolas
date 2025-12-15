package view;

import javax.swing.*;
import java.awt.*;

public class ControlPanel extends JPanel {
    private final JButton FIRE_BUTTON;
    private final JButton AUTO_TOGGLE_BUTTON;
    private final JSpinner INTERVAL_SPINNER;
    private final JRadioButton SIZE_RANDOM;
    private final JRadioButton SIZE_FIXED;
    private final JSpinner SIZE_MIN;
    private final JSpinner SIZE_MAX;
    private final JLabel FPS_LABEL;
    private final JButton PAUSE_BUTTON;
    private final JButton CLEAR_BUTTON;

    public ControlPanel() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        this.FIRE_BUTTON = new JButton("Añadir Bola");
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        add(FIRE_BUTTON, gbc);

        this.AUTO_TOGGLE_BUTTON = new JButton("Auto: OFF");
        gbc.gridy = 1; gbc.gridwidth = 2;
        add(AUTO_TOGGLE_BUTTON, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 2;
        add(new JLabel("Intervalo ms:"), gbc);
        this.INTERVAL_SPINNER = new JSpinner(new SpinnerNumberModel(500, 10, 10000, 10));
        gbc.gridx = 1; add(INTERVAL_SPINNER, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        this.SIZE_RANDOM = new JRadioButton("Tamaño aleatorio", true);
        add(SIZE_RANDOM, gbc);
        this.SIZE_FIXED = new JRadioButton("Tamaño intervalo");
        gbc.gridx = 1; add(SIZE_FIXED, gbc);
        ButtonGroup grp = new ButtonGroup(); grp.add(SIZE_RANDOM); grp.add(SIZE_FIXED);

        gbc.gridx = 0; gbc.gridy = 4; add(new JLabel("Min:"), gbc);
        this.SIZE_MIN = new JSpinner(new SpinnerNumberModel(8, 2, 200, 1));
        gbc.gridx = 1; add(SIZE_MIN, gbc);
        gbc.gridx = 0; gbc.gridy = 5; add(new JLabel("Max:"), gbc);
        this.SIZE_MAX = new JSpinner(new SpinnerNumberModel(24, 2, 400, 1));
        gbc.gridx = 1; add(SIZE_MAX, gbc);

        gbc.gridx = 0; gbc.gridy = 11; gbc.gridwidth = 2;
    FPS_LABEL = new JLabel("FPS: -- | Paint: -- ms");
    add(FPS_LABEL, gbc);
        gbc.gridy = 12; gbc.gridwidth = 1; gbc.gridx = 0;
        PAUSE_BUTTON = new JButton("Pausa: OFF");
        add(PAUSE_BUTTON, gbc);

    // place 'Borrar bolas' next to pause (same row)
    gbc.gridx = 1; gbc.gridy = 12;
        CLEAR_BUTTON = new JButton("Borrar bolas");
        add(CLEAR_BUTTON, gbc);
    }

    // getters for controller wiring
    public JButton getFireButton() { return FIRE_BUTTON; }
    public JButton getAutoToggleButton() { return AUTO_TOGGLE_BUTTON; }
    public JSpinner getIntervalSpinner() { return INTERVAL_SPINNER; }
    public boolean isSizeRandom() { return SIZE_RANDOM.isSelected(); }
    public int getSizeMin() { return (int) SIZE_MIN.getValue(); }
    public int getSizeMax() { return (int) SIZE_MAX.getValue(); }
    public void setFpsLabel(String s) { FPS_LABEL.setText(s); }
    public JButton getPauseButton() { return PAUSE_BUTTON; }
    public JButton getClearButton() { return CLEAR_BUTTON; }
    // acceleration getters removed

    

    // selection info removed from panel
}
