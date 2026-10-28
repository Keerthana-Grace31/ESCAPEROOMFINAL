package com.example.demo.spacecraft;

import java.util.List;

/** One puzzle. The correct answer never leaves the server (see ChallengeView). */
public record Challenge(
        String title,
        List<String> clues,
        String question,
        List<String> options,
        int correctIndex,
        String hint,
        String explanation) {
}
