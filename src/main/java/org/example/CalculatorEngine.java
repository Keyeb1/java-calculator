package org.example;

/**
 * Berechnet mit endlichen reellen Zahlen, unabhängig von einer Benutzeroberfläche.
 * Nicht endliche Operanden führen zu einer IllegalArgumentException;
 * ungültige Berechnungen oder nicht endliche Ergebnisse zu einer ArithmeticException.
 */
public class CalculatorEngine {

  public double add(double first, double second) {
    validateOperands(first, second);
    return checkedResult(first + second);
  }

  public double subtract(double first, double second) {
    validateOperands(first, second);
    return checkedResult(first - second);
  }

  public double multiply(double first, double second) {
    validateOperands(first, second);
    return checkedResult(first * second);
  }

  public double divide(double dividend, double divisor) {
    validateOperands(dividend, divisor);
    if (divisor == 0) {
      throw new ArithmeticException("Division durch null ist nicht erlaubt.");
    }
    return checkedResult(dividend / divisor);
  }

  public double sqrt(double number) {
    validateOperands(number);
    if (number < 0) {
      throw new ArithmeticException("Die Quadratwurzel einer negativen Zahl ist nicht erlaubt.");
    }
    return checkedResult(Math.sqrt(number));
  }

  public double abs(double number) {
    validateOperands(number);
    return checkedResult(Math.abs(number));
  }

  /** Berechnet den Sinus eines Winkels im Bogenmaß (Radiant). */
  public double sin(double angle) {
    validateOperands(angle);
    return checkedResult(Math.sin(angle));
  }

  /** Berechnet den Kosinus eines Winkels im Bogenmaß (Radiant). */
  public double cos(double angle) {
    validateOperands(angle);
    return checkedResult(Math.cos(angle));
  }

  public double power(double base, double exponent) {
    validateOperands(base, exponent);
    return checkedResult(Math.pow(base, exponent));
  }

  private static void validateOperands(double... operands) {
    for (double operand : operands) {
      if (!Double.isFinite(operand)) {
        throw new IllegalArgumentException("Operanden müssen endliche Zahlen sein (kein NaN oder Infinity).");
      }
    }
  }

  private static double checkedResult(double result) {
    if (!Double.isFinite(result)) {
      throw new ArithmeticException("Die Berechnung liefert kein endliches Ergebnis (NaN oder Infinity).");
    }
    return result;
  }
}
