package org.example;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.Set;

public class Calculator {
  private static final Set<String> VALID_OPERATORS = Set.of(
      "+", "-", "*", "/", "sqrt", "root",
      "abs", "sin", "cos", "tan",
      "log", "ln", "factorial", "power", "mode"
  );

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

    try {
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

        double root = 0.0;

        if (operator.equals("root")) {
          root = getValidNumber(scanner, output, "Gib den Wurzelexponenten ein: ");
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

            case "root":
              result = engine.nthRoot(number1, root);
              output.print("Du hast dich für die n-te Wurzel entschieden." + " Das Ergebnis ist: ");
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
              if (degreeMode) {
                double angle = number1 % 180;
                // Bereits beim Einlesen als double verlorene Stellen sind nicht rekonstruierbar.
                if (Math.abs(angle) == 90) {
                  throw new ArithmeticException("Tangens ist für diesen Winkel nicht definiert.");
                }
                if (angle > 90) {
                  angle -= 180;
                } else if (angle < -90) {
                  angle += 180;
                }
                // Nahe ±90° den kleinen Komplementwinkel und den Kehrwert nutzen.
                // So erhält die Engine nur Winkel bis ±π/4, niemals gerundete ±π/2.
                if (Math.abs(angle) > 45) {
                  double complement = Math.copySign(90.0, angle) - angle;
                  result = engine.divide(1.0, engine.tan(Math.toRadians(complement)));
                } else {
                  result = engine.tan(Math.toRadians(angle));
                }
              } else {
                result = engine.tan(number1);
              }
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

            case "factorial":
              result = engine.factorial(number1);
              output.print("Du hast dich für die Fakultät entschieden." + " Das Ergebnis ist: ");
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
    } catch (EndOfInputException exception) {
      output.println();
      output.println("Eingabe beendet. Taschenrechner wird beendet.");
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
    output.println("root   n-te Wurzel");
    output.println("abs    Betrag");
    output.println("sin    Sinus");
    output.println("cos    Kosinus");
    output.println("tan    Tangens");
    output.println("log    Dekadischer Logarithmus (Basis 10)");
    output.println("ln     Natürlicher Logarithmus (Basis e)");
    output.println("power  Potenzberechnung");
    output.println("factorial  Fakultät (ganze Zahlen von 0 bis 20)");
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
      requireInput(scanner);
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
    while (true) {
      output.print(inputText);
      requireInput(scanner);
      String operator = scanner.next();
      if (VALID_OPERATORS.contains(operator)) {
        return operator;
      }
      output.println("Bitte gib einen der erlaubten Operatoren ein!");
    }
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

      requireInput(scanner);
      String choice = scanner.next();
      if (choice.equals("2") || choice.equals("3") || (hasResult && choice.equals("1"))) {
        return choice;
      }
      output.println(hasResult ? "Bitte wähle 1, 2 oder 3." : "Bitte wähle 2 oder 3.");
    }
  }

  private static void requireInput(Scanner scanner) {
    if (!scanner.hasNext()) {
      throw new EndOfInputException();
    }
  }

  /** Internes Signal für ein kontrolliertes Ende der Konsoleneingabe. */
  private static final class EndOfInputException extends RuntimeException {
  }

}
