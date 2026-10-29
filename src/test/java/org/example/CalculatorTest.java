package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculatorTest {

  private static final double TOLERANCE = 1e-12;
  private static final String EOF_MESSAGE = "Eingabe beendet. Taschenrechner wird beendet.";

  @Test
  void additionWorks() {
    assertEquals(4, 2 + 2);
  }

  @Test
  void continuesRepeatedlyWithThePreviousResult() {
    String output = runCalculator("""
        2
        +
        7
        1
        sqrt
        1
        power
        2
        1
        *
        2
        1
        -
        6
        1
        /
        3
        1
        +
        1
        3
        """);

    assertEquals(List.of("9", "3", "9", "18", "12", "4", "5"), resultsIn(output));
    assertEquals(1, occurrencesOf(output, "Gib die erste Zahl ein: "));
    assertEquals(5, occurrencesOf(output, "Gib die zweite Zahl ein: "));
    assertEquals(1, occurrencesOf(output, "Gib den Exponent ein: "));
    assertEquals(6, occurrencesOf(output, "Aktuelles Ergebnis: "));
  }

  @Test
  void displaysTheCurrentResultBeforeTheNextOperatorSelection() {
    String output = runCalculator("10\n+\n5\n1\n*\n2\n3\n");

    assertEquals(List.of("15", "30"), resultsIn(output));
    assertTrue(output.contains("Aktuelles Ergebnis: 15" + System.lineSeparator()
        + "Wähle einen Operator aus"));
  }

  @Test
  void displaysFractionalResultsWhenContinuing() {
    String output = runCalculator("1\n/\n4\n1\n+\n0.5\n3\n");

    assertEquals(List.of("0.25", "0.75"), resultsIn(output));
    assertTrue(output.contains("Aktuelles Ergebnis: 0.25" + System.lineSeparator()
        + "Wähle einen Operator aus"));
  }

  @Test
  void startsANewCalculationWhenRequested() {
    String output = runCalculator("2\n+\n3\n2\n10\n-\n4\n3\n");

    assertEquals(List.of("5", "6"), resultsIn(output));
    assertEquals(2, occurrencesOf(output, "Gib die erste Zahl ein: "));
    assertFalse(output.contains("Aktuelles Ergebnis: "));
  }

  @Test
  void exitsAfterASuccessfulCalculation() {
    String output = runCalculator("2\n+\n3\n3\n10\n+\n20\n3\n");

    assertEquals(List.of("5"), resultsIn(output));
    assertEquals(1, occurrencesOf(output, "Was möchtest du als Nächstes tun?"));
    assertEquals(1, occurrencesOf(output, "Gib die erste Zahl ein: "));
    assertFalse(output.contains(EOF_MESSAGE));
  }

  @Test
  void redisplaysTheMenuUntilAValidChoiceIsEntered() {
    String output = runCalculator("2\n+\n3\nabc\n0\n4\n1.5\n1\n+\n1\n3\n");

    assertEquals(List.of("5", "6"), resultsIn(output));
    assertEquals(4, occurrencesOf(output, "Bitte wähle 1, 2 oder 3."));
    assertEquals(6, occurrencesOf(output, "Was möchtest du als Nächstes tun?"));
  }

  @Test
  void keepsTheLastResultAfterAFailedContinuation() {
    String output = runCalculator("2\n+\n3\n1\n/\n0\n1\n*\n2\n3\n");

    assertEquals(List.of("5", "10"), resultsIn(output));
    assertTrue(output.contains("Fehler: Division durch null ist nicht erlaubt."));
    assertEquals(1, occurrencesOf(output, "Gib die erste Zahl ein: "));
    assertEquals(2, occurrencesOf(output, "Aktuelles Ergebnis: 5"));
  }

  @Test
  void canStartANewCalculationAfterAFailedContinuation() {
    String output = runCalculator("2\n+\n3\n1\n/\n0\n2\n7\n+\n1\n3\n");

    assertEquals(List.of("5", "8"), resultsIn(output));
    assertEquals(2, occurrencesOf(output, "Gib die erste Zahl ein: "));
    assertTrue(output.contains("Fehler: Division durch null ist nicht erlaubt."));
  }

  @Test
  void canExitAfterAFailedContinuation() {
    String output = runCalculator("2\n+\n3\n1\n/\n0\n3\n");

    assertEquals(List.of("5"), resultsIn(output));
    assertTrue(output.contains("Fehler: Division durch null ist nicht erlaubt."));
    assertEquals(2, occurrencesOf(output, "Was möchtest du als Nächstes tun?"));
  }

  @Test
  void rejectsContinuationUntilAValidResultExists() {
    String output = runCalculator("5\n/\n0\n1\n2\n6\n+\n2\n3\n");

    assertEquals(List.of("8"), resultsIn(output));
    String beforeFirstResult = output.substring(0, output.indexOf("Das Ergebnis ist: 8"));
    assertFalse(beforeFirstResult.contains("1. Mit dem Ergebnis weiterrechnen"));
    assertTrue(beforeFirstResult.contains("Bitte wähle 2 oder 3."));
    assertEquals(2, occurrencesOf(output, "Gib die erste Zahl ein: "));
  }

  @Test
  void canExitAfterTheFirstCalculationFails() {
    String output = runCalculator("-1\nsqrt\n3\n");

    assertTrue(resultsIn(output).isEmpty());
    assertTrue(output.contains("Fehler: Die Quadratwurzel einer negativen Zahl ist nicht erlaubt."));
    assertFalse(output.contains("1. Mit dem Ergebnis weiterrechnen"));
    assertFalse(output.contains(EOF_MESSAGE));
  }

  @Test
  void discardsThePreviousResultWhenANewCalculationFails() {
    String output = runCalculator("2\n+\n3\n2\n-1\nsqrt\n1\n2\n6\n+\n2\n3\n");

    assertEquals(List.of("5", "8"), resultsIn(output));
    assertTrue(output.contains("Fehler: Die Quadratwurzel einer negativen Zahl ist nicht erlaubt."));
    assertEquals(3, occurrencesOf(output, "Gib die erste Zahl ein: "));
    String afterFailure = output.substring(output.indexOf("Fehler:"), output.indexOf("Das Ergebnis ist: 8"));
    assertFalse(afterFailure.contains("1. Mit dem Ergebnis weiterrechnen"));
    assertFalse(afterFailure.contains("Aktuelles Ergebnis: "));
    assertTrue(afterFailure.contains("Bitte wähle 2 oder 3."));
  }

  @Test
  void canContinueWithAZeroResult() {
    String output = runCalculator("2\n-\n2\n1\n+\n3\n3\n");

    assertEquals(List.of("0", "3"), resultsIn(output));
    assertEquals(1, occurrencesOf(output, "Gib die erste Zahl ein: "));
    assertTrue(output.contains("Aktuelles Ergebnis: 0" + System.lineSeparator()));
  }

  @Test
  void keepsTheLastResultAfterANonFiniteOperand() {
    String output = runCalculator("2\n+\n3\n1\n+\nNaN\n1\n*\n2\n3\n");

    assertEquals(List.of("5", "10"), resultsIn(output));
    assertTrue(output.contains("Fehler: Operanden müssen endliche Zahlen sein"));
  }

  @Test
  void keepsTheLastResultAfterAnOverflow() {
    String output = runCalculator("2\n+\n3\n1\npower\n1000\n1\n+\n1\n3\n");

    assertEquals(List.of("5", "6"), resultsIn(output));
    assertTrue(output.contains("Fehler: Die Berechnung liefert kein endliches Ergebnis"));
  }

  @Test
  void calculatesAbsoluteValuesAndContinuesWithThePreviousResult() {
    String output = runCalculator("-5\nabs\n1\n-\n8\n1\nabs\n3\n");

    assertEquals(List.of("5", "-3", "3"), resultsIn(output));
    assertTrue(output.contains("abs    Betrag"));
    assertTrue(output.contains("Aktuelles Ergebnis: -3"));
    assertEquals(1, occurrencesOf(output, "Gib die erste Zahl ein: "));
    assertEquals(1, occurrencesOf(output, "Gib die zweite Zahl ein: "));
  }

  @ParameterizedTest
  @ValueSource(strings = {"pi", "PI", "Pi", "pI"})
  void acceptsPiAsTheFirstOperand(String pi) {
    String output = runCalculator(pi + "\n+\n0\n3\n");

    assertEquals(List.of(Double.toString(Math.PI)), resultsIn(output));
    assertTrue(output.contains("Du kannst pi (auch PI oder Pi) statt einer Zahl eingeben."));
    assertFalse(output.contains("Bitte gebe eine Zahl ein!"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"pi", "PI", "Pi", "pI"})
  void acceptsPiAsTheSecondOperand(String pi) {
    String output = runCalculator("2\n+\n" + pi + "\n3\n");

    assertEquals(List.of(Double.toString(2 + Math.PI)), resultsIn(output));
    assertFalse(output.contains("Bitte gebe eine Zahl ein!"));
  }

  @Test
  void acceptsPiAsTheExponent() {
    String output = runCalculator("2\npower\npi\n3\n");

    assertEquals(List.of(Double.toString(Math.pow(2, Math.PI))), resultsIn(output));
    assertEquals(1, occurrencesOf(output, "Gib den Exponent ein: "));
  }

  @Test
  void acceptsPiWhenContinuingWithThePreviousResult() {
    String output = runCalculator("2\n+\n3\n1\n*\npi\n3\n");

    assertEquals(List.of("5", Double.toString(5 * Math.PI)), resultsIn(output));
    assertEquals(1, occurrencesOf(output, "Gib die erste Zahl ein: "));
    assertTrue(output.contains("Aktuelles Ergebnis: 5"));
  }

  @Test
  void rejectsOtherConstantsAndAcceptsPiOnRetry() {
    String output = runCalculator("tau\npi\n+\n0\n3\n");

    assertEquals(List.of(Double.toString(Math.PI)), resultsIn(output));
    assertEquals(1, occurrencesOf(output, "Bitte gebe eine Zahl ein!"));
    assertEquals(2, occurrencesOf(output, "Gib die erste Zahl ein: "));
  }

  @ParameterizedTest
  @ValueSource(strings = {"e", "E"})
  void acceptsEulerNumberAsTheFirstOperand(String e) {
    String output = runCalculator(e + "\n+\n0\n3\n");

    assertEquals(List.of(Double.toString(Math.E)), resultsIn(output));
    assertTrue(output.contains("Du kannst auch e oder E für die Eulersche Zahl eingeben."));
    assertFalse(output.contains("Bitte gebe eine Zahl ein!"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"e", "E"})
  void acceptsEulerNumberAsTheSecondOperand(String e) {
    String output = runCalculator("2\n+\n" + e + "\n3\n");

    assertEquals(List.of(Double.toString(2 + Math.E)), resultsIn(output));
    assertFalse(output.contains("Bitte gebe eine Zahl ein!"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"e", "E"})
  void acceptsEulerNumberAsTheExponent(String e) {
    String output = runCalculator("2\npower\n" + e + "\n3\n");

    assertEquals(List.of(Double.toString(Math.pow(2, Math.E))), resultsIn(output));
    assertEquals(1, occurrencesOf(output, "Gib den Exponent ein: "));
  }

  @ParameterizedTest
  @ValueSource(strings = {"e", "E"})
  void acceptsEulerNumberWhenContinuingWithThePreviousResult(String e) {
    String output = runCalculator("2\n+\n3\n1\n*\n" + e + "\n3\n");

    assertEquals(List.of("5", Double.toString(5 * Math.E)), resultsIn(output));
    assertEquals(1, occurrencesOf(output, "Gib die erste Zahl ein: "));
    assertTrue(output.contains("Aktuelles Ergebnis: 5"));
  }

  @ParameterizedTest
  @CsvSource({"0, 0", "90, 1", "180, 0", "-90, -1"})
  void calculatesSineInDegreesByDefault(int angle, double expected) {
    String output = runCalculator(angle + "\nsin\n3\n");

    assertEquals(1, resultsIn(output).size());
    assertEquals(expected, Double.parseDouble(resultsIn(output).getFirst()), TOLERANCE);
    assertTrue(output.contains("Aktueller Winkelmodus: DEG"));
    assertTrue(output.contains("sin    Sinus"));
    assertEquals(0, occurrencesOf(output, "Gib die zweite Zahl ein: "));
  }

  @Test
  void calculatesSineInRadiansFromAPreviousDivisionResult() {
    String output = runCalculator("pi\n/\n2\n1\nmode\nsin\n3\n");

    List<String> results = resultsIn(output);
    assertEquals(2, results.size());
    assertEquals(Math.PI / 2, Double.parseDouble(results.get(0)), TOLERANCE);
    assertEquals(1, Double.parseDouble(results.get(1)), TOLERANCE);
    assertTrue(output.contains("Aktueller Winkelmodus: RAD"));
    assertEquals(1, occurrencesOf(output, "Gib die erste Zahl ein: "));
    assertEquals(1, occurrencesOf(output, "Gib die zweite Zahl ein: "));
    assertEquals(2, occurrencesOf(output, "Was möchtest du als Nächstes tun?"));
  }

  @Test
  void switchesBackToDegreesWithoutCalculatingOrRequestingAnotherNumber() {
    String output = runCalculator("90\nmode\nmode\nsin\n3\n");

    assertEquals(1, resultsIn(output).size());
    assertEquals(1, Double.parseDouble(resultsIn(output).getFirst()), TOLERANCE);
    assertEquals(2, occurrencesOf(output, "Aktueller Winkelmodus: DEG"));
    assertEquals(1, occurrencesOf(output, "Aktueller Winkelmodus: RAD"));
    assertEquals(1, occurrencesOf(output, "Gib die erste Zahl ein: "));
    assertEquals(0, occurrencesOf(output, "Gib die zweite Zahl ein: "));
    assertEquals(1, occurrencesOf(output, "Was möchtest du als Nächstes tun?"));
  }

  @Test
  void preservesTheLastResultAndAngleModeAfterAFailedContinuation() {
    String output = runCalculator("90\n+\n0\n1\nmode\n/\n0\n1\nsin\n3\n");

    List<String> results = resultsIn(output);
    assertEquals(2, results.size());
    assertEquals("90", results.get(0));
    assertEquals(Math.sin(90), Double.parseDouble(results.get(1)), TOLERANCE);
    assertTrue(output.contains("Fehler: Division durch null ist nicht erlaubt."));
    assertEquals(2, occurrencesOf(output, "Aktuelles Ergebnis: 90"));
    assertEquals(1, occurrencesOf(output, "Gib die erste Zahl ein: "));
    assertEquals(3, occurrencesOf(output, "Was möchtest du als Nächstes tun?"));
  }

  @Test
  void preservesTheAngleModeWhenStartingANewCalculation() {
    String output = runCalculator("pi\nmode\nsin\n2\npi\nsin\n3\n");

    List<String> results = resultsIn(output);
    assertEquals(2, results.size());
    assertEquals(0, Double.parseDouble(results.get(0)), TOLERANCE);
    assertEquals(0, Double.parseDouble(results.get(1)), TOLERANCE);
    assertEquals(2, occurrencesOf(output, "Gib die erste Zahl ein: "));
    assertEquals(1, occurrencesOf(output, "Aktueller Winkelmodus: RAD"));
  }

  @Test
  void leavesArithmeticAndConstantsUnchangedInRadiansMode() {
    String output = runCalculator("e\nmode\n+\npi\n3\n");

    assertEquals(1, resultsIn(output).size());
    assertEquals(Math.E + Math.PI, Double.parseDouble(resultsIn(output).getFirst()), TOLERANCE);
    assertTrue(output.contains("Aktueller Winkelmodus: RAD"));
    assertEquals(1, occurrencesOf(output, "Gib die erste Zahl ein: "));
  }

  @ParameterizedTest
  @CsvSource({"0, 1", "60, 0.5", "90, 0", "180, -1"})
  void calculatesCosineInDegreesByDefault(int angle, double expected) {
    String output = runCalculator(angle + "\ncos\n3\n");

    assertEquals(1, resultsIn(output).size());
    assertEquals(expected, Double.parseDouble(resultsIn(output).getFirst()), TOLERANCE);
    assertTrue(output.contains("Aktueller Winkelmodus: DEG"));
    assertTrue(output.contains("cos    Kosinus"));
    assertEquals(0, occurrencesOf(output, "Gib die zweite Zahl ein: "));
  }

  @Test
  void calculatesCosineOfPiInRadiansMode() {
    String output = runCalculator("pi\nmode\ncos\n3\n");

    assertEquals(1, resultsIn(output).size());
    assertEquals(-1, Double.parseDouble(resultsIn(output).getFirst()), TOLERANCE);
    assertTrue(output.contains("Aktueller Winkelmodus: RAD"));
    assertEquals(0, occurrencesOf(output, "Gib die zweite Zahl ein: "));
  }

  @Test
  void calculatesCosineWhenContinuingWithThePreviousResult() {
    String output = runCalculator("30\n*\n2\n1\ncos\n3\n");

    List<String> results = resultsIn(output);
    assertEquals(2, results.size());
    assertEquals("60", results.get(0));
    assertEquals(0.5, Double.parseDouble(results.get(1)), TOLERANCE);
    assertTrue(output.contains("Aktuelles Ergebnis: 60"));
    assertEquals(1, occurrencesOf(output, "Gib die erste Zahl ein: "));
    assertEquals(1, occurrencesOf(output, "Gib die zweite Zahl ein: "));
  }

  @ParameterizedTest
  @ValueSource(strings = {"NaN", "Infinity", "-Infinity"})
  void rejectsNonFiniteCosineAngles(String angle) {
    String output = runCalculator(angle + "\ncos\n3\n");

    assertTrue(resultsIn(output).isEmpty());
    assertTrue(output.contains("Fehler: Operanden müssen endliche Zahlen sein"));
    assertFalse(output.contains("1. Mit dem Ergebnis weiterrechnen"));
    assertEquals(0, occurrencesOf(output, "Gib die zweite Zahl ein: "));
  }

  @ParameterizedTest
  @CsvSource({"0, 0", "45, 1", "180, 0", "-45, -1"})
  void calculatesTangentInDegreesByDefault(int angle, double expected) {
    String output = runCalculator(angle + "\ntan\n3\n");

    assertEquals(1, resultsIn(output).size());
    assertEquals(expected, Double.parseDouble(resultsIn(output).getFirst()), TOLERANCE);
    assertTrue(output.contains("tan    Tangens"));
    assertEquals(0, occurrencesOf(output, "Gib die zweite Zahl ein: "));
  }

  @ParameterizedTest
  @ValueSource(ints = {90, 270, -90, 450, -270})
  void rejectsTangentSingularitiesInDegrees(int angle) {
    String output = runCalculator(angle + "\ntan\n3\n");

    assertTrue(resultsIn(output).isEmpty());
    assertTrue(output.contains("Fehler: Tangens ist für diesen Winkel nicht definiert."));
    assertFalse(output.contains("1. Mit dem Ergebnis weiterrechnen"));
    assertEquals(0, occurrencesOf(output, "Gib die zweite Zahl ein: "));
  }

  @Test
  void rejectsTangentOfHalfPiInRadiansAndKeepsThePreviousResult() {
    String output = runCalculator("pi\n/\n2\n1\nmode\ntan\n1\n+\n1\n3\n");

    List<String> results = resultsIn(output);
    assertEquals(2, results.size());
    assertEquals(Math.PI / 2, Double.parseDouble(results.get(0)), TOLERANCE);
    assertEquals(Math.PI / 2 + 1, Double.parseDouble(results.get(1)), TOLERANCE);
    assertTrue(output.contains("Fehler: Tangens ist für diesen Winkel nicht definiert."));
    assertTrue(output.contains("Aktueller Winkelmodus: RAD"));
    assertEquals(2, occurrencesOf(output, "Aktuelles Ergebnis: " + Math.PI / 2));
    assertEquals(1, occurrencesOf(output, "Gib die erste Zahl ein: "));
  }

  @Test
  void keepsThePreviousResultAfterAFailedTangentInDegrees() {
    String output = runCalculator("45\n*\n2\n1\ntan\n1\n+\n1\n3\n");

    assertEquals(List.of("90", "91"), resultsIn(output));
    assertTrue(output.contains("Fehler: Tangens ist für diesen Winkel nicht definiert."));
    assertEquals(2, occurrencesOf(output, "Aktuelles Ergebnis: 90"));
    assertEquals(1, occurrencesOf(output, "Gib die erste Zahl ein: "));
  }

  @Test
  void calculatesTangentWhileSwitchingBetweenRadiansAndDegrees() {
    String output = runCalculator("pi\n/\n4\n1\nmode\ntan\n2\n45\nmode\ntan\n3\n");

    List<String> results = resultsIn(output);
    assertEquals(3, results.size());
    assertEquals(Math.PI / 4, Double.parseDouble(results.get(0)), TOLERANCE);
    assertEquals(1, Double.parseDouble(results.get(1)), TOLERANCE);
    assertEquals(1, Double.parseDouble(results.get(2)), TOLERANCE);
    assertEquals(1, occurrencesOf(output, "Aktueller Winkelmodus: RAD"));
    assertEquals(2, occurrencesOf(output, "Aktueller Winkelmodus: DEG"));
    assertEquals(2, occurrencesOf(output, "Gib die erste Zahl ein: "));
    assertEquals(1, occurrencesOf(output, "Gib die zweite Zahl ein: "));
  }

  @ParameterizedTest
  @ValueSource(ints = {90, 270, -90})
  void acceptsRepresentableDegreeAnglesNextToTangentSingularities(int singularity) {
    for (double angle : new double[] {Math.nextDown((double) singularity), Math.nextUp((double) singularity)}) {
      String output = runCalculator(angle + "\ntan\n3\n");

      assertEquals(1, resultsIn(output).size());
      assertEquals(Math.tan(Math.toRadians(angle)), Double.parseDouble(resultsIn(output).getFirst()), TOLERANCE);
      assertFalse(output.contains("Fehler:"));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"NaN", "Infinity", "-Infinity"})
  void rejectsNonFiniteTangentAngles(String angle) {
    String output = runCalculator(angle + "\ntan\n3\n");

    assertTrue(resultsIn(output).isEmpty());
    assertTrue(output.contains("Fehler: Operanden müssen endliche Zahlen sein"));
    assertFalse(output.contains("1. Mit dem Ergebnis weiterrechnen"));
  }

  @ParameterizedTest
  @CsvSource({"log, 100, 2", "ln, e, 1", "log, 0.1, -1", "ln, 0.5, -0.6931471805599453"})
  void calculatesLogarithmsAsUnaryOperationsInBothAngleModes(String operator, String number, double expected) {
    for (String modeCommand : List.of("", "mode\n")) {
      String output = runCalculator(number + "\n" + modeCommand + operator + "\n3\n");

      assertEquals(1, resultsIn(output).size());
      assertEquals(expected, Double.parseDouble(resultsIn(output).getFirst()), TOLERANCE);
      assertTrue(output.contains("log    Dekadischer Logarithmus (Basis 10)"));
      assertTrue(output.contains("ln     Natürlicher Logarithmus (Basis e)"));
      assertEquals(0, occurrencesOf(output, "Gib die zweite Zahl ein: "));
      assertEquals(0, occurrencesOf(output, "Gib den Exponent ein: "));
      if (!modeCommand.isEmpty()) {
        assertTrue(output.contains("Aktueller Winkelmodus: RAD"));
      }
    }
  }

  @ParameterizedTest
  @CsvSource({"log, 0", "ln, 0", "log, -1", "ln, -1"})
  void reportsNonPositiveLogarithmInputs(String operator, String number) {
    String output = runCalculator(number + "\n" + operator + "\n3\n");

    assertTrue(resultsIn(output).isEmpty());
    assertTrue(output.contains("Fehler:"));
    assertTrue(output.contains("Logarithmus ist nur für Zahlen größer als null definiert."));
    assertFalse(output.contains("1. Mit dem Ergebnis weiterrechnen"));
    assertEquals(0, occurrencesOf(output, "Gib die zweite Zahl ein: "));
  }

  @ParameterizedTest
  @ValueSource(strings = {"log", "ln"})
  void keepsThePreviousResultAfterAFailedLogarithm(String operator) {
    String output = runCalculator("2\n-\n3\n1\n" + operator + "\n1\nabs\n3\n");

    assertEquals(List.of("-1", "1"), resultsIn(output));
    assertTrue(output.contains("Fehler:"));
    assertEquals(2, occurrencesOf(output, "Aktuelles Ergebnis: -1"));
    assertEquals(1, occurrencesOf(output, "Gib die erste Zahl ein: "));
    assertEquals(1, occurrencesOf(output, "Gib die zweite Zahl ein: "));
  }

  @Test
  void continuesFromADecimalLogarithmWithANaturalLogarithm() {
    String output = runCalculator("1000\nlog\n1\nln\n3\n");

    List<String> results = resultsIn(output);
    assertEquals(2, results.size());
    assertEquals(3, Double.parseDouble(results.get(0)), TOLERANCE);
    assertEquals(1.0986122886681098, Double.parseDouble(results.get(1)), TOLERANCE);
    assertEquals(1, occurrencesOf(output, "Gib die erste Zahl ein: "));
    assertEquals(0, occurrencesOf(output, "Gib die zweite Zahl ein: "));
  }

  @ParameterizedTest
  @CsvSource({"0, 1", "5, 120", "20, 2432902008176640000"})
  void calculatesFactorialsAsUnaryOperationsInBothAngleModes(int number, long expected) {
    for (String modeCommand : List.of("", "mode\n")) {
      String output = runCalculator(number + "\n" + modeCommand + "factorial\n3\n");

      assertEquals(List.of(Long.toString(expected)), resultsIn(output));
      assertTrue(output.contains("factorial  Fakultät"));
      assertEquals(0, occurrencesOf(output, "Gib die zweite Zahl ein: "));
      assertEquals(0, occurrencesOf(output, "Gib den Exponent ein: "));
      if (!modeCommand.isEmpty()) {
        assertTrue(output.contains("Aktueller Winkelmodus: RAD"));
      }
    }
  }

  @ParameterizedTest
  @CsvSource({
      "-1, Die Fakultät ist nur für nichtnegative ganze Zahlen definiert.",
      "2.5, Die Fakultät ist nur für nichtnegative ganze Zahlen definiert.",
      "21, Für die Fakultät sind nur Werte von 0 bis 20 erlaubt."
  })
  void reportsInvalidFactorialOperands(String number, String expectedMessage) {
    String output = runCalculator(number + "\nfactorial\n3\n");

    assertTrue(resultsIn(output).isEmpty());
    assertTrue(output.contains("Fehler: " + expectedMessage));
    assertFalse(output.contains("1. Mit dem Ergebnis weiterrechnen"));
    assertEquals(0, occurrencesOf(output, "Gib die zweite Zahl ein: "));
  }

  @Test
  void continuesWithAFactorialResult() {
    String output = runCalculator("2\n+\n3\n1\nfactorial\n1\n/\n2\n3\n");

    assertEquals(List.of("5", "120", "60"), resultsIn(output));
    assertTrue(output.contains("Aktuelles Ergebnis: 120"));
    assertEquals(1, occurrencesOf(output, "Gib die erste Zahl ein: "));
    assertEquals(2, occurrencesOf(output, "Gib die zweite Zahl ein: "));
  }

  @ParameterizedTest
  @CsvSource({"-1, 0", "2.5, 3.5", "21, 22"})
  void keepsThePreviousResultAfterAFailedFactorial(String number, String expectedFinalResult) {
    String output = runCalculator(number + "\n+\n0\n1\nfactorial\n1\n+\n1\n3\n");

    assertEquals(List.of(number, expectedFinalResult), resultsIn(output));
    assertTrue(output.contains("Fehler:"));
    assertEquals(2, occurrencesOf(output, "Aktuelles Ergebnis: " + number));
    assertEquals(1, occurrencesOf(output, "Gib die erste Zahl ein: "));
    assertEquals(2, occurrencesOf(output, "Gib die zweite Zahl ein: "));
  }

  @ParameterizedTest
  @CsvSource({"27, 3, 3", "25, 2, 5", "-27, 3, -3", "-32, 5, -2", "0, 3, 0"})
  void calculatesNthRootsInBothAngleModes(String number, String root, double expected) {
    for (String modeCommand : List.of("", "mode\n")) {
      String output = runCalculator(number + "\n" + modeCommand + "root\n" + root + "\n3\n");

      assertEquals(1, resultsIn(output).size());
      assertEquals(expected, Double.parseDouble(resultsIn(output).getFirst()), TOLERANCE);
      assertTrue(output.contains("root   n-te Wurzel"));
      assertEquals(1, occurrencesOf(output, "Gib den Wurzelexponenten ein: "));
      assertEquals(0, occurrencesOf(output, "Gib die zweite Zahl ein: "));
      assertEquals(0, occurrencesOf(output, "Gib den Exponent ein: "));
      if (!modeCommand.isEmpty()) {
        assertTrue(output.contains("Aktueller Winkelmodus: RAD"));
      }
    }
  }

  @ParameterizedTest
  @CsvSource({
      "-16, 4, Eine gerade Wurzel aus einer negativen Zahl ist nicht erlaubt.",
      "27, 0, Der Wurzelexponent muss eine positive ganze Zahl sein.",
      "27, -3, Der Wurzelexponent muss eine positive ganze Zahl sein.",
      "27, 2.5, Der Wurzelexponent muss eine positive ganze Zahl sein."
  })
  void reportsInvalidNthRootInputsAndAllowsANewCalculation(String number, String root, String expectedMessage) {
    String output = runCalculator(number + "\nroot\n" + root + "\n2\n25\nsqrt\n3\n");

    assertEquals(List.of("5"), resultsIn(output));
    assertTrue(output.contains("Fehler: " + expectedMessage));
    assertEquals(1, occurrencesOf(output, "1. Mit dem Ergebnis weiterrechnen"));
    assertEquals(2, occurrencesOf(output, "Gib die erste Zahl ein: "));
    assertEquals(1, occurrencesOf(output, "Gib den Wurzelexponenten ein: "));
  }

  @ParameterizedTest
  @CsvSource({"NaN, 3", "Infinity, 3", "-Infinity, 3", "27, NaN", "27, Infinity", "27, -Infinity"})
  void rejectsNonFiniteNthRootInputs(String number, String root) {
    String output = runCalculator(number + "\nroot\n" + root + "\n3\n");

    assertTrue(resultsIn(output).isEmpty());
    assertTrue(output.contains("Fehler: Operanden müssen endliche Zahlen sein"));
    assertFalse(output.contains("1. Mit dem Ergebnis weiterrechnen"));
  }

  @Test
  void retriesANonNumericRootExponent() {
    String output = runCalculator("27\nroot\nthree\n3\n3\n");

    assertEquals(1, resultsIn(output).size());
    assertEquals(3, Double.parseDouble(resultsIn(output).getFirst()), TOLERANCE);
    assertEquals(1, occurrencesOf(output, "Bitte gebe eine Zahl ein!"));
    assertEquals(2, occurrencesOf(output, "Gib den Wurzelexponenten ein: "));
  }

  @Test
  void continuesWithNthRootResultsAndUsesThePreviousResultAsARadicand() {
    String output = runCalculator("16\nroot\n4\n1\npower\n4\n1\nroot\n2\n1\n+\n1\n3\n");

    assertEquals(List.of("2", "16", "4", "5"), resultsIn(output));
    assertEquals(1, occurrencesOf(output, "Gib die erste Zahl ein: "));
    assertEquals(2, occurrencesOf(output, "Gib den Wurzelexponenten ein: "));
    assertTrue(output.contains("Aktuelles Ergebnis: 16"));
  }

  @ParameterizedTest
  @CsvSource({"-16, 4, 1, -16", "27, 0, 3, 3", "27, -3, 3, 3", "27, 2.5, 3, 3"})
  void keepsThePreviousResultAfterAFailedNthRoot(String number, String invalidRoot,
                                                String validRoot, double expected) {
    String output = runCalculator(number + "\n+\n0\n1\nroot\n" + invalidRoot
        + "\n1\nroot\n" + validRoot + "\n3\n");

    List<String> results = resultsIn(output);
    assertEquals(2, results.size());
    assertEquals(number, results.getFirst());
    assertEquals(expected, Double.parseDouble(results.get(1)), TOLERANCE);
    assertTrue(output.contains("Fehler:"));
    assertEquals(2, occurrencesOf(output, "Aktuelles Ergebnis: " + number));
    assertEquals(1, occurrencesOf(output, "Gib die erste Zahl ein: "));
  }

  @Test
  void keepsSquareRootUnaryAndRejectsNegativeSquareRootInputs() {
    String output = runCalculator("25\nsqrt\n2\n-16\nsqrt\n3\n");

    assertEquals(List.of("5"), resultsIn(output));
    assertTrue(output.contains("Fehler: Die Quadratwurzel einer negativen Zahl ist nicht erlaubt."));
    assertEquals(0, occurrencesOf(output, "Gib den Wurzelexponenten ein: "));
    assertEquals(0, occurrencesOf(output, "Gib die zweite Zahl ein: "));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " \n\t "})
  void exitsAtEndOfInputBeforeTheFirstNumber(String input) {
    String output = runCalculatorUntilEndOfInput(input);

    assertTrue(resultsIn(output).isEmpty());
    assertEquals(1, occurrencesOf(output, "Gib die erste Zahl ein: "));
    assertFalse(output.contains("Bitte gebe eine Zahl ein!"));
    assertFalse(output.contains("Was möchtest du als Nächstes tun?"));
  }

  @Test
  void exitsAtEndOfInputWhenANewFirstNumberIsRequested() {
    String output = runCalculatorUntilEndOfInput("2\n+\n3\n2\n");

    assertEquals(List.of("5"), resultsIn(output));
    assertEquals(2, occurrencesOf(output, "Gib die erste Zahl ein: "));
    assertFalse(output.contains("Aktuelles Ergebnis: "));
  }

  @ParameterizedTest
  @ValueSource(strings = {"+", "-", "*", "/"})
  void exitsAtEndOfInputAfterSelectingABinaryOperator(String operator) {
    String output = runCalculatorUntilEndOfInput("5\n" + operator + "\n");

    assertTrue(resultsIn(output).isEmpty());
    assertEquals(1, occurrencesOf(output, "Gib die zweite Zahl ein: "));
    assertFalse(output.contains("Was möchtest du als Nächstes tun?"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"5", "5\nmode\n"})
  void exitsAtEndOfInputDuringOperatorSelection(String input) {
    String output = runCalculatorUntilEndOfInput(input);

    assertTrue(resultsIn(output).isEmpty());
    assertTrue(output.contains("Wähle einen Operator aus: "));
    assertFalse(output.contains("Bitte gib einen der erlaubten Operatoren ein!"));
  }

  @ParameterizedTest
  @CsvSource({"power, Gib den Exponent ein:", "root, Gib den Wurzelexponenten ein:"})
  void exitsAtEndOfInputWhenAnExponentIsRequested(String operator, String prompt) {
    String output = runCalculatorUntilEndOfInput("27\n" + operator + "\n");

    assertTrue(resultsIn(output).isEmpty());
    assertTrue(output.contains(prompt));
    assertFalse(output.contains("Gib die zweite Zahl ein: "));
  }

  @ParameterizedTest
  @ValueSource(strings = {"2\n+\n3", "2\n+\n3\n"})
  void exitsAtEndOfInputAfterASuccessfulCalculation(String input) {
    String output = runCalculatorUntilEndOfInput(input);

    assertEquals(List.of("5"), resultsIn(output));
    assertEquals(1, occurrencesOf(output, "Deine Auswahl: "));
  }

  @ParameterizedTest
  @ValueSource(strings = {"abc\n", "5\n+\nabc\n", "5\npower\nabc\n", "5\nroot\nabc\n"})
  void exitsAtEndOfInputAfterAnInvalidNumber(String input) {
    String output = runCalculatorUntilEndOfInput(input);

    assertTrue(resultsIn(output).isEmpty());
    assertEquals(1, occurrencesOf(output, "Bitte gebe eine Zahl ein!"));
  }

  @Test
  void exitsAtEndOfInputAfterAnInvalidOperator() {
    String output = runCalculatorUntilEndOfInput("5\n?\n");

    assertTrue(resultsIn(output).isEmpty());
    assertEquals(1, occurrencesOf(output, "Bitte gib einen der erlaubten Operatoren ein!"));
    assertEquals(2, occurrencesOf(output, "Wähle einen Operator aus: "));
  }

  @Test
  void exitsAtEndOfInputAfterAnInvalidMenuChoice() {
    String output = runCalculatorUntilEndOfInput("2\n+\n3\nabc\n");

    assertEquals(List.of("5"), resultsIn(output));
    assertEquals(1, occurrencesOf(output, "Bitte wähle 1, 2 oder 3."));
    assertEquals(2, occurrencesOf(output, "Deine Auswahl: "));
  }

  @Test
  void exitsAtEndOfInputAfterAMathematicalError() {
    String output = runCalculatorUntilEndOfInput("5\n/\n0\n");

    assertTrue(resultsIn(output).isEmpty());
    assertTrue(output.contains("Fehler: Division durch null ist nicht erlaubt."));
    assertFalse(output.contains("1. Mit dem Ergebnis weiterrechnen"));
  }

  @Test
  void exitsAtEndOfInputWhenContinuingWithThePreviousResult() {
    String output = runCalculatorUntilEndOfInput("2\n+\n3\n1\n");

    assertEquals(List.of("5"), resultsIn(output));
    assertTrue(output.contains("Aktuelles Ergebnis: 5"));
    assertEquals(1, occurrencesOf(output, "Gib die erste Zahl ein: "));
  }

  @Test
  void doesNotSwallowUnexpectedScannerFailures() {
    Scanner scanner = new Scanner("");
    scanner.close();
    try (PrintStream output = new PrintStream(new ByteArrayOutputStream())) {
      assertThrows(IllegalStateException.class, () -> Calculator.run(scanner, output));
    }
  }

  private static String runCalculatorUntilEndOfInput(String input) {
    String output = assertDoesNotThrow(() -> runCalculator(input));

    assertEquals(1, occurrencesOf(output, EOF_MESSAGE));
    assertTrue(output.endsWith(EOF_MESSAGE + System.lineSeparator()));
    assertFalse(output.contains("Exception"));
    return output;
  }

  private static String runCalculator(String input) {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try (Scanner scanner = new Scanner(input).useLocale(Locale.US);
         PrintStream output = new PrintStream(buffer, true, StandardCharsets.UTF_8)) {
      Calculator.run(scanner, output);
    }
    return buffer.toString(StandardCharsets.UTF_8);
  }

  private static List<String> resultsIn(String output) {
    return Pattern.compile("Das Ergebnis ist: ([^\\r\\n]+)").matcher(output).results()
        .map(match -> match.group(1))
        .toList();
  }

  private static long occurrencesOf(String output, String text) {
    return Pattern.compile(Pattern.quote(text)).matcher(output).results().count();
  }
}
