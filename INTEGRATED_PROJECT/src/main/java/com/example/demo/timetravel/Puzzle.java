package com.example.demo.timetravel;

/** Server-side puzzle. The answer is never serialised to the browser (see {@link PuzzleView}). */
public record Puzzle(String question, String answer, String hint) {
}
