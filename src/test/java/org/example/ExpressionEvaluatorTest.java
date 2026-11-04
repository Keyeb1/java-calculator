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
      "NaN", "Infinity", "-Infinity", "tau", "answer", "ans", "square(4)", "1;2", "1**2", "2^3", "1=1"
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

  @ParameterizedTest
  @ValueSource(strings = {"pi", "PI", "Pi", "pI"})
  void acceptsPiRegardlessOfCase(String expression) {
    assertEquals(Math.PI, evaluator.evaluate(expression));
  }

  @ParameterizedTest
  @ValueSource(strings = {"e", "E"})
  void acceptsEulerNumberRegardlessOfCase(String expression) {
    assertEquals(Math.E, evaluator.evaluate(expression));
  }

  @ParameterizedTest
  @CsvSource(value = {
      "2*pi | 6.283185307179586", "PI+e | 5.859874482048838",
      "1e-3+e | 2.719281828459045", "e-3 | -0.2817181715409549",
      "E+3 | 5.718281828459045", "2e3 | 2000", "2*e | 5.43656365691809",
      "power(1e-3, 2) | 0.000001", "power(e, 1) | 2.718281828459045"
  }, delimiter = '|')
  void distinguishesConstantsFromScientificNumbers(String expression, double expected) {
    assertEquals(expected, evaluator.evaluate(expression), TOLERANCE);
  }

  @ParameterizedTest
  @CsvSource(value = {
      "sqrt(9) | 3", "abs(-2.75) | 2.75", "sin(90) | 1", "cos(60) | 0.5",
      "tan(45) | 1", "log(100) | 2", "ln(e) | 1", "factorial(5) | 120",
      "factorial(0) | 1", "power(2, 3) | 8", "root(27, 3) | 3", "root(-27, 3) | -3"
  }, delimiter = '|')
  void evaluatesEveryScientificFunction(String expression, double expected) {
    assertEquals(expected, evaluator.evaluate(expression), TOLERANCE);
  }

  @ParameterizedTest
  @CsvSource(value = {
      "sqrt(4+5) | 3", "power(2, 1+2) | 8", "sqrt(abs(-9)) | 3",
      "sin(30+60) | 1", "cos(120/2) | 0.5", "tan(180/4) | 1",
      "root(power(2, 6), 1+2) | 4", "factorial(2+3) | 120",
      "ln(power(e, 2)) | 2", "log(root(10000, 2)) | 2",
      "power(2, power(2, 2)) | 16", "power(2, 3)+root(16, 2)*sqrt(9) | 20",
      "sqrt ( abs ( -9 ) ) | 3"
  }, delimiter = '|')
  void acceptsCompleteAndNestedExpressionsAsArguments(String expression, double expected) {
    assertEquals(expected, evaluator.evaluate(expression), TOLERANCE);
  }

  @ParameterizedTest
  @ValueSource(strings = {"sqrt", "abs", "sin", "cos", "tan", "log", "ln", "factorial"})
  void requiresExactlyOneNonEmptyArgument(String function) {
    for (String arguments : new String[] {"()", "(1,2)", "(,1)", "(1,)"}) {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
          () -> evaluator.evaluate(function + arguments));

      assertTrue(exception.getMessage().contains("'" + function + "' erwartet genau 1"));
      assertTrue(exception.getMessage().contains("Position"));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"power", "root"})
  void requiresExactlyTwoNonEmptyArguments(String function) {
    for (String arguments : new String[] {"()", "(1)", "(1,)", "(,2)", "(1,,2)", "(1,2,3)", "(1,2,)"}) {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
          () -> evaluator.evaluate(function + arguments));

      assertTrue(exception.getMessage().contains("'" + function + "' erwartet genau 2"));
      assertTrue(exception.getMessage().contains("Position"));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"sqrt(1,1/0)", "power(1,2,1/0)"})
  void rejectsExtraArgumentsBeforeEvaluatingThem(String expression) {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> evaluator.evaluate(expression));

    assertTrue(exception.getMessage().contains("erwartet genau"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"sqrt 9", "sqrt(9", "sqrt((9)", "power(2 3)", "power(2,3",
      "sqrt(abs(-9)", "sin(90))", "power(2,3)foo", "power((2,3),4)", "sqrt(1 2)"})
  void rejectsMalformedFunctionCallsAndRemainingInput(String expression) {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> evaluator.evaluate(expression));

    assertTrue(exception.getMessage().contains("Position"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"tau", "answer", "square(4)", "PIE", "Sin(90)", "SIN(90)",
      "Sqrt(4)", "POWER(2,3)", "Ans", "ANS", "sin90", "mode", "pi_2"})
  void rejectsUnknownNamesAndUppercaseFunctionOrAnsNames(String expression) {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> evaluator.evaluate(expression, AngleMode.DEG, 5.0));

    assertTrue(exception.getMessage().contains("Unbekannter Name"));
    assertTrue(exception.getMessage().contains("Position"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"2pi", "2e", "2 e", "pi e", "2(3)", "2sin(90)",
      "sin(90)cos(0)", "pi(2)", "(2)pi", "3ans"})
  void rejectsImplicitMultiplication(String expression) {
    assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(expression, AngleMode.DEG, 5.0));
  }

  @ParameterizedTest
  @CsvSource(value = {
      "sqrt(-1) | Die Quadratwurzel einer negativen Zahl ist nicht erlaubt.",
      "log(0) | Der dekadische Logarithmus ist nur für Zahlen größer als null definiert.",
      "ln(-1) | Der natürliche Logarithmus ist nur für Zahlen größer als null definiert.",
      "factorial(-1) | Die Fakultät ist nur für nichtnegative ganze Zahlen definiert.",
      "factorial(0.5) | Die Fakultät ist nur für nichtnegative ganze Zahlen definiert.",
      "factorial(21) | Für die Fakultät sind nur Werte von 0 bis 20 erlaubt.",
      "root(-16,4) | Eine gerade Wurzel aus einer negativen Zahl ist nicht erlaubt.",
      "root(27,0) | Der Wurzelexponent muss eine positive ganze Zahl sein.",
      "root(27,2.5) | Der Wurzelexponent muss eine positive ganze Zahl sein.",
      "power(-2,0.5) | Die Berechnung liefert kein endliches Ergebnis (NaN oder Infinity).",
      "power(1e308,2) | Die Berechnung liefert kein endliches Ergebnis (NaN oder Infinity)."
  }, delimiter = '|')
  void preservesEngineDomainAndOverflowErrorsForFunctions(String expression, String expectedMessage) {
    ArithmeticException exception = assertThrows(ArithmeticException.class, () -> evaluator.evaluate(expression));

    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  void requiresAnExplicitlyProvidedResultForAns() {
    assertEquals(5, evaluator.evaluate("2+3"));
    IllegalArgumentException defaultError = assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate("ans"));
    IllegalArgumentException nullError = assertThrows(IllegalArgumentException.class,
        () -> evaluator.evaluate("ans", AngleMode.DEG, null));

    assertTrue(defaultError.getMessage().contains("kein letztes Ergebnis"));
    assertEquals(defaultError.getMessage(), nullError.getMessage());
  }

  @ParameterizedTest
  @ValueSource(doubles = {0.0, -0.0, 5.0, -2.75, Double.MIN_VALUE, Double.MAX_VALUE})
  void acceptsFiniteAnsIncludingBothSignedZeros(double lastResult) {
    assertEquals(Double.doubleToLongBits(lastResult),
        Double.doubleToLongBits(evaluator.evaluate("ans", AngleMode.DEG, lastResult)));
  }

  @ParameterizedTest
  @ValueSource(doubles = {0.0, -0.0})
  void appliesDivisionByZeroProtectionToAns(double lastResult) {
    ArithmeticException exception = assertThrows(ArithmeticException.class,
        () -> evaluator.evaluate("1/ans", AngleMode.DEG, lastResult));

    assertEquals("Division durch null ist nicht erlaubt.", exception.getMessage());
  }

  @Test
  void usesTheSameProvidedAnsThroughoutTheExpressionAndInFunctions() {
    assertEquals(11, evaluator.evaluate("ans+1+ans", AngleMode.DEG, 5.0));
    assertEquals(26, evaluator.evaluate("power(ans,2)+1", AngleMode.RAD, 5.0));
    assertEquals(5, evaluator.evaluate("ans", AngleMode.DEG, 5.0));
  }

  @ParameterizedTest
  @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
  void rejectsNonFiniteLastResultsEvenWhenAnsIsUnused(double lastResult) {
    for (String expression : new String[] {"ans", "1"}) {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
          () -> evaluator.evaluate(expression, AngleMode.DEG, lastResult));

      assertTrue(exception.getMessage().contains("letzte Ergebnis muss eine endliche Zahl sein"));
    }
  }

  @Test
  void rejectsAMissingAngleMode() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> evaluator.evaluate("1", null, null));

    assertTrue(exception.getMessage().contains("Winkelmodus"));
  }

  @ParameterizedTest
  @CsvSource(value = {"sin(90) | DEG | 1", "sin(pi/2) | RAD | 1", "cos(60) | DEG | 0.5",
      "cos(pi) | RAD | -1", "tan(45) | DEG | 1", "tan(pi/4) | RAD | 1"}, delimiter = '|')
  void appliesTheSelectedAngleMode(String expression, AngleMode mode, double expected) {
    assertEquals(expected, evaluator.evaluate(expression, mode, null), TOLERANCE);
  }

  @Test
  void keepsContextsIndependentAfterSuccessfulAndFailedCalls() {
    assertEquals(1, evaluator.evaluate("sin(ans)", AngleMode.DEG, 90.0), TOLERANCE);
    assertEquals(1, evaluator.evaluate("sin(ans)", AngleMode.RAD, Math.PI / 2), TOLERANCE);
    assertEquals(1, evaluator.evaluate("sin(90)"), TOLERANCE);
    assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate("ans"));
    assertThrows(ArithmeticException.class, () -> evaluator.evaluate("tan(ans)", AngleMode.DEG, 90.0));
    assertEquals(Double.doubleToLongBits(-0.0),
        Double.doubleToLongBits(evaluator.evaluate("ans", AngleMode.RAD, -0.0)));
    assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate("ans", AngleMode.DEG, null));
    assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate("sqrt()", AngleMode.RAD, 7.0));
    assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate("1", AngleMode.DEG, Double.NaN));
    assertEquals(5, evaluator.evaluate("ans", AngleMode.RAD, 5.0));
    assertEquals(1, evaluator.evaluate("sin(90)"), TOLERANCE);
  }

  @ParameterizedTest
  @ValueSource(ints = {90, 270, 990, -90, -270, -990, 450, -450, 1350, -1350})
  void rejectsExactTangentSingularitiesInDegrees(int angle) {
    ArithmeticException exception = assertThrows(ArithmeticException.class,
        () -> evaluator.evaluate("tan(" + angle + ")"));

    assertEquals("Tangens ist für diesen Winkel nicht definiert.", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(ints = {90, 270, 990, 1350, 1890, -90, -270, -990, -1350, -1890})
  void acceptsTangentNeighborsInDegreesWithoutFalseSingularityErrors(int singularity) {
    for (double angle : new double[] {Math.nextDown((double) singularity), Math.nextUp((double) singularity)}) {
      double expected = 1.0 / Math.toRadians(singularity - angle);
      double actual = evaluator.evaluate("tan(" + angle + ")", AngleMode.DEG, null);

      assertEquals(expected, actual, Math.abs(expected) * TOLERANCE);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"89.999999999999999", "90.000000000000001"})
  void rejectsDegreeInputsAlreadyRoundedToAnExactSingularity(String angle) {
    assertEquals(90, Double.parseDouble(angle));
    assertThrows(ArithmeticException.class, () -> evaluator.evaluate("tan(" + angle + ")"));
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 3, 11, -1, -3, -11})
  void preservesTangentSingularityRulesInRadians(int oddMultiple) {
    double angle = oddMultiple * Math.PI / 2;
    ArithmeticException exception = assertThrows(ArithmeticException.class,
        () -> evaluator.evaluate("tan(" + angle + ")", AngleMode.RAD, null));

    assertEquals("Tangens ist für diesen Winkel nicht definiert.", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(strings = {"tan(pi/2)", "tan(3*pi/2)", "tan(-pi/2)"})
  void detectsTangentSingularitiesComputedFromPiInRadians(String expression) {
    assertThrows(ArithmeticException.class, () -> evaluator.evaluate(expression, AngleMode.RAD, null));
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 3, 11, -1, -3, -11})
  void preservesTangentNeighborsInRadians(int oddMultiple) {
    double singularity = oddMultiple * Math.PI / 2;
    for (double angle : new double[] {Math.nextDown(singularity), Math.nextUp(singularity)}) {
      double expected = Math.tan(angle);

      assertEquals(expected, evaluator.evaluate("tan(" + angle + ")", AngleMode.RAD, null), 2 * Math.ulp(expected));
    }
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

    assertTrue(exception.getMessage().contains("höchstens 50 Operationen"));
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

    assertTrue(exception.getMessage().contains("höchstens 50 Operationen"));
    assertTrue(exception.getMessage().contains("Position 51"));
  }

  @Test
  void countsBinaryOperatorsAndUnarySignsTogether() {
    String expression = "+".repeat(25) + "1" + "+1".repeat(25);

    assertEquals(26, evaluator.evaluate(expression));
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> evaluator.evaluate(expression + "+1"));
    assertTrue(exception.getMessage().contains("höchstens 50 Operationen"));
  }

  @ParameterizedTest
  @CsvSource({"1e-3, 50.001", "1e+0, 51"})
  void doesNotCountScientificExponentSignsAsOperators(String number, double expected) {
    assertEquals(expected, evaluator.evaluate(number + "+1".repeat(50)), TOLERANCE);
  }

  @Test
  void countsFunctionsBinaryOperatorsAndUnarySignsTogetherAtTheLimit() {
    String expression = "+" + "abs(1)+".repeat(24) + "abs(1)";

    assertEquals(25, evaluator.evaluate(expression));
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> evaluator.evaluate(expression + "+1"));
    assertTrue(exception.getMessage().contains("höchstens 50 Operationen"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"sqrt(1)", "abs(1)", "sin(1)", "cos(1)", "tan(1)",
      "log(1)", "ln(1)", "factorial(1)", "power(2,3)", "root(16,2)"})
  void countsEachFunctionCallExactlyOnce(String functionCall) {
    assertEquals(evaluator.evaluate(functionCall), evaluator.evaluate("+".repeat(49) + functionCall));
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> evaluator.evaluate("+".repeat(50) + functionCall));
    assertTrue(exception.getMessage().contains("höchstens 50 Operationen"));
  }

  @Test
  void doesNotCountScientificSignsInsideFunctionArgumentsAsOperations() {
    assertEquals(0.001, evaluator.evaluate("+".repeat(49) + "abs(1e-3)"), TOLERANCE);
  }

  @ParameterizedTest
  @CsvSource({"pi, 3.141592653589793", "e, 2.718281828459045", "ans, 0"})
  void doesNotCountConstantsOrAnsAsOperations(String name, double expected) {
    assertEquals(expected, evaluator.evaluate("+".repeat(50) + name, AngleMode.DEG, 0.0));
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
  void countsFunctionAndNormalParenthesesTogether() {
    String expression = "(".repeat(8) + "abs(".repeat(8) + "1" + ")".repeat(16);

    assertEquals(1, evaluator.evaluate(expression));
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> evaluator.evaluate("(" + expression + ")"));
    assertTrue(exception.getMessage().contains("höchstens 16 Klammerebenen"));
  }

  @Test
  void limitsPureFunctionNestingToSixteenLevels() {
    assertEquals(1, evaluator.evaluate("abs(".repeat(16) + "1" + ")".repeat(16)));
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> evaluator.evaluate("abs(".repeat(17) + "1" + ")".repeat(17)));
    assertTrue(exception.getMessage().contains("höchstens 16 Klammerebenen"));
  }

  @Test
  void closesArgumentParenthesesBeforeParsingTheNextArgument() {
    String first = "(".repeat(15) + "16" + ")".repeat(15);
    String second = "(".repeat(15) + "2" + ")".repeat(15);

    assertEquals(4, evaluator.evaluate("root(" + first + "," + second + ")"));
    assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate("root((" + first + "),2)"));
    assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate("root(16,(" + second + "))"));
  }

  @Test
  void acceptsAllThreeLimitsWithFunctionsAtTheSameTime() {
    String expression = "(".repeat(15) + "+".repeat(49) + "abs(1)" + ")".repeat(15);
    expression += " ".repeat(256 - expression.length());

    assertEquals(256, expression.length());
    assertEquals(1, evaluator.evaluate(expression));
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
