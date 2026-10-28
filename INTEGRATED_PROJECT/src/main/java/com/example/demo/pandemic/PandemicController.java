package com.example.demo.pandemic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Pandemic Escape: story page, Student and Doctor scenarios (multiple-choice puzzles).
 *
 * Doctor scenario API (used by static/js/doctor.js):
 * <pre>
 *   GET  /api/pandemic/doctor/scenario   current run: status, stats, room list, active patient case
 *   POST /api/pandemic/doctor/action     {"caseId":"medical","answer":"A"} -> verdict + updated state
 * </pre>
 * The server owns lives, progress, infection / recovery rates and the clock (kept in the HttpSession);
 * the answer key never leaves the server. The older /api/doctor/* endpoints are kept for compatibility.
 *
 * The home page ("/") lives in HomeController; it used to be mapped here AND in the
 * spacecraft controller, which made the application fail to start.
 */
@Controller
public class PandemicController {

    // =========================
    // PAGE ROUTES
    // =========================

    @GetMapping("/pandemic")
    public String pandemic() {
        return "pandemic";
    }

    @GetMapping("/student")
    public String student() {
        return "student";
    }

    @GetMapping("/doctor")
    public String doctor() {
        return "doctor";
    }

    // =========================
    // STUDENT
    // =========================

    /** Easy: which equipment shows microorganisms? A. Microscope */
    @PostMapping("/api/student/laboratory")
    @ResponseBody
    public Map<String, Object> laboratoryPuzzle(@RequestParam String answer) {
        return check(answer, "A",
                "Correct! A microscope is used to observe very small microorganisms.",
                "Wrong answer! The correct answer is Microscope.");
    }

    /** Medium: which sample has the highest value? B. Sample B (7) */
    @PostMapping("/api/student/computer")
    @ResponseBody
    public Map<String, Object> computerPuzzle(@RequestParam String answer) {
        return check(answer, "B",
                "Correct! Sample B has the highest recorded value.",
                "Wrong answer! Check the sample values again.");
    }

    /**
     * Hard: the clues say the code is 4, 7, 2 -> option A (472).
     * (The original service accepted B = 427 and printed "311", which contradicted the clues.)
     */
    @PostMapping("/api/student/security")
    @ResponseBody
    public Map<String, Object> securityPuzzle(@RequestParam String answer) {
        return check(answer, "A",
                "Correct! The emergency access code is 472.",
                "Wrong answer! Re-read the clues: first digit, second digit, final digit.");
    }

    // =========================
    // DOCTOR (legacy stateless checks; doctor.js now uses /api/pandemic/doctor/*)
    // =========================

    /** Easy: what measures body temperature? A. Thermometer */
    @PostMapping("/api/doctor/medical")
    @ResponseBody
    public Map<String, Object> medicalPuzzle(@RequestParam String answer) {
        return check(answer, "A",
                "Correct! A thermometer measures body temperature.",
                "Wrong answer! Try again.");
    }

    /** Medium: what to do before a final diagnosis? C. Verify the test information */
    @PostMapping("/api/doctor/research")
    @ResponseBody
    public Map<String, Object> researchPuzzle(@RequestParam String answer) {
        return check(answer, "C",
                "Correct! The patient's test information should be verified.",
                "Wrong answer! Check what should be done before a final diagnosis.");
    }

    /** Hard: who has the lowest oxygen level? C. Patient C (82%) */
    @PostMapping("/api/doctor/emergency")
    @ResponseBody
    public Map<String, Object> emergencyPuzzle(@RequestParam String answer) {
        return check(answer, "C",
                "Correct! Patient C has the lowest oxygen level.",
                "Wrong answer! Check the oxygen levels carefully.");
    }

    // =========================
    // DOCTOR: server-side scenario
    // =========================

    static final String DOCTOR_RUN_KEY = "PANDEMIC_DOCTOR_RUN";
    static final int MAX_LIVES = 3;
    static final int TIME_LIMIT_SECONDS = 600;   // same 10:00 countdown as pandemic.js
    static final int START_INFECTION = 60;       // percent
    static final int START_RECOVERY = 20;        // percent
    static final int WRONG_INFECTION_RISE = 10;
    static final int WRONG_RECOVERY_DROP = 10;

    /** Scenario state for the current player. Kept in the session, so a page refresh resumes the run. */
    @GetMapping("/api/pandemic/doctor/scenario")
    @ResponseBody
    public ResponseEntity<DoctorResponse> doctorScenario(HttpSession session) {
        DoctorRun run = (DoctorRun) session.getAttribute(DOCTOR_RUN_KEY);
        // First visit, a finished run, or an expired clock: start a fresh mission.
        if (run == null || run.status != DoctorStatus.IN_PROGRESS || run.timeUp(now())) {
            run = new DoctorRun(now());
            session.setAttribute(DOCTOR_RUN_KEY, run);
        }
        synchronized (run) {
            return ResponseEntity.ok().header("Cache-Control", "no-store")
                    .body(snapshot(run, null, null));
        }
    }

