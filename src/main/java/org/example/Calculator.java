package org.example;
import java.util.Scanner;

public class Calculator {
  public static void main(String[] args) {

    Scanner scanner = new Scanner(System.in);
    String answer;

    do {

      double number1 = getValidNumber(scanner, "Gib die erste Zahl ein: ");

      String operator = getValidOperator(scanner, "Wähle einen Operator aus: ");

      double number2 = getValidNumber(scanner, "Gib die zweite Zahl ein: ");


      switch (operator) {
        case "+":
          System.out.println("Du hast dich für die Addition entschieden." + " Das Ergebnis ist: " +
              (number1 + number2));
          break;

        case "-":
          System.out.println(
              "Du hast dich für die Subtraktion entschieden." + " Das Ergebnis ist: " +
                  (number1 - number2));
          break;

        case "*":
          System.out.println(
              "Du hast dich für die Multiplikation entschieden." + " Das Ergebnis ist: " +
                  (number1 * number2));
          break;

        case "/":
          if(number2 == 0) {
            System.out.println(
                "Du hast dich für die Division entschieden, aber es ist nicht erlaubt durch 0 zu teilen!");
          } else {
            System.out.println(
                "Du hast dich für die Division entschieden." + " Das Ergebnis ist: " +
                    (number1 / number2));
          }

          break;

      }


      do {
        System.out.print("Möchtest du weiter rechnen? Ja oder Nein? ");
        answer = scanner.next();

        if(!answer.equalsIgnoreCase("Ja") && !answer.equalsIgnoreCase("Nein")) {
          System.out.println("Bitte sag entweder Ja oder Nein! ");
        }
      } while (!answer.equalsIgnoreCase("Ja") && !answer.equalsIgnoreCase("Nein"));

    } while(answer.equalsIgnoreCase("Ja"));

    scanner.close();

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
      if(!operator.equals("+") && !operator.equals("-") && !operator.equals("*") && !operator.equals("/")) {
        System.out.println("Bitte gib einen der erlaubten Operatoren ein!");
      }
    } while (!operator.equals("+") && !operator.equals("-") && !operator.equals("*") && !operator.equals("/"));

    return operator;

  }
}