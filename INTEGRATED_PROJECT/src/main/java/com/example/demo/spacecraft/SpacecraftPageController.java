package com.example.demo.spacecraft;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpacecraftPageController {

    /** Destination selection. No puzzles live here. */
    @GetMapping("/spacecraft")
    public String selection() {
        return "spacecraft";
    }

    @GetMapping("/spacecraft/mars")
    public String mars(Model model) {
        model.addAttribute("mission", MissionType.MARS);
        return "mars";
    }

    @GetMapping("/spacecraft/lunar")
    public String lunar(Model model) {
        model.addAttribute("mission", MissionType.LUNAR);
        return "lunar";
    }
}