    /** Submit the answer for the active room. Wrong answers cost a life and raise the infection rate. */
    @PostMapping("/api/pandemic/doctor/action")
    @ResponseBody
    public ResponseEntity<DoctorResponse> doctorAction(@RequestBody DoctorAction request, HttpSession session) {
        DoctorRun run = (DoctorRun) session.getAttribute(DOCTOR_RUN_KEY);
        if (run == null) {
            run = new DoctorRun(now());
            session.setAttribute(DOCTOR_RUN_KEY, run);
            return reply(HttpStatus.CONFLICT, run, "Your session expired, so the mission was restarted. Pick a room again.");
        }

        synchronized (run) {
            if (run.status != DoctorStatus.IN_PROGRESS) {
                return reply(HttpStatus.CONFLICT, run, "This mission is already over. Reload the page to try again.");
            }
            if (run.timeUp(now())) {
                run.finish(DoctorStatus.LOST, now());
                return reply(HttpStatus.OK, run, false, "Time is up! The quarantine lockdown is now permanent.");
            }
            if (request == null || request.caseId() == null || request.answer() == null) {
                return reply(HttpStatus.BAD_REQUEST, run, "Send both caseId and answer.");
            }

            DoctorCase active = DOCTOR_CASES.get(run.stage);
            DoctorCase asked = findCase(request.caseId());
            if (asked == null) {
                return reply(HttpStatus.BAD_REQUEST, run, "Unknown room: " + request.caseId());
            }
            if (!asked.id().equals(active.id())) {
                return reply(HttpStatus.CONFLICT, run, "Clear the " + active.room() + " first.");
            }
            String key = request.answer().trim().toUpperCase(Locale.ROOT);
            if (!active.hasOption(key)) {
                return reply(HttpStatus.BAD_REQUEST, run, "Answer must be one of A, B, C or D.");
            }

            if (key.equals(active.answer())) {
                run.stage++;
                run.infection = clamp(run.infection - active.infectionDrop());
                run.recovery = clamp(run.recovery + active.recoveryGain());
                String message = active.okMessage();
                if (run.stage >= DOCTOR_CASES.size()) {
                    run.finish(DoctorStatus.WON, now());
                    message += " Every patient is stable. The emergency exit is unlocked!";
                }
                return reply(HttpStatus.OK, run, true, message);
            }

            run.lives--;
            run.infection = clamp(run.infection + WRONG_INFECTION_RISE);
            run.recovery = clamp(run.recovery - WRONG_RECOVERY_DROP);
            String message = active.badMessage();
            if (run.lives <= 0) {
                run.finish(DoctorStatus.LOST, now());
                message += " You have lost all your lives.";
            }
            return reply(HttpStatus.OK, run, false, message);
        }
    }

    /** Clock hook (overridable in tests). */
    protected long now() {
        return System.currentTimeMillis();
    }

    private static int clamp(int percent) {
        return Math.max(0, Math.min(100, percent));
    }

    private static DoctorCase findCase(String id) {
        for (DoctorCase c : DOCTOR_CASES) {
            if (c.id().equalsIgnoreCase(id.trim())) {
                return c;
            }
        }
        return null;
    }

    private ResponseEntity<DoctorResponse> reply(HttpStatus http, DoctorRun run, String message) {
        return ResponseEntity.status(http).body(snapshot(run, null, message));
    }

    private ResponseEntity<DoctorResponse> reply(HttpStatus http, DoctorRun run, boolean correct, String message) {
        return ResponseEntity.status(http).body(snapshot(run, correct, message));
    }

    private DoctorResponse snapshot(DoctorRun run, Boolean correct, String message) {
        long at = run.endedAt > 0 ? run.endedAt : now();
        List<DoctorCaseSummary> summaries = new ArrayList<>();
        for (int i = 0; i < DOCTOR_CASES.size(); i++) {
            DoctorCase c = DOCTOR_CASES.get(i);
            String state = i < run.stage ? "SOLVED"
                    : (i == run.stage && run.status == DoctorStatus.IN_PROGRESS) ? "ACTIVE" : "LOCKED";
            summaries.add(new DoctorCaseSummary(c.id(), c.room(), c.difficulty(), c.icon(), state));
        }
        DoctorCaseView active = null;
        if (run.status == DoctorStatus.IN_PROGRESS) {
            DoctorCase c = DOCTOR_CASES.get(run.stage);
            active = new DoctorCaseView(c.id(), run.stage + 1, c.room(), c.difficulty(), c.icon(),
                    c.brief(), c.prompt(), c.data(), c.options());   // no answer key
        }
        DoctorStats stats = new DoctorStats(run.lives, MAX_LIVES, run.infection, run.recovery,
                run.stage, DOCTOR_CASES.size(), run.secondsLeft(at));
        return new DoctorResponse(run.status.name(), correct, message, stats, summaries, active);
    }

