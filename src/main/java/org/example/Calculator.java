package org.example;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class Calculator {

  static void main(String[] args) {
    try (Scanner scanner = new Scanner(System.in)) {
      run(scanner, System.out);
    }
  }

  static void run(Scanner scanner, PrintStream output) {
    showWelcomeMessage(output);
    ExpressionEvaluator evaluator = new ExpressionEvaluator();
    List<String> history = new ArrayList<>();
    Double lastResult = null;
    AngleMode angleMode = AngleMode.DEG;
    output.println("Aktueller Winkelmodus: " + angleMode);

    try {
      while (true) {
        output.print("Gib einen Rechenausdruck ein: ");
        String expression = readLine(scanner);
        if (expression.strip().equals("mode")) {
          angleMode = angleMode == AngleMode.DEG ? AngleMode.RAD : AngleMode.DEG;
          output.println("Aktueller Winkelmodus: " + angleMode);
          continue;
        }

        try {
          double result = evaluator.evaluate(expression, angleMode, lastResult);
          history.add(formatHistoryEntry(expression, result, angleMode, lastResult));
          output.print("Das Ergebnis ist: ");
          printNumber(output, result);
          lastResult = result;
        } catch (IllegalArgumentException | ArithmeticException exception) {
          output.println("Fehler: " + exception.getMessage());
        }

        String choice = getValidNextAction(scanner, output, lastResult != null, history);
        if (choice.equals("3")) {
          return;
        }
        if (choice.equals("1")) {
          output.print("Aktuelles Ergebnis: ");
          printNumber(output, lastResult);
          output.println("Verwende ans im nächsten Ausdruck für dieses Ergebnis. "
              + "Ohne ans wird der Ausdruck eigenständig ausgewertet.");
        } else {
          lastResult = null;
        }
      }
    } catch (EndOfInputException exception) {
      output.println();
      output.println("Eingabe beendet. Taschenrechner wird beendet.");
    }
  }

  private static void printNumber(PrintStream output, double number) {
    output.println(formatNumber(number));
  }

  private static String formatNumber(double number) {
    return number % 1 == 0
        ? String.format(Locale.US, "%.0f", number)
        : Double.toString(number);
  }

  private static String formatHistoryEntry(String expression, double result,
      AngleMode angleMode, Double previousResult) {
    String context = angleMode.toString();
    if (previousResult != null) {
      context += ", ans=" + formatNumber(previousResult);
    }
    return expression + " [" + context + "] = " + formatNumber(result);
  }

  private static void showHistory(PrintStream output, List<String> history) {
    output.println();
    output.println("Rechenverlauf:");
    if (history.isEmpty()) {
      output.println("Noch keine erfolgreichen Berechnungen.");
    } else {
      for (int index = 0; index < history.size(); index++) {
        output.println((index + 1) + ". " + history.get(index));
      }
    }
  }

  private static void showWelcomeMessage(PrintStream output) {
    output.println("================================ JAVA CALCULATOR ================================");
    output.println("Gib vollständige Ausdrücke ein, z. B. 3+4+5*4-5 oder (3+4)*2.");
    output.println("Punkt vor Strich; Klammern ändern die Reihenfolge. Operatoren: +, -, *, /.");
    output.println("Multiplikation immer mit * schreiben, z. B. 2*pi.");
    output.println("Zahlen mit Dezimalpunkt: 3.14, .5, 3.; wissenschaftlich: 1.25e-7.");
    output.println("Konstanten: pi (auch PI/Pi) und e/E.");
    output.println("Funktionen: sqrt(x), abs(x), sin(x), cos(x), tan(x), log(x), ln(x), factorial(x).");
    output.println("Kommas trennen Argumente: power(2,3) und root(27,3).");
    output.println("ans verwendet das letzte Ergebnis; nach einer neuen Berechnung ist ans nicht verfügbar.");
    output.println("ans und Funktionsnamen werden kleingeschrieben.");
    output.println("mode wechselt an der Ausdrucksabfrage zwischen DEG und RAD; Standard ist DEG.");
    output.println("Grenzen: 256 Zeichen einschließlich Leerzeichen, 50 Operationen "
        + "(Operatoren, Vorzeichen, Funktionsaufrufe), 16 gleichzeitig offene Klammerebenen.");
    output.println("=================================================================================");
  }

  private static String getValidNextAction(Scanner scanner, PrintStream output,
      boolean hasResult, List<String> history) {
    while (true) {
      output.println();
      output.println("Was möchtest du als Nächstes tun?");
      if (hasResult) {
        output.println("1. Mit dem Ergebnis weiterrechnen");
      }
      output.println("2. Neue Berechnung starten");
      output.println("3. Taschenrechner beenden");
      output.println("4. Verlauf anzeigen");
      output.print("Deine Auswahl: ");

      String choice = readLine(scanner).strip();
      if (choice.equals("4")) {
        showHistory(output, history);
        continue;
      }
      if (choice.equals("2") || choice.equals("3") || (hasResult && choice.equals("1"))) {
        return choice;
      }
      output.println(hasResult ? "Bitte wähle 1, 2, 3 oder 4." : "Bitte wähle 2, 3 oder 4.");
    }
  }

  private static String readLine(Scanner scanner) {
    if (!scanner.hasNextLine()) {
      throw new EndOfInputException();
    }
    return scanner.nextLine();
  }

  /** Internes Signal für ein kontrolliertes Ende der Konsoleneingabe. */
  private static final class EndOfInputException extends RuntimeException {
  }
}
