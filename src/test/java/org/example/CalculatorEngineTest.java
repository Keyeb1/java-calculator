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
  @ValueSource(ints = {1, 3, 5, 11, -1, -3})
  void rejectsUsualTangentSingularities(int oddMultiple) {
    double angle = oddMultiple * Math.PI / 2;

    ArithmeticException exception = assertThrows(ArithmeticException.class, () -> engine.tan(angle));
    assertEquals("Tangens ist für diesen Winkel nicht definiert.", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 3, -1, -3})
  void acceptsRepresentableAnglesNextToTangentSingularities(int oddMultiple) {
    double singularity = oddMultiple * Math.PI / 2;
    double below = Math.nextDown(singularity);
    double above = Math.nextUp(singularity);

    assertEquals(Math.tan(below), engine.tan(below), TOLERANCE);
    assertEquals(Math.tan(above), engine.tan(above), TOLERANCE);
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
    assertThrows(IllegalArgumentException.class, () -> engine.abs(operand));
    assertThrows(IllegalArgumentException.class, () -> engine.sin(operand));
    assertThrows(IllegalArgumentException.class, () -> engine.cos(operand));
    assertThrows(IllegalArgumentException.class, () -> engine.tan(operand));
    assertThrows(IllegalArgumentException.class, () -> engine.power(operand, 1));
    assertThrows(IllegalArgumentException.class, () -> engine.power(1, operand));
  }
}