    // ---- JSON shapes (public so Jackson can read them) ----

    public record DoctorAction(String caseId, String answer) { }

    public record DoctorOption(String key, String text) { }

    public record DoctorCaseSummary(String id, String room, String difficulty, String icon, String state) { }

    public record DoctorCaseView(String id, int step, String room, String difficulty, String icon,
                                 String brief, String prompt, List<String> data, List<DoctorOption> options) { }

    public record DoctorStats(int lives, int maxLives, int infectionRate, int recoveryRate,
                              int solved, int total, int secondsLeft) { }

    /** status: IN_PROGRESS | WON | LOST. correct / message are null on GET. */
    public record DoctorResponse(String status, Boolean correct, String message, DoctorStats stats,
                                 List<DoctorCaseSummary> cases, DoctorCaseView activeCase) { }

    // ---- internal state ----

    private enum DoctorStatus { IN_PROGRESS, WON, LOST }

    private static final class DoctorRun implements Serializable {
        private static final long serialVersionUID = 1L;
        final long startedAt;
        long endedAt;
        int lives = MAX_LIVES;
        int stage;
        int infection = START_INFECTION;
        int recovery = START_RECOVERY;
        DoctorStatus status = DoctorStatus.IN_PROGRESS;

        DoctorRun(long startedAt) {
            this.startedAt = startedAt;
        }

        boolean timeUp(long now) {
            return secondsLeft(now) <= 0;
        }

        int secondsLeft(long now) {
            return (int) Math.max(0, TIME_LIMIT_SECONDS - (now - startedAt) / 1000);
        }

        void finish(DoctorStatus result, long now) {
            this.status = result;
            this.endedAt = now;
        }
    }

    private record DoctorCase(String id, String room, String difficulty, String icon, String brief,
                              String prompt, List<String> data, List<DoctorOption> options, String answer,
                              String okMessage, String badMessage, int infectionDrop, int recoveryGain) {
        boolean hasOption(String key) {
            return options.stream().anyMatch(o -> o.key().equals(key));
        }
    }

    /** Easy -> Medium -> Hard. Rooms must be cleared in this order. */
    private static final List<DoctorCase> DOCTOR_CASES = List.of(
            new DoctorCase("medical", "MEDICAL LAB", "EASY", "🩺",
                    "Intake desk: a patient arrives with chills and says they feel unusually hot.",
                    "Which instrument is commonly used to measure body temperature?",
                    List.of(),
                    List.of(new DoctorOption("A", "Thermometer"), new DoctorOption("B", "Microscope"),
                            new DoctorOption("C", "Keyboard"), new DoctorOption("D", "Calculator")),
                    "A",
                    "Correct! A thermometer measures body temperature.",
                    "Wrong answer! Try again.",
                    10, 15),
            new DoctorCase("research", "RESEARCH ROOM", "MEDIUM", "📋",
                    "Lab bench 2: a patient sample shows signs of a possible infection.",
                    "What should the doctor do before making a final diagnosis?",
                    List.of(),
                    List.of(new DoctorOption("A", "Ignore the test result"),
                            new DoctorOption("B", "Immediately discharge the patient"),
                            new DoctorOption("C", "Verify the patient's test information"),
                            new DoctorOption("D", "Delete the medical record")),
                    "C",
                    "Correct! The patient's test information should be verified.",
                    "Wrong answer! Check what should be done before a final diagnosis.",
                    15, 20),
            new DoctorCase("emergency", "EMERGENCY ROOM", "HARD", "🚨",
                    "Triage board: four patients are waiting in the emergency room. Their oxygen levels are shown below.",
                    "Which patient should receive immediate medical attention first?",
                    List.of("Patient A → Oxygen level: 98%", "Patient B → Oxygen level: 94%",
                            "Patient C → Oxygen level: 82%", "Patient D → Oxygen level: 97%"),
                    List.of(new DoctorOption("A", "Patient A"), new DoctorOption("B", "Patient B"),
                            new DoctorOption("C", "Patient C"), new DoctorOption("D", "Patient D")),
                    "C",
                    "Correct! Patient C has the lowest oxygen level.",
                    "Wrong answer! Check the oxygen levels carefully.",
                    20, 25));

    // =========================

    private static Map<String, Object> check(String answer, String expected, String okMessage, String badMessage) {
        boolean correct = answer != null && answer.trim().equalsIgnoreCase(expected);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("correct", correct);
        response.put("message", correct ? okMessage : badMessage);
        return response;
    }
}
