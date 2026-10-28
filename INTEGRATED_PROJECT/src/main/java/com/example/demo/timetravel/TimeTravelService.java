package com.example.demo.timetravel;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class TimeTravelService {

    // ==============================
    // SCENARIO 1 - LOST INVENTION
    // ==============================

    public List<Puzzle> getScenario1(TimePeriod period) {

        List<Puzzle> puzzles = new ArrayList<>();

        if (period == TimePeriod.PAST) {

            puzzles.add(new Puzzle(
                    "Decode: WKH WLPH PDFKLQH using Caesar shift 3.",
                    "THE TIME MACHINE",
                    "Move every letter 3 positions backwards."
            ));

            puzzles.add(new Puzzle(
                    "I can take you to yesterday, but I cannot move myself. What am I?",
                    "TIME MACHINE",
                    "Think about something that allows time travel."
            ));

            puzzles.add(new Puzzle(
                    "The inventor was born in 1950 and created the machine 35 years later. What year?",
                    "1985",
                    "Add 35 to 1950."
            ));

            puzzles.add(new Puzzle(
                    "Complete the sequence: 1985, 1995, 2005, ?",
                    "2015",
                    "The difference is 10."
            ));

            puzzles.add(new Puzzle(
                    "In PAST, PRESENT, FUTURE, which time period appears first?",
                    "PAST",
                    "Look at the beginning of the phrase."
            ));
        }

        else if (period == TimePeriod.PRESENT) {

            puzzles.add(new Puzzle(
                    "Put these years in chronological order: 2080, 1985, 2026.",
                    "1985,2026,2080",
                    "Start with the oldest year."
            ));

            puzzles.add(new Puzzle(
                    "What comes next? 2, 4, 8, 16, ?",
                    "32",
                    "The numbers double."
            ));

            puzzles.add(new Puzzle(
                    "If the current year is 2026, how many years until 2080?",
                    "54",
                    "Calculate 2080 - 2026."
            ));

            puzzles.add(new Puzzle(
                    "Complete the cycle: PRESENT → FUTURE → PAST → ?",
                    "PRESENT",
                    "The timeline repeats."
            ));

            puzzles.add(new Puzzle(
                    "The scientist has 3 time crystals and finds 4 more. How many does he have?",
                    "7",
                    "Add the crystals."
            ));
        }

        else if (period == TimePeriod.FUTURE) {

            puzzles.add(new Puzzle(
                    "What year comes after 2080 if we travel 20 years forward?",
                    "2100",
                    "Add 20 years."
            ));

            puzzles.add(new Puzzle(
                    "The machine shows the code 2-0-8-0. What year is it?",
                    "2080",
                    "Combine the four digits."
            ));

            puzzles.add(new Puzzle(
                    "If the time machine travels 50 years forward from 2080, what year does it reach?",
                    "2130",
                    "Add 50 to 2080."
            ));

            puzzles.add(new Puzzle(
                    "Complete: 2100, 2120, 2140, ?",
                    "2160",
                    "Add 20 each time."
            ));

            puzzles.add(new Puzzle(
                    "After FUTURE, which period comes next if the timeline repeats?",
                    "PAST",
                    "Think of the cycle Past → Present → Future."
            ));
        }

        return puzzles;
    }


    // ==============================
    // SCENARIO 2 - SAVE THE FUTURE
    // ==============================

    public List<Puzzle> getScenario2(TimePeriod period) {

        List<Puzzle> puzzles = new ArrayList<>();

        if (period == TimePeriod.FUTURE) {

            puzzles.add(new Puzzle(
                    "Complete: 2026, 2036, 2046, ?",
                    "2056",
                    "Add 10 years."
            ));

            puzzles.add(new Puzzle(
                    "Go back 40 years from 2026. What year do you reach?",
                    "1986",
                    "Subtract 40."
            ));

            puzzles.add(new Puzzle(
                    "The clock shows 2080. Travel 25 years forward. What year?",
                    "2105",
                    "Add 25."
            ));

            puzzles.add(new Puzzle(
                    "Which year comes first: 2050, 2030, 2070, 2040?",
                    "2030",
                    "Choose the smallest year."
            ));

            puzzles.add(new Puzzle(
                    "There are 5 broken time crystals. 2 are repaired. How many remain broken?",
                    "3",
                    "Subtract 2 from 5."
            ));
        }

        else if (period == TimePeriod.PRESENT) {

            puzzles.add(new Puzzle(
                    "What year is 10 years after 2026?",
                    "2036",
                    "Add 10."
            ));

            puzzles.add(new Puzzle(
                    "Put these years in chronological order: 2026, 1986, 2056.",
                    "1986,2026,2056",
                    "Start with the oldest."
            ));

            puzzles.add(new Puzzle(
                    "The time machine has 12 energy cells and uses 5. How many remain?",
                    "7",
                    "Subtract 5 from 12."
            ));

            puzzles.add(new Puzzle(
                    "Complete: 2026 → 2028 → 2030 → ?",
                    "2032",
                    "Add 2 each time."
            ));

            puzzles.add(new Puzzle(
                    "Which year is 40 years before 2026?",
                    "1986",
                    "Subtract 40."
            ));
        }

        else if (period == TimePeriod.PAST) {

            puzzles.add(new Puzzle(
                    "The inventor created the machine in 1986. What year is 40 years later?",
                    "2026",
                    "Add 40."
            ));

            puzzles.add(new Puzzle(
                    "Complete: 1986, 1996, 2006, ?",
                    "2016",
                    "Add 10."
            ));

            puzzles.add(new Puzzle(
                    "Which year is earlier: 1986 or 1996?",
                    "1986",
                    "Choose the earlier year."
            ));

            puzzles.add(new Puzzle(
                    "The inventor has 8 components and uses 3. How many remain?",
                    "5",
                    "Subtract 3 from 8."
            ));

            puzzles.add(new Puzzle(
                    "You fixed the original invention. What should happen? "
                    + "A: Timeline is restored, "
                    + "B: Timeline becomes worse, "
                    + "C: Time machine disappears.",
                    "A",
                    "You successfully repaired the timeline."
            ));
        }

        return puzzles;
    }


    // ==============================
    // GET ONE PUZZLE
    // ==============================

    public Puzzle getPuzzle(
            int scenario,
            TimePeriod period,
            int questionNumber) {

        List<Puzzle> puzzles;

        if (scenario == 1) {
            puzzles = getScenario1(period);
        } else if (scenario == 2) {
            puzzles = getScenario2(period);
        } else {
            return null;
        }

        if (questionNumber < 1 || questionNumber > puzzles.size()) {
            return null;
        }

        return puzzles.get(questionNumber - 1);
    }


    // ==============================
    // CHECK ANSWER
    // ==============================

    /** Number of puzzles in a scenario/era, or 0 when the scenario is unknown. */
    public int countPuzzles(int scenario, TimePeriod period) {
        if (scenario == 1) return getScenario1(period).size();
        if (scenario == 2) return getScenario2(period).size();
        return 0;
    }

    public boolean checkAnswer(
            int scenario,
            TimePeriod period,
            int questionNumber,
            String answer) {

        Puzzle puzzle = getPuzzle(scenario, period, questionNumber);

        if (puzzle == null || answer == null) {
            return false;
        }

        return normalize(puzzle.answer()).equals(normalize(answer));
    }

    /** Case-insensitive and whitespace-insensitive, so "1985, 2026, 2080" matches "1985,2026,2080". */
    private static String normalize(String s) {
        return s.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }
}
