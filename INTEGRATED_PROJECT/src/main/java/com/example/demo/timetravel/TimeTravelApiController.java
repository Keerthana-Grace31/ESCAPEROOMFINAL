package com.example.demo.timetravel;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * JSON API for the Time Travel quiz missions.
 * (Originally GET /game/puzzle and GET /game/answer, which returned the answer to the browser.)
 */
@RestController
@RequestMapping("/api/timetravel")
public class TimeTravelApiController {

    private final TimeTravelService service;

    public TimeTravelApiController(TimeTravelService service) {
        this.service = service;
    }

    @GetMapping("/puzzle")
    public PuzzleView puzzle(@RequestParam int scenario,
                             @RequestParam TimePeriod period,
                             @RequestParam int question) {
        Puzzle p = service.getPuzzle(scenario, period, question);
        if (p == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown puzzle");
        }
        return new PuzzleView(question, service.countPuzzles(scenario, period), p.question(), p.hint());
    }

    @PostMapping("/answer")
    public AnswerCheck.Result answer(@RequestBody AnswerCheck.Request req) {
        if (req == null || req.period() == null
                || service.getPuzzle(req.scenario(), req.period(), req.question()) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown puzzle");
        }
        boolean ok = service.checkAnswer(req.scenario(), req.period(), req.question(), req.answer());
        return new AnswerCheck.Result(ok,
                ok ? "Correct! You can continue your time travel." : "Wrong answer! Try again.");
    }
}
