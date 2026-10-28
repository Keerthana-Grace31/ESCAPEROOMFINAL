package com.example.demo.spacecraft;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** JSON API used by static/js/spacecraft.js. */
@RestController
@RequestMapping("/api/spacecraft/{mission}")
public class SpacecraftApiController {

    private final GameService game;

    public SpacecraftApiController(GameService game) { this.game = game; }

    private static MissionType parse(String slug) {
        return MissionType.fromSlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown mission"));
    }

    @PostMapping("/start")
    public GameResponse start(@PathVariable String mission, HttpSession session) {
        return game.start(session, parse(mission));
    }

    @GetMapping("/state")
    public GameResponse state(@PathVariable String mission, HttpSession session) {
        return game.current(session, parse(mission));
    }

    @PostMapping("/answer")
    public ResponseEntity<?> answer(@PathVariable String mission,
                                    @RequestBody AnswerRequest body,
                                    HttpSession session) {
        MissionType type = parse(mission);
        if (body == null || body.choice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing choice");
        }
        try {
            return ResponseEntity.ok(game.answer(session, type, body.choice()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }
}
