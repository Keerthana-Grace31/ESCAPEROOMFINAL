package com.example.demo.zombie;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.servlet.http.HttpSession; // Spring Boot 2.x: javax.servlet.http.HttpSession
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Zombie world controller: hub page + two scenarios (School, Hospital).
 *
 * <pre>
 * PAGES
 *   GET  /zombie            -> zombie-index.html     (hub: instructions, stats, scenario cards)
 *   GET  /zombie/school     -> zombie-school.html
 *   GET  /zombie/hospital   -> zombie-hospital.html
 *
 * JSON API ({slug} = school | hospital)
 *   GET  /zombie/api/stats                 overall player stats (this session) + per-scenario status
 *   GET  /zombie/api/{slug}/state          current run state (use on page load / refresh)
 *   POST /zombie/api/{slug}/start          start the run (timer begins); resumes if already active
 *   POST /zombie/api/submit                {"scenario":"school","answer":"..."}  validate the current puzzle
 *   POST /zombie/api/{slug}/answer         {"answer":"..."}  same thing, scenario taken from the URL
 *   POST /zombie/api/{slug}/hint           next hint for the current puzzle (costs time)
 *   POST /zombie/api/{slug}/timeout        client countdown reached 0; server re-verifies
 *   POST /zombie/api/{slug}/restart        reset lives/timer/progress and start again
 * </pre>
 *
 * RULES
 *  - Each scenario has its OWN run in the session: lives, current puzzle step, timer.
 *  - Wrong answer: -1 life. Zero lives or zero time: MISSION FAILED. All puzzles solved: VICTORY.
 *  - The server owns the clock and the lives; the browser only displays them.
 *
 * SOUNDS (paths are returned in the JSON "sound" field; the browser plays them)
 *  - click.mp3   : played client-side on every button press (never returned by the server)
 *  - correct.mp3 : correct answer, mission continues
 *  - success.mp3 : last puzzle solved (victory)
 *  - wrong.mp3   : wrong answer, mission continues
 *  - failure.mp3 : mission failed (no lives left or time up)
 *
 * Every API response uses the same JSON shape (see {@link ApiResponse}), so one front-end
 * render function can handle all of them. See zombie-game.js for the client side.
 */
@Controller
@RequestMapping("/zombie")
public class ZombieController {

    // ------------------------------------------------------------------ constants
    private static final String STATS_KEY = "ZOMBIE_STATS";
    private static final String RUN_KEY_PREFIX = "ZOMBIE_RUN_";
    private static final String HUB_URL = "/zombie";
    private static final String MAIN_MENU = "/";

    private static final String SUCCESS_COLOR = "#35E08A";
    private static final String ERROR_COLOR = "#FF4D5A";
    private static final String WARNING_COLOR = "#FFB547";

    private static final String SND_CLICK = "/sounds/common/click.mp3";
    private static final String SND_CORRECT = "/sounds/common/correct.mp3";
    private static final String SND_SUCCESS = "/sounds/common/success.mp3";
    private static final String SND_WRONG = "/sounds/common/wrong.mp3";
    private static final String SND_FAILURE = "/sounds/common/failure.mp3";

    private static final String FOOTER_BRAND = "DIGITAL ESCAPE ROOM";
    private static final String FOOTER_SUBTITLE = "Zombie Mission";
    private static final String FOOTER_TEAM = "2026 Digital Escape Room Team";
    private static final String FOOTER_LINE =
            "DIGITAL ESCAPE ROOM \u2022 Zombie Mission \u2022 2026 Digital Escape Room Team";

    // ------------------------------------------------------------------ configuration
    @Value("${escape.zombie.lives:3}")
    private int maxLives;

    @Value("${escape.zombie.time-limit-seconds:600}")
    private int timeLimitSeconds;

    @Value("${escape.zombie.hint-penalty-seconds:30}")
    private int hintPenaltySeconds;

    @Value("${escape.zombie.max-hints-per-puzzle:3}")
    private int maxHintsPerPuzzle;

    // ------------------------------------------------------------------ scenarios & puzzles

    public enum Scenario {
        SCHOOL("school", "Zombie School", "zombie-school", "/sounds/zombie/school-ambient.mp3"),
        HOSPITAL("hospital", "Zombie Hospital", "zombie-hospital", "/sounds/zombie/hospital-ambient.mp3");

