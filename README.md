# Java Calculator

A Java 25 console calculator that is gradually developing into a scientific calculator, with a longer-term goal of becoming a modern, interactive application with AI and LLM capabilities.

I started this project to refresh and deepen my Java knowledge after completing my Computer Science degree. Building a real application in small steps helps me connect language fundamentals with refactoring, testing and maintainable code.

## Project Goals

- Strengthen my Java skills through practical development.
- Add useful mathematical features incrementally.
- Practice clear responsibilities, automated testing and professional Git and GitHub workflows.
- Build a foundation for future interactive and conversational interfaces.

## Current Features

### Mathematical Operations

| Area | Supported operations |
| --- | --- |
| Basic arithmetic | Addition (`+`), subtraction (`-`), multiplication (`*`) and division (`/`) |
| Roots, absolute values and powers | Square root (`sqrt`), absolute value (`abs`) and exponentiation (`power`) |
| Trigonometry | Sine (`sin`), cosine (`cos`) and tangent (`tan`) |

- **Angle modes:** DEG is active at startup. Entering `mode` at the operator prompt switches between DEG and RAD without changing the current operand or the last valid result. The console displays the active mode. The calculation engine always receives angles in radians.
- **Constants:** `pi` and `e` are accepted regardless of letter case wherever a number is requested, including the first operand, second operand and exponent.
- **Calculation flow:** Continue with the previous result, start a new calculation or exit. Starting a new calculation clears the saved result and asks for a new first operand; the angle mode stays active.

### Validation and Error Handling

- Validate numeric input, operators and menu choices, with another prompt after invalid input.
- Reject division by zero, square roots of negative numbers, non-finite operands and calculations that produce `NaN` or infinity, including invalid powers and overflows.
- Detect undefined tangent values at `90 + k * 180` degrees before conversion to radians, and recognize common singularities at odd multiples of `Math.PI / 2` in the engine.
- Preserve the last valid result after a calculation error until a new calculation is explicitly started. When no valid result exists, the continuation option is unavailable.

Calculations use `double`, so small rounding differences are expected. Tangent checks target recognizable singularities without excluding a tolerance range around them. Different rounding paths and very large angles can limit singularity detection.

### Structure and Testing

- `Calculator` handles the console dialog and session state; `CalculatorEngine` contains the mathematical operations and their validation without console input or output.
- JUnit 5 tests cover the engine and console interaction, including parameterized cases, mathematical errors, constants, angle modes, continuation and restarting calculations.

## Technologies

### Currently Used

