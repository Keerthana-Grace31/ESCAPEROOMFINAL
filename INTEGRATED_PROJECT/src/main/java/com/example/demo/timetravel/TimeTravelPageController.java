package com.example.demo.timetravel;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Controller
public class TimeTravelPageController {

    @GetMapping("/timetravel")
    public String missions() {
        return "timetravel";
    }

    /** Mission 1 story game (client-side, three eras). */
    @GetMapping("/timetravel/game")
    public String story() {
        return "timegame";
    }

    /** Server-driven quiz for mission 1 (practice) or mission 2. */
    @GetMapping("/timetravel/mission/{scenario}")
    public String quiz(@PathVariable int scenario, Model model) {
        if (scenario != 1 && scenario != 2) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown mission");
        }
        model.addAttribute("scenario", scenario);
        model.addAttribute("title", scenario == 1 ? "MISSION 1: THE LOST INVENTION" : "MISSION 2: SAVE THE FUTURE");
        return "timetravel-quiz";
    }
}
