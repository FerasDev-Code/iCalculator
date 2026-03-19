package com.calchunter;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.WindowConstants;

public final class CalculatorFrame extends JFrame {
    private static final Font HEADER_FONT = new Font("SF Pro Text", Font.PLAIN, 15);
    private static final Font TITLE_FONT = new Font("SF Pro Display", Font.BOLD, 16);
    private static final Font DISPLAY_FONT = new Font("SF Pro Display", Font.PLAIN, 58);
    private static final Font EXPRESSION_FONT = new Font("SF Pro Text", Font.PLAIN, 16);
    private static final Font BUTTON_FONT = new Font("SF Pro Display", Font.PLAIN, 28);
    private static final Font SMALL_BUTTON_FONT = new Font("SF Pro Text", Font.PLAIN, 20);
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");

    private final CalculatorEngine engine = new CalculatorEngine();
    private final Map<String, BigDecimal> currencyRates = createCurrencyRates();

    private final JLabel titleLabel = new JLabel("Calculator");
    private final JComboBox<String> modeSelector = new JComboBox<>(new String[]{"Basic", "Scientific", "Programmer", "Currency"});
    private final JToggleButton themeToggle = new JToggleButton("Dark");
    private final JLabel expressionLabel = new JLabel(" ");
    private final JLabel displayLabel = new JLabel("0");
    private final JLabel programmerHexValue = new JLabel("HEX 0");
    private final JLabel programmerDecValue = new JLabel("DEC 0");
    private final JLabel programmerOctValue = new JLabel("OCT 0");
    private final JLabel programmerBinValue = new JLabel("BIN 0");
    private final JComboBox<String> fromCurrency = new JComboBox<>(currencyRates.keySet().toArray(String[]::new));
    private final JComboBox<String> toCurrency = new JComboBox<>(currencyRates.keySet().toArray(String[]::new));
    private final JLabel currencyResultLabel = new JLabel("0.00");
    private final JPanel shell = new RoundedPanel();
    private final JPanel modePanel = new JPanel(new CardLayout());

