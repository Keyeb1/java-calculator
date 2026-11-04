package org.example;

/** Wertet dezimale Rechenausdrücke ohne Konsoleninteraktion aus. */
public class ExpressionEvaluator {
  private static final int MAX_EXPRESSION_LENGTH = 256;
  private static final int MAX_OPERATORS = 50;
  private static final int MAX_PARENTHESIS_DEPTH = 16;

  private final CalculatorEngine engine = new CalculatorEngine();

  /**
   * Unterstützt +, -, *, /, unäre Vorzeichen und runde Klammern.
   * Ungültige Eingaben und überschrittene Grenzen führen zu einer
   * IllegalArgumentException; Syntaxpositionen werden ab 1 gezählt.
   * ArithmeticExceptions der Engine werden unverändert weitergegeben.
   * Zahlen und Ergebnisse unterliegen den üblichen double-Rundungen.
   */
  public double evaluate(String expression) {
    if (expression == null) {
      throw new IllegalArgumentException("Der Ausdruck darf nicht null sein.");
    }
    if (expression.length() > MAX_EXPRESSION_LENGTH) {
      throw errorAt("Ein Ausdruck darf höchstens " + MAX_EXPRESSION_LENGTH
          + " Zeichen einschließlich Leerzeichen enthalten", MAX_EXPRESSION_LENGTH);
    }
    if (expression.isBlank()) {
      throw errorAt("Der Ausdruck darf nicht leer sein", 0);
    }
    return new Parser(expression, engine).parse();
  }

  private static IllegalArgumentException errorAt(String message, int position) {
    return new IllegalArgumentException(message + " (Position " + (position + 1) + ").");
  }

  /** Enthält ausschließlich den Zustand einer einzelnen Auswertung. */
  private static final class Parser {
    private final String input;
    private final CalculatorEngine engine;
    private int position;
    private int operatorCount;
    private int parenthesisDepth;

    private Parser(String input, CalculatorEngine engine) {
      this.input = input;
      this.engine = engine;
    }

    private double parse() {
      double result = parseExpression();
      skipWhitespace();
      if (position < input.length()) {
        throw errorAt("Unerwartetes Zeichen '" + input.charAt(position) + "'", position);
      }
      return result;
    }

    // Ausdruck: Term ((+ | -) Term)*
    private double parseExpression() {
      double result = parseTerm();
      while (true) {
        skipWhitespace();
        if (position == input.length()
            || (input.charAt(position) != '+' && input.charAt(position) != '-')) {
          return result;
        }
        char operator = consumeOperator();
        double operand = parseTerm();
        result = operator == '+' ? engine.add(result, operand) : engine.subtract(result, operand);
      }
    }

    // Term: Vorzeichen ((* | /) Vorzeichen)*
    private double parseTerm() {
      double result = parseUnary();
      while (true) {
        skipWhitespace();
        if (position == input.length()
            || (input.charAt(position) != '*' && input.charAt(position) != '/')) {
          return result;
        }
        char operator = consumeOperator();
        double operand = parseUnary();
        result = operator == '*' ? engine.multiply(result, operand) : engine.divide(result, operand);
      }
    }

    private double parseUnary() {
      skipWhitespace();
      if (position < input.length()
          && (input.charAt(position) == '+' || input.charAt(position) == '-')) {
        char operator = consumeOperator();
        double operand = parseUnary();
        // Multiplikation erhält auch das Vorzeichen von -0.0.
        return operator == '-' ? engine.multiply(-1.0, operand) : operand;
      }
      return parsePrimary();
    }

    private double parsePrimary() {
      skipWhitespace();
      if (position < input.length() && input.charAt(position) == '(') {
        if (parenthesisDepth == MAX_PARENTHESIS_DEPTH) {
          throw errorAt("Es dürfen höchstens " + MAX_PARENTHESIS_DEPTH
              + " Klammerebenen gleichzeitig geöffnet sein", position);
        }
        position++;
        parenthesisDepth++;
        skipWhitespace();
        if (position < input.length() && input.charAt(position) == ')') {
          throw errorAt("Leere Klammern sind nicht erlaubt", position);
        }
        double result = parseExpression();
        skipWhitespace();
        if (position == input.length() || input.charAt(position) != ')') {
          throw errorAt("Schließende Klammer ')' erwartet", position);
        }
        position++;
        parenthesisDepth--;
        return result;
      }
      return parseNumber();
    }

    private double parseNumber() {
      int start = position;
      consumeDigits();
      boolean hasDigits = position > start;
      if (position < input.length() && input.charAt(position) == '.') {
        position++;
        int fractionStart = position;
        consumeDigits();
        hasDigits |= position > fractionStart;
      }
      if (!hasDigits) {
        throw errorAt("Zahl oder öffnende Klammer '(' erwartet", start);
      }
      if (position < input.length()
          && (input.charAt(position) == 'e' || input.charAt(position) == 'E')) {
        position++;
        if (position < input.length()
            && (input.charAt(position) == '+' || input.charAt(position) == '-')) {
          position++;
        }
        int exponentStart = position;
        consumeDigits();
        if (position == exponentStart) {
          throw errorAt("Der Exponent muss mindestens eine Ziffer enthalten", position);
        }
      }
      double number = Double.parseDouble(input.substring(start, position));
      if (!Double.isFinite(number)) {
        throw errorAt("Die Zahl muss endlich sein (kein NaN oder Infinity)", start);
      }
      return number;
    }

    private char consumeOperator() {
      if (operatorCount == MAX_OPERATORS) {
        throw errorAt("Ein Ausdruck darf höchstens " + MAX_OPERATORS
            + " Operatoren einschließlich unärer Vorzeichen enthalten", position);
      }
      operatorCount++;
      return input.charAt(position++);
    }

    private void consumeDigits() {
      while (position < input.length()
          && input.charAt(position) >= '0' && input.charAt(position) <= '9') {
        position++;
      }
    }

    private void skipWhitespace() {
      while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
        position++;
      }
    }
  }
}
