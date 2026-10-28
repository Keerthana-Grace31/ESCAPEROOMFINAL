package com.example.demo.pandemic;
import java.util.Scanner;
public class PandemicConsole {
    static Scanner sc = new Scanner(System.in);

    /** Reads a whole line and parses it; returns -1 for blank or non-numeric input instead of crashing. */
    static int readInt() {
        if (!sc.hasNextLine()) {
            return -1;
        }
        try {
            return Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static void main(String[] args) {

        System.out.println("====================================");
        System.out.println("        PANDEMIC ESCAPE");
        System.out.println("====================================");

        System.out.println("\nA mysterious virus has spread across the city.");
        System.out.println("You are trapped inside a research facility.");
        System.out.println("The emergency lockdown will activate soon.");
        System.out.println("Solve the puzzles and escape!\n");

        System.out.println("Choose your scenario:");
        System.out.println("1. Student Escape");
        System.out.println("2. Doctor Escape");
        System.out.println("3. Scientist Escape");

        System.out.print("\nEnter your choice: ");
        int choice = readInt();

        switch (choice) {

            case 1:
                studentScenario();
                break;

            case 2:
                doctorScenario();
                break;

            case 3:
                scientistScenario();
                break;

            default:
                System.out.println("Invalid choice!");
        }

        sc.close();
    }

    // ==========================================
    // STUDENT SCENARIO
    // ==========================================

    static void studentScenario() {

        System.out.println("\n====================================");
        System.out.println("          STUDENT ESCAPE");
        System.out.println("====================================");

        System.out.println("\nA virus outbreak has locked down the college laboratory.");
        System.out.println("You are trapped inside.");
        System.out.println("Find the access code and escape!\n");

        System.out.println("Objects in the room:");
        System.out.println("- Laboratory");
        System.out.println("- Computer");
        System.out.println("- Research Notes");
        System.out.println("- Security Door");

        // Puzzle 1
        System.out.println("\n---------- PUZZLE 1 ----------");

        System.out.println("Research Notes:");
        System.out.println("Sample A -> 3");
        System.out.println("Sample B -> 7");
        System.out.println("Sample C -> 2");

        System.out.println("\nClue:");
        System.out.println("Sample B was collected first.");
        System.out.println("Sample C was collected second.");
        System.out.println("Sample A was collected last.");

        System.out.print("\nWhich sample was collected first? ");
        String answer = sc.hasNextLine() ? sc.nextLine() : "";

        if (!answer.equalsIgnoreCase("B") &&
            !answer.equalsIgnoreCase("Sample B")) {

            System.out.println("Wrong answer! You cannot continue.");
            return;
        }

        System.out.println("Correct! Puzzle 1 solved.");

        // Puzzle 2
        System.out.println("\n---------- PUZZLE 2 ----------");

        System.out.println("I have keys but no locks.");
        System.out.println("I have space but no room.");
        System.out.println("What am I?");

        System.out.print("\nYour answer: ");
        answer = sc.hasNextLine() ? sc.nextLine() : "";

        if (!answer.equalsIgnoreCase("keyboard")) {
            System.out.println("Wrong answer!");
            return;
        }

        System.out.println("Correct! Computer unlocked.");

        // Puzzle 3
        System.out.println("\n---------- PUZZLE 3 ----------");

        System.out.println("ACCESS CODE:");
        System.out.println("First digit  = Number of samples");
        System.out.println("Second digit = Number of computers");
        System.out.println("Third digit  = Number of emergency exits");

        System.out.println("\nYou observe the room:");
        System.out.println("Samples = 3");
        System.out.println("Computers = 1");
        System.out.println("Emergency exits = 2");

        System.out.print("\nEnter the 3-digit security code: ");
        int code = readInt();

        if (code == 312) {
            escapeSuccess();
        } else {
            System.out.println("Wrong security code!");
            System.out.println("The security door remains locked.");
        }
    }

    // ==========================================
    // DOCTOR SCENARIO
    // ==========================================

    static void doctorScenario() {

        System.out.println("\n====================================");
        System.out.println("           DOCTOR ESCAPE");
        System.out.println("====================================");

        System.out.println("\nA mysterious virus has infected the city.");
        System.out.println("You are a doctor inside an emergency hospital.");
        System.out.println("The hospital has entered lockdown.");
        System.out.println("Solve the puzzles to escape!\n");

        System.out.println("Objects in the room:");
        System.out.println("- Medical Laboratory");
        System.out.println("- Doctor's Computer");
        System.out.println("- Patient Records");
        System.out.println("- Emergency Door");

        // Puzzle 1
        System.out.println("\n---------- PUZZLE 1 ----------");

        System.out.println("Patient Samples:");
        System.out.println("Patient A -> 8");
        System.out.println("Patient B -> 4");
        System.out.println("Patient C -> 6");

        System.out.println("\nClue:");
        System.out.println("Patient C was tested first.");
        System.out.println("Patient A was tested second.");
        System.out.println("Patient B was tested last.");

        System.out.print("\nWhich patient was tested first? ");
        String answer = sc.hasNextLine() ? sc.nextLine() : "";

        if (!answer.equalsIgnoreCase("C") &&
            !answer.equalsIgnoreCase("Patient C")) {

            System.out.println("Wrong answer!");
            return;
        }

        System.out.println("Correct! Puzzle 1 solved.");

        // Puzzle 2
        System.out.println("\n---------- PUZZLE 2 ----------");

        System.out.println("I have a screen but I am not a TV.");
        System.out.println("I can store information.");
        System.out.println("Doctors use me to view patient records.");
        System.out.println("What am I?");

        System.out.print("\nYour answer: ");
        answer = sc.hasNextLine() ? sc.nextLine() : "";

        if (!answer.equalsIgnoreCase("computer")) {
            System.out.println("Wrong answer!");
            return;
        }

        System.out.println("Correct! Computer unlocked.");

        // Puzzle 3
        System.out.println("\n---------- PUZZLE 3 ----------");

        System.out.println("ACCESS CODE:");
        System.out.println("First digit  = Number of patient samples");
        System.out.println("Second digit = Number of computers");
        System.out.println("Third digit  = Number of emergency exits");

        System.out.println("\nYou observe the room:");
        System.out.println("Patient samples = 3");
        System.out.println("Computers = 2");
        System.out.println("Emergency exits = 1");

        System.out.print("\nEnter the 3-digit security code: ");
        int code = readInt();

        if (code == 321) {
            escapeSuccess();
        } else {
            System.out.println("Wrong security code!");
            System.out.println("The emergency door remains locked.");
        }
    }

    // ==========================================
    // SCIENTIST SCENARIO
    // ==========================================

    static void scientistScenario() {

        System.out.println("\n====================================");
        System.out.println("         SCIENTIST ESCAPE");
        System.out.println("====================================");

        System.out.println("\nYou are a scientist studying a mysterious virus.");
        System.out.println("Suddenly, the research facility enters lockdown.");
        System.out.println("The main door requires a 3-digit code.");
        System.out.println("Solve the puzzles and escape!\n");

        System.out.println("Objects in the room:");
        System.out.println("- Microscope");
        System.out.println("- Virus Samples");
        System.out.println("- Research Computer");
        System.out.println("- Security Door");

        // Puzzle 1
        System.out.println("\n---------- PUZZLE 1 ----------");

        System.out.println("Virus Samples:");
        System.out.println("Sample X -> 5");
        System.out.println("Sample Y -> 9");
        System.out.println("Sample Z -> 1");

        System.out.println("\nClue:");
        System.out.println("Sample Z was collected first.");
        System.out.println("Sample X was collected second.");
        System.out.println("Sample Y was collected last.");

        System.out.print("\nWhich sample was collected first? ");
        String answer = sc.hasNextLine() ? sc.nextLine() : "";

        if (!answer.equalsIgnoreCase("Z") &&
            !answer.equalsIgnoreCase("Sample Z")) {

            System.out.println("Wrong answer!");
            return;
        }

        System.out.println("Correct! Puzzle 1 solved.");

        // Puzzle 2
        System.out.println("\n---------- PUZZLE 2 ----------");

        System.out.println("I can show information.");
        System.out.println("I can run programs.");
        System.out.println("Scientists use me for research.");
        System.out.println("What am I?");

        System.out.print("\nYour answer: ");
        answer = sc.hasNextLine() ? sc.nextLine() : "";

        if (!answer.equalsIgnoreCase("computer")) {
            System.out.println("Wrong answer!");
            return;
        }

        System.out.println("Correct! Research computer unlocked.");

        // Puzzle 3
        System.out.println("\n---------- PUZZLE 3 ----------");

        System.out.println("ACCESS CODE:");
        System.out.println("First digit  = Number of virus samples");
        System.out.println("Second digit = Number of computers");
        System.out.println("Third digit  = Number of emergency exits");

        System.out.println("\nYou observe the room:");
        System.out.println("Virus samples = 3");
        System.out.println("Computers = 1");
        System.out.println("Emergency exits = 2");

        System.out.print("\nEnter the 3-digit security code: ");
        int code = readInt();

        if (code == 312) {
            escapeSuccess();
        } else {
            System.out.println("Wrong security code!");
            System.out.println("The security door remains locked.");
        }
    }

    // ==========================================
    // SUCCESS MESSAGE
    // ==========================================

    static void escapeSuccess() {

        System.out.println("\n====================================");
        System.out.println("          ESCAPE SUCCESSFUL!");
        System.out.println("====================================");

        System.out.println("\nThe security door opens...");
        System.out.println("You escaped the facility!");
        System.out.println("Congratulations! 🎉");
    }
}
