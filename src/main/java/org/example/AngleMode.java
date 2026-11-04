package org.example;

/** Gemeinsame Winkelbehandlung für Konsolendialog und Ausdrucksauswertung. */
public enum AngleMode {
  DEG,
  RAD;

  double sin(CalculatorEngine engine, double angle) {
    return engine.sin(this == DEG ? Math.toRadians(angle) : angle);
  }

  double cos(CalculatorEngine engine, double angle) {
    return engine.cos(this == DEG ? Math.toRadians(angle) : angle);
  }

  double tan(CalculatorEngine engine, double angle) {
    if (this == RAD) {
      return engine.tan(angle);
    }

    angle %= 180;
    // Bereits beim Einlesen als double verlorene Stellen sind nicht rekonstruierbar.
    if (Math.abs(angle) == 90) {
      throw new ArithmeticException("Tangens ist für diesen Winkel nicht definiert.");
    }
    if (angle > 90) {
      angle -= 180;
    } else if (angle < -90) {
      angle += 180;
    }
    // Nahe ±90° den kleinen Komplementwinkel und den Kehrwert nutzen.
    // So erhält die Engine nur Winkel bis ±π/4, niemals gerundete ±π/2.
    if (Math.abs(angle) > 45) {
      double complement = Math.copySign(90.0, angle) - angle;
      return engine.divide(1.0, engine.tan(Math.toRadians(complement)));
    }
    return engine.tan(Math.toRadians(angle));
  }
}
