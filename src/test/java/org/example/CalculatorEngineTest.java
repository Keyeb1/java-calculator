package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculatorEngineTest {

  private final CalculatorEngine engine = new CalculatorEngine();
  private static final double TOLERANCE = 1e-12;

  @ParameterizedTest
  @CsvSource({"2, 3, 5", "-2, 2, 0", "0.1, 0.2, 0.3"})
  void addsNumbers(double first, double second, double expected) {
    assertEquals(expected, engine.add(first, second), TOLERANCE);
  }

  @ParameterizedTest
  @CsvSource({"7, 3, 4", "2, 5, -3", "0.5, 0.25, 0.25"})
  void subtractsNumbers(double first, double second, double expected) {
    assertEquals(expected, engine.subtract(first, second), TOLERANCE);
  }

  @ParameterizedTest
  @CsvSource({"3, 4, 12", "-2, 3, -6", "0, 5, 0", "0.5, 0.25, 0.125"})
  void multipliesNumbers(double first, double second, double expected) {
    assertEquals(expected, engine.multiply(first, second), TOLERANCE);
  }

  @ParameterizedTest
  @CsvSource({"8, 2, 4", "-6, 2, -3", "1, 4, 0.25", "0, 5, 0"})
  void dividesNumbers(double dividend, double divisor, double expected) {
    assertEquals(expected, engine.divide(dividend, divisor), TOLERANCE);
  }

  @ParameterizedTest
  @CsvSource({"9, 3", "0, 0", "2, 1.4142135623730951"})
  void calculatesSquareRoots(double number, double expected) {
    assertEquals(expected, engine.sqrt(number), TOLERANCE);
  }

  @ParameterizedTest
  @CsvSource({
      "25, 2, 5", "27, 3, 3", "16, 4, 2", "32, 5, 2",
      "-27, 3, -3", "-32, 5, -2", "0, 3, 0", "0.125, 3, 0.5",
      "81, 1, 81", "-81, 1, -81"
  })
  void calculatesNthRoots(double number, double root, double expected) {
    assertEquals(expected, engine.nthRoot(number, root), TOLERANCE);
  }

  @ParameterizedTest
  @ValueSource(doubles = {0, -0.0, -3, 2.5, 0.5, Double.MIN_VALUE, 2.9999999999999996, 3.0000000000000004})
  void rejectsNonPositiveOrFractionalRootExponents(double root) {
    ArithmeticException exception = assertThrows(ArithmeticException.class, () -> engine.nthRoot(27, root));
    assertEquals("Der Wurzelexponent muss eine positive ganze Zahl sein.", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(doubles = {2, 4, 2147483648d, 9007199254740994d, 9223372036854775808d, Double.MAX_VALUE})
  void rejectsEvenRootsOfNegativeNumbers(double root) {
    ArithmeticException exception = assertThrows(ArithmeticException.class, () -> engine.nthRoot(-16, root));
    assertEquals("Eine gerade Wurzel aus einer negativen Zahl ist nicht erlaubt.", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(doubles = {2147483649d, 9007199254740991d})
  void acceptsLargeOddRootsOfNegativeNumbers(double root) {
    assertEquals(-1, engine.nthRoot(-1, root), TOLERANCE);
  }

  @ParameterizedTest
  @ValueSource(doubles = {9007199254740992d, 9223372036854775808d, Double.MAX_VALUE})
  void acceptsVeryLargeRootExponentsWithoutIntegerConversion(double root) {
    assertEquals(1, engine.nthRoot(16, root), TOLERANCE);
    assertEquals(0, engine.nthRoot(0, root), TOLERANCE);
  }

  @Test
  void usesTheRepresentedParityWhenAnOddRootExponentIsRoundedAtTwoToThe53() {
    // 2^53 + 1 ist als double nicht darstellbar und wird bereits beim Einlesen gerade.
    double roundedRoot = Double.parseDouble("9007199254740993");

    assertEquals(9007199254740992d, roundedRoot);
    assertThrows(ArithmeticException.class, () -> engine.nthRoot(-1, roundedRoot));
  }

  @ParameterizedTest
  @CsvSource({"5, 5", "-5, 5", "0, 0", "-0.0, 0.0", "2.75, 2.75", "-2.75, 2.75"})
  void calculatesAbsoluteValues(double number, double expected) {
    assertEquals(expected, engine.abs(number));
  }

  @Test
  void calculatesSineOfZero() {
    assertEquals(0, engine.sin(0), TOLERANCE);
  }

  @Test
  void calculatesSineOfHalfPiInRadians() {
    assertEquals(1, engine.sin(Math.PI / 2), TOLERANCE);
  }

  @Test
  void calculatesSineOfPiInRadians() {
    assertEquals(0, engine.sin(Math.PI), TOLERANCE);
  }

  @Test
  void calculatesCosineOfZero() {
    assertEquals(1, engine.cos(0), TOLERANCE);
  }

  @Test
  void calculatesCosineOfHalfPiInRadians() {
    assertEquals(0, engine.cos(Math.PI / 2), TOLERANCE);
  }

  @Test
  void calculatesCosineOfPiInRadians() {
    assertEquals(-1, engine.cos(Math.PI), TOLERANCE);
  }

  @Test
  void calculatesTangentOfZero() {
    assertEquals(0, engine.tan(0), TOLERANCE);
  }

  @Test
  void calculatesTangentOfQuarterPiInRadians() {
    assertEquals(1, engine.tan(Math.PI / 4), TOLERANCE);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 3, 5, 11, -1, -3, -5, -11})
  void rejectsUsualTangentSingularities(int oddMultiple) {
    double angle = oddMultiple * Math.PI / 2;

    ArithmeticException exception = assertThrows(ArithmeticException.class, () -> engine.tan(angle));
    assertEquals("Tangens ist für diesen Winkel nicht definiert.", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 3, 11, -1, -3, -11})
  void acceptsRepresentableAnglesNextToTangentSingularities(int oddMultiple) {
    double singularity = oddMultiple * Math.PI / 2;
    double below = Math.nextDown(singularity);
    double above = Math.nextUp(singularity);

    double expectedBelow = Math.tan(below);
    double expectedAbove = Math.tan(above);
    assertEquals(expectedBelow, engine.tan(below), 2 * Math.ulp(expectedBelow));
    assertEquals(expectedAbove, engine.tan(above), 2 * Math.ulp(expectedAbove));
  }

  @ParameterizedTest
  @CsvSource({"1, 0", "10, 1", "100, 2", "1000, 3", "0.1, -1", "0.01, -2"})
  void calculatesDecimalLogarithms(double number, double expected) {
    assertEquals(expected, engine.log(number), TOLERANCE);
  }

  @ParameterizedTest
  @CsvSource({"1, 0", "0.5, -0.6931471805599453"})
  void calculatesNaturalLogarithms(double number, double expected) {
    assertEquals(expected, engine.ln(number), TOLERANCE);
  }

  @Test
  void calculatesNaturalLogarithmOfE() {
    assertEquals(1, engine.ln(Math.E), TOLERANCE);
  }

  @ParameterizedTest
  @ValueSource(doubles = {0.0, -0.0, -1, -10})
  void rejectsNonPositiveLogarithmOperands(double number) {
    assertThrows(ArithmeticException.class, () -> engine.log(number));
    assertThrows(ArithmeticException.class, () -> engine.ln(number));
  }

  @ParameterizedTest
  @CsvSource({"0, 1", "-0.0, 1", "1, 1", "2, 2", "4, 24", "5, 120", "10, 3628800", "20, 2432902008176640000"})
  void calculatesFactorials(double number, long expected) {
    assertEquals((double) expected, engine.factorial(number));
  }

  @ParameterizedTest
  @ValueSource(doubles = {-1, -5, -0.5})
  void rejectsNegativeFactorialOperands(double number) {
    assertThrows(ArithmeticException.class, () -> engine.factorial(number));
  }

  @ParameterizedTest
  @ValueSource(doubles = {0.5, 1.5, 19.5, 20.5})
  void rejectsFractionalFactorialOperands(double number) {
    assertThrows(ArithmeticException.class, () -> engine.factorial(number));
  }

  @ParameterizedTest
  @ValueSource(doubles = {21, 100, Double.MAX_VALUE})
  void rejectsFactorialOperandsGreaterThanTwenty(double number) {
    ArithmeticException exception = assertThrows(ArithmeticException.class, () -> engine.factorial(number));
    assertEquals("Für die Fakultät sind nur Werte von 0 bis 20 erlaubt.", exception.getMessage());
  }

  @ParameterizedTest
  @CsvSource({"2, 3, 8", "-2, 3, -8", "5, 0, 1", "2, -2, 0.25", "9, 0.5, 3", "0, 0, 1"})
  void calculatesPowers(double base, double exponent, double expected) {
    assertEquals(expected, engine.power(base, exponent), TOLERANCE);
  }

  @ParameterizedTest
  @ValueSource(doubles = {0.0, -0.0})
  void rejectsDivisionByZero(double divisor) {
    assertThrows(ArithmeticException.class, () -> engine.divide(5, divisor));
  }

  @ParameterizedTest
  @ValueSource(doubles = {-1, -9})
  void rejectsSquareRootsOfNegativeNumbers(double number) {
    assertThrows(ArithmeticException.class, () -> engine.sqrt(number));
  }

  @ParameterizedTest
  @CsvSource({"-2, 0.5", "0, -1"})
  void rejectsPowersWithNonFiniteResults(double base, double exponent) {
    assertThrows(ArithmeticException.class, () -> engine.power(base, exponent));
  }

  @Test
  void rejectsOverflowingResults() {
    assertThrows(ArithmeticException.class, () -> engine.add(Double.MAX_VALUE, Double.MAX_VALUE));
    assertThrows(ArithmeticException.class, () -> engine.subtract(-Double.MAX_VALUE, Double.MAX_VALUE));
    assertThrows(ArithmeticException.class, () -> engine.multiply(Double.MAX_VALUE, 2));
    assertThrows(ArithmeticException.class, () -> engine.divide(Double.MAX_VALUE, 0.5));
    assertThrows(ArithmeticException.class, () -> engine.power(Double.MAX_VALUE, 2));
  }

  @ParameterizedTest
  @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
  void rejectsNonFiniteOperands(double operand) {
    assertThrows(IllegalArgumentException.class, () -> engine.add(operand, 1));
    assertThrows(IllegalArgumentException.class, () -> engine.add(1, operand));
    assertThrows(IllegalArgumentException.class, () -> engine.subtract(operand, 1));
    assertThrows(IllegalArgumentException.class, () -> engine.subtract(1, operand));
    assertThrows(IllegalArgumentException.class, () -> engine.multiply(operand, 1));
    assertThrows(IllegalArgumentException.class, () -> engine.multiply(1, operand));
    assertThrows(IllegalArgumentException.class, () -> engine.divide(operand, 1));
    assertThrows(IllegalArgumentException.class, () -> engine.divide(1, operand));
    assertThrows(IllegalArgumentException.class, () -> engine.sqrt(operand));
    assertThrows(IllegalArgumentException.class, () -> engine.nthRoot(operand, 3));
    assertThrows(IllegalArgumentException.class, () -> engine.nthRoot(27, operand));
    assertThrows(IllegalArgumentException.class, () -> engine.abs(operand));
    assertThrows(IllegalArgumentException.class, () -> engine.sin(operand));
    assertThrows(IllegalArgumentException.class, () -> engine.cos(operand));
    assertThrows(IllegalArgumentException.class, () -> engine.tan(operand));
    assertThrows(IllegalArgumentException.class, () -> engine.log(operand));
    assertThrows(IllegalArgumentException.class, () -> engine.ln(operand));
    assertThrows(IllegalArgumentException.class, () -> engine.factorial(operand));
    assertThrows(IllegalArgumentException.class, () -> engine.power(operand, 1));
    assertThrows(IllegalArgumentException.class, () -> engine.power(1, operand));
  }
}