        final String slug;
        final String displayName;
        final String view;
        final String ambientSound;

        Scenario(String slug, String displayName, String view, String ambientSound) {
            this.slug = slug;
            this.displayName = displayName;
            this.view = view;
            this.ambientSound = ambientSound;
        }

        static Scenario fromSlug(String slug) {
            for (Scenario s : values()) {
                if (s.slug.equalsIgnoreCase(slug)) return s;
            }
            return null;
        }
    }

    /** Server-side puzzle definition. Answers and hints never leave the server until requested. */
    private record Puzzle(String title, String story, String question, String visual,
                          List<String> answers, List<String> hints) {}

    // TODO: move to a database / properties file once the content is final.
    private static final Map<Scenario, List<Puzzle>> PUZZLES = new EnumMap<>(Scenario.class);

    static {
        PUZZLES.put(Scenario.SCHOOL, List.of(
                new Puzzle("Classroom - The Bell",
                        "The alarm bell rings in a pattern and the classroom door only opens for the right rhythm. "
                                + "The teacher's note lists the earlier ring counts.",
                        "How many rings come next?",
                        "2 \u2022 4 \u2022 8 \u2022 16 \u2022 ?",
                        List.of("32"),
                        List.of("Compare each number with the one before it.",
                                "Every number is double the previous one.",
                                "Double 16.")),
                new Puzzle("Library - The Scrawled Door",
                        "Someone scrawled a message on the library door. A sticky note says every letter was shifted "
                                + "forward by 2 places.",
                        "Decode the message to find the safe room.",
                        "NKDTCTA",
                        List.of("LIBRARY"),
                        List.of("This is a Caesar cipher.",
                                "Shift each letter back by 2 places.",
                                "N becomes L.")),
                new Puzzle("Parking Lot - The Bus",
                        "The school bus is your way out. Its keypad shows the evacuation formula from the principal's office.",
                        "What is the bus ignition code?",
                        "(3 FLOORS \u00D7 6 ROOMS) + 4 EXITS",
                        List.of("22"),
                        List.of("Multiply first, then add.",
                                "Start with 3 \u00D7 6.",
                                "18 plus 4."))));

        PUZZLES.put(Scenario.HOSPITAL, List.of(
                new Puzzle("Entrance - Main Lab",
                        "The hospital doors hang open and something is moving on the floors above. The lab keypad "
                                + "still has power. A note beside it shows the ward numbers the staff used as a passcode.",
                        "What number comes next in the ward sequence?",
                        "3 \u2022 6 \u2022 9 \u2022 12 \u2022 ?",
                        List.of("15"),
                        List.of("Look at the difference between each pair of numbers.",
                                "The numbers grow by the same amount every step.",
                                "Each step adds 3.")),
                new Puzzle("Decontamination - Vaccine Chamber",
                        "The chamber seals behind you. The label on the last vaccine vial is scrambled. The lead "
                                + "researcher scratched a warning into the wall: SHIFT +3.",
                        "Decode the label on the vial.",
                        "WKH FXUH",
                        List.of("CURE", "THE CURE"),
                        List.of("This is a Caesar cipher.",
                                "Every letter was moved 3 places forward in the alphabet.",
                                "Shift each letter back by 3. W becomes T.")),
                new Puzzle("Final Extraction - Power Grid",
                        "The vaccine is safe in your bag. The extraction gate needs power, and the grid panel shows "
                                + "three fuses and a formula.",
                        "What is the generator code?",
                        "RED 7 \u2022 BLUE 8 \u2022 GREEN 6   |   CODE = (RED \u00D7 BLUE) \u2212 GREEN",
                        List.of("50"),
                        List.of("Read the formula on the panel carefully.",
                                "Multiply first, then subtract.",
                                "Red times Blue, minus Green."))));
    }

    // ================================================================== PAGE ROUTES

    @GetMapping({"", "/"})
    public String hub(Model model) {
        addFooter(model);
        model.addAttribute("maxLives", maxLives);
        model.addAttribute("timeLimitSeconds", timeLimitSeconds);
        model.addAttribute("ambientSound", "/sounds/zombie/hub-ambient.mp3");
        return "zombie-index";
    }

