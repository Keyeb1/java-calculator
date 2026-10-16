package org.example;

import org.junit.jupiter.api.Test;

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
