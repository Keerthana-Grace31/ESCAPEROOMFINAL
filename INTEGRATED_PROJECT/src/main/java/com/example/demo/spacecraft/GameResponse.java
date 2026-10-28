package com.example.demo.spacecraft;

/** What the browser receives after every API call. */
public record GameResponse(
        String mission,
        GameStatus status,
        int lives,
        int maxLives,
        int challengeNumber,
        int totalChallenges,
        ChallengeView challenge,
        Boolean correct,
        String message) {
}