    @GetMapping("/school")
    public String school(Model model) {
        return scenarioPage(Scenario.SCHOOL, model);
    }

    @GetMapping("/hospital")
    public String hospital(Model model) {
        return scenarioPage(Scenario.HOSPITAL, model);
    }

    /**
     * Model attributes for the scenario pages (if rendered with Thymeleaf):
     * scenarioName, scenarioSlug, apiBase, hubUrl, mainMenuUrl, maxLives, timeLimitSeconds,
     * totalPuzzles, ambientSound, sounds, colors, footerBrand/Subtitle/Team/Line.
     * The page itself holds no game state: it asks GET /zombie/api/{slug}/state.
     */
    private String scenarioPage(Scenario sc, Model model) {
        model.addAttribute("scenarioName", sc.displayName);
        model.addAttribute("scenarioSlug", sc.slug);
        model.addAttribute("apiBase", HUB_URL + "/api/" + sc.slug);
        model.addAttribute("hubUrl", HUB_URL);
        model.addAttribute("mainMenuUrl", MAIN_MENU);
        model.addAttribute("maxLives", maxLives);
        model.addAttribute("timeLimitSeconds", timeLimitSeconds);
        model.addAttribute("hintPenaltySeconds", hintPenaltySeconds);
        model.addAttribute("totalPuzzles", PUZZLES.get(sc).size());
        model.addAttribute("ambientSound", sc.ambientSound);

        Map<String, String> sounds = new LinkedHashMap<>();
        sounds.put("click", SND_CLICK);
        sounds.put("correct", SND_CORRECT);
        sounds.put("success", SND_SUCCESS);
        sounds.put("wrong", SND_WRONG);
        sounds.put("failure", SND_FAILURE);
        model.addAttribute("sounds", sounds);

        Map<String, String> colors = new LinkedHashMap<>();
        colors.put("success", SUCCESS_COLOR);
        colors.put("error", ERROR_COLOR);
        colors.put("warning", WARNING_COLOR);
        model.addAttribute("colors", colors);

        addFooter(model);
        return sc.view;
    }

    private void addFooter(Model model) {
        model.addAttribute("footerBrand", FOOTER_BRAND);
        model.addAttribute("footerSubtitle", FOOTER_SUBTITLE);
        model.addAttribute("footerTeam", FOOTER_TEAM);
        model.addAttribute("footerLine", FOOTER_LINE);
    }

    // ================================================================== JSON API

    /** Hub page: overall stats + status of each scenario ("NOT_STARTED", "ACTIVE", "FAILED", "VICTORY"). */
    @GetMapping("/api/stats")
    @ResponseBody
    public StatsView overallStats(HttpSession session) {
        PlayerStats stats = stats(session);
        synchronized (stats) {
            Map<String, String> status = new LinkedHashMap<>();
            for (Scenario sc : Scenario.values()) {
                Run run = run(session, sc);
                settleTimer(sc, run, stats);
                status.put(sc.slug, run.status.name());
            }
            return new StatsView(stats.missionsStarted, stats.victories, stats.failures,
                    stats.correctAnswers, stats.wrongAnswers, new LinkedHashMap<>(stats.bestTimes),
                    status, maxLives, timeLimitSeconds);
        }
    }

    /** Current state of a scenario run. Safe to call on every page load / refresh. */
    @GetMapping("/api/{slug}/state")
    @ResponseBody
    public ResponseEntity<Object> state(@PathVariable String slug, HttpSession session) {
        Scenario sc = Scenario.fromSlug(slug);
        if (sc == null) return unknown();
        PlayerStats stats = stats(session);
        synchronized (stats) {
            Run run = run(session, sc);
            settleTimer(sc, run, stats);
            return ok(build(sc, run, true, null, null, null, null));
        }
    }

