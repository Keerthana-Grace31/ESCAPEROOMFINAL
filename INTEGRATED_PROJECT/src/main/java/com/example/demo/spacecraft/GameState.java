package com.example.demo.spacecraft;

import java.io.Serializable;

/** Per-player, per-mission state kept in the HTTP session. */
public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int MAX_LIVES = 3;

    private final MissionType mission;
    private int lives = MAX_LIVES;
    private int challengeIndex = 0;
    private GameStatus status = GameStatus.IN_PROGRESS;

    public GameState(MissionType mission) { this.mission = mission; }

    public MissionType mission() { return mission; }
    public int lives() { return lives; }
    public int challengeIndex() { return challengeIndex; }
    public GameStatus status() { return status; }

    void loseLife() {
        if (lives > 0) lives--;
        if (lives == 0) status = GameStatus.FAILED;
    }

    void advance(int total) {
        challengeIndex++;
        if (challengeIndex >= total) status = GameStatus.COMPLETED;
    }
}
