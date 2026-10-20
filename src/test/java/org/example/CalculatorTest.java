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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculatorTest {

  private static final double TOLERANCE = 1e-12;

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
