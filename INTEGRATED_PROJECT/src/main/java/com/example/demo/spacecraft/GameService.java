package com.example.demo.spacecraft;

import java.util.List;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Service;

/** Core game rules: 3 lives, 4 challenges, a wrong answer costs one life. */
@Service
public class GameService {

    private final MissionCatalog catalog;

    public GameService(MissionCatalog catalog) { this.catalog = catalog; }

    private static String key(MissionType type) { return "spacecraft." + type.slug(); }

    /** Begin (or restart) a mission with full lives. */
    public GameResponse start(HttpSession session, MissionType type) {
        GameState state = new GameState(type);
        session.setAttribute(key(type), state);
        return respond(state, null, "Mission started. Good luck, commander.");
    }

    /** Current state; starts a new mission if none exists yet. */
    public GameResponse current(HttpSession session, MissionType type) {
        GameState state = (GameState) session.getAttribute(key(type));
        if (state == null) return start(session, type);
        return respond(state, null, null);
    }

    public GameResponse answer(HttpSession session, MissionType type, int choice) {
        GameState state = (GameState) session.getAttribute(key(type));
        if (state == null) state = new GameState(type);
        session.setAttribute(key(type), state);

        List<Challenge> list = catalog.forMission(type);
        if (state.status() != GameStatus.IN_PROGRESS) {
            throw new IllegalStateException("This mission is already over. Restart to play again.");
        }
        Challenge challenge = list.get(state.challengeIndex());
        if (choice < 0 || choice >= challenge.options().size()) {
            throw new IllegalArgumentException("Invalid choice.");
        }

        if (choice == challenge.correctIndex()) {
            state.advance(list.size());
            return respond(state, true, challenge.explanation());
        }
        state.loseLife();
        return respond(state, false, "That wasn't the right decision.");
    }

    private GameResponse respond(GameState state, Boolean correct, String message) {
        List<Challenge> list = catalog.forMission(state.mission());
        boolean playing = state.status() == GameStatus.IN_PROGRESS;
        ChallengeView view = playing ? ChallengeView.of(list.get(state.challengeIndex())) : null;
        int number = playing ? state.challengeIndex() + 1 : Math.min(state.challengeIndex(), list.size());
        return new GameResponse(state.mission().slug(), state.status(), state.lives(),
                GameState.MAX_LIVES, number, list.size(), view, correct, message);
    }
}
