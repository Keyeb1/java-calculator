package org.example;
import java.util.Scanner;

public class Calculator {
  public static void main(String[] args) {

    showWelcomeMessage();
    Scanner scanner = new Scanner(System.in);
    CalculatorEngine engine = new CalculatorEngine();
    String answer;

    do {

      double number1 = getValidNumber(scanner, "Gib die erste Zahl ein: ");

      String operator = getValidOperator(scanner, "Wähle einen Operator aus ('+','-','*','/', 'sqrt', 'power'): ");

      double number2 = 0.0;

      if (operator.equals("+") || operator.equals("-") || operator.equals("*") || operator.equals("/")) {
        number2 = getValidNumber(scanner, "Gib die zweite Zahl ein: ");
      }

      double exponent = 0.0;

      if (operator.equals("power")) {
        exponent = getValidNumber(scanner, "Gib den Exponent ein: ");
      }

      try {
        double result;
        switch (operator) {
          case "+":
            result = engine.add(number1, number2);
            System.out.print("Du hast dich für die Addition entschieden." + " Das Ergebnis ist: ");
            break;

          case "-":
            result = engine.subtract(number1, number2);
            System.out.print("Du hast dich für die Subtraktion entschieden." + " Das Ergebnis ist: ");
            break;

          case "*":
            result = engine.multiply(number1, number2);
            System.out.print("Du hast dich für die Multiplikation entschieden." + " Das Ergebnis ist: ");
            break;

          case "/":
            result = engine.divide(number1, number2);
            System.out.print("Du hast dich für die Division entschieden." + " Das Ergebnis ist: ");
            break;

          case "sqrt":
            result = engine.sqrt(number1);
            System.out.print("Du hast dich für die Quadratwurzel entschieden." + " Das Ergebnis ist: ");
            break;

          case "power":
            result = engine.power(number1, exponent);
            System.out.print("Du hast dich für die Potenz entschieden." + " Das Ergebnis ist: ");
            break;

          default:
            throw new IllegalArgumentException("Unbekannter Operator: " + operator);
        }

        if (result % 1 == 0) {
          System.out.printf("%.0f%n", result);
        } else {
          System.out.println(result);
        }
      } catch (IllegalArgumentException | ArithmeticException exception) {
        System.out.println("Fehler: " + exception.getMessage());
      }
      answer = getValidAnswer(scanner, "Möchtest du den Taschenrechner neustarten? (Ja/Nein): ");
    } while(answer.equalsIgnoreCase("Ja"));

    scanner.close();

  }


  public static void showWelcomeMessage() {
    System.out.println("======================================== JAVA CALCULATOR ========================================");
    System.out.println("Verfügbare Operationen:");
    System.out.println("+      Addition");
    System.out.println("-      Subtraktion");
    System.out.println("*      Multiplikation");
    System.out.println("/      Division");
    System.out.println("sqrt   Quadratwurzel");
    System.out.println("power  Potenzberechnung");
    System.out.println();
    System.out.println("Folge einfach den Anweisungen und gib die");
    System.out.println("gewünschten Werte in der Konsole ein.");
    System.out.println();
    System.out.println("Los geht's!");
    System.out.println("===============================================================================================");

  }

  public static double getValidNumber(Scanner scanner, String inputText) {

    boolean validNumber = false;
    double number = 0;
    do {
      System.out.print(inputText);
      if(scanner.hasNextDouble()) {
        number = scanner.nextDouble();
        validNumber = true;
      } else {
        System.out.println("Bitte gebe eine Zahl ein! ");
        scanner.next();
      }
    } while (!validNumber);

      return number;
  }


  public static String getValidOperator(Scanner scanner, String inputText) {

    String operator;
    do {
      System.out.print(inputText);
      operator = scanner.next();
      if(!operator.equals("+") && !operator.equals("-") && !operator.equals("*") && !operator.equals("/") && !operator.equals("sqrt") && !operator.equals("power")) {
        System.out.println("Bitte gib einen der erlaubten Operatoren ein!");
      }
    } while (!operator.equals("+") && !operator.equals("-") && !operator.equals("*") && !operator.equals("/") && !operator.equals("sqrt") && !operator.equals("power"));

    return operator;

  }


  public static String getValidAnswer(Scanner scanner, String inputText) {

    String answer;
    do {
      System.out.print(inputText);
      answer = scanner.next();

      if(!answer.equalsIgnoreCase("Ja") && !answer.equalsIgnoreCase("Nein")) {
        System.out.println("Bitte sag entweder Ja oder Nein! ");
      }
    } while (!answer.equalsIgnoreCase("Ja") && !answer.equalsIgnoreCase("Nein"));

    return answer;
  }

}