    /** Start button. Timer and lives begin here. Calling it again while ACTIVE just resumes. */
    @PostMapping("/api/{slug}/start")
    @ResponseBody
    public ResponseEntity<Object> start(@PathVariable String slug, HttpSession session) {
        Scenario sc = Scenario.fromSlug(slug);
        if (sc == null) return unknown();
        PlayerStats stats = stats(session);
        synchronized (stats) {
            Run run = run(session, sc);
            settleTimer(sc, run, stats);
            if (run.status == Status.NOT_STARTED) {
                begin(run, stats);
            } else if (run.status != Status.ACTIVE) {
                return conflict(sc, run, "This mission is over. Use Try Again to restart.");
            }
            return ok(build(sc, run, true, "Mission started. Good luck.", SUCCESS_COLOR, null, null));
        }
    }

    /**
     * Main answer endpoint: {"scenario":"school|hospital","answer":"..."}.
     * Returns the common JSON (success, message, lives, status, ...). The server knows which
     * puzzle is current, so the client never sends a puzzle number.
     */
    @PostMapping("/api/submit")
    @ResponseBody
    public ResponseEntity<Object> submit(@RequestBody SubmitRequest req, HttpSession session) {
        Scenario sc = Scenario.fromSlug(req == null ? null : req.scenario());
        if (sc == null) return unknown();
        return processAnswer(sc, req.answer(), session);
    }

    /** Same as {@link #submit} with the scenario taken from the URL. */
    @PostMapping("/api/{slug}/answer")
    @ResponseBody
    public ResponseEntity<Object> answer(@PathVariable String slug, @RequestBody AnswerRequest req,
                                         HttpSession session) {
        Scenario sc = Scenario.fromSlug(slug);
        if (sc == null) return unknown();
        return processAnswer(sc, req == null ? null : req.answer(), session);
    }

    private ResponseEntity<Object> processAnswer(Scenario sc, String rawAnswer, HttpSession session) {
        PlayerStats stats = stats(session);
        synchronized (stats) {
            Run run = run(session, sc);
            settleTimer(sc, run, stats);

            if (run.status == Status.FAILED) {
                return conflict(sc, run, "Mission failed. Use Try Again to restart.");
            }
            if (run.status != Status.ACTIVE) {
                return conflict(sc, run, run.status == Status.VICTORY
                        ? "Mission already completed." : "Start the mission first.");
            }

            String given = normalize(rawAnswer);
            if (given.isEmpty() || given.length() > 200) {
                // Nothing was really tried: no life lost, no wrong sound.
                return ok(build(sc, run, false, "Enter an answer first.", WARNING_COLOR, null, null));
            }

            List<Puzzle> puzzles = PUZZLES.get(sc);
            Puzzle current = puzzles.get(run.step);
            boolean correct = current.answers().stream().map(ZombieController::normalize).anyMatch(given::equals);

            if (correct) {
                run.step++;
                stats.correctAnswers++;
                if (run.step >= puzzles.size()) {
                    finish(sc, run, stats, Status.VICTORY, null);
                    return ok(build(sc, run, true, "Victory! You escaped.", SUCCESS_COLOR, SND_SUCCESS, null));
                }
                return ok(build(sc, run, true, "Correct! Moving on.", SUCCESS_COLOR, SND_CORRECT, null));
            }

            // Wrong answer: lose a life.
            run.lives--;
            stats.wrongAnswers++;
            if (run.lives <= 0) {
                finish(sc, run, stats, Status.FAILED, "NO_LIVES");
                return ok(build(sc, run, false, "No lives left. Mission failed.", ERROR_COLOR, SND_FAILURE, null));
            }
            return ok(build(sc, run, false,
                    "Wrong answer. Lives left: " + run.lives + ".", ERROR_COLOR, SND_WRONG, null));
        }
    }

    /** Hint for the current puzzle. Costs {@code hintPenaltySeconds}, not a life. */
    @PostMapping("/api/{slug}/hint")
    @ResponseBody
    public ResponseEntity<Object> hint(@PathVariable String slug, HttpSession session) {
        Scenario sc = Scenario.fromSlug(slug);
        if (sc == null) return unknown();
        PlayerStats stats = stats(session);
        synchronized (stats) {
            Run run = run(session, sc);
            settleTimer(sc, run, stats);
            if (run.status != Status.ACTIVE) {
                return conflict(sc, run, "No active mission.");
            }

            Puzzle current = PUZZLES.get(sc).get(run.step);
            int used = run.hintsUsed[run.step];
            if (used >= maxHintsPerPuzzle || used >= current.hints().size()) {
                return ok(build(sc, run, false, "No hints left for this puzzle.", WARNING_COLOR, null, null));
            }

            String text = current.hints().get(used);
            run.hintsUsed[run.step] = used + 1;
            run.penaltyMillis += hintPenaltySeconds * 1000L;
            settleTimer(sc, run, stats); // the penalty itself can end the run
            if (run.status == Status.FAILED) {
                return ok(build(sc, run, false, "The hint cost you the last seconds. Mission failed.",
                        ERROR_COLOR, SND_FAILURE, null));
            }
            return ok(build(sc, run, true, "Hint used (-" + hintPenaltySeconds + "s).", WARNING_COLOR, null, text));
        }
    }

