package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculatorTest {
  private static final double TOLERANCE = 1e-12;
  private static final String EXPRESSION_PROMPT = "Gib einen Rechenausdruck ein: ";
  private static final String EOF_MESSAGE = "Eingabe beendet. Taschenrechner wird beendet.";
  private static final String FULL_MENU = String.join(System.lineSeparator(),
      "Was möchtest du als Nächstes tun?",
      "1. Mit dem Ergebnis weiterrechnen",
      "2. Neue Berechnung starten",
      "3. Taschenrechner beenden",
      "4. Verlauf anzeigen",
      "Deine Auswahl: ");
  private static final String NO_RESULT_MENU = String.join(System.lineSeparator(),
      "Was möchtest du als Nächstes tun?",
      "2. Neue Berechnung starten",
      "3. Taschenrechner beenden",
      "4. Verlauf anzeigen",
      "Deine Auswahl: ");

  @Test
  void explainsExpressionInputAndStartsInDegreesWithoutAns() {
    String output = runCalculator("ans\n3\n");

    for (String hint : List.of("vollständige Ausdrücke", "Punkt vor Strich", "Klammern",
        "Dezimalpunkt", "1.25e-7", "sqrt(x)", "power(2,3)", "root(27,3)",
        "Kommas trennen Argumente", "ans", "mode", "256 Zeichen", "50 Operationen", "16 gleichzeitig")) {
      assertTrue(output.contains(hint), hint);
    }
    assertTrue(output.contains("Aktueller Winkelmodus: DEG"));
    assertTrue(output.contains("kein letztes Ergebnis"));
    assertTrue(output.contains(NO_RESULT_MENU));
    assertTrue(resultsIn(output).isEmpty());
    assertFalse(output.contains(EOF_MESSAGE));
  }

  @ParameterizedTest
  @CsvSource(value = {
      "3+4+5+6+7 | 25", "3+4+5*4-5 | 22", "(3+4+5)*4-5 | 43",
      "20/5*2 | 8", "10-3-2 | 5", "16/4/2 | 2", "2*(-4) | -8",
      "-(3+4) | -7", "1e-3+2 | 2.001", " 3 + 4 * 2  | 11",
      "sqrt(4+5) | 3", "power(2, 1+2) | 8", "sqrt(abs(-9)) | 3"
  }, delimiter = '|')
  void evaluatesFullExpressionsWithPrecedenceAndParentheses(String expression, double expected) {
    String output = runCalculator(expression + "\n3\n");

    assertEquals(1, resultsIn(output).size());
    assertEquals(expected, Double.parseDouble(resultsIn(output).getFirst()), TOLERANCE);
    assertEquals(1, occurrencesOf(output, EXPRESSION_PROMPT));
    assertFalse(output.contains("Fehler:"));
    assertFalse(output.contains(EOF_MESSAGE));
  }

  @Test
  void continuesRepeatedlyWithExplicitAns() {
    String output = runCalculator("""
        2+7
        1
        sqrt(ans)
        1
        power(ans,2)
        1
        ans*2
        1
        ans-6
        1
        ans/3
        1
        ans+1
        3
        """);

    assertEquals(List.of("9", "3", "9", "18", "12", "4", "5"), resultsIn(output));
    assertEquals(7, occurrencesOf(output, EXPRESSION_PROMPT));
    assertEquals(6, occurrencesOf(output, "Aktuelles Ergebnis: "));
    assertTrue(output.contains("Verwende ans im nächsten Ausdruck"));
    assertFalse(output.contains(EOF_MESSAGE));
  }

  @ParameterizedTest
  @CsvSource(value = {"2 | 2", "+2 | 2", "-2 | -2", "2*3 | 6"}, delimiter = '|')
  void evaluatesExpressionsWithoutAnsIndependentlyWhenContinuing(String expression, String expected) {
    String output = runCalculator("2+3\n1\n" + expression + "\n4\n3\n");

    assertEquals(List.of("5", expected), resultsIn(output));
    assertEquals(List.of("1. 2+3 [DEG] = 5",
        "2. " + expression + " [DEG, ans=5] = " + expected), historyEntriesIn(output));
    assertTrue(output.contains("Aktuelles Ergebnis: 5" + System.lineSeparator()));
    assertTrue(output.contains("Ohne ans wird der Ausdruck eigenständig ausgewertet."));
  }

  @Test
  void displaysFractionalResultsWhenContinuing() {
    String output = runCalculator("1/4\n1\nans+0.5\n3\n");

    assertEquals(List.of("0.25", "0.75"), resultsIn(output));
    assertTrue(output.contains("Aktuelles Ergebnis: 0.25" + System.lineSeparator()));
  }

  @Test
  void startsANewCalculationWithoutThePreviousAns() {
    String output = runCalculator("2+3\n2\n10-4\n4\n3\n");

    assertEquals(List.of("5", "6"), resultsIn(output));
    assertEquals(List.of("1. 2+3 [DEG] = 5", "2. 10-4 [DEG] = 6"), historyEntriesIn(output));
    assertFalse(output.contains("Aktuelles Ergebnis: "));
  }

  @Test
  void exitsWithoutReadingFurtherExpressions() {
    String output = runCalculator("2+3\n3\n10+20\n3\n");

    assertEquals(List.of("5"), resultsIn(output));
    assertEquals(1, occurrencesOf(output, EXPRESSION_PROMPT));
    assertEquals(1, occurrencesOf(output, FULL_MENU));
    assertFalse(output.contains(EOF_MESSAGE));
  }

  @ParameterizedTest
  @ValueSource(strings = {"abc", "0", "5", "1.5", "1 2", "3 trailing", "4 3", "mode", "", " \t "})
  void rejectsInvalidMenuLinesWithoutInterpretingSeparateTokens(String choice) {
    String output = runCalculator("2+3\n" + choice + "\n3\n");

    assertEquals(List.of("5"), resultsIn(output));
    assertEquals(1, occurrencesOf(output, "Bitte wähle 1, 2, 3 oder 4."));
    assertEquals(2, occurrencesOf(output, FULL_MENU));
    assertEquals(1, occurrencesOf(output, EXPRESSION_PROMPT));
    assertFalse(output.contains("Rechenverlauf:"));
    assertFalse(output.contains(EOF_MESSAGE));
  }

  @ParameterizedTest
  @ValueSource(strings = {"1", "5", "", "abc", "2 3"})
  void rejectsContinuationAndInvalidMenuLinesWhenNoResultExists(String choice) {
    String output = runCalculator("5/0\n" + choice + "\n3\n");

    assertTrue(resultsIn(output).isEmpty());
    assertFalse(output.contains("1. Mit dem Ergebnis weiterrechnen"));
    assertEquals(1, occurrencesOf(output, "Bitte wähle 2, 3 oder 4."));
    assertEquals(2, occurrencesOf(output, NO_RESULT_MENU));
    assertFalse(output.contains(EOF_MESSAGE));
  }

  @Test
  void acceptsOuterWhitespaceForAllMenuChoicesWithoutLosingTheNextExpression() {
    String output = runCalculator("2+3\n \t4 \t\n 1 \nans*2\n 2 \n7\n\t3\t\n");

    assertEquals(List.of("5", "10", "7"), resultsIn(output));
    assertEquals(List.of("1. 2+3 [DEG] = 5"), historyEntriesIn(output));
    assertEquals(3, occurrencesOf(output, EXPRESSION_PROMPT));
    assertFalse(output.contains("Bitte wähle"));
    assertFalse(output.contains("Fehler:"));
    assertFalse(output.contains(EOF_MESSAGE));
  }

  @Test
  void acceptsWindowsLineEndingsAndAnUnterminatedFinalMenuLine() {
    String output = runCalculator("2+3\r\n1\r\nans*2\r\n3");

    assertEquals(List.of("5", "10"), resultsIn(output));
    assertFalse(output.contains("Fehler:"));
    assertFalse(output.contains(EOF_MESSAGE));
  }

  @ParameterizedTest
  @ValueSource(strings = {"ans/0", "ans+", "power(ans,1000)", "1e309", "sqrt(ans,2)", ""})
  void keepsTheLastResultAfterFailedContinuationAndAllowsAnsAgain(String expression) {
    String output = runCalculator("2+3\n1\n" + expression + "\n4\n1\nans*2\n3\n");

    assertEquals(List.of("5", "10"), resultsIn(output));
    assertEquals(List.of("1. 2+3 [DEG] = 5"), historyEntriesIn(output));
    assertEquals(1, occurrencesOf(output, "Fehler:"));
    assertEquals(2, occurrencesOf(output, "Aktuelles Ergebnis: 5"));
    assertFalse(output.contains(EOF_MESSAGE));
  }

  @Test
  void canStartANewCalculationAfterFailedContinuation() {
    String output = runCalculator("2+3\n1\nans/0\n2\n7+1\n4\n3\n");

    assertEquals(List.of("5", "8"), resultsIn(output));
    assertTrue(output.contains("Fehler: Division durch null ist nicht erlaubt."));
    assertEquals(List.of("1. 2+3 [DEG] = 5", "2. 7+1 [DEG] = 8"), historyEntriesIn(output));
  }

  @Test
  void canExitAfterFailedContinuation() {
    String output = runCalculator("2+3\n1\nans/0\n3\n");

    assertEquals(List.of("5"), resultsIn(output));
    assertTrue(output.contains("Fehler: Division durch null ist nicht erlaubt."));
    assertEquals(2, occurrencesOf(output, FULL_MENU));
    assertFalse(output.contains(EOF_MESSAGE));
  }

  @Test
  void rejectsContinuationUntilAValidResultExists() {
    String output = runCalculator("5/0\n1\n2\n6+2\n3\n");

    assertEquals(List.of("8"), resultsIn(output));
    String beforeSuccess = output.substring(0, output.indexOf("Das Ergebnis ist: 8"));
    assertFalse(beforeSuccess.contains("1. Mit dem Ergebnis weiterrechnen"));
    assertTrue(beforeSuccess.contains("Bitte wähle 2, 3 oder 4."));
  }

  @ParameterizedTest
  @ValueSource(strings = {"ans+1", "sqrt(-1)", "2+"})
  void showsPreviousHistoryButRejectsAnsAndContinuationAfterANewCalculationFails(String expression) {
    String output = runCalculator("2+3\n2\n" + expression + "\n4\n1\n2\n6+2\n4\n3\n");

    assertEquals(List.of("5", "8"), resultsIn(output));
    assertEquals(List.of("1. 2+3 [DEG] = 5",
        "1. 2+3 [DEG] = 5", "2. 6+2 [DEG] = 8"), historyEntriesIn(output));
    String afterFailure = output.substring(output.indexOf("Fehler:"), output.indexOf("Das Ergebnis ist: 8"));
    assertFalse(afterFailure.contains("1. Mit dem Ergebnis weiterrechnen"));
    assertTrue(afterFailure.contains("Bitte wähle 2, 3 oder 4."));
    assertFalse(output.contains("Aktuelles Ergebnis: "));
    if (expression.contains("ans")) {
      assertTrue(output.contains("kein letztes Ergebnis"));
    }
    assertFalse(output.contains(EOF_MESSAGE));
  }

  @ParameterizedTest
  @CsvSource(value = {"2-2 | 0", "-0.0 | -0"}, delimiter = '|')
  void canContinueWithEitherSignedZeroAfterShowingHistory(String expression, String expected) {
    String output = runCalculator(expression + "\n4\n1\nans\n4\n3\n");

    assertEquals(List.of(expected, expected), resultsIn(output));
    assertTrue(output.contains("Aktuelles Ergebnis: " + expected + System.lineSeparator()));
    assertEquals(List.of("1. " + expression + " [DEG] = " + expected,
        "1. " + expression + " [DEG] = " + expected,
        "2. ans [DEG, ans=" + expected + "] = " + expected), historyEntriesIn(output));
    assertEquals(Double.doubleToLongBits(Double.parseDouble(expected)),
        Double.doubleToLongBits(Double.parseDouble(resultsIn(output).getLast())));
    assertFalse(output.contains("Fehler:"));
  }

  @Test
  void recordsExpressionsAndTheirPreviousAnsBeforeUpdatingTheResult() {
    String output = runCalculator("""
        3+4+5*4-5
        1
        ans*2+6
        4
        3
        """);

    assertEquals(List.of("22", "50"), resultsIn(output));
    assertEquals(List.of("1. 3+4+5*4-5 [DEG] = 22",
        "2. ans*2+6 [DEG, ans=22] = 50"), historyEntriesIn(output));
  }

  @Test
  void showsHistoryRepeatedlyWithoutDuplicatingEntriesOrChangingTheMenu() {
    String output = runCalculator("2+3\n4\n4\n1\nans*2\n4\n3\n");

    assertEquals(List.of("5", "10"), resultsIn(output));
    assertEquals(List.of("1. 2+3 [DEG] = 5", "1. 2+3 [DEG] = 5",
        "1. 2+3 [DEG] = 5", "2. ans*2 [DEG, ans=5] = 10"), historyEntriesIn(output));
    assertEquals(5, occurrencesOf(output, FULL_MENU));
    assertEquals(2, occurrencesOf(output, EXPRESSION_PROMPT));
  }

  @Test
  void recordsOnlySuccessfulExpressionsInOrderAfterErrors() {
    String output = runCalculator("2+3\n1\nans*2\n1\nans/0\n4\n1\nans+1\n4\n3\n");

    assertEquals(List.of("5", "10", "11"), resultsIn(output));
    assertEquals(List.of("1. 2+3 [DEG] = 5", "2. ans*2 [DEG, ans=5] = 10",
        "1. 2+3 [DEG] = 5", "2. ans*2 [DEG, ans=5] = 10",
        "3. ans+1 [DEG, ans=10] = 11"), historyEntriesIn(output));
    assertEquals(1, occurrencesOf(output, "Fehler:"));
  }

  @Test
  void showsEmptyHistoryAfterFirstFailureAndKeepsTheMenuWithoutContinuation() {
    String output = runCalculator("sqrt(-1)\n4\n4\n1\n3\n");

    assertTrue(resultsIn(output).isEmpty());
    assertTrue(historyEntriesIn(output).isEmpty());
    assertEquals(2, occurrencesOf(output, "Noch keine erfolgreichen Berechnungen."));
    assertEquals(4, occurrencesOf(output, NO_RESULT_MENU));
    assertFalse(output.contains("1. Mit dem Ergebnis weiterrechnen"));
    assertEquals(1, occurrencesOf(output, "Bitte wähle 2, 3 oder 4."));
  }

  @Test
  void preservesOriginalExpressionWhitespaceInHistory() {
    String expression = "  3 + 4  ";
    String output = runCalculator(expression + "\n4\n3\n");

    assertEquals(List.of("7"), resultsIn(output));
    assertEquals(List.of("1. " + expression + " [DEG] = 7"), historyEntriesIn(output));
  }

  @Test
  void startsEveryRunWithEmptyHistoryNoAnsAndDegrees() {
    String first = runCalculator("mode\n2+3\n1\nans*2\n4\n3\n");
    String second = runCalculator("ans\n4\n3\n");

    assertEquals(List.of("1. 2+3 [RAD] = 5", "2. ans*2 [RAD, ans=5] = 10"), historyEntriesIn(first));
    assertTrue(historyEntriesIn(second).isEmpty());
    assertTrue(second.contains("Aktueller Winkelmodus: DEG"));
    assertFalse(second.contains("Aktueller Winkelmodus: RAD"));
    assertTrue(second.contains("kein letztes Ergebnis"));
    assertTrue(second.contains("Noch keine erfolgreichen Berechnungen."));
    assertFalse(second.contains("1. Mit dem Ergebnis weiterrechnen"));
  }

  @Test
  void switchesModesWithoutResultsHistoryOrAnIntermediateMenu() {
    String output = runCalculator(" \tmode \nmode\nsin(90)\n4\n3\n");

    assertEquals(List.of("1"), resultsIn(output));
    assertEquals(List.of("1. sin(90) [DEG] = 1"), historyEntriesIn(output));
    assertEquals(2, occurrencesOf(output, "Aktueller Winkelmodus: DEG"));
    assertEquals(1, occurrencesOf(output, "Aktueller Winkelmodus: RAD"));
    assertEquals(3, occurrencesOf(output, EXPRESSION_PROMPT));
    assertEquals(2, occurrencesOf(output, FULL_MENU));
  }

  @ParameterizedTest
  @ValueSource(strings = {"MODE", "mode()", "mode+1", "sin(0) mode"})
  void recognizesModeOnlyAsAStandaloneLowercaseCommand(String expression) {
    String output = runCalculator(expression + "\n3\n");

    assertTrue(resultsIn(output).isEmpty());
    assertTrue(output.contains("Fehler:"));
    assertFalse(output.contains("Aktueller Winkelmodus: RAD"));
    assertTrue(output.contains(NO_RESULT_MENU));
  }

  @Test
  void switchesToRadiansWhileContinuingWithAns() {
    String output = runCalculator("pi/2\n1\nmode\nsin(ans)\n4\n3\n");

    assertEquals(2, resultsIn(output).size());
    assertEquals(Math.PI / 2, Double.parseDouble(resultsIn(output).getFirst()), TOLERANCE);
    assertEquals("1", resultsIn(output).getLast());
    assertEquals(List.of("1. pi/2 [DEG] = " + Math.PI / 2,
        "2. sin(ans) [RAD, ans=" + Math.PI / 2 + "] = 1"), historyEntriesIn(output));
    assertEquals(1, occurrencesOf(output, "Aktuelles Ergebnis: "));
  }

  @Test
  void preservesResultAndAngleModeAfterErrorsAndKeepsModeOnRestart() {
    String output = runCalculator("90\n1\nmode\nans/0\n1\nsin(ans)\n2\nsin(90)\n3\n");
    List<String> results = resultsIn(output);

    assertEquals(3, results.size());
    assertEquals("90", results.getFirst());
    assertEquals(Math.sin(90), Double.parseDouble(results.get(1)), TOLERANCE);
    assertEquals(results.get(1), results.getLast());
    assertEquals(2, occurrencesOf(output, "Aktuelles Ergebnis: 90"));
    assertEquals(1, occurrencesOf(output, "Aktueller Winkelmodus: RAD"));
  }

  @Test
  void preservesHistoricalModesAndAnsValuesWhenDisplayingHistory() {
    String output = runCalculator("sin(90)\n4\n2\nmode\nsin(pi/2)\n4\n1\nmode\ncos(ans*60)\n4\n3\n");
    List<String> results = resultsIn(output);

    assertEquals(3, results.size());
    assertEquals("1", results.get(0));
    assertEquals("1", results.get(1));
    assertEquals(0.5, Double.parseDouble(results.get(2)), TOLERANCE);
    assertEquals(List.of("1. sin(90) [DEG] = 1",
        "1. sin(90) [DEG] = 1", "2. sin(pi/2) [RAD] = 1",
        "1. sin(90) [DEG] = 1", "2. sin(pi/2) [RAD] = 1",
        "3. cos(ans*60) [DEG, ans=1] = " + results.getLast()), historyEntriesIn(output));
  }

  @ParameterizedTest
  @CsvSource(value = {
      "2+3 | 5", "5-2 | 3", "5*2 | 10", "5/2 | 2.5",
      "sqrt(9) | 3", "abs(-2.75) | 2.75", "log(100) | 2", "log(0.1) | -1",
      "ln(e) | 1", "ln(0.5) | -0.6931471805599453",
      "factorial(0) | 1", "factorial(5) | 120", "factorial(20) | 2432902008176640000",
      "power(2,3) | 8", "root(27,3) | 3", "root(25,2) | 5",
      "root(-27,3) | -3", "root(-32,5) | -2", "root(0,3) | 0"
  }, delimiter = '|')
  void evaluatesExistingNonAngularOperationsInBothModes(String expression, double expected) {
    for (String prefix : List.of("", "mode\n")) {
      String output = runCalculator(prefix + expression + "\n4\n3\n");

      assertEquals(1, resultsIn(output).size());
      assertEquals(expected, Double.parseDouble(resultsIn(output).getFirst()), TOLERANCE);
      String mode = prefix.isEmpty() ? "DEG" : "RAD";
      assertEquals(List.of("1. " + expression + " [" + mode + "] = " + resultsIn(output).getFirst()),
          historyEntriesIn(output));
      assertFalse(output.contains("Fehler:"));
    }
  }

  @ParameterizedTest
  @CsvSource(value = {
      "sqrt(-1) | Quadratwurzel einer negativen Zahl",
      "5/0 | Division durch null", "1/-0.0 | Division durch null",
      "log(0) | Logarithmus ist nur für Zahlen größer als null", "log(-1) | Logarithmus ist nur für Zahlen größer als null",
      "ln(0) | Logarithmus ist nur für Zahlen größer als null", "ln(-1) | Logarithmus ist nur für Zahlen größer als null",
      "factorial(-1) | nichtnegative ganze Zahlen", "factorial(2.5) | nichtnegative ganze Zahlen",
      "factorial(21) | Werte von 0 bis 20",
      "root(-16,4) | gerade Wurzel aus einer negativen Zahl", "root(27,0) | positive ganze Zahl",
      "root(27,-3) | positive ganze Zahl", "root(27,2.5) | positive ganze Zahl",
      "power(-2,0.5) | kein endliches Ergebnis", "power(1e308,2) | kein endliches Ergebnis",
      "1e308+1e308 | kein endliches Ergebnis", "1e309 | Zahl muss endlich sein"
  }, delimiter = '|')
  void reportsMathErrorsWithoutHistoryAndAllowsANewCalculation(String expression, String message) {
    String output = runCalculator(expression + "\n4\n2\nsqrt(25)\n3\n");

    assertEquals(List.of("5"), resultsIn(output));
    assertTrue(output.contains("Fehler:"));
    assertTrue(output.contains(message));
    assertTrue(historyEntriesIn(output).isEmpty());
    assertTrue(output.contains("Noch keine erfolgreichen Berechnungen."));
    String beforeSuccess = output.substring(0, output.indexOf("Das Ergebnis ist: 5"));
    assertFalse(beforeSuccess.contains("1. Mit dem Ergebnis weiterrechnen"));
    assertFalse(output.contains(EOF_MESSAGE));
  }

  @ParameterizedTest
  @CsvSource(value = {
      "-1 | log(ans) | 0", "-1 | ln(ans) | 0", "-1 | sqrt(ans) | 0",
      "-1 | factorial(ans) | 0", "2.5 | factorial(ans) | 3.5",
      "21 | factorial(ans) | 22", "-16 | root(ans,4) | -15",
      "27 | root(ans,0) | 28", "27 | root(ans,-3) | 28",
      "27 | root(ans,2.5) | 28", "90 | tan(ans) | 91"
  }, delimiter = '|')
  void preservesAnsAfterScientificFunctionErrors(String initial, String failedExpression, double expected) {
    String output = runCalculator(initial + "\n1\n" + failedExpression + "\n4\n1\nans+1\n3\n");

    assertEquals(2, resultsIn(output).size());
    assertEquals(expected, Double.parseDouble(resultsIn(output).getLast()), TOLERANCE);
    assertEquals(List.of("1. " + initial + " [DEG] = " + initial), historyEntriesIn(output));
    assertEquals(2, occurrencesOf(output, "Aktuelles Ergebnis: " + initial));
    assertEquals(1, occurrencesOf(output, "Fehler:"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"NaN", "Infinity", "-Infinity", "cos(NaN)", "tan(Infinity)",
      "root(27,NaN)", "root(Infinity,3)", "root(27,-Infinity)", "sin(1e309)"})
  void rejectsNonFiniteInputTokensAndFunctionOperands(String expression) {
    String output = runCalculator(expression + "\n4\n3\n");

    assertTrue(resultsIn(output).isEmpty());
    assertTrue(historyEntriesIn(output).isEmpty());
    assertTrue(output.contains("Fehler:"));
    assertTrue(output.contains(NO_RESULT_MENU));
    assertFalse(output.contains(EOF_MESSAGE));
  }

  @ParameterizedTest
  @ValueSource(strings = {"pi", "PI", "Pi", "pI", "e", "E"})
  void acceptsConstantsAsFirstOperands(String constant) {
    String output = runCalculator(constant + "+0\n3\n");

    assertEquals(List.of(Double.toString(constantValue(constant))), resultsIn(output));
  }

  @ParameterizedTest
  @ValueSource(strings = {"pi", "PI", "Pi", "pI", "e", "E"})
  void acceptsConstantsAsSecondOperands(String constant) {
    String output = runCalculator("2+" + constant + "\n3\n");

    assertEquals(List.of(Double.toString(2 + constantValue(constant))), resultsIn(output));
  }

  @ParameterizedTest
  @ValueSource(strings = {"pi", "PI", "Pi", "pI", "e", "E"})
  void acceptsConstantsAsExponents(String constant) {
    String output = runCalculator("power(2," + constant + ")\n3\n");

    assertEquals(List.of(Double.toString(Math.pow(2, constantValue(constant)))), resultsIn(output));
  }

  @ParameterizedTest
  @ValueSource(strings = {"pi", "PI", "Pi", "pI", "e", "E"})
  void acceptsConstantsWhenContinuingWithAns(String constant) {
    String output = runCalculator("2+3\n1\nans*" + constant + "\n3\n");

    assertEquals(List.of("5", Double.toString(5 * constantValue(constant))), resultsIn(output));
    assertTrue(output.contains("Aktuelles Ergebnis: 5"));
  }

  private static double constantValue(String constant) {
    return constant.equalsIgnoreCase("pi") ? Math.PI : Math.E;
  }

  @ParameterizedTest
  @CsvSource(value = {
      "sin(0) | 0", "sin(90) | 1", "sin(180) | 0", "sin(-90) | -1",
      "cos(0) | 1", "cos(60) | 0.5", "cos(90) | 0", "cos(180) | -1",
      "tan(0) | 0", "tan(30) | 0.5773502691896257", "tan(45) | 1",
      "tan(60) | 1.7320508075688772", "tan(135) | -1", "tan(180) | 0",
      "tan(-30) | -0.5773502691896257", "tan(-45) | -1",
      "tan(-60) | -1.7320508075688772", "tan(-135) | 1"
  }, delimiter = '|')
  void calculatesTrigonometricExpressionsInDegreesByDefault(String expression, double expected) {
    String output = runCalculator(expression + "\n3\n");

    assertEquals(1, resultsIn(output).size());
    assertEquals(expected, Double.parseDouble(resultsIn(output).getFirst()), TOLERANCE);
    assertTrue(output.contains("Aktueller Winkelmodus: DEG"));
    assertFalse(output.contains("Fehler:"));
  }

  @ParameterizedTest
  @CsvSource(value = {"sin(pi/2) | 1", "cos(pi) | -1", "tan(pi/4) | 1"}, delimiter = '|')
  void calculatesTrigonometricExpressionsInRadians(String expression, double expected) {
    String output = runCalculator("mode\n" + expression + "\n3\n");

    assertEquals(1, resultsIn(output).size());
    assertEquals(expected, Double.parseDouble(resultsIn(output).getFirst()), TOLERANCE);
    assertTrue(output.contains("Aktueller Winkelmodus: RAD"));
    assertFalse(output.contains("Fehler:"));
  }

  @ParameterizedTest
  @ValueSource(ints = {90, 270, 990, -90, -270, -990, 450, -450, 1350, -1350})
  void rejectsTangentSingularitiesInDegrees(int angle) {
    String output = runCalculator("tan(" + angle + ")\n4\n3\n");

    assertTrue(resultsIn(output).isEmpty());
    assertTrue(historyEntriesIn(output).isEmpty());
    assertTrue(output.contains("Fehler: Tangens ist für diesen Winkel nicht definiert."));
    assertFalse(output.contains("1. Mit dem Ergebnis weiterrechnen"));
  }

  @ParameterizedTest
  @ValueSource(ints = {90, 270, 990, -90, -270, -990})
  void keepsThePreviousResultAfterFailedTangentInDegrees(int angle) {
    String output = runCalculator(angle + "\n1\ntan(ans)\n1\nans+1\n3\n");

    assertEquals(List.of(Integer.toString(angle), Integer.toString(angle + 1)), resultsIn(output));
    assertTrue(output.contains("Fehler: Tangens ist für diesen Winkel nicht definiert."));
    assertEquals(2, occurrencesOf(output, "Aktuelles Ergebnis: " + angle));
  }

  @Test
  void rejectsTangentOfHalfPiInRadiansAndKeepsThePreviousResult() {
    String output = runCalculator("pi/2\n1\nmode\ntan(ans)\n1\nans+1\n3\n");

    assertEquals(2, resultsIn(output).size());
    assertEquals(Math.PI / 2, Double.parseDouble(resultsIn(output).getFirst()), TOLERANCE);
    assertEquals(Math.PI / 2 + 1, Double.parseDouble(resultsIn(output).getLast()), TOLERANCE);
    assertTrue(output.contains("Fehler: Tangens ist für diesen Winkel nicht definiert."));
    assertEquals(2, occurrencesOf(output, "Aktuelles Ergebnis: " + Math.PI / 2));
  }

  @Test
  void calculatesTangentWhileSwitchingBetweenRadiansAndDegrees() {
    String output = runCalculator("pi/4\n1\nmode\ntan(ans)\n2\nmode\ntan(45)\n3\n");

    assertEquals(3, resultsIn(output).size());
    assertEquals(Math.PI / 4, Double.parseDouble(resultsIn(output).getFirst()), TOLERANCE);
    assertEquals(1, Double.parseDouble(resultsIn(output).get(1)), TOLERANCE);
    assertEquals(1, Double.parseDouble(resultsIn(output).getLast()), TOLERANCE);
    assertEquals(1, occurrencesOf(output, "Aktueller Winkelmodus: RAD"));
    assertEquals(2, occurrencesOf(output, "Aktueller Winkelmodus: DEG"));
  }

  @ParameterizedTest
  @ValueSource(ints = {90, 270, 990, 1350, 1890, -90, -270, -990, -1350, -1890})
  void acceptsRepresentableDegreeAnglesNextToTangentSingularities(int singularity) {
    for (double angle : new double[] {Math.nextDown((double) singularity), Math.nextUp((double) singularity)}) {
      String output = runCalculator("tan(" + angle + ")\n3\n");
      // cot(delta) ist bei diesen Abständen bis auf double-Rundungen gleich 1/delta.
      double expected = 1.0 / Math.toRadians(singularity - angle);

      assertEquals(1, resultsIn(output).size());
      assertEquals(expected, Double.parseDouble(resultsIn(output).getFirst()), Math.abs(expected) * TOLERANCE);
      assertFalse(output.contains("Fehler:"));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"89.999999999999999", "90.000000000000001"})
  void rejectsDegreeInputsAlreadyRoundedToASingularity(String angle) {
    assertEquals(90, Double.parseDouble(angle));
    String output = runCalculator("tan(" + angle + ")\n3\n");

    assertTrue(resultsIn(output).isEmpty());
    assertTrue(output.contains("Fehler: Tangens ist für diesen Winkel nicht definiert."));
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 3, 11, -1, -3, -11})
  void keepsTangentSingularityRulesInRadians(int oddMultiple) {
    double angle = oddMultiple * Math.PI / 2;
    String output = runCalculator("mode\ntan(" + angle + ")\n3\n");

    assertTrue(resultsIn(output).isEmpty());
    assertTrue(output.contains("Fehler: Tangens ist für diesen Winkel nicht definiert."));
    assertTrue(output.contains("Aktueller Winkelmodus: RAD"));
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 3, 11, -1, -3, -11})
  void keepsTangentNeighborValuesInRadians(int oddMultiple) {
    double singularity = oddMultiple * Math.PI / 2;
    for (double angle : new double[] {Math.nextDown(singularity), Math.nextUp(singularity)}) {
      String output = runCalculator("mode\ntan(" + angle + ")\n3\n");
      double expected = Math.tan(angle);

      assertEquals(1, resultsIn(output).size());
      assertEquals(expected, Double.parseDouble(resultsIn(output).getFirst()), 2 * Math.ulp(expected));
      assertFalse(output.contains("Fehler:"));
    }
  }

  @ParameterizedTest
  @CsvSource({
      "3.14, 3.14", "0.5, 0.5", "-2.75, -2.75", "42, 42", "42.0, 42", "-0.0, -0",
      "+3.5, 3.5", ".5, 0.5", "3., 3", "1.25e-7, 1.25E-7", "-2.75E-4, -2.75E-4",
      "1e20, 100000000000000000000"
  })
  void preservesDecimalFormattingRegardlessOfScannerLocale(String expression, String expected) {
    for (Locale locale : List.of(Locale.GERMANY, Locale.US)) {
      String output = runCalculator(expression + "\n4\n3\n", locale);

      assertEquals(List.of(expected), resultsIn(output), locale.toLanguageTag());
      assertEquals(List.of("1. " + expression + " [DEG] = " + expected), historyEntriesIn(output));
      assertFalse(output.contains("Fehler:"));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"3,14", "0,5", "-2,75", "1,234", "-1,234", "+1,234",
      "1,234.56", "1,234e2", "1,234,567", "1.234,56", "0x1.6p1", "٣.٥"})
  void rejectsFormatsOutsideEvaluatorSyntaxAndAllowsACorrectedExpression(String expression) {
    for (Locale locale : List.of(Locale.GERMANY, Locale.US)) {
      String output = runCalculator(expression + "\n2\n3.14\n3\n", locale);

      assertEquals(List.of("3.14"), resultsIn(output), locale.toLanguageTag());
      assertEquals(1, occurrencesOf(output, "Fehler:"));
      assertEquals(2, occurrencesOf(output, EXPRESSION_PROMPT));
      assertTrue(output.contains(NO_RESULT_MENU));
      assertFalse(output.contains(EOF_MESSAGE));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"abc", "2+", "()", "(1+2", "1+2)", "1 2", "sqrt()",
      "sqrt(1,2)", "power(2)", "power(2,,3)", "Sin(90)", "2pi", "2^3"})
  void reportsSyntaxErrorsAndAllowsANewExpression(String expression) {
    String output = runCalculator(expression + "\n2\n2+2\n3\n");

    assertEquals(List.of("4"), resultsIn(output));
    assertEquals(1, occurrencesOf(output, "Fehler:"));
    assertTrue(output.contains("Position"));
    assertTrue(output.contains(NO_RESULT_MENU));
    assertFalse(output.contains(EOF_MESSAGE));
  }

  @ParameterizedTest
  @ValueSource(strings = {"de-DE", "en-US"})
  void usesDecimalPointsAndArgumentSeparatorsWhenContinuing(String localeTag) {
    String output = runCalculator("""
        2+3,14
        2
        2+0.5
        1
        power(ans,2,0)
        1
        power(ans,2)
        1
        root(ans,2,0)
        1
        root(ans,2)
        4
        3
        """, Locale.forLanguageTag(localeTag));

    assertEquals(List.of("2.5", "6.25", "2.5"), resultsIn(output));
    assertEquals(3, occurrencesOf(output, "Fehler:"));
    assertEquals(List.of("1. 2+0.5 [DEG] = 2.5", "2. power(ans,2) [DEG, ans=2.5] = 6.25",
        "3. root(ans,2) [DEG, ans=6.25] = 2.5"), historyEntriesIn(output));
    assertFalse(output.contains(EOF_MESSAGE));
  }

  @ParameterizedTest
  @ValueSource(doubles = {Double.MIN_VALUE, -0.0, Math.PI, Math.E, 1e20})
  void reentersDisplayedNumbersWithTheSameDoubleValueWhenTheyFitTheInputLimit(double number) {
    for (Locale locale : List.of(Locale.GERMANY, Locale.US)) {
      String output = runCalculator(Double.toString(number) + "\n3\n", locale);
      assertEquals(1, resultsIn(output).size());
      String displayed = resultsIn(output).getFirst();

      String repeated = runCalculator(displayed + "\n3\n", locale);
      assertEquals(1, resultsIn(repeated).size());
      assertEquals(Double.doubleToLongBits(number),
          Double.doubleToLongBits(Double.parseDouble(resultsIn(repeated).getFirst())));
    }
  }

  @Test
  void acceptsDoubleMaxInScientificNotationButRejectsItsOversizedFormattedIntegerOnReentry() {
    String output = runCalculator(Double.toString(Double.MAX_VALUE) + "\n1\nans\n3\n");
    assertEquals(2, resultsIn(output).size());
    String displayed = resultsIn(output).getFirst();
    assertEquals(displayed, resultsIn(output).getLast());
    assertTrue(displayed.length() > 256);

    String repeated = runCalculator(displayed + "\n3\n");
    assertTrue(resultsIn(repeated).isEmpty());
    assertTrue(repeated.contains("höchstens 256 Zeichen"));
    assertTrue(repeated.contains(NO_RESULT_MENU));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("expressionBoundaries")
  void enforcesEveryInputLimitThroughTheConsole(String scenario, String valid, String invalid,
      double expected, String message) {
    String output = runCalculator(valid + "\n1\n" + invalid + "\n4\n1\nans+1\n3\n");
    List<String> results = resultsIn(output);

    assertEquals(2, results.size(), scenario);
    assertEquals(expected, Double.parseDouble(results.getFirst()), TOLERANCE, scenario);
    assertEquals(expected + 1, Double.parseDouble(results.getLast()), TOLERANCE, scenario);
    assertEquals(List.of("1. " + valid + " [DEG] = " + results.getFirst()), historyEntriesIn(output));
    assertEquals(1, occurrencesOf(output, "Fehler:"), scenario);
    assertTrue(output.contains(message), scenario);
    assertEquals(3, occurrencesOf(output, EXPRESSION_PROMPT), scenario);
    assertEquals(2, occurrencesOf(output, "Aktuelles Ergebnis: "), scenario);
    assertFalse(output.contains(EOF_MESSAGE), scenario);
  }

  private static Stream<Arguments> expressionBoundaries() {
    String length = " ".repeat(127) + "1" + " ".repeat(128);
    String binary = "1" + "+1".repeat(50);
    String mixed = "+" + "abs(1)+".repeat(24) + "abs(1)";
    String parentheses = "(".repeat(16) + "1" + ")".repeat(16);
    String functions = "abs(".repeat(16) + "1" + ")".repeat(16);
    String mixedDepth = "(".repeat(8) + "abs(".repeat(8) + "1" + ")".repeat(16);
    String scientific = "1e-3" + "+1".repeat(50);
    String combined = "(".repeat(15) + "+".repeat(49) + "abs(1)" + ")".repeat(15);
    combined += " ".repeat(256 - combined.length());
    return Stream.of(
        Arguments.of("256/257 Zeichen einschließlich Leerzeichen", length, length + " ", 1, "höchstens 256 Zeichen"),
        Arguments.of("50/51 binäre Operationen", binary, binary + "+1", 51, "höchstens 50 Operationen"),
        Arguments.of("50/51 unäre Vorzeichen", "+".repeat(50) + "1", "+".repeat(51) + "1", 1,
            "höchstens 50 Operationen"),
        Arguments.of("50/51 gemischte Operationen", mixed, mixed + "+1", 25, "höchstens 50 Operationen"),
        Arguments.of("16/17 normale Klammerebenen", parentheses, "(" + parentheses + ")", 1,
            "höchstens 16 Klammerebenen"),
        Arguments.of("16/17 Funktionsklammerebenen", functions, "abs(" + functions + ")", 1,
            "höchstens 16 Klammerebenen"),
        Arguments.of("16/17 gemeinsame Klammerebenen", mixedDepth, "(" + mixedDepth + ")", 1,
            "höchstens 16 Klammerebenen"),
        Arguments.of("Wissenschaftliche Exponentenvorzeichen zählen nicht", scientific, scientific + "+1", 50.001,
            "höchstens 50 Operationen"),
        Arguments.of("Alle Grenzen gleichzeitig", combined, combined + " ", 1, "höchstens 256 Zeichen")
    );
  }

  @Test
  void exitsAtEndOfInputBeforeTheFirstExpression() {
    String output = runCalculatorUntilEndOfInput("");

    assertTrue(resultsIn(output).isEmpty());
    assertEquals(1, occurrencesOf(output, EXPRESSION_PROMPT));
    assertFalse(output.contains("Fehler:"));
    assertFalse(output.contains("Was möchtest du als Nächstes tun?"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"\n", " \t\n"})
  void treatsBlankLinesAsInvalidExpressionsRatherThanEndOfInput(String input) {
    String output = runCalculatorUntilEndOfInput(input);

    assertTrue(resultsIn(output).isEmpty());
    assertTrue(output.contains("Ausdruck darf nicht leer sein"));
    assertEquals(1, occurrencesOf(output, NO_RESULT_MENU));
  }

  @ParameterizedTest
  @ValueSource(strings = {"2+3", "2+3\n", "sqrt(25)"})
  void exitsAtEndOfInputAfterASuccessfulCalculation(String input) {
    String output = runCalculatorUntilEndOfInput(input);

    assertEquals(List.of("5"), resultsIn(output));
    assertEquals(1, occurrencesOf(output, FULL_MENU));
  }

  @Test
  void exitsAtEndOfInputWhenANewExpressionIsRequestedAfterRestart() {
    String output = runCalculatorUntilEndOfInput("2+3\n2");

    assertEquals(List.of("5"), resultsIn(output));
    assertEquals(2, occurrencesOf(output, EXPRESSION_PROMPT));
    assertFalse(output.contains("Aktuelles Ergebnis: "));
  }

  @Test
  void exitsAtEndOfInputWhenContinuingWithThePreviousResult() {
    String output = runCalculatorUntilEndOfInput("2+3\n1");

    assertEquals(List.of("5"), resultsIn(output));
    assertTrue(output.contains("Aktuelles Ergebnis: 5"));
    assertTrue(output.contains("Verwende ans im nächsten Ausdruck"));
    assertEquals(2, occurrencesOf(output, EXPRESSION_PROMPT));
  }

  @ParameterizedTest
  @ValueSource(strings = {"abc", "2+", "power(2,", "root(27,", "ans", "3,14", "sqrt(-1)"})
  void exitsAtEndOfInputAfterSyntaxOrMathErrors(String input) {
    String output = runCalculatorUntilEndOfInput(input);

    assertTrue(resultsIn(output).isEmpty());
    assertEquals(1, occurrencesOf(output, "Fehler:"));
    assertEquals(1, occurrencesOf(output, NO_RESULT_MENU));
  }

  @Test
  void exitsAtEndOfInputAfterAnInvalidMenuLine() {
    String output = runCalculatorUntilEndOfInput("2+3\n1 2");

    assertEquals(List.of("5"), resultsIn(output));
    assertEquals(1, occurrencesOf(output, "Bitte wähle 1, 2, 3 oder 4."));
    assertEquals(2, occurrencesOf(output, FULL_MENU));
  }

  @ParameterizedTest
  @ValueSource(strings = {"2+3\n4", "sqrt(-1)\n4"})
  void exitsAtEndOfInputImmediatelyAfterShowingHistory(String input) {
    String output = runCalculatorUntilEndOfInput(input);

    assertEquals(1, occurrencesOf(output, "Rechenverlauf:"));
    assertEquals(2, occurrencesOf(output, "Was möchtest du als Nächstes tun?"));
    if (resultsIn(output).isEmpty()) {
      assertTrue(historyEntriesIn(output).isEmpty());
      assertTrue(output.contains("Noch keine erfolgreichen Berechnungen."));
    } else {
      assertEquals(List.of("1. 2+3 [DEG] = 5"), historyEntriesIn(output));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"mode", "mode\nmode"})
  void exitsAtEndOfInputAfterSwitchingMode(String input) {
    String output = runCalculatorUntilEndOfInput(input);

    assertTrue(resultsIn(output).isEmpty());
    assertTrue(output.contains("Aktueller Winkelmodus: RAD"));
    assertFalse(output.contains("Was möchtest du als Nächstes tun?"));
    assertFalse(output.contains("Fehler:"));
  }

  @Test
  void doesNotSwallowUnexpectedScannerFailures() {
    Scanner scanner = new Scanner("");
    scanner.close();
    try (PrintStream output = new PrintStream(new ByteArrayOutputStream())) {
      assertThrows(IllegalStateException.class, () -> Calculator.run(scanner, output));
    }
  }

  private static String runCalculator(String input) {
    return runCalculator(input, Locale.US);
  }

  private static String runCalculator(String input, Locale locale) {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try (Scanner scanner = new Scanner(input).useLocale(locale);
         PrintStream output = new PrintStream(buffer, true, StandardCharsets.UTF_8)) {
      Calculator.run(scanner, output);
    }
    return buffer.toString(StandardCharsets.UTF_8);
  }

  private static String runCalculatorUntilEndOfInput(String input) {
    String output = assertDoesNotThrow(() -> runCalculator(input));

    assertEquals(1, occurrencesOf(output, EOF_MESSAGE));
    assertTrue(output.endsWith(EOF_MESSAGE + System.lineSeparator()));
    assertFalse(output.contains("Exception"));
    return output;
  }

  private static List<String> resultsIn(String output) {
    return Pattern.compile("Das Ergebnis ist: ([^\\r\\n]+)").matcher(output).results()
        .map(match -> match.group(1))
        .toList();
  }

  private static List<String> historyEntriesIn(String output) {
    return Pattern.compile("(?m)^\\d+\\. [^\\r\\n]+ = [^\\r\\n]+").matcher(output).results()
        .map(match -> match.group())
        .toList();
  }

  private static long occurrencesOf(String output, String text) {
    return Pattern.compile(Pattern.quote(text)).matcher(output).results().count();
  }
}
