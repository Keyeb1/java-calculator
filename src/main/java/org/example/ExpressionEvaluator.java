package org.example;

/** Wertet Rechenausdrücke mit Konstanten und Funktionen ohne Konsoleninteraktion aus. */
public class ExpressionEvaluator {
  private static final int MAX_EXPRESSION_LENGTH = 256;
  private static final int MAX_OPERATIONS = 50;
  private static final int MAX_PARENTHESIS_DEPTH = 16;

  private final CalculatorEngine engine = new CalculatorEngine();

  /**
   * Wertet mit DEG und ohne letztes Ergebnis aus. Unterstützt +, -, *, /,
   * unäre Vorzeichen, Klammern, Konstanten und wissenschaftliche Funktionen.
   * Ungültige Eingaben und überschrittene Grenzen führen zu einer
   * IllegalArgumentException; Syntaxpositionen werden ab 1 gezählt.
   * ArithmeticExceptions der Engine werden unverändert weitergegeben.
   * Zahlen und Ergebnisse unterliegen den üblichen double-Rundungen.
   */
  public double evaluate(String expression) {
    return evaluate(expression, AngleMode.DEG, null);
  }

  /**
   * Der Kontext gilt nur für diesen Aufruf; null als lastResult bedeutet kein Ergebnis.
   * Auch ein unbenutztes übergebenes Ergebnis muss endlich sein.
   * pi/e sind unabhängig von Groß-/Kleinschreibung; ans und Funktionen kleingeschrieben.
   * Zahlen verwenden den Dezimalpunkt und optional e/E mit direkt folgendem Exponenten;
   * ein alleinstehendes e ist die Konstante. Kommas trennen Funktionsargumente.
   * Operatoren, unäre Vorzeichen und Funktionsaufrufe zählen jeweils als eine Operation.
   */
  public double evaluate(String expression, AngleMode angleMode, Double lastResult) {
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
    if (angleMode == null) {
      throw new IllegalArgumentException("Ein Winkelmodus (DEG oder RAD) muss angegeben werden.");
    }
    if (lastResult != null && !Double.isFinite(lastResult)) {
      throw new IllegalArgumentException("Das letzte Ergebnis muss eine endliche Zahl sein (kein NaN oder Infinity).");
    }
    return new Parser(expression, engine, angleMode, lastResult).parse();
  }

  private static IllegalArgumentException errorAt(String message, int position) {
    return new IllegalArgumentException(message + " (Position " + (position + 1) + ").");
  }

  /** Enthält ausschließlich den Zustand einer einzelnen Auswertung. */
  private static final class Parser {
    private final String input;
    private final CalculatorEngine engine;
    private final AngleMode angleMode;
    private final Double lastResult;
    private int position;
    private int operationCount;
    private int parenthesisDepth;

    private Parser(String input, CalculatorEngine engine, AngleMode angleMode, Double lastResult) {
      this.input = input;
      this.engine = engine;
      this.angleMode = angleMode;
      this.lastResult = lastResult;
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
        openParenthesis();
        skipWhitespace();
        if (position < input.length() && input.charAt(position) == ')') {
          throw errorAt("Leere Klammern sind nicht erlaubt", position);
        }
        double result = parseExpression();
        closeParenthesis();
        return result;
      }
      if (position < input.length() && Character.isLetter(input.charAt(position))) {
        return parseIdentifier();
      }
      return parseNumber();
    }

    private double parseIdentifier() {
      int start = position;
      while (position < input.length()
          && (Character.isLetterOrDigit(input.charAt(position)) || input.charAt(position) == '_')) {
        position++;
      }
      String name = input.substring(start, position);
      if (name.equalsIgnoreCase("pi")) {
        return Math.PI;
      }
      if (name.equalsIgnoreCase("e")) {
        return Math.E;
      }
      if (name.equals("ans")) {
        if (lastResult == null) {
          throw errorAt("Für ans ist noch kein letztes Ergebnis vorhanden", start);
        }
        return lastResult;
      }

      int argumentCount = switch (name) {
        case "sqrt", "abs", "sin", "cos", "tan", "log", "ln", "factorial" -> 1;
        case "power", "root" -> 2;
        default -> throw errorAt("Unbekannter Name '" + name + "'", start);
      };
      skipWhitespace();
      if (position == input.length() || input.charAt(position) != '(') {
        throw errorAt("Nach Funktion '" + name + "' wird '(' erwartet", position);
      }
      countOperation(start);
      openParenthesis();
      double first = parseArgument(name, argumentCount);
      double second = 0.0;
      if (argumentCount == 2) {
        skipWhitespace();
        if (position == input.length() || input.charAt(position) != ',') {
          throw argumentError(name, argumentCount);
        }
        position++;
        second = parseArgument(name, argumentCount);
      }
      skipWhitespace();
      if (position < input.length() && input.charAt(position) == ',') {
        throw argumentError(name, argumentCount);
      }
      closeParenthesis();
      return switch (name) {
        case "sqrt" -> engine.sqrt(first);
        case "abs" -> engine.abs(first);
        case "sin" -> angleMode.sin(engine, first);
        case "cos" -> angleMode.cos(engine, first);
        case "tan" -> angleMode.tan(engine, first);
        case "log" -> engine.log(first);
        case "ln" -> engine.ln(first);
        case "factorial" -> engine.factorial(first);
        case "power" -> engine.power(first, second);
        case "root" -> engine.nthRoot(first, second);
        default -> throw errorAt("Unbekannte Funktion '" + name + "'", start);
      };
    }

    private double parseArgument(String name, int argumentCount) {
      skipWhitespace();
      if (position == input.length() || input.charAt(position) == ',' || input.charAt(position) == ')') {
        throw argumentError(name, argumentCount);
      }
      return parseExpression();
    }

    private IllegalArgumentException argumentError(String name, int argumentCount) {
      return errorAt("Funktion '" + name + "' erwartet genau " + argumentCount
          + (argumentCount == 1 ? " nichtleeres Argument" : " nichtleere Argumente"), position);
    }

    private void openParenthesis() {
      if (parenthesisDepth == MAX_PARENTHESIS_DEPTH) {
        throw errorAt("Es dürfen höchstens " + MAX_PARENTHESIS_DEPTH
            + " Klammerebenen gleichzeitig geöffnet sein", position);
      }
      position++;
      parenthesisDepth++;
    }

    private void closeParenthesis() {
      skipWhitespace();
      if (position == input.length() || input.charAt(position) != ')') {
        throw errorAt("Schließende Klammer ')' erwartet", position);
      }
      position++;
      parenthesisDepth--;
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
      countOperation(position);
      return input.charAt(position++);
    }

    private void countOperation(int operationPosition) {
      if (operationCount == MAX_OPERATIONS) {
        throw errorAt("Ein Ausdruck darf höchstens " + MAX_OPERATIONS
            + " Operationen (Operatoren, unäre Vorzeichen und Funktionsaufrufe) enthalten", operationPosition);
      }
      operationCount++;
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
