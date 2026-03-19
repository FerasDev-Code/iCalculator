package com.calchunter;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class CalcHunterApp {
    private CalcHunterApp() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            applySystemLookAndFeel();
            CalculatorFrame frame = new CalculatorFrame();
            frame.setVisible(true);
        });
    }

    private static void applySystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Use default Swing look and feel when the system style is unavailable.
        }
    }
}
