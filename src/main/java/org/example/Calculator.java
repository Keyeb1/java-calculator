package org.example;
import java.util.Scanner;

public class Calculator {
  public static void main(String[] args) {

    Scanner scanner = new Scanner(System.in); //System.in = Eingaben werden über Tastatur gelesen
    String answer;
    double number1 = 0;
    double number2 = 0;
    boolean validNumber;

    do {

      validNumber = false;
      do {
        System.out.print("Gib die erste Zahl ein: ");
        if(scanner.hasNextDouble()) {
          number1 = scanner.nextDouble();
          validNumber = true;
        } else {
          System.out.println("Bitte gebe eine Zahl ein! ");
          scanner.next();
        }
      } while(!validNumber);

      String operator;
      do {
        System.out.print("Wähle einen Operator (+, -, *, /): ");
        operator = scanner.next();
        if(!operator.equals("+") && !operator.equals("-") && !operator.equals("*") && !operator.equals("/")) {
          System.out.println("Bitte gib einen der erlaubten Operatoren ein!");
        }
      } while (!operator.equals("+") && !operator.equals("-") && !operator.equals("*") && !operator.equals("/"));


      validNumber = false;
      do {
        System.out.print("Gib die zweite Zahl ein: ");
        if(scanner.hasNextDouble()) {
          number2 = scanner.nextDouble();
          validNumber = true;
        } else {
          System.out.println("Bitte gebe eine Zahl ein! ");
          scanner.next();
        }
      } while(!validNumber);


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
}