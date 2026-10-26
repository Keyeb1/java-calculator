package org.example;
import java.io.PrintStream;
import java.util.Scanner;

public class Calculator {
  static void main(String[] args) {
    try (Scanner scanner = new Scanner(System.in)) {
      run(scanner, System.out);
    }
  }

  static void run(Scanner scanner, PrintStream output) {
    showWelcomeMessage(output);
    CalculatorEngine engine = new CalculatorEngine();
    Double lastResult = null;
    boolean continueWithResult = false;
    boolean degreeMode = true;
    output.println("Aktueller Winkelmodus: DEG");

    while (true) {
      double number1 = continueWithResult
          ? lastResult
          : getValidNumber(scanner, output, "Gib die erste Zahl ein: ");

      if (continueWithResult) {
        output.print("Aktuelles Ergebnis: ");
        printNumber(output, number1);
      }

      String operator;
      do {
        operator = getValidOperator(scanner, output,
            "Wähle einen Operator aus: ");
        if (operator.equals("mode")) {
          degreeMode = !degreeMode;
          output.println("Aktueller Winkelmodus: " + (degreeMode ? "DEG" : "RAD"));
        }
      } while (operator.equals("mode"));

      double number2 = 0.0;

      if (operator.equals("+") || operator.equals("-") || operator.equals("*") || operator.equals("/")) {
        number2 = getValidNumber(scanner, output, "Gib die zweite Zahl ein: ");
      }

      double exponent = 0.0;

      if (operator.equals("power")) {
        exponent = getValidNumber(scanner, output, "Gib den Exponent ein: ");
      }

      try {
        double result;
        switch (operator) {
          case "+":
            result = engine.add(number1, number2);
            output.print("Du hast dich für die Addition entschieden." + " Das Ergebnis ist: ");
            break;

          case "-":
            result = engine.subtract(number1, number2);
            output.print("Du hast dich für die Subtraktion entschieden." + " Das Ergebnis ist: ");
            break;

          case "*":
            result = engine.multiply(number1, number2);
            output.print("Du hast dich für die Multiplikation entschieden." + " Das Ergebnis ist: ");
            break;

          case "/":
            result = engine.divide(number1, number2);
            output.print("Du hast dich für die Division entschieden." + " Das Ergebnis ist: ");
            break;

          case "sqrt":
            result = engine.sqrt(number1);
            output.print("Du hast dich für die Quadratwurzel entschieden." + " Das Ergebnis ist: ");
            break;

          case "abs":
            result = engine.abs(number1);
            output.print("Du hast dich für die Betragsfunktion entschieden." + " Das Ergebnis ist: ");
            break;

          case "sin":
            result = engine.sin(degreeMode ? Math.toRadians(number1) : number1);
            output.print("Du hast dich für den Sinus entschieden." + " Das Ergebnis ist: ");
            break;

          case "cos":
            result = engine.cos(degreeMode ? Math.toRadians(number1) : number1);
            output.print("Du hast dich für den Kosinus entschieden." + " Das Ergebnis ist: ");
            break;

          case "tan":
            // Grad-Singularitäten vor der Umrechnung in Radiant prüfen.
            if (degreeMode && Math.abs(number1 % 180) == 90) {
              throw new ArithmeticException("Tangens ist für diesen Winkel nicht definiert.");
            }
            result = engine.tan(degreeMode ? Math.toRadians(number1) : number1);
            output.print("Du hast dich für den Tangens entschieden." + " Das Ergebnis ist: ");
            break;

          case "log":
            result = engine.log(number1);
            output.print("Du hast dich für den dekadischen Logarithmus entschieden." + " Das Ergebnis ist: ");
            break;

          case "ln":
            result = engine.ln(number1);
            output.print("Du hast dich für den natürlichen Logarithmus entschieden." + " Das Ergebnis ist: ");
            break;

          case "power":
            result = engine.power(number1, exponent);
            output.print("Du hast dich für die Potenz entschieden." + " Das Ergebnis ist: ");
            break;

          default:
            throw new IllegalArgumentException("Unbekannter Operator: " + operator);
        }

        lastResult = result;
        printNumber(output, result);
      } catch (IllegalArgumentException | ArithmeticException exception) {
        output.println("Fehler: " + exception.getMessage());
      }
      String choice = getValidNextAction(scanner, output, lastResult != null);
      if (choice.equals("3")) {
        return;
      }
      continueWithResult = choice.equals("1");
      if (choice.equals("2")) {
        lastResult = null;
      }
    }
  }