    /** Client countdown hit 0. The server re-checks its own deadline before failing the run. */
    @PostMapping("/api/{slug}/timeout")
    @ResponseBody
    public ResponseEntity<Object> timeout(@PathVariable String slug, HttpSession session) {
        Scenario sc = Scenario.fromSlug(slug);
        if (sc == null) return unknown();
        PlayerStats stats = stats(session);
        synchronized (stats) {
            Run run = run(session, sc);
            boolean wasActive = run.status == Status.ACTIVE;
            settleTimer(sc, run, stats);
            if (wasActive && run.status == Status.FAILED) {
                return ok(build(sc, run, false, "Time is up. Mission failed.", ERROR_COLOR, SND_FAILURE, null));
            }
            return ok(build(sc, run, true, null, null, null, null));
        }
    }

    /** Try Again / Play Again: fresh lives, fresh timer, puzzle 1, starts immediately. */
    @PostMapping("/api/{slug}/restart")
    @ResponseBody
    public ResponseEntity<Object> restart(@PathVariable String slug, HttpSession session) {
        Scenario sc = Scenario.fromSlug(slug);
        if (sc == null) return unknown();
        PlayerStats stats = stats(session);
        synchronized (stats) {
            Run fresh = new Run(maxLives, PUZZLES.get(sc).size());
            session.setAttribute(RUN_KEY_PREFIX + sc.slug, fresh);
            begin(fresh, stats);
            return ok(build(sc, fresh, true, "Mission restarted.", SUCCESS_COLOR, null, null));
        }
    }

    // ================================================================== core logic

    private void begin(Run run, PlayerStats stats) {
        run.status = Status.ACTIVE;
        run.startedAtMillis = System.currentTimeMillis();
        stats.missionsStarted++;
    }

    /** Ends the run once. Updates the overall stats exactly one time per run. */
    private void finish(Scenario sc, Run run, PlayerStats stats, Status result, String reason) {
        if (run.status != Status.ACTIVE) return;
        run.status = result;
        run.failReason = reason;
        run.finishedAtMillis = System.currentTimeMillis();
        if (result == Status.VICTORY) {
            stats.victories++;
            stats.bestTimes.merge(sc.slug, elapsedMillis(run) / 1000, Math::min);
        } else {
            stats.failures++;
        }
    }

    /** Fails an ACTIVE run whose time has run out. Call before every read or decision. */
    private void settleTimer(Scenario sc, Run run, PlayerStats stats) {
        if (run.status == Status.ACTIVE && remainingSeconds(run) <= 0) {
            finish(sc, run, stats, Status.FAILED, "TIME_UP");
        }
    }

    private long elapsedMillis(Run run) {
        if (run.status == Status.NOT_STARTED) return 0;
        boolean over = run.status == Status.FAILED || run.status == Status.VICTORY;
        long end = over ? run.finishedAtMillis : System.currentTimeMillis();
        return (end - run.startedAtMillis) + run.penaltyMillis;
    }