    private Theme theme = Theme.createDark();
    public CalculatorFrame() {
        super("Calculator");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(470, 860));
        setSize(470, 860);
        setLocationRelativeTo(null);
        setContentPane(buildContent());
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                shell.repaint();
            }
        });
        applyTheme();
        refreshDerivedViews();
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout());
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        shell.setLayout(new BorderLayout(0, 18));
        shell.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        shell.add(buildHeader(), BorderLayout.NORTH);
        shell.add(buildCenter(), BorderLayout.CENTER);
        root.add(shell, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);

        titleLabel.setFont(TITLE_FONT);

        modeSelector.setFont(HEADER_FONT);
        modeSelector.addActionListener(event -> {
            CardLayout layout = (CardLayout) modePanel.getLayout();
            layout.show(modePanel, currentMode());
        });

        themeToggle.setFont(HEADER_FONT);
        themeToggle.setFocusPainted(false);
        themeToggle.addActionListener(event -> {
            theme = themeToggle.isSelected() ? Theme.createDark() : Theme.createLight();
            themeToggle.setText(themeToggle.isSelected() ? "Dark" : "Light");
            applyTheme();
            refreshDerivedViews();
        });
                                                                                                                                                                                                                                                                                                    
        JPanel leftControls = new JPanel();
        leftControls.setOpaque(false);
        leftControls.setLayout(new BoxLayout(leftControls, BoxLayout.X_AXIS));
        leftControls.add(titleLabel);

        JPanel rightControls = new JPanel();
        rightControls.setOpaque(false);
        rightControls.setLayout(new BoxLayout(rightControls, BoxLayout.X_AXIS));
        rightControls.add(modeSelector);
        rightControls.add(Box.createRigidArea(new Dimension(10, 0)));
        rightControls.add(themeToggle);

        header.add(leftControls, BorderLayout.WEST);
        header.add(rightControls, BorderLayout.EAST);
        return header;
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(0, 18));
        center.setOpaque(false);
        center.add(buildDisplay(), BorderLayout.NORTH);
        center.add(buildBody(), BorderLayout.CENTER);
        return center;
    }

    private JPanel buildDisplay() {
        JPanel displayPanel = new JPanel(new BorderLayout(0, 6));
        displayPanel.setOpaque(true);
        displayPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.cardBorder, 1),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));

        expressionLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        expressionLabel.setFont(EXPRESSION_FONT);

        displayLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        displayLabel.setFont(DISPLAY_FONT);

        displayPanel.add(expressionLabel, BorderLayout.NORTH);
        displayPanel.add(displayLabel, BorderLayout.CENTER);
        return displayPanel;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        body.add(buildModePanel(), BorderLayout.NORTH);
        body.add(buildButtonGrid(), BorderLayout.CENTER);
        return body;
    }

    private JPanel buildModePanel() {
        modePanel.setOpaque(false);
        modePanel.add(createEmptyModePanel(), "Basic");
        modePanel.add(createScientificPanel(), "Scientific");
        modePanel.add(createProgrammerPanel(), "Programmer");
        modePanel.add(createCurrencyPanel(), "Currency");
        return modePanel;
    }

    private JPanel createEmptyModePanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(0, 8));
        return panel;
    }

    private JPanel createScientificPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 5, 10, 10));
        panel.setOpaque(false);
        addActionButton(panel, "pi");
        addActionButton(panel, "x^2");
        addActionButton(panel, "x^y");
        addActionButton(panel, "sqrt");
        addActionButton(panel, "1/x");
        addActionButton(panel, "sin");
        addActionButton(panel, "cos");
        addActionButton(panel, "tan");
        addActionButton(panel, "ln");
        addActionButton(panel, "log");
        return panel;
    }

    private JPanel createProgrammerPanel() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 10));
        wrapper.setOpaque(false);

        JPanel values = new JPanel(new GridLayout(2, 2, 10, 10));
        values.setOpaque(false);
        values.add(createInfoCard(programmerHexValue));
        values.add(createInfoCard(programmerDecValue));
        values.add(createInfoCard(programmerOctValue));
        values.add(createInfoCard(programmerBinValue));

        JPanel actions = new JPanel(new GridLayout(1, 4, 10, 10));
        actions.setOpaque(false);
        addActionButton(actions, "NOT");
        addActionButton(actions, "<<");
        addActionButton(actions, ">>");
        addActionButton(actions, "DEL");

        wrapper.add(values, BorderLayout.CENTER);
        wrapper.add(actions, BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel createCurrencyPanel() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 10));
        wrapper.setOpaque(false);

        JPanel selectors = new JPanel(new GridLayout(1, 2, 10, 0));
        selectors.setOpaque(false);
        fromCurrency.setFont(HEADER_FONT);
        toCurrency.setFont(HEADER_FONT);
        fromCurrency.setSelectedItem("USD");
        toCurrency.setSelectedItem("EUR");
        fromCurrency.addActionListener(event -> updateCurrencyResult());
        toCurrency.addActionListener(event -> updateCurrencyResult());
        selectors.add(fromCurrency);
        selectors.add(toCurrency);

        JLabel rateNote = new JLabel("Reference rates included for demo conversion");
        rateNote.setFont(new Font("SF Pro Text", Font.PLAIN, 12));

        currencyResultLabel.setFont(new Font("SF Pro Display", Font.PLAIN, 24));
        currencyResultLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel resultCard = createInfoCard(currencyResultLabel);
        resultCard.setLayout(new BorderLayout());
        resultCard.add(rateNote, BorderLayout.NORTH);
        resultCard.add(currencyResultLabel, BorderLayout.SOUTH);

        wrapper.add(selectors, BorderLayout.NORTH);
        wrapper.add(resultCard, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createInfoCard(JLabel label) {
        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(true);
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        label.setFont(new Font("SF Pro Text", Font.PLAIN, 14));
        card.add(label, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildButtonGrid() {
        JPanel grid = new JPanel(new GridLayout(5, 4, 12, 12));
        grid.setOpaque(false);

        addButton(grid, "AC", ButtonRole.FUNCTION);
        addButton(grid, "+/-", ButtonRole.FUNCTION);
        addButton(grid, "%", ButtonRole.FUNCTION);
        addButton(grid, "÷", ButtonRole.OPERATOR);

        addButton(grid, "7", ButtonRole.NUMBER);
        addButton(grid, "8", ButtonRole.NUMBER);
        addButton(grid, "9", ButtonRole.NUMBER);
        addButton(grid, "×", ButtonRole.OPERATOR);

        addButton(grid, "4", ButtonRole.NUMBER);
        addButton(grid, "5", ButtonRole.NUMBER);
        addButton(grid, "6", ButtonRole.NUMBER);
        addButton(grid, "-", ButtonRole.OPERATOR);

        addButton(grid, "1", ButtonRole.NUMBER);
        addButton(grid, "2", ButtonRole.NUMBER);
        addButton(grid, "3", ButtonRole.NUMBER);
        addButton(grid, "+", ButtonRole.OPERATOR);

        addButton(grid, "ICON", ButtonRole.NUMBER);
        addButton(grid, "0", ButtonRole.NUMBER);
        addButton(grid, ".", ButtonRole.NUMBER);
        addButton(grid, "=", ButtonRole.OPERATOR);

        return grid;
    }

    private void addButton(JPanel panel, String text, ButtonRole role) {
        CircleButton button = new CircleButton(text, role);
        button.addActionListener(this::handleButtonPress);
        panel.add(button);
    }

    private void addActionButton(JPanel panel, String text) {
        ActionButton button = new ActionButton(text);
        button.addActionListener(this::handleButtonPress);
        panel.add(button);
    }

    private void handleButtonPress(ActionEvent event) {
        String action = ((JButton) event.getSource()).getText();
        if ("ICON".equals(action)) {
            return;
        }

        String result = switch (action) {
            case "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" -> engine.inputDigit(action);
            case "." -> engine.inputDecimal();
            case "AC" -> engine.clear();
            case "DEL" -> engine.backspace();
            case "+/-" -> engine.toggleSign();
            case "%" -> engine.percent();
            case "+", "-", "×", "÷", "x^y" -> engine.applyOperator(action);
            case "=" -> engine.evaluate();
            case "pi" -> engine.inputPi();
            case "x^2" -> engine.applyUnaryOperation("square");
            case "sqrt" -> engine.applyUnaryOperation("sqrt");
            case "1/x" -> engine.applyUnaryOperation("inverse");
            case "sin", "cos", "tan", "ln", "log" -> engine.applyUnaryOperation(action);
            case "NOT" -> setProgrammerValue(~engine.getProgrammerValue());
            case "<<" -> setProgrammerValue(engine.getProgrammerValue() << 1);
            case ">>" -> setProgrammerValue(engine.getProgrammerValue() >> 1);
            default -> displayLabel.getText();
        };

        displayLabel.setText(result);
        expressionLabel.setText(engine.getExpressionText());
        refreshDerivedViews();
        pulseDisplay();
    }

    private String setProgrammerValue(long value) {
        engine.clear();
        String text = Long.toString(value);
        for (char digit : text.toCharArray()) {
            if (digit == '-') {
                engine.toggleSign();
            } else {
                engine.inputDigit(String.valueOf(digit));
            }
        }
        return engine.getDisplayText();
    }

    private void refreshDerivedViews() {
        updateProgrammerValues();
        updateCurrencyResult();
    }

    private void updateProgrammerValues() {
        long value = engine.getProgrammerValue();
        programmerHexValue.setText("HEX " + Long.toHexString(value).toUpperCase());
        programmerDecValue.setText("DEC " + value);
        programmerOctValue.setText("OCT " + Long.toOctalString(value));
        programmerBinValue.setText("BIN " + Long.toBinaryString(value));
    }

    private void updateCurrencyResult() {
        String from = (String) fromCurrency.getSelectedItem();
        String to = (String) toCurrency.getSelectedItem();
        if (from == null || to == null) {
            currencyResultLabel.setText("0.00");
            return;
        }

        BigDecimal amount = engine.getCurrentValue();
        BigDecimal fromRate = currencyRates.get(from);
        BigDecimal toRate = currencyRates.get(to);
        BigDecimal usdValue = amount.divide(fromRate, 8, RoundingMode.HALF_UP);
        BigDecimal converted = usdValue.multiply(toRate).setScale(2, RoundingMode.HALF_UP);
        currencyResultLabel.setText(CURRENCY_FORMAT.format(converted) + " " + to);
    }

    private String currentMode() {
        return (String) modeSelector.getSelectedItem();
    }

    private void pulseDisplay() {
        final Color start = theme.displayText;
        final Color glow = theme.displayPulse;
        Timer timer = new Timer(18, null);
        timer.addActionListener(new java.awt.event.ActionListener() {
            private int tick;

            @Override
            public void actionPerformed(ActionEvent event) {
                tick++;
                float progress = Math.min(1f, tick / 8f);
                displayLabel.setForeground(blend(glow, start, progress));
                if (progress >= 1f) {
                    displayLabel.setForeground(start);
                    timer.stop();
                }
            }
        });
        timer.start();
    }

    private void applyTheme() {
        shell.repaint();
        titleLabel.setForeground(theme.headerText);
        expressionLabel.setForeground(theme.secondaryText);
        displayLabel.setForeground(theme.displayText);
        themeToggle.setSelected(theme.dark);
        themeToggle.setText(theme.dark ? "Dark" : "Light");
        themeToggle.setBackground(theme.controlBackground);
        themeToggle.setForeground(theme.headerText);
        themeToggle.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        modeSelector.setBackground(theme.controlBackground);
        modeSelector.setForeground(theme.headerText);
        modeSelector.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        fromCurrency.setBackground(theme.controlBackground);
        fromCurrency.setForeground(theme.headerText);
        toCurrency.setBackground(theme.controlBackground);
        toCurrency.setForeground(theme.headerText);

        Component displayPanel = expressionLabel.getParent();
        if (displayPanel instanceof JPanel panel) {
            panel.setBackground(theme.displayBackground);
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(theme.cardBorder, 1),
                    BorderFactory.createEmptyBorder(18, 18, 18, 18)
            ));
        }

        updateCardColors(modePanel);
        repaint();
    }

    private void updateCardColors(Component component) {
        if (component instanceof JPanel panel && panel.getBorder() != null && panel.getComponentCount() == 1 && panel.getComponent(0) instanceof JLabel) {
            panel.setBackground(theme.cardBackground);
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(theme.cardBorder, 1),
                    BorderFactory.createEmptyBorder(12, 14, 12, 14)
            ));
            JLabel label = (JLabel) panel.getComponent(0);
            label.setForeground(theme.headerText);
        }

        if (component instanceof JPanel panel) {
            for (Component child : panel.getComponents()) {
                updateCardColors(child);
            }
        }

        if (component instanceof JLabel label && component != displayLabel && component != expressionLabel) {
            label.setForeground(theme.headerText);
        }
    }

    private static Map<String, BigDecimal> createCurrencyRates() {
        Map<String, BigDecimal> rates = new LinkedHashMap<>();
        rates.put("USD", BigDecimal.valueOf(1.00));
        rates.put("EUR", BigDecimal.valueOf(0.92));
        rates.put("GBP", BigDecimal.valueOf(0.79));
        rates.put("JPY", BigDecimal.valueOf(149.80));
        rates.put("MAD", BigDecimal.valueOf(9.93));
        return rates;
    }

    private static Color blend(Color from, Color to, float progress) {
        int red = (int) (from.getRed() + (to.getRed() - from.getRed()) * progress);
        int green = (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * progress);
        int blue = (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * progress);
        int alpha = (int) (from.getAlpha() + (to.getAlpha() - from.getAlpha()) * progress);
        return new Color(red, green, blue, alpha);
    }

    private enum ButtonRole {
        NUMBER,
        FUNCTION,
        OPERATOR
    }

    private final class RoundedPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            RoundRectangle2D shape = new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 38, 38);
            g2.setPaint(new GradientPaint(0, 0, theme.surfaceTop, 0, getHeight(), theme.surfaceBottom));
            g2.fill(shape);

            g2.setColor(theme.surfaceHighlight);
            g2.fillRoundRect(1, 1, getWidth() - 2, Math.max(120, getHeight() / 3), 38, 38);

            g2.setColor(theme.surfaceBorder);
            g2.setStroke(new BasicStroke(1.1f));
            g2.draw(shape);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private final class CircleButton extends JButton {
        private final ButtonRole role;
        private float hoverAmount;
        private float pressAmount;
        private Timer animationTimer;

        private CircleButton(String text, ButtonRole role) {
            super(text);
            this.role = role;
            setFont(resolveFont(text, role));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(84, 84));
        }

        @Override
        protected void processMouseEvent(MouseEvent event) {
            super.processMouseEvent(event);
            switch (event.getID()) {
                case MouseEvent.MOUSE_ENTERED -> animateToward(1f, pressAmount);
                case MouseEvent.MOUSE_EXITED -> animateToward(0f, 0f);
                case MouseEvent.MOUSE_PRESSED -> animateToward(hoverAmount, 1f);
                case MouseEvent.MOUSE_RELEASED -> animateToward(contains(event.getPoint()) ? 1f : 0f, 0f);
                default -> {
                }
            }
        }

        @Override
        public boolean contains(int x, int y) {
            int size = Math.min(getWidth(), getHeight());
            int offsetX = (getWidth() - size) / 2;
            int offsetY = (getHeight() - size) / 2;
            return new Ellipse2D.Float(offsetX, offsetY, size, size).contains(x, y);
        }

        private void animateToward(float hoverTarget, float pressTarget) {
            if (animationTimer != null && animationTimer.isRunning()) {
                animationTimer.stop();
            }

            animationTimer = new Timer(14, null);
            animationTimer.addActionListener(event -> {
                hoverAmount += (hoverTarget - hoverAmount) * 0.18f;
                pressAmount += (pressTarget - pressAmount) * 0.24f;
                repaint();

                if (Math.abs(hoverAmount - hoverTarget) < 0.02f && Math.abs(pressAmount - pressTarget) < 0.02f) {
                    hoverAmount = hoverTarget;
                    pressAmount = pressTarget;
                    repaint();
                    animationTimer.stop();
                }
            });
            animationTimer.start();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int size = Math.min(getWidth(), getHeight()) - 2;
            int diameter = Math.round(size - (pressAmount * 4f));
            int x = (getWidth() - diameter) / 2;
            int y = (getHeight() - diameter) / 2;

            Color base = switch (role) {
                case NUMBER -> theme.numberButton;
                case FUNCTION -> theme.functionButton;
                case OPERATOR -> theme.operatorButton;
            };
            Color hover = switch (role) {
                case NUMBER -> theme.numberButtonHover;
                case FUNCTION -> theme.functionButtonHover;
                case OPERATOR -> theme.operatorButtonHover;
            };

            g2.setColor(theme.buttonShadow);
            g2.fillOval(x, y + 3, diameter, diameter);
            g2.setColor(blend(base, hover, hoverAmount));
            g2.fillOval(x, y, diameter, diameter);
            g2.setColor(theme.buttonBorder);
            g2.setStroke(new BasicStroke(1f));
            g2.drawOval(x, y, diameter, diameter);

            if ("ICON".equals(getText())) {
                paintIcon(g2, x, y, diameter);
            } else {
                g2.setFont(getFont());
                g2.setColor(role == ButtonRole.FUNCTION ? theme.functionText : theme.buttonText);
                FontMetrics metrics = g2.getFontMetrics();
                int textX = (getWidth() - metrics.stringWidth(getText())) / 2;
                int textY = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent() - Math.round(pressAmount * 2f);
                g2.drawString(getText(), textX, textY);
            }
            g2.dispose();
        }

        private void paintIcon(Graphics2D g2, int x, int y, int diameter) {
            int iconWidth = 20;
            int iconHeight = 24;
            int iconX = x + (diameter - iconWidth) / 2;
            int iconY = y + (diameter - iconHeight) / 2;
            g2.setColor(theme.buttonText);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(iconX, iconY, iconWidth, iconHeight, 5, 5);
            g2.drawRoundRect(iconX + 4, iconY + 4, iconWidth - 8, 5, 3, 3);
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    g2.fillOval(iconX + 4 + (col * 5), iconY + 13 + (row * 5), 3, 3);
                }
            }
        }
    }

    private final class ActionButton extends JButton {
        private ActionButton(String text) {
            super(text);
            setFont(new Font("SF Pro Text", Font.PLAIN, 16));
            setFocusPainted(false);
            setBorder(BorderFactory.createLineBorder(theme.cardBorder, 1));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMargin(new Insets(10, 10, 10, 10));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            setBackground(theme.cardBackground);
            setForeground(theme.headerText);
            setBorder(BorderFactory.createLineBorder(theme.cardBorder, 1));
            super.paintComponent(graphics);
        }
    }

    private static Font resolveFont(String text, ButtonRole role) {
        if ("ICON".equals(text)) {
            return SMALL_BUTTON_FONT;
        }
        if (role == ButtonRole.FUNCTION && text.length() > 1) {
            return SMALL_BUTTON_FONT;
        }
        return BUTTON_FONT;
    }

    private record Theme(
            boolean dark,
            Color surfaceTop,
            Color surfaceBottom,
            Color surfaceHighlight,
            Color surfaceBorder,
            Color headerText,
            Color secondaryText,
            Color displayText,
            Color displayPulse,
            Color displayBackground,
            Color controlBackground,
            Color cardBackground,
            Color cardBorder,
            Color numberButton,
            Color numberButtonHover,
            Color functionButton,
            Color functionButtonHover,
            Color operatorButton,
            Color operatorButtonHover,
            Color functionText,
            Color buttonText,
            Color buttonBorder,
            Color buttonShadow
    ) {
        private static Theme createDark() {
            return new Theme(
                    true,
                    new Color(88, 81, 91, 244),
                    new Color(50, 47, 57, 244),
                    new Color(255, 255, 255, 16),
                    new Color(255, 255, 255, 34),
                    new Color(244, 239, 244),
                    new Color(199, 190, 202),
                    new Color(248, 244, 248),
                    new Color(255, 224, 171),
                    new Color(66, 61, 70, 180),
                    new Color(74, 70, 78, 230),
                    new Color(76, 71, 82, 220),
                    new Color(255, 255, 255, 28),
                    new Color(110, 107, 119, 228),
                    new Color(132, 129, 141, 238),
                    new Color(176, 171, 179, 232),
                    new Color(199, 194, 203, 240),
                    new Color(255, 159, 10, 245),
                    new Color(255, 181, 54, 250),
                    new Color(46, 42, 48),
                    new Color(250, 247, 250),
                    new Color(255, 255, 255, 30),
                    new Color(0, 0, 0, 50)
            );
        }

        private static Theme createLight() {
            return new Theme(
                    false,
                    new Color(246, 244, 247, 250),
                    new Color(234, 232, 238, 250),
                    new Color(255, 255, 255, 130),
                    new Color(186, 183, 192, 170),
                    new Color(50, 47, 56),
                    new Color(96, 92, 104),
                    new Color(56, 52, 60),
                    new Color(255, 164, 38),
                    new Color(255, 255, 255, 185),
                    new Color(232, 229, 236),
                    new Color(236, 233, 239),
                    new Color(200, 197, 206),
                    new Color(132, 129, 141),
                    new Color(152, 149, 161),
                    new Color(201, 198, 205),
                    new Color(216, 213, 221),
                    new Color(255, 159, 10),
                    new Color(255, 181, 54),
                    new Color(70, 66, 74),
                    new Color(251, 249, 251),
                    new Color(255, 255, 255, 90),
                    new Color(0, 0, 0, 22)
            );
        }
    }
}
