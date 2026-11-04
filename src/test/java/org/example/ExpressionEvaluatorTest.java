package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpressionEvaluatorTest {
  private static final double TOLERANCE = 1e-12;

  private final ExpressionEvaluator evaluator = new ExpressionEvaluator();

  @ParameterizedTest
  @CsvSource({
      "3+4+5+6+7, 25",
      "3+4+5*4-5, 22",
      "(3+4+5)*4-5, 43",
      "20/5*2, 8",
      "2*(-4), -8",
      "-(3+4), -7",
      "1e-3+2, 2.001"
  })
  void evaluatesTheRequestedExamples(String expression, double expected) {
    assertEquals(expected, evaluator.evaluate(expression), TOLERANCE);
  }

  @ParameterizedTest
  @CsvSource({
      "10-3-2, 5", "16/4/2, 2", "8/2*(2+2), 16",
      "1-2*3+4, -1", "12/3+2*5, 14", "8/(4/2), 4",
      "18/(3*2), 3", "3-(4-5), 4"
  })
  void respectsPrecedenceParenthesesAndLeftAssociativity(String expression, double expected) {
    assertEquals(expected, evaluator.evaluate(expression), TOLERANCE);
  }

  @ParameterizedTest
  @CsvSource({
      "+2, 2", "-2, -2", "++2, 2", "--2, 2", "+-2, -2", "-+2, -2",
      "2++3, 5", "2+-3, -1", "2--3, 5", "2/-4, -0.5",
      "-2*-3, 6", "2*+4, 8", "+(3+4), 7", "--(3+4), 7",
      "-(-(-2)), -2", "-(2+3)*-2, 10"
  })
  void supportsUnarySignsBeforeNumbersAndParentheses(String expression, double expected) {
    assertEquals(expected, evaluator.evaluate(expression), TOLERANCE);
  }

  @ParameterizedTest
  @CsvSource({
      "0, 0", "42, 42", "0003.00, 3", "3.14, 3.14", ".5, 0.5", "3., 3",
      "1.25e-7, 0.000000125", "2E+3, 2000", "1E-3, 0.001",
      ".5e+1, 5", "3.e2, 300", "-2.75, -2.75"
  })
  void acceptsDecimalAndScientificNumberFormats(String expression, double expected) {
    assertEquals(expected, evaluator.evaluate(expression), TOLERANCE);
  }

  @ParameterizedTest
  @ValueSource(strings = {" 3 + 4 * 2 ", "3+4*2", "\t3 +\n4 * 2\r\n", "+ ( 3 + 4 ) + 4", "3 + - - 4 * 2"})
  void allowsWhitespaceBetweenExpressionParts(String expression) {
    assertEquals(11, evaluator.evaluate(expression), TOLERANCE);
  }

  @Test
  void acceptsFiniteDoubleExtremesAndUnderflowToZero() {
    assertEquals(Double.MAX_VALUE, evaluator.evaluate("1.7976931348623157e308"));
    assertEquals(Double.MIN_VALUE, evaluator.evaluate("4.9e-324"));
    // double kann sehr kleine endliche Dezimalzahlen nur noch als null darstellen.
    assertEquals(0.0, evaluator.evaluate("1e-400"));
  }

  @Test
  void preservesSignedZeroThroughUnarySigns() {
    assertEquals(Double.doubleToLongBits(-0.0), Double.doubleToLongBits(evaluator.evaluate("-0.0")));
    assertEquals(Double.doubleToLongBits(0.0), Double.doubleToLongBits(evaluator.evaluate("--0.0")));
    assertEquals(Double.doubleToLongBits(-0.0), Double.doubleToLongBits(evaluator.evaluate("+(-0.0)")));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t\n\r"})
  void rejectsNullAndEmptyExpressionsExplicitly(String expression) {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> evaluator.evaluate(expression));

    assertTrue(exception.getMessage().contains(expression == null ? "null" : "leer"));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "1 2", "1 .2", ".5 .6", "1e 3", "1 e3", "1e- 3",
      "1e", "1e+", "1e-", "1e--2", "1e+-2", ".", ".e2", "1..2", "1.2.3",
      "()", "( )", "(", ")", "(1+2", "1+2)", "((1)", "1+", "*1", "1/*2", "1+()",
      "2(3)", "(2)3", "(2)(3)", "1,5", "1_000", "0x1.0p0", "1d",
      "NaN", "Infinity", "-Infinity", "pi", "e", "ans", "sqrt(4)", "1;2", "1**2", "2^3", "1=1"
  })
  void rejectsMalformedOrUnsupportedExpressionsWithoutIgnoringRemainingInput(String expression) {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> evaluator.evaluate(expression));

    assertTrue(exception.getMessage().contains("Position"));
  }

  @ParameterizedTest
  @CsvSource({
      "1 2, 3", "'1,2', 2", "1+(2*), 6", "(), 2", "(1+2, 5",
      "1+2), 4", "1e+, 4", "'  1+?', 5", "1.2.3, 4", "1(2), 2"
  })
  void reportsOneBasedPositionsInTheOriginalInput(String expression, int expectedPosition) {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> evaluator.evaluate(expression));

    assertTrue(exception.getMessage().contains("(Position " + expectedPosition + ")."), exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(strings = {"1e309", "-1e309", "0*1e309", "1e309-1e309"})
  void rejectsNonFiniteNumericLiterals(String expression) {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> evaluator.evaluate(expression));

    assertTrue(exception.getMessage().contains("endlich"));
    assertTrue(exception.getMessage().contains("Position"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"1/0", "1/-0", "1/(-0.0)", "1/(2-2)", "0/0"})
  void propagatesTheEngineDivisionByZeroError(String expression) {
    ArithmeticException exception = assertThrows(ArithmeticException.class, () -> evaluator.evaluate(expression));

    assertEquals("Division durch null ist nicht erlaubt.", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "1e308+1e308", "-1e308-1e308", "1e308*2", "1e308/0.5",
      "(1e308+1e308)-1e308", "(1e308*2)/2", "0*(1e308*2)"
  })
  void rejectsNonFiniteIntermediateResultsUsingTheEngineError(String expression) {
    ArithmeticException exception = assertThrows(ArithmeticException.class, () -> evaluator.evaluate(expression));

    assertEquals("Die Berechnung liefert kein endliches Ergebnis (NaN oder Infinity).", exception.getMessage());
  }

  @Test
  void acceptsExactly256CharactersIncludingWhitespace() {
    String expression = " ".repeat(127) + "1" + " ".repeat(128);

    assertEquals(256, expression.length());
    assertEquals(1, evaluator.evaluate(expression));
  }

  @Test
  void rejectsThe257thCharacterIncludingWhitespace() {
    String expression = "1" + " ".repeat(256);

    assertEquals(257, expression.length());
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> evaluator.evaluate(expression));
    assertTrue(exception.getMessage().contains("höchstens 256 Zeichen"));
    assertTrue(exception.getMessage().contains("Position 257"));
  }

  @Test
  void checksExpressionLengthBeforeParsingOrCalculating() {
    String expression = "1/0" + " ".repeat(254);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> evaluator.evaluate(expression));
    assertTrue(exception.getMessage().contains("höchstens 256 Zeichen"));
  }

  @ParameterizedTest
  @CsvSource({"+, 51", "-, -49", "*, 1", "/, 1"})
  void acceptsExactlyFiftyBinaryOperators(String operator, double expected) {
    assertEquals(expected, evaluator.evaluate("1" + (operator + "1").repeat(50)));
  }

  @ParameterizedTest
  @ValueSource(strings = {"+", "-", "*", "/"})
  void rejectsTheFiftyFirstBinaryOperator(String operator) {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> evaluator.evaluate("1" + (operator + "1").repeat(51)));

    assertTrue(exception.getMessage().contains("höchstens 50 Operatoren"));
    assertTrue(exception.getMessage().contains("Position 102"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"+", "-"})
  void acceptsExactlyFiftyUnarySigns(String sign) {
    assertEquals(2, evaluator.evaluate(sign.repeat(50) + "2"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"+", "-"})
  void rejectsTheFiftyFirstUnarySign(String sign) {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> evaluator.evaluate(sign.repeat(51) + "2"));

    assertTrue(exception.getMessage().contains("höchstens 50 Operatoren"));
    assertTrue(exception.getMessage().contains("Position 51"));
  }

  @Test
  void countsBinaryOperatorsAndUnarySignsTogether() {
    String expression = "+".repeat(25) + "1" + "+1".repeat(25);

    assertEquals(26, evaluator.evaluate(expression));
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> evaluator.evaluate(expression + "+1"));
    assertTrue(exception.getMessage().contains("höchstens 50 Operatoren"));
  }

  @ParameterizedTest
  @CsvSource({"1e-3, 50.001", "1e+0, 51"})
  void doesNotCountScientificExponentSignsAsOperators(String number, double expected) {
    assertEquals(expected, evaluator.evaluate(number + "+1".repeat(50)), TOLERANCE);
  }

  @Test
  void acceptsExactlySixteenOpenParenthesisLevels() {
    assertEquals(1, evaluator.evaluate("(".repeat(16) + "1" + ")".repeat(16)));
  }

  @Test
  void rejectsTheSeventeenthOpenParenthesisLevel() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> evaluator.evaluate("(".repeat(17) + "1" + ")".repeat(17)));

    assertTrue(exception.getMessage().contains("höchstens 16 Klammerebenen"));
    assertTrue(exception.getMessage().contains("Position 17"));
  }

  @Test
  void countsOnlySimultaneouslyOpenParentheses() {
    assertEquals(51, evaluator.evaluate("(1)+".repeat(50) + "(1)"));
  }

  @Test
  void acceptsAllThreeLimitsAtTheSameTime() {
    String expression = "(".repeat(16) + "1" + "+1".repeat(50) + ")".repeat(16);
    expression += " ".repeat(256 - expression.length());

    assertEquals(256, expression.length());
    assertEquals(51, evaluator.evaluate(expression));
  }

  @Test
  void canBeReusedAfterSuccessfulEvaluationsAtTheLimits() {
    String expression = "(".repeat(16) + "1" + "+1".repeat(50) + ")".repeat(16);

    assertEquals(51, evaluator.evaluate(expression));
    assertEquals(51, evaluator.evaluate(expression));
    assertEquals(6, evaluator.evaluate("2*3"));
  }

  @Test
  void canBeReusedAfterSyntaxCalculationAndLimitErrors() {
    assertEquals(5, evaluator.evaluate("2+3"));
    assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate("1+"));
    assertEquals(42, evaluator.evaluate("6*7"));
    assertThrows(ArithmeticException.class, () -> evaluator.evaluate("1/0"));
    assertEquals(4, evaluator.evaluate("8/2"));
    assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate("1e309"));
    assertEquals(-3, evaluator.evaluate("-3"));
    assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate("(".repeat(17) + "1"));
    assertEquals(5, evaluator.evaluate("(2+3)"));
    assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate("+".repeat(51) + "1"));
    assertEquals(2, evaluator.evaluate("1+1"));
    assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate("1" + " ".repeat(256)));
    assertEquals(3, evaluator.evaluate("1+2"));
    assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(null));
    assertEquals(4, evaluator.evaluate("2+2"));
  }
}
