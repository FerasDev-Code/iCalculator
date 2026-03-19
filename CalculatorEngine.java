package com.calchunter;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public final class CalculatorEngine {
    private static final MathContext DIVISION_CONTEXT = new MathContext(16, RoundingMode.HALF_UP);
    private static final MathContext SCIENTIFIC_CONTEXT = new MathContext(14, RoundingMode.HALF_UP);

    private BigDecimal storedValue;
    private String pendingOperator;
    private boolean startNewInput = true;
    private boolean showingError;
    private String displayText = "0";
    private String expressionText = " ";

    public String inputDigit(String digit) {
        clearErrorIfNeeded();

        if (startNewInput) {
            displayText = digit;
            startNewInput = false;
            updateExpressionWithCurrentValue();
            return displayText;
        }

        if ("0".equals(displayText)) {
            displayText = digit;
        } else {
            displayText += digit;
        }
        updateExpressionWithCurrentValue();
        return displayText;
    }

    public String inputDecimal() {
        clearErrorIfNeeded();

        if (startNewInput) {
            displayText = "0.";
            startNewInput = false;
            updateExpressionWithCurrentValue();
            return displayText;
        }

        if (!displayText.contains(".")) {
            displayText += ".";
        }
        updateExpressionWithCurrentValue();
        return displayText;
    }

    public String clear() {
        storedValue = null;
        pendingOperator = null;
        startNewInput = true;
        showingError = false;
        displayText = "0";
        expressionText = " ";
        return displayText;
    }

    public String backspace() {
        clearErrorIfNeeded();

        if (startNewInput || displayText.length() == 1) {
            displayText = "0";
            startNewInput = true;
            updateExpressionWithCurrentValue();
            return displayText;
        }

        displayText = displayText.substring(0, displayText.length() - 1);
        if ("-".equals(displayText)) {
            displayText = "0";
            startNewInput = true;
        }
        updateExpressionWithCurrentValue();
        return displayText;
    }

    public String toggleSign() {
        clearErrorIfNeeded();

        if ("0".equals(displayText)) {
            return displayText;
        }

        displayText = displayText.startsWith("-")
                ? displayText.substring(1)
                : "-" + displayText;
        updateExpressionWithCurrentValue();
        return displayText;
    }

    public String percent() {
        clearErrorIfNeeded();
        return setCurrentValue(parseDisplay().divide(BigDecimal.valueOf(100), DIVISION_CONTEXT), true, "%");
    }

    public String inputPi() {
        clearErrorIfNeeded();
        return setCurrentValue(BigDecimal.valueOf(Math.PI), false, "pi");
    }

    public String applyOperator(String operator) {
        clearErrorIfNeeded();

        BigDecimal currentValue = parseDisplay();
        if (storedValue == null) {
            storedValue = currentValue;
        } else if (!startNewInput && pendingOperator != null) {
            storedValue = calculate(storedValue, currentValue, pendingOperator);
            displayText = format(storedValue);
        }

        pendingOperator = operator;
        startNewInput = true;
        expressionText = format(storedValue) + " " + operator;
        return displayText;
    }

    public String evaluate() {
        clearErrorIfNeeded();

        if (pendingOperator == null || storedValue == null) {
            return displayText;
        }

        BigDecimal rightValue = parseDisplay();
        String leftText = format(storedValue);
        String rightText = format(rightValue);
        BigDecimal result = calculate(storedValue, rightValue, pendingOperator);

        if (!showingError) {
            expressionText = leftText + " " + pendingOperator + " " + rightText + " =";
            displayText = format(result);
            storedValue = result;
            pendingOperator = null;
            startNewInput = true;
        }
        return displayText;
    }

    public String applyUnaryOperation(String operation) {
        clearErrorIfNeeded();
        BigDecimal value = parseDisplay();

        return switch (operation) {
            case "sqrt" -> {
                requireNonNegative(value);
                yield setCurrentValue(BigDecimal.valueOf(Math.sqrt(value.doubleValue())), true, "sqrt");
            }
            case "square" -> setCurrentValue(value.multiply(value, SCIENTIFIC_CONTEXT), true, "x^2");
            case "inverse" -> {
                requireNonZero(value);
                yield setCurrentValue(BigDecimal.ONE.divide(value, DIVISION_CONTEXT), true, "1/x");
            }
            case "sin" -> setCurrentValue(BigDecimal.valueOf(Math.sin(value.doubleValue())), true, "sin");
            case "cos" -> setCurrentValue(BigDecimal.valueOf(Math.cos(value.doubleValue())), true, "cos");
            case "tan" -> setCurrentValue(BigDecimal.valueOf(Math.tan(value.doubleValue())), true, "tan");
            case "ln" -> {
                requirePositive(value);
                yield setCurrentValue(BigDecimal.valueOf(Math.log(value.doubleValue())), true, "ln");
            }
            case "log" -> {
                requirePositive(value);
                yield setCurrentValue(BigDecimal.valueOf(Math.log10(value.doubleValue())), true, "log");
            }
            case "fact" -> setCurrentValue(BigDecimal.valueOf(factorial(value)), true, "n!");
            default -> displayText;
        };
    }

    public String getDisplayText() {
        return displayText;
    }

    public String getExpressionText() {
        return expressionText;
    }

    public BigDecimal getCurrentValue() {
        clearErrorIfNeeded();
        return parseDisplay();
    }

    public long getProgrammerValue() {
        return getCurrentValue().longValue();
    }

    private String setCurrentValue(BigDecimal value, boolean startFresh, String prefix) {
        displayText = format(value);
        startNewInput = startFresh;
        if (prefix == null || prefix.isBlank()) {
            updateExpressionWithCurrentValue();
        } else {
            expressionText = prefix + "(" + displayText + ")";
        }
        return displayText;
    }

    private void clearErrorIfNeeded() {
        if (showingError) {
            clear();
        }
    }

    private BigDecimal parseDisplay() {
        return new BigDecimal(displayText);
    }

    private BigDecimal calculate(BigDecimal left, BigDecimal right, String operator) {
        try {
            return switch (operator) {
                case "+" -> left.add(right, SCIENTIFIC_CONTEXT);
                case "-" -> left.subtract(right, SCIENTIFIC_CONTEXT);
                case "x", "×" -> left.multiply(right, SCIENTIFIC_CONTEXT);
                case "/", "÷" -> {
                    requireNonZero(right);
                    yield left.divide(right, DIVISION_CONTEXT);
                }
                case "x^y" -> BigDecimal.valueOf(Math.pow(left.doubleValue(), right.doubleValue()));
                default -> right;
            };
        } catch (ArithmeticException | IllegalArgumentException ex) {
            showError(ex.getMessage() == null ? "Invalid operation" : ex.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private void requireNonNegative(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Invalid input");
        }
    }

    private void requirePositive(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid input");
        }
    }

    private void requireNonZero(BigDecimal value) {
        if (BigDecimal.ZERO.compareTo(value) == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        }
    }

    private long factorial(BigDecimal value) {
        if (value.scale() > 0 && value.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Whole numbers only");
        }
        long number = value.longValue();
        if (number < 0 || number > 20) {
            throw new IllegalArgumentException("Range is 0-20");
        }
        long result = 1;
        for (long i = 2; i <= number; i++) {
            result *= i;
        }
        return result;
    }

    private void showError(String message) {
        showingError = true;
        pendingOperator = null;
        storedValue = null;
        startNewInput = true;
        displayText = "Error";
        expressionText = message;
    }

    private void updateExpressionWithCurrentValue() {
        if (pendingOperator != null && storedValue != null) {
            expressionText = format(storedValue) + " " + pendingOperator + " " + displayText;
        } else if (!"0".equals(displayText)) {
            expressionText = displayText;
        } else {
            expressionText = " ";
        }
    }

    private String format(BigDecimal value) {
        BigDecimal normalized = value.stripTrailingZeros();
        if (normalized.scale() < 0) {
            normalized = normalized.setScale(0);
        }
        return normalized.toPlainString();
    }
}