  private static void printNumber(PrintStream output, double number) {
    if (number % 1 == 0) {
      output.printf("%.0f%n", number);
    } else {
      output.println(number);
    }
  }


  private static void showWelcomeMessage(PrintStream output) {
    output.println("======================================== JAVA CALCULATOR ========================================");
    output.println("Verfügbare Operationen:");
    output.println("+      Addition");
    output.println("-      Subtraktion");
    output.println("*      Multiplikation");
    output.println("/      Division");
    output.println("sqrt   Quadratwurzel");
    output.println("abs    Betrag");
    output.println("sin    Sinus");
    output.println("cos    Kosinus");
    output.println("tan    Tangens");
    output.println("log    Dekadischer Logarithmus (Basis 10)");
    output.println("ln     Natürlicher Logarithmus (Basis e)");
    output.println("power  Potenzberechnung");
    output.println("mode   Zwischen DEG (Grad) und RAD (Radiant) wechseln; Standard ist DEG");
    output.println();
    output.println("Folge einfach den Anweisungen und gib die");
    output.println("gewünschten Werte in der Konsole ein.");
    output.println("Du kannst pi (auch PI oder Pi) statt einer Zahl eingeben.");
    output.println("Du kannst auch e oder E für die Eulersche Zahl eingeben.");
    output.println();
    output.println("Los geht's!");
    output.println("===============================================================================================");

  }

  private static double getValidNumber(Scanner scanner, PrintStream output, String inputText) {

    boolean validNumber = false;
    double number = 0;
    do {
      output.print(inputText);
      if(scanner.hasNextDouble()) {
        number = scanner.nextDouble();
        validNumber = true;
      } else {
        String input = scanner.next();
        if (input.equalsIgnoreCase("pi")) {
          number = Math.PI;
          validNumber = true;
        } else if (input.equalsIgnoreCase("e")) {
          number = Math.E;
          validNumber = true;
        } else {
          output.println("Bitte gebe eine Zahl ein! ");
        }
      }
    } while (!validNumber);

    return number;
  }


  private static String getValidOperator(Scanner scanner, PrintStream output, String inputText) {

    String operator;
    do {
      output.print(inputText);
      operator = scanner.next();
      if(!operator.equals("+") && !operator.equals("-") && !operator.equals("*") && !operator.equals("/") && !operator.equals("sqrt") && !operator.equals("abs") && !operator.equals("sin") && !operator.equals("cos") && !operator.equals("tan") && !operator.equals("log") && !operator.equals("ln") && !operator.equals("power") && !operator.equals("mode")) {
        output.println("Bitte gib einen der erlaubten Operatoren ein!");
      }
    } while (!operator.equals("+") && !operator.equals("-") && !operator.equals("*") && !operator.equals("/") && !operator.equals("sqrt") && !operator.equals("abs") && !operator.equals("sin") && !operator.equals("cos") && !operator.equals("tan") && !operator.equals("log") && !operator.equals("ln") && !operator.equals("power") && !operator.equals("mode"));

    return operator;

  }


  private static String getValidNextAction(Scanner scanner, PrintStream output, boolean hasResult) {
    while (true) {
      output.println();
      output.println("Was möchtest du als Nächstes tun?");
      if (hasResult) {
        output.println("1. Mit dem Ergebnis weiterrechnen");
      }
      output.println("2. Neue Berechnung starten");
      output.println("3. Taschenrechner beenden");
      output.print("Deine Auswahl: ");

      String choice = scanner.next();
      if (choice.equals("2") || choice.equals("3") || (hasResult && choice.equals("1"))) {
        return choice;
      }
      output.println(hasResult ? "Bitte wähle 1, 2 oder 3." : "Bitte wähle 2 oder 3.");
    }
  }

}