    private long remainingSeconds(Run run) {
        long leftMs = timeLimitSeconds * 1000L - elapsedMillis(run);
        return Math.max(0, (leftMs + 999) / 1000); // round up: 0 means really expired
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    // ------------------------------------------------------------------ session access

    private PlayerStats stats(HttpSession session) {
        Object o = session.getAttribute(STATS_KEY);
        if (o instanceof PlayerStats ps) return ps;
        PlayerStats fresh = new PlayerStats();
        session.setAttribute(STATS_KEY, fresh);
        return fresh;
    }

    private Run run(HttpSession session, Scenario sc) {
        String key = RUN_KEY_PREFIX + sc.slug;
        Object o = session.getAttribute(key);
        if (o instanceof Run r) return r;
        Run fresh = new Run(maxLives, PUZZLES.get(sc).size());
        session.setAttribute(key, fresh);
        return fresh;
    }

    // ------------------------------------------------------------------ response building

    private ApiResponse build(Scenario sc, Run run, boolean success, String message, String color,
                              String sound, String hint) {
        List<Puzzle> puzzles = PUZZLES.get(sc);
        PuzzleView view = null;
        Integer hintsLeft = null;
        if (run.status == Status.ACTIVE) {
            Puzzle p = puzzles.get(run.step);
            int used = run.hintsUsed[run.step];
            view = new PuzzleView(run.step + 1, puzzles.size(), p.title(), p.story(), p.question(), p.visual(),
                    List.copyOf(p.hints().subList(0, Math.min(used, p.hints().size()))));
            hintsLeft = Math.max(0, Math.min(maxHintsPerPuzzle, p.hints().size()) - used);
        }
        return new ApiResponse(success, message, color, sound, run.status.name(), run.failReason,
                run.lives, maxLives, remainingSeconds(run), run.step, puzzles.size(), hint, hintsLeft, view);
    }

    private static ResponseEntity<Object> ok(ApiResponse body) {
        return ResponseEntity.ok(body);
    }

    private ResponseEntity<Object> conflict(Scenario sc, Run run, String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(build(sc, run, false, message, WARNING_COLOR, null, null));
    }

    private static ResponseEntity<Object> unknown() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "message", "Unknown scenario.", "feedbackColor", ERROR_COLOR));
    }

    // ================================================================== DTOs / session state

    public record AnswerRequest(String answer) {}

    public record SubmitRequest(String scenario, String answer) {}

    /** The puzzle the player should see right now. Never contains answers or hints not yet requested. */
    public record PuzzleView(int number, int total, String title, String story, String question, String visual,
                             List<String> hintsShown) {}

    /**
     * Uniform response for every scenario endpoint. Null fields are omitted.
     * status: NOT_STARTED | ACTIVE | FAILED | VICTORY; failReason: NO_LIVES | TIME_UP (when FAILED).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ApiResponse(
            boolean success,
            String message,
            String feedbackColor,   // #35E08A success, #FF4D5A error, #FFB547 warning/hint
            String sound,           // path to play (correct/success/wrong/failure) or absent
            String status,
            String failReason,
            int lives,
            int maxLives,
            long remainingSeconds,  // server-authoritative timer
            int solved,             // puzzles solved so far
            int totalPuzzles,
            String hint,            // only on the hint endpoint
            Integer hintsRemaining, // for the current puzzle, only while ACTIVE
            PuzzleView puzzle) {}   // current puzzle, only while ACTIVE

    public record StatsView(
            int missionsStarted,
            int victories,
            int failures,
            int correctAnswers,
            int wrongAnswers,
            Map<String, Long> bestTimesSeconds,   // slug -> fastest victory
            Map<String, String> scenarioStatus,   // slug -> NOT_STARTED/ACTIVE/FAILED/VICTORY
            int maxLives,
            int timeLimitSeconds) {}

    public enum Status { NOT_STARTED, ACTIVE, FAILED, VICTORY }

    /** One scenario run. Stored in the session so it survives page changes and refreshes. */
    static class Run implements Serializable {
        private static final long serialVersionUID = 1L;
        Status status = Status.NOT_STARTED;
        int lives;
        int step;                     // index of the current puzzle
        long startedAtMillis;
        long finishedAtMillis;
        long penaltyMillis;           // hint penalties
        final int[] hintsUsed;
        String failReason;

        Run(int lives, int puzzleCount) {
            this.lives = lives;
            this.hintsUsed = new int[puzzleCount];
        }
    }

    /** Overall stats across scenarios (session-scoped: they reset when the session ends). */
    static class PlayerStats implements Serializable {
        private static final long serialVersionUID = 1L;
        int missionsStarted;
        int victories;
        int failures;
        int correctAnswers;
        int wrongAnswers;
        final Map<String, Long> bestTimes = new LinkedHashMap<>();
    }
}