| Technology | Role |
| --- | --- |
| Java 25 | Application language and target release; Eclipse Temurin is used locally |
| Maven | Build and dependency management, with the Maven Compiler and Surefire plugins |
| JUnit 5 / Jupiter 5.14.4 | Automated unit tests and console interaction tests |
| IntelliJ IDEA | Development environment |
| Git | Version control and incremental commits |
| [GitHub](https://github.com/Keyeb1/java-calculator) | Repository hosting and development history |

### Possible Future Technologies

UI tooling, API clients and LLM integration libraries may be added as the application evolves. Specific frameworks and AI providers will depend on actual requirements; the roadmap does not commit to particular products.

## Concepts Practiced

- **Java fundamentals:** Variables, data types, arithmetic, console input with `Scanner`, methods, parameters and return values.
- **Control flow and validation:** Conditionals, `switch`, loops and reusable input helpers.
- **Object-oriented programming and separation of concerns:** A dedicated calculation engine used by the console application.
- **Exception handling:** Reporting invalid operands and mathematical operations through exceptions and handling them in the console dialog.
- **State management:** Tracking the last valid result and angle mode, preserving results after errors and resetting them for new calculations.
- **Testing with JUnit:** Assertions, exception tests, parameterized tests and floating-point comparisons with appropriate tolerances.
- **Testable console interaction:** Supplying input and capturing output without replacing global console streams.
- **Lambda expressions:** Exception assertions and processing captured test output.
- **Incremental refactoring:** Extracting calculation logic and extending behavior in small, tested steps.
- **Git and GitHub workflows:** Tracking changes through commits and maintaining a hosted repository.

## Running the Project

### Prerequisites

- JDK 25, for example Eclipse Temurin 25.
- Maven available on the command line for the commands below.
- Git to clone the repository.
- IntelliJ IDEA or another Java IDE is optional.

Make sure `JAVA_HOME` and the Java executable on your `PATH` point to JDK 25. Check the Java version used by both the shell and Maven:

```sh
java -version
mvn -version
```

### Clone and Run

```sh
git clone https://github.com/Keyeb1/java-calculator
cd java-calculator
mvn compile
java -cp target/classes org.example.Calculator
```

Alternatively, open the repository as a Maven project in IntelliJ IDEA, select JDK 25 as both the Project SDK and Maven Runner JRE, and run the `main` method in `Calculator.java`.

### Using the Console

The current console prompts are in German. Enter the first number or a constant, then choose an operator. Basic arithmetic asks for a second number; `power` asks for an exponent. The remaining mathematical operations use a single operand.

At the operator prompt, `mode` switches the angle mode and returns to operator selection with the same operand. After a successful calculation, the menu offers:

1. Continue with the result.
2. Start a new calculation and discard the stored result.
3. Exit the calculator.

After a mathematical error, the same choices are available if a valid result remains; otherwise, only starting a new calculation or exiting is offered.

Numeric input follows the system's default locale. Enter calculations as separate prompted values and operators: to calculate half of pi, enter `pi`, then `/`, then `2`. Direct expressions such as `pi/2` are not supported.

### Running the Tests

From the project root, run:

```sh
mvn test
```

Maven compiles the application and test sources, then runs the JUnit Jupiter tests through Surefire. Test reports are written to `target/surefire-reports/`. The same `test` lifecycle phase can also be run from IntelliJ IDEA's Maven tool window.

## Project Structure

```text
pom.xml
src/
  main/java/org/example/
    Calculator.java              Console interaction and session state
    CalculatorEngine.java        Mathematical operations and validation
  test/java/org/example/
    CalculatorTest.java          Console interaction and workflow tests
    CalculatorEngineTest.java    Calculation and error-case tests
```

## Roadmap

Checked items are implemented. Unchecked items are possible future steps, whose scope and order may change as the project develops.

### Phase 1 — Java Fundamentals

- [x] Implement the four basic arithmetic operations and a console dialog.
- [x] Validate numbers, operators and menu choices with reusable methods.
- [x] Handle division by zero and repeat calculations within one session.
- [x] Use Git and GitHub to record incremental development.

### Phase 2 — Scientific Calculator & Software Engineering

- [x] Configure Java 25 and the Maven build.
- [x] Separate console interaction from the calculation engine.
- [x] Add square root, absolute value and exponentiation.
- [x] Add sine, cosine and tangent with DEG/RAD mode switching.
- [x] Support the constants pi and e in numeric input.
- [x] Continue with the previous result and reset stored results for new calculations.
- [x] Handle mathematical exceptions and preserve valid results after errors.
- [x] Add JUnit 5 unit tests, parameterized tests and console interaction tests.
- [ ] Add logarithms (`log` and `ln`).
- [ ] Add factorials.
- [ ] Explore additional scientific calculations.

### Phase 3 — Application Evolution

- [ ] Add calculation history.
- [ ] Refine the architecture and input handling as requirements grow.
- [ ] Build a modern, interactive user interface.
- [ ] Explore application APIs and integration with external services.

### Phase 4 — AI & LLM Integration

- [ ] Accept natural-language mathematical input.
- [ ] Support conversational interaction.
- [ ] Integrate an LLM through an API.
- [ ] Translate language requests into validated operations executed by the calculation engine.
- [ ] Offer explanations based on the calculations performed by the application.

## Long-Term Vision

The long-term goal is a calculator that people can interact with through natural language and conversation. An LLM would help interpret requests and communicate results, while the application would validate and execute the mathematical operations through its own deterministic calculation logic.

**AI and LLM features are not implemented yet.** The current application is a scientific console calculator. UI, API and language-model integration remain future development directions.

## Why This Project?

This is a personal learning and portfolio project. The repository records how the application and my software engineering skills develop together, from Java fundamentals toward a more structured and interactive application.
