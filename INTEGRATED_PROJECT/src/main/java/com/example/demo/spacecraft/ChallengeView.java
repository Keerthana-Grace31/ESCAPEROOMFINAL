package com.example.demo.spacecraft;

import java.util.List;

/** Client-safe version of a Challenge: no answer, no explanation. */
public record ChallengeView(String title, List<String> clues, String question, List<String> options, String hint) {
    public static ChallengeView of(Challenge c) {
        return new ChallengeView(c.title(), c.clues(), c.question(), c.options(), c.hint());
    }
}
